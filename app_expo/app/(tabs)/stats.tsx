import { useQueryClient } from '@tanstack/react-query';
import React, { useCallback, useMemo, useState } from 'react';
import {
  Dimensions,
  LayoutAnimation,
  Platform,
  Pressable,
  RefreshControl,
  ScrollView,
  StyleSheet,
  UIManager,
  View,
} from 'react-native';
import { ActivityIndicator, Text, useTheme } from 'react-native-paper';
import Svg, {
  Circle,
  Defs,
  G,
  Line as SvgLine,
  LinearGradient,
  Path,
  Rect,
  Stop,
  Text as SvgText,
} from 'react-native-svg';
import { useStatsFull, useStatsCategory, useStatsTrends } from '@/hooks/useStats';
import type { BackendCategoryStats, BackendTrend } from '../../services/types';

if (Platform.OS === 'android' && UIManager.setLayoutAnimationEnabledExperimental) {
  UIManager.setLayoutAnimationEnabledExperimental(true);
}

const { width: SCREEN_W } = Dimensions.get('window');
// Outer padding: 16×2 = 32. Chart card padding: 16×2 = 32. Total: 64.
const CHART_W = SCREEN_W - 64;

// ─── helpers ────────────────────────────────────────────────────────────────

function fmt(n: number) {
  if (n >= 100000) return `₹${(n / 100000).toFixed(1)}L`;
  if (n >= 1000) return `₹${(n / 1000).toFixed(1)}K`;
  return `₹${Math.abs(n).toFixed(0)}`;
}

function fmtFull(n: number) {
  return `₹${Math.abs(n).toLocaleString('en-IN', { maximumFractionDigits: 2 })}`;
}

const CAT_COLORS = [
  '#5A5A7A',
  '#4A90D9',
  '#FF9500',
  '#5B4CF5',
  '#E74C3C',
  '#2ECC71',
  '#F39C12',
  '#9B59B6',
  '#1ABC9C',
  '#E67E22',
];

type Period = { label: string; days: number };
type TxType = 'all' | 'income' | 'expense';
type TrendView = 'line' | 'bar' | 'heatmap';
type CatView = 'list' | 'donut';

const PERIODS: Period[] = [
  { label: 'This Month', days: 30 },
  { label: '3 Months', days: 90 },
  { label: 'Current FY', days: 365 },
  { label: 'All Time', days: 3650 },
];

// ─── SVG charts ─────────────────────────────────────────────────────────────

function polarToCartesian(cx: number, cy: number, r: number, angleDeg: number) {
  const a = ((angleDeg - 90) * Math.PI) / 180;
  return { x: cx + r * Math.cos(a), y: cy + r * Math.sin(a) };
}

function arcPath(cx: number, cy: number, ro: number, ri: number, start: number, end: number) {
  const s = polarToCartesian(cx, cy, ro, start);
  const e = polarToCartesian(cx, cy, ro, end);
  const si = polarToCartesian(cx, cy, ri, start);
  const ei = polarToCartesian(cx, cy, ri, end);
  const large = end - start > 180 ? 1 : 0;
  return [
    `M ${s.x.toFixed(2)} ${s.y.toFixed(2)}`,
    `A ${ro} ${ro} 0 ${large} 1 ${e.x.toFixed(2)} ${e.y.toFixed(2)}`,
    `L ${ei.x.toFixed(2)} ${ei.y.toFixed(2)}`,
    `A ${ri} ${ri} 0 ${large} 0 ${si.x.toFixed(2)} ${si.y.toFixed(2)}`,
    'Z',
  ].join(' ');
}

function DonutChart({ categories }: { categories: BackendCategoryStats[] }) {
  const size = 180;
  const cx = size / 2;
  const cy = size / 2;
  const ro = 78;
  const ri = 50;
  const gap = 2;

  const total = categories.reduce((s, c) => s + c.total, 0);
  let cursor = 0;
  const slices = categories.slice(0, 8).map((cat, i) => {
    const pct = total > 0 ? (cat.total / total) * 100 : 0;
    const sweep = (pct / 100) * 360;
    const s = cursor + gap / 2;
    const e = cursor + sweep - gap / 2;
    cursor += sweep;
    return { cat, s, e, color: CAT_COLORS[i % CAT_COLORS.length] };
  });

  return (
    <Svg width={size} height={size}>
      {slices.map((sl, i) =>
        sl.e - sl.s > 1 ? (
          <Path key={i} d={arcPath(cx, cy, ro, ri, sl.s, sl.e)} fill={sl.color} />
        ) : null
      )}
      {categories.length === 0 && (
        <Circle cx={cx} cy={cy} r={ro} fill="none" stroke="#302c40" strokeWidth={ro - ri} />
      )}
    </Svg>
  );
}

function LineChart({ trends, field }: { trends: BackendTrend[]; field: 'expense' | 'income' | 'net' }) {
  const padL = 32;
  const padB = 24;
  const padT = 12;
  const padR = 8;
  const h = 170;
  const chartW = CHART_W - padL - padR;
  const chartH = h - padT - padB;

  const values = trends.map(t => Math.max(0, t[field]));
  const max = Math.max(...values, 1);

  const getX = (i: number) =>
    trends.length <= 1 ? padL + chartW / 2 : padL + (i / (trends.length - 1)) * chartW;
  // 15% headroom so spikes don't hug the top edge
  const displayMax = max * 1.15;
  const getY = (v: number) => padT + (1 - v / displayMax) * chartH;

  const linePath = trends
    .map((t, i) => `${i === 0 ? 'M' : 'L'} ${getX(i).toFixed(1)},${getY(values[i]).toFixed(1)}`)
    .join(' ');

  const areaPath =
    linePath +
    ` L ${getX(trends.length - 1).toFixed(1)},${(padT + chartH).toFixed(1)}` +
    ` L ${getX(0).toFixed(1)},${(padT + chartH).toFixed(1)} Z`;

  const yTicks = [0, 0.5, 1].map(p => ({ value: displayMax * p, y: getY(displayMax * p) }));

  const xLabels = trends.reduce<{ t: BackendTrend; i: number }[]>((acc, t, i) => {
    if (trends.length <= 6 || i % Math.ceil(trends.length / 6) === 0) acc.push({ t, i });
    return acc;
  }, []);

  const lineColor = field === 'income' ? '#4CAF50' : field === 'net' ? '#a995c9' : '#E74C3C';

  return (
    <Svg width={CHART_W} height={h}>
      <Defs>
        <LinearGradient id="areaGrad" x1="0" y1="0" x2="0" y2="1">
          <Stop offset="0" stopColor={lineColor} stopOpacity="0.25" />
          <Stop offset="1" stopColor={lineColor} stopOpacity="0" />
        </LinearGradient>
      </Defs>

      {yTicks.map((tick, i) => (
        <G key={i}>
          <SvgLine
            x1={padL} y1={tick.y} x2={CHART_W - padR} y2={tick.y}
            stroke="#302c40" strokeWidth={1}
          />
          <SvgText x={padL - 4} y={tick.y + 4} fontSize={9} fill="#a09aad" textAnchor="end">
            {fmt(tick.value)}
          </SvgText>
        </G>
      ))}

      <Path d={areaPath} fill="url(#areaGrad)" />
      <Path d={linePath} fill="none" stroke={lineColor} strokeWidth={2} strokeLinejoin="round" />

      {trends.map((t, i) => (
        <Circle key={i} cx={getX(i)} cy={getY(values[i])} r={3.5} fill="#e0ddef" />
      ))}

      {xLabels.map(({ t, i }) => (
        <SvgText key={i} x={getX(i)} y={h - 4} fontSize={9} fill="#a09aad" textAnchor="middle">
          {t.month_name.slice(0, 3)} {t.month.slice(2, 4)}
        </SvgText>
      ))}
    </Svg>
  );
}

function BarChart({ trends, field }: { trends: BackendTrend[]; field: 'expense' | 'income' | 'net' }) {
  const padL = 32;
  const padB = 24;
  const padT = 12;
  const padR = 8;
  const h = 170;
  const chartW = CHART_W - padL - padR;
  const chartH = h - padT - padB;

  const values = trends.map(t => Math.max(0, t[field]));
  const max = Math.max(...values, 1);

  const n = trends.length;
  const barW = Math.max(4, (chartW / n) * 0.55);
  const step = chartW / n;

  const getX = (i: number) => padL + i * step + step / 2 - barW / 2;
  const getH = (v: number) => (v / max) * chartH;
  const getY = (v: number) => padT + chartH - getH(v);

  const yTicks = [0, 0.5, 1].map(p => ({ value: max * p, y: padT + chartH - (p * chartH) }));
  const xLabels = trends.reduce<{ t: BackendTrend; i: number }[]>((acc, t, i) => {
    if (n <= 6 || i % Math.ceil(n / 6) === 0) acc.push({ t, i });
    return acc;
  }, []);

  const barColor = field === 'income' ? '#4CAF50' : field === 'net' ? '#a995c9' : '#a995c9';

  return (
    <Svg width={CHART_W} height={h}>
      {yTicks.map((tick, i) => (
        <G key={i}>
          <SvgLine x1={padL} y1={tick.y} x2={CHART_W - padR} y2={tick.y} stroke="#302c40" strokeWidth={1} />
          <SvgText x={padL - 4} y={tick.y + 4} fontSize={9} fill="#a09aad" textAnchor="end">
            {fmt(tick.value)}
          </SvgText>
        </G>
      ))}

      {trends.map((t, i) => {
        const bh = getH(values[i]);
        if (bh < 1) return null;
        return (
          <Rect
            key={i}
            x={getX(i)}
            y={getY(values[i])}
            width={barW}
            height={bh}
            rx={barW / 3}
            fill={barColor}
            opacity={i === trends.length - 1 ? 1 : 0.65}
          />
        );
      })}

      {xLabels.map(({ t, i }) => (
        <SvgText key={i} x={getX(i) + barW / 2} y={h - 4} fontSize={9} fill="#a09aad" textAnchor="middle">
          {t.month_name.slice(0, 3)} {t.month.slice(2, 4)}
        </SvgText>
      ))}

      {/* Value labels on taller bars */}
      {trends.map((t, i) => {
        const bh = getH(values[i]);
        if (bh < 20) return null;
        return (
          <SvgText key={i} x={getX(i) + barW / 2} y={getY(values[i]) - 4} fontSize={8} fill="#e0ddef" textAnchor="middle">
            {fmt(values[i])}
          </SvgText>
        );
      })}
    </Svg>
  );
}

function HeatmapChart({ trends }: { trends: BackendTrend[] }) {
  const days = ['M', 'T', 'W', 'T', 'F', 'S', 'S'];
  const cellSize = 13;
  const gap = 2;
  const padL = 20;
  const padT = 8;

  // Distribute monthly expense data across weeks for visual representation
  const numWeeks = Math.max(12, trends.length * 4);
  const maxVal = Math.max(...trends.map(t => t.expense), 1);

  const getCellOpacity = (weekIdx: number, dayIdx: number) => {
    const monthIdx = Math.floor((weekIdx / numWeeks) * trends.length);
    const trend = trends[Math.min(monthIdx, trends.length - 1)];
    if (!trend) return 0.05;
    const base = trend.expense / maxVal;
    const noise = Math.sin(weekIdx * 7 + dayIdx * 13) * 0.15;
    return Math.max(0.04, Math.min(0.95, base + noise));
  };

  const totalW = padL + numWeeks * (cellSize + gap);
  const totalH = padT + 7 * (cellSize + gap) + 20;

  // Month labels
  const monthLabels: { label: string; x: number }[] = [];
  trends.forEach((t, i) => {
    const weekIdx = Math.floor((i / trends.length) * numWeeks);
    monthLabels.push({ label: t.month_name.slice(0, 3), x: padL + weekIdx * (cellSize + gap) });
  });

  return (
    <ScrollView horizontal showsHorizontalScrollIndicator={false}>
      <Svg width={Math.max(totalW, CHART_W)} height={totalH}>
        {days.map((d, row) => (
          <SvgText key={row} x={0} y={padT + row * (cellSize + gap) + cellSize - 2} fontSize={9} fill="#a09aad">
            {d}
          </SvgText>
        ))}

        {Array.from({ length: numWeeks }).map((_, col) =>
          Array.from({ length: 7 }).map((__, row) => {
            const opacity = getCellOpacity(col, row);
            return (
              <Rect
                key={`${col}-${row}`}
                x={padL + col * (cellSize + gap)}
                y={padT + row * (cellSize + gap)}
                width={cellSize}
                height={cellSize}
                rx={3}
                fill={`rgba(169,149,201,${opacity})`}
              />
            );
          })
        )}

        {monthLabels.map((ml, i) => (
          <SvgText key={i} x={ml.x} y={totalH - 2} fontSize={9} fill="#a09aad">
            {ml.label}
          </SvgText>
        ))}
      </Svg>
    </ScrollView>
  );
}

// ─── sub-components ──────────────────────────────────────────────────────────

function Chip({
  label,
  selected,
  onPress,
  icon,
  color,
}: {
  label: string;
  selected: boolean;
  onPress: () => void;
  icon?: string;
  color?: string;
}) {
  const theme = useTheme();
  return (
    <Pressable
      onPress={onPress}
      style={[
        styles.chip,
        {
          backgroundColor: selected
            ? color
              ? `${color}22`
              : theme.colors.primaryContainer
            : theme.colors.surface,
          borderColor: selected
            ? color ?? theme.colors.primary
            : theme.colors.outline,
        },
      ]}
    >
      {icon ? <Text style={{ fontSize: 13, marginRight: 4 }}>{icon}</Text> : null}
      <Text
        style={{
          fontSize: 12,
          fontWeight: selected ? '700' : '500',
          color: selected
            ? color ?? theme.colors.primary
            : theme.colors.onSurfaceVariant,
        }}
      >
        {label}
      </Text>
    </Pressable>
  );
}

function SummaryCard({
  total,
  categories,
  txType,
  days,
  theme,
}: {
  total: number;
  categories: BackendCategoryStats[];
  txType: TxType;
  days: number;
  theme: any;
}) {
  const txnCount = categories.reduce((s, c) => s + c.count, 0);
  const avg = days > 0 ? total / days : 0;
  const topCat = categories[0];

  return (
    <View style={[styles.summaryCard, { backgroundColor: theme.colors.surface }]}>
      <View style={styles.summaryTop}>
        <View style={{ flex: 1 }}>
          <Text style={[styles.summaryLabel, { color: theme.colors.onSurfaceVariant }]}>TOTAL {txType === 'all' ? '(IN+OUT)' : ''}</Text>
          <Text style={[styles.summaryAmount, { color: theme.colors.onSurface }]}>{fmtFull(total)}</Text>
        </View>
        <View style={[styles.txnBadge, { backgroundColor: theme.colors.surfaceVariant }]}>
          <Text style={{ fontSize: 11, color: theme.colors.onSurfaceVariant, marginRight: 4 }}>≡</Text>
          <Text style={{ fontSize: 12, fontWeight: '700', color: theme.colors.onSurface }}>
            {txnCount} TXNS
          </Text>
        </View>
      </View>

      <View style={[styles.summaryDivider, { backgroundColor: theme.colors.outline }]} />

      <View style={styles.summaryBottom}>
        <View>
          <Text style={[styles.summaryLabel, { color: theme.colors.onSurfaceVariant }]}>AVERAGE</Text>
          <View style={{ flexDirection: 'row', alignItems: 'baseline', gap: 2 }}>
            <Text style={[styles.summaryAvg, { color: theme.colors.onSurface }]}>{fmtFull(avg)}</Text>
            <Text style={{ fontSize: 12, color: theme.colors.onSurfaceVariant }}>/day</Text>
          </View>
        </View>
        {topCat && (
          <View style={{ alignItems: 'flex-end' }}>
            <Text style={{ fontSize: 11, color: theme.colors.onSurfaceVariant, marginBottom: 4 }}>
              {topCat.percentage.toFixed(0)}% of total
            </Text>
            <View style={[styles.catBadge, { backgroundColor: theme.colors.surfaceVariant }]}>
              <Text style={{ fontSize: 11, color: theme.colors.onSurface, fontWeight: '600' }}>
                {topCat.category_name}
              </Text>
            </View>
          </View>
        )}
      </View>
    </View>
  );
}

function SectionHeader({
  title,
  right,
  theme,
}: {
  title: string;
  right?: React.ReactNode;
  theme: any;
}) {
  return (
    <View style={styles.sectionHeader}>
      <Text style={[styles.sectionTitle, { color: theme.colors.onSurface }]}>{title}</Text>
      {right}
    </View>
  );
}

function ToggleButton({
  options,
  value,
  onChange,
  theme,
}: {
  options: { value: string; label: string }[];
  value: string;
  onChange: (v: string) => void;
  theme: any;
}) {
  return (
    <View style={[styles.toggleWrap, { backgroundColor: theme.colors.surfaceVariant, borderColor: theme.colors.outline }]}>
      {options.map(opt => {
        const sel = opt.value === value;
        return (
          <Pressable
            key={opt.value}
            onPress={() => onChange(opt.value)}
            style={[
              styles.toggleBtn,
              sel && { backgroundColor: theme.colors.surface },
            ]}
          >
            <Text style={{ fontSize: 12, fontWeight: sel ? '700' : '500', color: sel ? theme.colors.onSurface : theme.colors.onSurfaceVariant }}>
              {opt.label}
            </Text>
          </Pressable>
        );
      })}
    </View>
  );
}

function CategoryList({ categories, theme }: { categories: BackendCategoryStats[]; theme: any }) {
  const [showAll, setShowAll] = useState(false);
  const visible = showAll ? categories : categories.slice(0, 5);

  return (
    <View style={[styles.catCard, { backgroundColor: theme.colors.surface }]}>
      <Text style={[styles.catCardTitle, { color: theme.colors.onSurface }]}>Spending by Category</Text>
      {visible.map((cat, i) => (
        <View key={cat.category_id ?? i} style={styles.catRow}>
          <View style={{ flex: 1 }}>
            <View style={styles.catRowTop}>
              <Text style={[styles.catName, { color: theme.colors.onSurface }]}>{cat.category_name}</Text>
              <View style={{ flexDirection: 'row', gap: 8, alignItems: 'center' }}>
                <Text style={{ fontSize: 12, color: theme.colors.onSurfaceVariant }}>
                  {cat.percentage.toFixed(0)}%
                </Text>
                <Text style={{ fontSize: 13, fontWeight: '700', color: theme.colors.onSurface }}>
                  {fmtFull(cat.total)}
                </Text>
              </View>
            </View>
            <View style={[styles.progressTrack, { backgroundColor: theme.colors.outlineVariant }]}>
              <View
                style={[
                  styles.progressFill,
                  {
                    width: `${Math.min(cat.percentage, 100)}%`,
                    backgroundColor: CAT_COLORS[i % CAT_COLORS.length],
                  },
                ]}
              />
            </View>
          </View>
        </View>
      ))}

      {categories.length > 5 && (
        <Pressable onPress={() => setShowAll(!showAll)} style={styles.viewMoreBtn}>
          <Text style={{ fontSize: 13, color: theme.colors.onSurfaceVariant, fontWeight: '600' }}>
            {showAll ? 'Show Less' : `View All (${categories.length - 5} more)`}
          </Text>
        </Pressable>
      )}
    </View>
  );
}

function DonutSection({ categories, theme }: { categories: BackendCategoryStats[]; theme: any }) {
  const top = categories.slice(0, 6);
  return (
    <View style={{ flexDirection: 'row', alignItems: 'center', gap: 16 }}>
      <DonutChart categories={categories} />
      <View style={{ flex: 1, gap: 8 }}>
        {top.map((cat, i) => (
          <View key={cat.category_id ?? i} style={{ flexDirection: 'row', alignItems: 'center', gap: 8 }}>
            <View style={{ width: 10, height: 10, borderRadius: 5, backgroundColor: CAT_COLORS[i % CAT_COLORS.length] }} />
            <View style={{ flex: 1 }}>
              <Text style={{ fontSize: 12, fontWeight: '600', color: theme.colors.onSurface }} numberOfLines={1}>
                {cat.category_name}
              </Text>
              <Text style={{ fontSize: 10, color: theme.colors.onSurfaceVariant }}>
                {fmt(cat.total)} ({cat.percentage.toFixed(0)}%)
              </Text>
            </View>
          </View>
        ))}
      </View>
    </View>
  );
}

// ─── main screen ─────────────────────────────────────────────────────────────

export default function StatsScreen() {
  const theme = useTheme();
  const queryClient = useQueryClient();

  const [period, setPeriod] = useState<Period>(PERIODS[0]);
  const [txType, setTxType] = useState<TxType>('expense');
  const [trendView, setTrendView] = useState<TrendView>('bar');
  const [catView, setCatView] = useState<CatView>('list');
  const [showFilters, setShowFilters] = useState(false);
  const [refreshing, setRefreshing] = useState(false);

  const { data: stats } = useStatsFull(period.days);
  const { data: expenseCatData } = useStatsCategory(period.days, 'expense');
  const { data: incomeCatData } = useStatsCategory(period.days, 'income');
  const { data: trendsData } = useStatsTrends(24);

  const loading = !stats;
  const trends: BackendTrend[] = trendsData?.trends ?? [];
  const categories: BackendCategoryStats[] = useMemo(() => {
    const expenseCategories = expenseCatData?.categories ?? [];
    const incomeCategories = incomeCatData?.categories ?? [];

    if (txType === 'expense') return expenseCategories;
    if (txType === 'income') return incomeCategories;

    const merged = new Map<string, BackendCategoryStats>();

    for (const cat of [...expenseCategories, ...incomeCategories]) {
      const key = cat.category_id ?? `name:${cat.category_name}`;
      const existing = merged.get(key);
      if (existing) {
        existing.total += cat.total;
        existing.count += cat.count;
      } else {
        merged.set(key, {
          category_id: cat.category_id,
          category_name: cat.category_name,
          total: cat.total,
          percentage: 0,
          count: cat.count,
        });
      }
    }

    const result = Array.from(merged.values()).sort((a, b) => b.total - a.total);
    const grandTotal = result.reduce((sum, cat) => sum + cat.total, 0);

    return result.map((cat) => ({
      ...cat,
      percentage: grandTotal > 0 ? Number(((cat.total / grandTotal) * 100).toFixed(2)) : 0,
    }));
  }, [expenseCatData?.categories, incomeCatData?.categories, txType]);

  const summaryTotal = useMemo(() => {
    if (!stats) return 0;
    if (txType === 'income') return stats.total_income;
    if (txType === 'expense') return stats.total_expense;
    return stats.total_income + stats.total_expense;
  }, [stats, txType]);

  const trendField: 'expense' | 'income' | 'net' =
    txType === 'income' ? 'income' : txType === 'expense' ? 'expense' : 'net';

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await queryClient.invalidateQueries({ queryKey: ['stats'] });
    setRefreshing(false);
  }, [queryClient]);

  const toggleFilters = useCallback(() => {
    LayoutAnimation.configureNext(LayoutAnimation.Presets.easeInEaseOut);
    setShowFilters(v => !v);
  }, []);

  return (
    <ScrollView
      style={{ flex: 1, backgroundColor: theme.colors.background }}
      contentContainerStyle={{ paddingBottom: 120 }}
      showsVerticalScrollIndicator={false}
      refreshControl={
        <RefreshControl
          refreshing={refreshing}
          onRefresh={onRefresh}
          colors={[theme.colors.primary]}
          tintColor={theme.colors.primary}
        />
      }
    >
      {/* ── Header ── */}
      <View style={styles.header}>
        <Text style={[styles.headerTitle, { color: theme.colors.onSurface }]}>Analytics</Text>
      </View>

      {/* ── Period chips ── */}
      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        contentContainerStyle={styles.chipRow}
      >
        {PERIODS.map(p => (
          <Chip
            key={p.label}
            label={p.label}
            selected={period.label === p.label}
            onPress={() => setPeriod(p)}
          />
        ))}
      </ScrollView>

      {/* ── More Filters toggle ── */}
      <Pressable onPress={toggleFilters} style={styles.moreFiltersBtn}>
        <Text style={{ fontSize: 13, color: theme.colors.onSurfaceVariant, fontWeight: '600', marginRight: 6 }}>
          ≡ More Filters
        </Text>
        <Text style={{ color: theme.colors.onSurfaceVariant, fontSize: 14 }}>
          {showFilters ? '∧' : '∨'}
        </Text>
      </Pressable>

      {showFilters && (
        <View style={{ paddingHorizontal: 16, gap: 8, marginBottom: 16 }}>
          <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: 8, paddingBottom: 4 }}>
            {(['all', 'income', 'expense'] as TxType[]).map(t => (
              <Chip
                key={t}
                label={t === 'all' ? 'All' : t === 'income' ? 'Income' : 'Expense'}
                icon={t === 'income' ? '↑' : t === 'expense' ? '↓' : undefined}
                selected={txType === t}
                onPress={() => setTxType(t)}
                color={t === 'income' ? '#4CAF50' : t === 'expense' ? '#E74C3C' : undefined}
              />
            ))}
          </ScrollView>
        </View>
      )}

      <View style={{ paddingHorizontal: 16, gap: 24 }}>
        {/* ── Summary card ── */}
        {loading ? (
          <ActivityIndicator size="large" color={theme.colors.primary} style={{ marginTop: 40 }} />
        ) : (
          <>
            <SummaryCard total={summaryTotal} categories={categories} txType={txType} days={period.days} theme={theme} />

            {/* ── Trends ── */}
            <View>
              <SectionHeader
                title="Trends"
                theme={theme}
                right={
                  <ToggleButton
                    options={[
                      { value: 'line', label: 'Line' },
                      { value: 'bar', label: 'Bar' },
                      { value: 'heatmap', label: 'Heatmap' },
                    ]}
                    value={trendView}
                    onChange={v => setTrendView(v as TrendView)}
                    theme={theme}
                  />
                }
              />

              <View style={[styles.chartCard, { backgroundColor: theme.colors.surface }]}>
                {trends.length === 0 ? (
                  <View style={styles.emptyChart}>
                    <Text style={{ color: theme.colors.onSurfaceVariant }}>No trend data</Text>
                  </View>
                ) : trendView === 'line' ? (
                  <>
                    <View style={{ flexDirection: 'row', alignItems: 'center', gap: 8, marginBottom: 12 }}>
                      <View style={{ width: 10, height: 10, borderRadius: 5, backgroundColor: '#e0ddef' }} />
                      <Text style={{ fontSize: 12, color: theme.colors.onSurfaceVariant }}>Balance Trend</Text>
                    </View>
                    <LineChart trends={trends} field={trendField} />
                  </>
                ) : trendView === 'bar' ? (
                  <BarChart trends={trends} field={trendField} />
                ) : (
                  <HeatmapChart trends={trends} />
                )}
              </View>
            </View>

            {/* ── Top Categories ── */}
            {categories.length > 0 && (
              <View>
                <SectionHeader
                  title="Top Categories"
                  theme={theme}
                  right={
                    <ToggleButton
                      options={[
                        { value: 'list', label: 'List' },
                        { value: 'donut', label: 'Pie' },
                      ]}
                      value={catView}
                      onChange={v => setCatView(v as CatView)}
                      theme={theme}
                    />
                  }
                />

                {catView === 'list' ? (
                  <CategoryList categories={categories} theme={theme} />
                ) : (
                  <View style={[styles.chartCard, { backgroundColor: theme.colors.surface }]}>
                    <DonutSection categories={categories} theme={theme} />
                  </View>
                )}
              </View>
            )}

            {/* ── Overview stats row ── */}
            <View>
              <Text style={[styles.sectionTitle, { color: theme.colors.onSurface, marginBottom: 12 }]}>
                Period Summary
              </Text>
              <View style={{ flexDirection: 'row', gap: 10 }}>
                {[
                  { label: 'Income', value: stats.total_income, color: '#4CAF50' },
                  { label: 'Expense', value: stats.total_expense, color: '#E74C3C' },
                  { label: 'Net', value: stats.net, color: '#a995c9' },
                ].map(item => (
                  <View
                    key={item.label}
                    style={[styles.statPill, { backgroundColor: theme.colors.surface }]}
                  >
                    <View style={[styles.statPillBar, { backgroundColor: item.color }]} />
                    <Text style={{ fontSize: 11, color: theme.colors.onSurfaceVariant, marginBottom: 4 }}>
                      {item.label}
                    </Text>
                    <Text style={{ fontSize: 13, fontWeight: '800', color: item.color }}>
                      {fmt(item.value)}
                    </Text>
                  </View>
                ))}
              </View>
            </View>

            {/* ── Category detail with count ── */}
            {categories.length > 0 && (
              <View style={{ gap: 12 }}>
                <Text style={[styles.sectionTitle, { color: theme.colors.onSurface }]}>
                  Top Merchants
                </Text>
                {categories.slice(0, 4).map((cat, i) => (
                  <View
                    key={cat.category_id ?? i}
                    style={[styles.merchantRow, { backgroundColor: theme.colors.surface }]}
                  >
                    <View style={[styles.merchantIcon, { backgroundColor: `${CAT_COLORS[i % CAT_COLORS.length]}22` }]}>
                      <Text style={{ fontSize: 18 }}>
                        {['🛍️', '🏦', '🍔', '📱', '🎬', '🛒', '✈️', '💊'][i] ?? '💳'}
                      </Text>
                    </View>
                    <View style={{ flex: 1 }}>
                      <Text style={{ fontSize: 14, fontWeight: '600', color: theme.colors.onSurface }}>
                        {cat.category_name}
                      </Text>
                      <Text style={{ fontSize: 12, color: theme.colors.onSurfaceVariant }}>
                        {cat.count} transaction{cat.count !== 1 ? 's' : ''}
                      </Text>
                    </View>
                    <Text style={{ fontSize: 15, fontWeight: '700', color: theme.colors.onSurface }}>
                      {fmtFull(cat.total)}
                    </Text>
                  </View>
                ))}
              </View>
            )}
          </>
        )}
      </View>
    </ScrollView>
  );
}

// ─── styles ──────────────────────────────────────────────────────────────────

const styles = StyleSheet.create({
  header: {
    paddingHorizontal: 16,
    paddingTop: 52,
    paddingBottom: 8,
  },
  headerTitle: {
    fontSize: 28,
    fontWeight: '800',
    letterSpacing: -0.5,
  },
  chipRow: {
    paddingHorizontal: 16,
    paddingVertical: 12,
    gap: 8,
  },
  chip: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 20,
    borderWidth: 1,
  },
  moreFiltersBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingBottom: 12,
  },
  summaryCard: {
    borderRadius: 20,
    padding: 20,
  },
  summaryTop: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
    marginBottom: 16,
  },
  summaryLabel: {
    fontSize: 11,
    fontWeight: '700',
    letterSpacing: 1,
    marginBottom: 6,
  },
  summaryAmount: {
    fontSize: 30,
    fontWeight: '800',
    letterSpacing: -1,
  },
  txnBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 12,
  },
  summaryDivider: {
    height: 1,
    marginBottom: 16,
    opacity: 0.5,
  },
  summaryBottom: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-end',
  },
  summaryAvg: {
    fontSize: 22,
    fontWeight: '800',
    letterSpacing: -0.5,
  },
  catBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 10,
    paddingVertical: 5,
    borderRadius: 12,
    gap: 4,
  },
  sectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '700',
  },
  toggleWrap: {
    flexDirection: 'row',
    borderRadius: 20,
    padding: 3,
    gap: 2,
    borderWidth: 1,
  },
  toggleBtn: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 16,
  },
  chartCard: {
    borderRadius: 20,
    padding: 16,
  },
  emptyChart: {
    height: 140,
    alignItems: 'center',
    justifyContent: 'center',
  },
  catCard: {
    borderRadius: 20,
    padding: 20,
    gap: 14,
  },
  catCardTitle: {
    fontSize: 15,
    fontWeight: '700',
    marginBottom: 4,
  },
  catRow: {
    gap: 6,
  },
  catRowTop: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 5,
  },
  catName: {
    fontSize: 14,
    fontWeight: '500',
  },
  progressTrack: {
    height: 4,
    borderRadius: 2,
    overflow: 'hidden',
  },
  progressFill: {
    height: 4,
    borderRadius: 2,
  },
  viewMoreBtn: {
    alignItems: 'center',
    paddingTop: 8,
  },
  statPill: {
    flex: 1,
    borderRadius: 16,
    padding: 14,
    overflow: 'hidden',
  },
  statPillBar: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    height: 3,
    borderTopLeftRadius: 16,
    borderTopRightRadius: 16,
  },
  merchantRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    padding: 14,
    borderRadius: 16,
  },
  merchantIcon: {
    width: 48,
    height: 48,
    borderRadius: 24,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
