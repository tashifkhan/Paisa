import { MaterialCommunityIcons } from '@expo/vector-icons';
import { useQuery } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import React, { useMemo, useState } from 'react';
import { FlatList, Pressable, ScrollView, StyleSheet, View } from 'react-native';
import { ActivityIndicator, Button, Card, Chip, FAB, Modal, Portal, Text, TextInput, useTheme } from 'react-native-paper';
import { CustomSegmentedTabs } from '../components/CustomSegmentedTabs';
import { useTransactions } from '../hooks/useTransactions';
import { useWallets } from '../hooks/useWallets';
import { categoryService } from '../services/categoryService';

function formatAmount(amount: number, currency: string = 'INR') {
  const symbol = currency === 'INR' ? '₹' : currency === 'USD' ? '$' : currency;
  return `${symbol}${Math.abs(amount).toLocaleString('en-IN', { maximumFractionDigits: 2 })}`;
}

function formatDate(dateStr: string) {
  const d = new Date(dateStr);
  return d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });
}

type FilterType = 'all' | 'expense' | 'income' | 'transfer';

export default function AllTransactionsScreen() {
  const theme = useTheme();
  const router = useRouter();
  const [searchQuery, setSearchQuery] = useState('');
  const [filterType, setFilterType] = useState<FilterType>('all');
  const [filterModalVisible, setFilterModalVisible] = useState(false);
  const [selectedCategoryId, setSelectedCategoryId] = useState<string | null>(null);
  const [selectedWalletId, setSelectedWalletId] = useState<string | null>(null);

  const { data: transactions = [], isLoading } = useTransactions({});
  const { data: categories = [] } = useQuery({ queryKey: ['categories'], queryFn: () => categoryService.getCategories() });
  const { data: wallets = [] } = useWallets();

  const filtered = useMemo(() => {
    let list = transactions;

    // Type filter
    if (filterType !== 'all') {
      list = list.filter((tx) => tx.type === filterType);
    }

    // Advanced filters
    if (selectedCategoryId) {
      list = list.filter((tx) => tx.category_id === selectedCategoryId);
    }
    if (selectedWalletId) {
      list = list.filter((tx) => tx.wallet_id === selectedWalletId);
    }

    // Search filter (local, over note/title/description)
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase().trim();
      list = list.filter(
        (tx) =>
          (tx.note && tx.note.toLowerCase().includes(q)) ||
          (tx.title && tx.title.toLowerCase().includes(q)) ||
          (tx.description && tx.description.toLowerCase().includes(q))
      );
    }

    return list;
  }, [transactions, filterType, searchQuery, selectedCategoryId, selectedWalletId]);

  const renderTransaction = ({ item: tx }: any) => (
    <Card
      style={[styles.txCard, { backgroundColor: theme.colors.surface }]}
      elevation={0}
      onPress={() => router.push(`/transaction/${tx.id}`)}
    >
      <Card.Content style={styles.txContent}>
        <View style={[styles.txIcon, { backgroundColor: tx.type === 'income' ? '#dcfce7' : tx.type === 'transfer' ? '#e0e7ff' : '#fee2e2' }]}>
          <MaterialCommunityIcons 
            name={tx.type === 'income' ? 'arrow-down' : tx.type === 'transfer' ? 'swap-horizontal' : 'arrow-up'} 
            size={20} 
            color={tx.type === 'income' ? '#16a34a' : tx.type === 'transfer' ? '#6366f1' : '#dc2626'} 
          />
        </View>
        <View style={{ flex: 1 }}>
          <Text variant="titleSmall" style={{ fontWeight: '600' }}>
            {tx.note || tx.title || (tx.type === 'income' ? 'Income' : tx.type === 'transfer' ? 'Transfer' : 'Expense')}
          </Text>
          <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>
            {formatDate(tx.date)}
          </Text>
        </View>
        <Text
          variant="titleSmall"
          style={{ fontWeight: '700', color: tx.type === 'income' ? '#16a34a' : tx.type === 'transfer' ? '#6366f1' : '#dc2626' }}
        >
          {tx.type === 'income' ? '+' : tx.type === 'transfer' ? '' : '-'}{formatAmount(tx.amount, tx.currency)}
        </Text>
      </Card.Content>
    </Card>
  );

  return (
    <View style={[styles.container, { backgroundColor: theme.colors.background }]}>
      {/* Header */}
      <View style={styles.header}>
        <Pressable onPress={() => router.back()} style={{ padding: 8 }}>
          <MaterialCommunityIcons name="arrow-left" size={24} color={theme.colors.onSurface} />
        </Pressable>
        <Text variant="titleMedium" style={{ fontWeight: '700', flex: 1, textAlign: 'center' }}>
          All Transactions
        </Text>
        <View style={{ width: 40 }} />
      </View>

      {/* Search Bar */}
      <View style={{ paddingHorizontal: 16, marginBottom: 8 }}>
        <TextInput
          mode="outlined"
          placeholder="Search transactions..."
          value={searchQuery}
          onChangeText={setSearchQuery}
          left={<TextInput.Icon icon="magnify" />}
          right={searchQuery ? <TextInput.Icon icon="close" onPress={() => setSearchQuery('')} /> : undefined}
          outlineColor={theme.colors.surfaceVariant}
          activeOutlineColor={theme.colors.primary}
          style={{ backgroundColor: theme.colors.surfaceVariant, borderRadius: 16 }}
          outlineStyle={{ borderRadius: 16 }}
          dense
        />
      </View>

      {/* Filter Tabs */}
      <View style={{ paddingHorizontal: 16, marginBottom: 8 }}>
        <CustomSegmentedTabs
          tabs={[
            { value: 'all', label: 'All' },
            { value: 'expense', label: 'Expense' },
            { value: 'income', label: 'Income' },
            { value: 'transfer', label: 'Transfer' },
          ]}
          value={filterType}
          onValueChange={(v) => setFilterType(v as FilterType)}
        />
      </View>

      {/* Count */}
      <View style={{ paddingHorizontal: 20, paddingVertical: 8 }}>
        <Text variant="labelMedium" style={{ color: theme.colors.onSurfaceVariant }}>
          {filtered.length} transaction{filtered.length !== 1 ? 's' : ''}
        </Text>
      </View>

      {/* Transaction List */}
      {isLoading ? (
        <ActivityIndicator style={{ marginTop: 40 }} />
      ) : (
        <FlatList
          data={filtered}
          keyExtractor={(item) => item.id}
          renderItem={renderTransaction}
          contentContainerStyle={{ paddingHorizontal: 16, paddingBottom: 40, gap: 8 }}
          showsVerticalScrollIndicator={false}
          ListEmptyComponent={
            <View style={{ alignItems: 'center', paddingTop: 60 }}>
              <MaterialCommunityIcons name="magnify" size={48} color={theme.colors.onSurfaceVariant} style={{ opacity: 0.4 }} />
              <Text variant="bodyLarge" style={{ opacity: 0.5, marginTop: 16 }}>
                {searchQuery ? 'No matching transactions' : 'No transactions yet'}
              </Text>
            </View>
          }
        />
      )}

      {/* Filter FAB */}
      <FAB
        icon="filter-variant"
        style={[styles.fab, { backgroundColor: theme.colors.primaryContainer }]}
        color={theme.colors.onPrimaryContainer}
        onPress={() => setFilterModalVisible(true)}
      />

      <Portal>
        <Modal 
          visible={filterModalVisible} 
          onDismiss={() => setFilterModalVisible(false)} 
          contentContainerStyle={[styles.modalContent, { backgroundColor: theme.colors.surface }]}
        >
          <Text variant="titleLarge" style={{ fontWeight: '700', marginBottom: 16 }}>Advanced Filters</Text>

          <ScrollView style={{ maxHeight: 400 }} showsVerticalScrollIndicator={false}>
            <Text variant="titleMedium" style={{ fontWeight: '600', marginBottom: 8, marginTop: 8 }}>Wallets</Text>
            <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: 8 }}>
              <Chip
                selected={null === selectedWalletId}
                onPress={() => setSelectedWalletId(null)}
                showSelectedOverlay
              >
                All Wallets
              </Chip>
              {wallets.map(w => (
                <Chip
                  key={w.id}
                  selected={w.id === selectedWalletId}
                  onPress={() => setSelectedWalletId(w.id)}
                  showSelectedOverlay
                >
                  {w.name}
                </Chip>
              ))}
            </View>

            <Text variant="titleMedium" style={{ fontWeight: '600', marginBottom: 8, marginTop: 24 }}>Categories</Text>
            <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: 8 }}>
              <Chip
                selected={null === selectedCategoryId}
                onPress={() => setSelectedCategoryId(null)}
                showSelectedOverlay
              >
                All Categories
              </Chip>
              {categories.map(c => (
                <Chip
                  key={c.id}
                  selected={c.id === selectedCategoryId}
                  onPress={() => setSelectedCategoryId(c.id)}
                  showSelectedOverlay
                >
                  {c.name}
                </Chip>
              ))}
            </View>
          </ScrollView>

          <Button 
            mode="contained" 
            onPress={() => setFilterModalVisible(false)} 
            style={{ marginTop: 24 }}
          >
            Apply Filters Let's Go
          </Button>
        </Modal>
      </Portal>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 12,
    paddingTop: 52,
    paddingBottom: 12,
  },
  txCard: { borderRadius: 20 },
  txContent: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 12 },
  txIcon: { width: 44, height: 44, borderRadius: 22, alignItems: 'center', justifyContent: 'center' },
  fab: {
    position: 'absolute',
    margin: 16,
    right: 0,
    bottom: 0,
    borderRadius: 16,
  },
  modalContent: {
    margin: 20,
    padding: 24,
    borderRadius: 24,
  },
});
