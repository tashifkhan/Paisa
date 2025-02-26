import React from 'react';
import { StyleSheet, View, FlatList, ListRenderItem } from 'react-native';
import { Card } from '@/components/ui/Card';
import { ThemedText } from '@/components/ThemedText';
import { useThemeColor } from '@/hooks/useThemeColor';

export interface Transaction {
  id: string;
  title: string;
  amount: number;
  type: 'expense' | 'income';
  date: string;
  category?: string;
  icon?: string;
}

interface TransactionListProps {
  transactions: Transaction[];
  onTransactionPress?: (transaction: Transaction) => void;
}

export function TransactionList({ transactions, onTransactionPress }: TransactionListProps) {
  const positiveColor = useThemeColor({ light: '#4CAF50', dark: '#81C784' });
  const negativeColor = useThemeColor({ light: '#F44336', dark: '#E57373' });

  const renderTransaction: ListRenderItem<Transaction> = ({ item }) => (
    <Card
      variant="secondary"
      style={styles.transactionCard}
      onPress={() => onTransactionPress?.(item)}>
      <View style={styles.transactionRow}>
        <View style={styles.transactionInfo}>
          <ThemedText type="defaultSemiBold">{item.title}</ThemedText>
          <ThemedText style={styles.date}>{item.date}</ThemedText>
        </View>
        <ThemedText
          type="defaultSemiBold"
          style={[styles.amount, { color: item.type === 'income' ? positiveColor : negativeColor }]}>
          {item.type === 'income' ? '+' : '-'}${Math.abs(item.amount).toFixed(2)}
        </ThemedText>
      </View>
    </Card>
  );

  return (
    <FlatList
      data={transactions}
      renderItem={renderTransaction}
      keyExtractor={(item) => item.id}
      contentContainerStyle={styles.list}
    />
  );
}

const styles = StyleSheet.create({
  list: {
    padding: 16,
    gap: 8,
  },
  transactionCard: {
    marginBottom: 8,
  },
  transactionRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  transactionInfo: {
    flex: 1,
  },
  date: {
    fontSize: 12,
    marginTop: 4,
    opacity: 0.7,
  },
  amount: {
    marginLeft: 16,
  },
}));