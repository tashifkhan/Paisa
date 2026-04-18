import CreditCardComponent from '@/components/CreditCard';
import { useProfile } from '@/hooks/useProfile';
import { useStatsComparison } from '@/hooks/useStats';
import { useTransactions } from '@/hooks/useTransactions';
import { useWalletTotal, useWallets } from '@/hooks/useWallets';
import { useRouter } from 'expo-router';
import { useColorScheme } from 'nativewind';
import React, { useCallback, useMemo, useState } from 'react';
import { RefreshControl, ScrollView, StyleSheet, View } from 'react-native';
import { ActivityIndicator, Button, Card, IconButton, Text, useTheme } from 'react-native-paper';
import { useQueryClient } from '@tanstack/react-query';

function formatAmount(amount: number, currency: string = 'INR') {
  const symbol = currency === 'INR' ? '₹' : currency === 'USD' ? '$' : currency;
  return `${symbol}${Math.abs(amount).toLocaleString('en-IN', { maximumFractionDigits: 2 })}`;
}

function formatDate(dateStr: string) {
  const d = new Date(dateStr);
  return d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });
}

const GRADIENTS = [
  'from-[#8a79ab] to-[#6a5990]',
  'from-[#77b8a1] to-[#4a9080]',
  'from-[#e6a5b8] to-[#c87a94]',
  'from-[#f0c88d] to-[#d4964a]',
  'from-[#a0bbe3] to-[#6090c8]',
];

export default function HomeScreen() {
  const { colorScheme, toggleColorScheme } = useColorScheme();
  const isDark = colorScheme === 'dark';
  const router = useRouter();
  const theme = useTheme();
  const queryClient = useQueryClient();

  const { data: user } = useProfile();
  const { data: wallets = [], isLoading: walletsLoading } = useWallets();
  const { data: totalData, isLoading: totalLoading } = useWalletTotal();
  const { data: transactions = [], isLoading: txLoading } = useTransactions({ limit: 8 });
  const { data: comparison, isLoading: comparisonLoading } = useStatsComparison(30);
  const [refreshing, setRefreshing] = useState(false);
  const [hideBalance, setHideBalance] = useState(false);

  const loading = walletsLoading || totalLoading || txLoading || comparisonLoading;
  const totalBalance = totalData?.total_balance ?? 0;

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['profile'] }),
      queryClient.invalidateQueries({ queryKey: ['wallets'] }),
      queryClient.invalidateQueries({ queryKey: ['transactions'] }),
      queryClient.invalidateQueries({ queryKey: ['stats', 'comparison', 30] }),
    ]);
    setRefreshing(false);
  }, [queryClient]);

  const displayName = useMemo(() => user?.name?.split(' ')[0] || 'there', [user?.name]);

  return (
    <ScrollView
      style={{ flex: 1, backgroundColor: theme.colors.background }}
      contentContainerStyle={{ paddingBottom: 100 }}
      showsVerticalScrollIndicator={false}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} colors={['#8a79ab']} />}
    >
      {/* Header */}
      <View style={styles.header}>
        <View>
          <Text variant="headlineMedium" style={styles.greeting}>Hi, {displayName} 👋</Text>
          <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>
            {new Date().toLocaleDateString('en-IN', { weekday: 'long', month: 'long', day: 'numeric' })}
          </Text>
        </View>
        <View style={styles.headerActions}>
          <IconButton
            icon={isDark ? 'white-balance-sunny' : 'weather-night'}
            mode="contained-tonal"
            size={20}
            onPress={toggleColorScheme}
          />
          <IconButton
            icon="bell-outline"
            mode="outlined"
            size={20}
            onPress={() => {}}
          />
        </View>
      </View>

      {/* Balance Card */}
      <View style={styles.px}>
        <Card style={[styles.balanceCard, { backgroundColor: isDark ? '#2d2840' : '#FFF6F1' }]} elevation={0}>
          <Card.Content style={styles.balanceContent}>
            <View style={styles.balanceHeader}>
              <Text variant="titleMedium" style={{ color: isDark ? '#e0ddef' : '#3E2E28', opacity: 0.8 }}>
                Total Balance
              </Text>
              <IconButton
                icon={hideBalance ? 'eye-outline' : 'eye-off-outline'}
                size={22}
                iconColor={isDark ? '#e0ddef' : '#3E2E28'}
                onPress={() => setHideBalance(!hideBalance)}
                style={{ margin: 0 }}
              />
            </View>

            {loading ? (
              <ActivityIndicator style={{ marginVertical: 16 }} />
            ) : (
              <Text variant="displaySmall" style={[styles.balanceAmount, { color: isDark ? '#e0ddef' : '#2D1F16' }]}>
                {hideBalance ? '••••••' : formatAmount(totalBalance ?? 0)}
              </Text>
            )}

            <Text variant="titleSmall" style={{ color: isDark ? '#e0ddef' : '#3E2E28', fontWeight: '600', marginBottom: 12 }}>
              This month
            </Text>

            {comparison && (
              <View style={styles.statsRow}>
                <View style={styles.statItem}>
                  <Text variant="bodySmall" style={{ color: isDark ? '#a09aad' : '#6B5748', marginBottom: 2 }}>Income</Text>
                  <Text variant="titleMedium" style={{ fontWeight: '700', color: isDark ? '#e0ddef' : '#2D1F16' }}>
                    {formatAmount(comparison.income.current)}
                  </Text>
                  {comparison.income.change_percent !== 0 && (
                    <Text variant="labelSmall" style={{ color: comparison.income.change >= 0 ? '#16a34a' : '#dc2626' }}>
                      {comparison.income.change >= 0 ? '↑' : '↓'} {Math.abs(comparison.income.change_percent).toFixed(1)}%
                    </Text>
                  )}
                </View>
                <View style={[styles.statDivider, { backgroundColor: isDark ? '#302c40' : '#F5E6DE' }]} />
                <View style={styles.statItem}>
                  <Text variant="bodySmall" style={{ color: isDark ? '#a09aad' : '#6B5748', marginBottom: 2 }}>Expense</Text>
                  <Text variant="titleMedium" style={{ fontWeight: '700', color: isDark ? '#e0ddef' : '#2D1F16' }}>
                    {formatAmount(comparison.expense.current)}
                  </Text>
                  {comparison.expense.change_percent !== 0 && (
                    <Text variant="labelSmall" style={{ color: comparison.expense.change >= 0 ? '#dc2626' : '#16a34a' }}>
                      {comparison.expense.change >= 0 ? '↑' : '↓'} {Math.abs(comparison.expense.change_percent).toFixed(1)}%
                    </Text>
                  )}
                </View>
              </View>
            )}
          </Card.Content>
        </Card>
      </View>

      {/* My Cards */}
      <View style={styles.sectionHeader}>
        <Text variant="titleLarge" style={styles.sectionTitle}>My Cards</Text>
        <Button mode="text" compact onPress={() => router.push('/(tabs)/wallets')} labelStyle={styles.viewAllLabel}>
          View All
        </Button>
      </View>

      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.cardsScroll}>
        {loading ? (
          <ActivityIndicator style={{ marginHorizontal: 24 }} />
        ) : wallets.length === 0 ? (
          <Text style={{ marginHorizontal: 24, opacity: 0.6 }}>No wallets yet</Text>
        ) : (
          wallets.slice(0, 3).map((wallet, i) => (
            <View key={wallet.id} style={{ width: 300, marginRight: 16 }}>
              <CreditCardComponent
                type={wallet.type?.toUpperCase() || 'WALLET'}
                number={wallet.name}
                holder={user?.name || ''}
                exp="--"
                balance={hideBalance ? undefined : formatAmount(wallet.balance, wallet.currency)}
                gradient={GRADIENTS[i % GRADIENTS.length]}
              />
            </View>
          ))
        )}
        <View
          style={[styles.addCardBtn, { backgroundColor: theme.colors.surfaceVariant, borderColor: theme.colors.outline }]}
        >
          <IconButton
            icon="plus"
            size={24}
            iconColor={theme.colors.onSurfaceVariant}
            onPress={() => router.push('/(tabs)/wallets')}
          />
        </View>
      </ScrollView>

      {/* Recent Transactions */}
      <View style={styles.sectionHeader}>
        <Text variant="titleLarge" style={styles.sectionTitle}>Recent Transactions</Text>
        <Button mode="text" compact onPress={() => router.push('/all-transactions')} labelStyle={styles.viewAllLabel}>
          See All
        </Button>
      </View>

      <View style={styles.px}>
        {loading ? (
          <ActivityIndicator style={{ marginVertical: 20 }} />
        ) : transactions.length === 0 ? (
          <Card style={{ borderRadius: 24 }}>
            <Card.Content style={{ alignItems: 'center', paddingVertical: 32 }}>
              <Text variant="bodyLarge" style={{ opacity: 0.5 }}>No transactions yet</Text>
              <Button mode="contained" style={{ marginTop: 16, borderRadius: 20 }} onPress={() => router.push('/add-expense')}>
                Add First Expense
              </Button>
            </Card.Content>
          </Card>
        ) : (
          <View style={{ gap: 8 }}>
            {transactions.map((tx) => (
              <Card 
                key={tx.id} 
                style={[styles.txCard, { backgroundColor: theme.colors.surface }]} 
                elevation={0}
                onPress={() => router.push(`/transaction/${tx.id}`)}
              >
                <Card.Content style={styles.txContent}>
                  <View style={[styles.txIcon, { backgroundColor: tx.type === 'income' ? '#dcfce7' : '#fee2e2' }]}>
                    <Text style={{ fontSize: 18 }}>
                      {tx.type === 'income' ? '💰' : '💸'}
                    </Text>
                  </View>
                  <View style={{ flex: 1 }}>
                    <Text variant="titleSmall" style={{ fontWeight: '600' }}>
                      {tx.note || (tx.type === 'income' ? 'Income' : 'Expense')}
                    </Text>
                    <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>
                      {formatDate(tx.date)}
                    </Text>
                  </View>
                  <Text
                    variant="titleSmall"
                    style={{ fontWeight: '700', color: tx.type === 'income' ? '#16a34a' : '#dc2626' }}
                  >
                    {tx.type === 'income' ? '+' : '-'}{formatAmount(tx.amount, tx.currency)}
                  </Text>
                </Card.Content>
              </Card>
            ))}
          </View>
        )}
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 24, paddingTop: 52, paddingBottom: 16 },
  greeting: { fontWeight: '700' },
  headerActions: { flexDirection: 'row', gap: 4 },
  px: { paddingHorizontal: 20, marginBottom: 20 },
  balanceCard: { borderRadius: 28 },
  balanceContent: { padding: 24 },
  balanceHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 },
  balanceAmount: { fontWeight: '800', marginBottom: 16, letterSpacing: -1 },
  statsRow: { flexDirection: 'row', alignItems: 'center' },
  statItem: { flex: 1 },
  statDivider: { width: 1, height: 40, marginHorizontal: 16 },
  sectionHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 20, marginBottom: 12 },
  sectionTitle: { fontWeight: '700' },
  viewAllLabel: { fontSize: 13 },
  cardsScroll: { paddingLeft: 20, paddingRight: 20, marginBottom: 24 },
  addCardBtn: { width: 72, height: 200, borderRadius: 24, alignItems: 'center', justifyContent: 'center', borderWidth: 2, borderStyle: 'dashed' },
  txCard: { borderRadius: 20, marginBottom: 0 },
  txContent: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 12 },
  txIcon: { width: 44, height: 44, borderRadius: 22, alignItems: 'center', justifyContent: 'center' },
});
