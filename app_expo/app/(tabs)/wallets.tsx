import CreditCardComponent from '@/components/CreditCard';
import { useDeleteWallet, useWalletTotal, useWallets } from '@/hooks/useWallets';
import { useRouter } from 'expo-router';
import { Banknote, CreditCard as CardIcon, Cpu, Wifi } from 'lucide-react-native';
import React, { useCallback, useState, useRef } from 'react';
import { RefreshControl, ScrollView, StyleSheet, View, Dimensions, FlatList, Pressable } from 'react-native';
import { ActivityIndicator, Card, Chip, Text, useTheme } from 'react-native-paper';
import { useQueryClient, useQuery } from '@tanstack/react-query';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { expenseService } from '@/services/expenseService';

const { width: SCREEN_WIDTH } = Dimensions.get('window');
const WALLET_TABS = ['All', 'Cards', 'Cash', 'Virtual'] as const;
type WalletTab = typeof WALLET_TABS[number];

const GRADIENTS = [
  'from-[#8a79ab] to-[#6a5990]',
  'from-[#77b8a1] to-[#4a9080]',
  'from-[#e6a5b8] to-[#c87a94]',
  'from-[#f0c88d] to-[#d4964a]',
  'from-[#a0bbe3] to-[#6090c8]',
  'from-green-600 to-teal-700',
];

function getWalletIcon(type?: string) {
  if (!type) return Wifi;
  const t = type.toLowerCase();
  if (t === 'cash') return Banknote;
  if (t === 'virtual') return Cpu;
  return CardIcon;
}

function formatAmount(amount: number, currency = 'INR') {
  const symbol = currency === 'INR' ? '₹' : currency;
  return `${symbol}${amount.toLocaleString('en-IN', { maximumFractionDigits: 2 })}`;
}

function formatDate(dateStr: string) {
  const d = new Date(dateStr);
  return d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });
}

export default function WalletsScreen() {
  const theme = useTheme();
  const router = useRouter();
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<WalletTab>('All');
  const [activeIndex, setActiveIndex] = useState(0);

  const { data: wallets = [], isLoading: walletsLoading } = useWallets();
  const { data: totalData, isLoading: totalLoading } = useWalletTotal();
  const deleteWalletMutation = useDeleteWallet();
  const [refreshing, setRefreshing] = useState(false);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['wallets'] }),
      queryClient.invalidateQueries({ queryKey: ['wallets', 'total'] }),
    ]);
    setRefreshing(false);
  }, [queryClient]);

  const filteredWallets = wallets.filter((w) => {
    if (activeTab === 'All') return true;
    const t = (w.type || 'card').toLowerCase();
    return t === activeTab.toLowerCase();
  });

  const activeWallet = filteredWallets[activeIndex] || null;

  const { data: transactions = [], isLoading: txLoading } = useQuery({
    queryKey: ['transactions', 'wallet', activeWallet?.id],
    queryFn: () => expenseService.getTransactions({ wallet_id: activeWallet?.id }),
    enabled: !!activeWallet?.id,
  });

  const handleScroll = (event: any) => {
    const slideSize = event.nativeEvent.layoutMeasurement.width;
    const index = event.nativeEvent.contentOffset.x / slideSize;
    const roundIndex = Math.round(index);
    if (roundIndex !== activeIndex && roundIndex >= 0 && roundIndex < filteredWallets.length) {
      setActiveIndex(roundIndex);
    }
  };

  const totalBalance = totalData?.total_balance ?? null;
  const loading = walletsLoading || totalLoading;

  return (
    <View style={{ flex: 1, backgroundColor: theme.colors.background }}>
      {/* Top utility layout (hidden scroll) */}
      <ScrollView
        contentContainerStyle={{ paddingBottom: 160 }}
        showsVerticalScrollIndicator={false}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} colors={[theme.colors.primary]} />}
      >
        {/* Header Tools */}
        <View style={styles.headerTop}>
          <MaterialCommunityIcons name="wallet" size={24} color={theme.colors.onSurfaceVariant} />
          <View style={{ flexDirection: 'row', gap: 16 }}>
             <MaterialCommunityIcons name="gift-outline" size={24} color={theme.colors.onSurfaceVariant} />
             <MaterialCommunityIcons name="line-scan" size={24} color={theme.colors.onSurfaceVariant} />
             <MaterialCommunityIcons name="menu" size={24} color={theme.colors.onSurfaceVariant} />
             <MaterialCommunityIcons name="account-circle-outline" size={24} color={theme.colors.onSurfaceVariant} />
          </View>
        </View>

        {/* Carousel */}
        {loading ? (
          <ActivityIndicator style={{ marginTop: 40 }} />
        ) : filteredWallets.length === 0 ? (
          <Card style={{ margin: 20, borderRadius: 24 }}>
            <Card.Content style={{ alignItems: 'center', paddingVertical: 40 }}>
              <Text variant="bodyLarge" style={{ opacity: 0.5, marginBottom: 16 }}>No wallets found</Text>
            </Card.Content>
          </Card>
        ) : (
          <>
            <FlatList
              data={filteredWallets}
              keyExtractor={(item) => item.id}
              horizontal
              pagingEnabled
              showsHorizontalScrollIndicator={false}
              onScroll={handleScroll}
              scrollEventThrottle={16}
              snapToAlignment="center"
              contentContainerStyle={{ paddingHorizontal: 20, marginTop: 16 }}
              ItemSeparatorComponent={() => <View style={{ width: 16 }} />}
              renderItem={({ item, index }) => (
                <View style={{ width: SCREEN_WIDTH - 40 }}>
                  <CreditCardComponent
                    type={(item.type?.toUpperCase() || 'WALLET')}
                    number={item.name}
                    holder=""
                    exp="--"
                    balance={formatAmount(item.balance, item.currency)}
                    gradient={GRADIENTS[index % GRADIENTS.length]}
                    icon={getWalletIcon(item.type)}
                    isCash={item.type?.toLowerCase() === 'cash'}
                    isVirtual={item.type?.toLowerCase() === 'virtual'}
                  />
                </View>
              )}
            />
            {/* Dots */}
            <View style={styles.dotsContainer}>
              {filteredWallets.map((_, i) => (
                <View key={i} style={[styles.dot, { backgroundColor: i === activeIndex ? theme.colors.onSurface : theme.colors.surfaceVariant }]} />
              ))}
            </View>

            {/* Wallet Stats */}
            {activeWallet && (
              <View style={styles.statsContainer}>
                <View style={{ flex: 1 }}>
                  <Text style={{ fontSize: 12, color: theme.colors.onSurfaceVariant }}>Income</Text>
                  <Text style={{ fontSize: 16, fontWeight: '700', color: theme.colors.onSurface }}>
                    {formatAmount(24006, activeWallet.currency)} <Text style={{ color: '#10b981', fontSize: 12 }}>↑100.00%</Text>
                  </Text>
                  <Text style={{ fontSize: 10, color: theme.colors.onSurfaceVariant, marginTop: 2 }}>Compared to ₹0.00 last month</Text>
                </View>
                <View style={{ flex: 1 }}>
                  <Text style={{ fontSize: 12, color: theme.colors.onSurfaceVariant }}>Expense</Text>
                  <Text style={{ fontSize: 16, fontWeight: '700', color: theme.colors.onSurface }}>
                    {formatAmount(0, activeWallet.currency)} <Text style={{ color: '#ef4444', fontSize: 12 }}>↓0.00%</Text>
                  </Text>
                  <Text style={{ fontSize: 10, color: theme.colors.onSurfaceVariant, marginTop: 2 }}>Compared to ₹0.00 last month</Text>
                </View>
              </View>
            )}

            {/* Transactions List */}
            <View style={styles.txListContainer}>
              {txLoading ? (
                <ActivityIndicator style={{ marginTop: 20 }} />
              ) : transactions.length === 0 ? (
                <Text style={{ textAlign: 'center', opacity: 0.5, marginTop: 20 }}>No transactions for this wallet.</Text>
              ) : (
                transactions.map((tx) => (
                  <Card 
                    key={tx.id} 
                    style={[styles.txCard, { backgroundColor: theme.colors.surface }]} 
                    elevation={0}
                    onPress={() => router.push(`/transaction/${tx.id}`)}
                  >
                    <Card.Content style={styles.txContent}>
                      <View style={[styles.txIcon, { backgroundColor: tx.type === 'income' ? '#dcfce7' : '#fee2e2' }]}>
                        <MaterialCommunityIcons 
                          name={tx.type === 'income' ? 'arrow-down' : 'arrow-up'} 
                          size={20} 
                          color={tx.type === 'income' ? '#16a34a' : '#dc2626'} 
                        />
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
                ))
              )}
            </View>
          </>
        )}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  headerTop: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    paddingTop: 52,
    paddingBottom: 8,
  },
  dotsContainer: {
    flexDirection: 'row',
    justifyContent: 'center',
    gap: 8,
    marginTop: 16,
  },
  dot: {
    width: 8,
    height: 8,
    borderRadius: 4,
  },
  statsContainer: {
    flexDirection: 'row',
    paddingHorizontal: 20,
    marginTop: 24,
    gap: 16,
  },
  txListContainer: {
    marginTop: 32,
    paddingHorizontal: 20,
    gap: 8,
  },
  txCard: { borderRadius: 20, marginBottom: 0 },
  txContent: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 12 },
  txIcon: { width: 44, height: 44, borderRadius: 22, alignItems: 'center', justifyContent: 'center' },
});
