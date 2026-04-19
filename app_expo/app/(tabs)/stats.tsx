import { useQueryClient } from '@tanstack/react-query';
import React, { useCallback, useState } from 'react';
import { RefreshControl, ScrollView, StyleSheet, View } from 'react-native';
import { ActivityIndicator, Button, Card, List, Text, useTheme } from 'react-native-paper';
import { CustomSegmentedTabs } from '../../components/CustomSegmentedTabs';
import { useStatsFull } from '@/hooks/useStats';
import type { BackendFullStats } from '../../services/types';

function formatAmount(amount: number) {
  return `₹${Math.abs(amount).toLocaleString('en-IN', { maximumFractionDigits: 0 })}`;
}

export default function StatsScreen() {
  const theme = useTheme();
  const queryClient = useQueryClient();
  const [period, setPeriod] = useState(30);
  const [tab, setTab] = useState<'overview' | 'expenses' | 'income' | 'transactions'>('overview');
  const [refreshing, setRefreshing] = useState(false);

  const { data: stats } = useStatsFull(period);
  const loading = !stats;

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await queryClient.invalidateQueries({ queryKey: ['stats', 'full', period] });
    setRefreshing(false);
  }, [period, queryClient]);

  const tabs = ['Overview', 'Expenses', 'Income', 'Transactions'];

  return (
    <ScrollView
      style={{ flex: 1, backgroundColor: theme.colors.background }}
      contentContainerStyle={{ paddingBottom: 120 }}
      showsVerticalScrollIndicator={false}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} colors={[theme.colors.primary]} />}
    >
      {/* Header */}
      <View style={styles.header}>
        <View>
          <Text variant="headlineMedium" style={styles.title}>Analysis</Text>
          <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant, marginTop: 2 }}>Detailed Breakdown</Text>
        </View>
        <Button
          mode="contained-tonal"
          buttonColor={theme.colors.surface}
          textColor={theme.colors.onSurface}
          icon="chevron-down"
          contentStyle={{ flexDirection: 'row-reverse' }}
          style={{ borderRadius: 20 }}
          labelStyle={{ fontWeight: '600', fontSize: 13 }}
          compact
        >
          {period} days
        </Button>
      </View>

      {/* Empty Chart placeholder */}
      <View style={{ paddingHorizontal: 16, marginBottom: 24 }}>
        <View style={{ 
          height: 220, 
          backgroundColor: theme.colors.surface, 
          borderRadius: 24, 
          alignItems: 'center', 
          justifyContent: 'center' 
        }}>
          <Text style={{ color: theme.colors.onSurfaceVariant }}>No expense data for this period</Text>
        </View>
      </View>

      <CustomSegmentedTabs
        tabs={[
          { value: 'overview', label: 'Overview' },
          { value: 'expenses', label: 'Expenses' },
          { value: 'income', label: 'Income' },
          { value: 'transactions', label: 'Transactions' },
        ]}
        value={tab}
        onValueChange={(v) => setTab(v as any)}
        containerStyle={{ paddingHorizontal: 16, marginBottom: 24 }}
      />

      {/* Content */}
      {loading ? (
        <ActivityIndicator style={{ marginTop: 40 }} size="large" />
      ) : tab === 'overview' ? (
        <OverviewTab stats={stats} theme={theme} />
      ) : (
        <View style={{ alignItems: 'center', marginTop: 40 }}>
          <Text style={{ color: theme.colors.onSurfaceVariant }}>Coming soon</Text>
        </View>
      )}
    </ScrollView>
  );
}

function OverviewTab({ stats, theme }: { stats: BackendFullStats; theme: any }) {
  const showSaved = stats.total_income > 0;
  const savingsRate = showSaved ? ((stats.net / stats.total_income) * 100).toFixed(0) : '0';

  return (
    <View style={{ paddingHorizontal: 16, gap: 24 }}>
      {/* 3 Stats Row */}
      <View style={{ flexDirection: 'row', gap: 12 }}>
        <Card style={[{ flex: 1, backgroundColor: theme.colors.surface }]} elevation={0}>
          <Card.Content style={{ paddingVertical: 16, alignItems: 'center' }}>
            <Text variant="labelSmall" style={{ color: theme.colors.onSurfaceVariant, marginBottom: 8, fontWeight: '700' }}>Income</Text>
            <Text variant="titleMedium" style={{ fontWeight: '800' }}>{formatAmount(stats.total_income)}</Text>
          </Card.Content>
        </Card>
        <Card style={[{ flex: 1, backgroundColor: theme.colors.surface }]} elevation={0}>
          <Card.Content style={{ paddingVertical: 16, alignItems: 'center' }}>
            <Text variant="labelSmall" style={{ color: theme.colors.onSurfaceVariant, marginBottom: 8, fontWeight: '700' }}>Expense</Text>
            <Text variant="titleMedium" style={{ fontWeight: '800' }}>{formatAmount(stats.total_expense)}</Text>
          </Card.Content>
        </Card>
        <Card style={[{ flex: 1, backgroundColor: theme.colors.surface }]} elevation={0}>
          <Card.Content style={{ paddingVertical: 16, alignItems: 'center' }}>
            <Text variant="labelSmall" style={{ color: theme.colors.onSurfaceVariant, marginBottom: 8, fontWeight: '700' }}>Net</Text>
            <Text variant="titleMedium" style={{ fontWeight: '800' }}>{formatAmount(stats.net)}</Text>
          </Card.Content>
        </Card>
      </View>

      {/* Debts Overview */}
      <View style={{ gap: 16 }}>
        <Text variant="titleMedium" style={{ fontWeight: '700' }}>Debts Overview</Text>
        
        <Card style={{ backgroundColor: theme.colors.surface, borderRadius: 24 }} elevation={0}>
          <Card.Content style={{ flexDirection: 'row', alignItems: 'center', paddingVertical: 24 }}>
            <View style={[styles.circlePlaceholder, { backgroundColor: theme.colors.surfaceVariant }]} />
            <View style={{ flexDirection: 'row', flex: 1, justifyContent: 'flex-start', gap: 32 }}>
              <View>
                <Text variant="labelSmall" style={styles.debtLabel}>OWED TO YOU</Text>
                <Text variant="titleLarge" style={{ fontWeight: '800', color: '#4ade80' }}>₹0</Text>
              </View>
              <View>
                <Text variant="labelSmall" style={styles.debtLabel}>YOU OWE</Text>
                <Text variant="titleLarge" style={{ fontWeight: '800', color: theme.colors.onSurface }}>₹0</Text>
              </View>
            </View>
          </Card.Content>
        </Card>

        {/* Savings Compare */}
        <Card style={{ backgroundColor: theme.colors.surface, borderRadius: 24 }} elevation={0}>
          <Card.Content style={{ flexDirection: 'row', alignItems: 'center', paddingVertical: 20 }}>
            <List.Icon icon="arrow-top-right" color={theme.colors.onSurfaceVariant} style={{ margin: 0, marginRight: 16 }} />
            <Text style={{ color: theme.colors.onSurfaceVariant, flex: 1, lineHeight: 20, fontSize: 13 }}>
              You saved <Text style={{ fontWeight: '800', color: theme.colors.onSurface }}>{savingsRate}%</Text> compared to last period
            </Text>
          </Card.Content>
        </Card>
      </View>

      <View style={{ gap: 16, marginTop: 8 }}>
        <Text variant="labelSmall" style={styles.sectionHeader}>UPCOMING DUES</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  header: {
    paddingHorizontal: 16, paddingTop: 52, paddingBottom: 24,
    flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center',
  },
  title: { fontWeight: '700' },
  circlePlaceholder: { width: 64, height: 64, borderRadius: 32, marginRight: 24 },
  debtLabel: { fontWeight: '800', letterSpacing: 1, color: '#a09aad', marginBottom: 8, fontSize: 10 },
  sectionHeader: { fontWeight: '800', letterSpacing: 1, color: '#a09aad', fontSize: 11 },
});
