import { StyleSheet } from 'react-native';

import { BalanceCard } from '@/components/BalanceCard';
import { TransactionList } from '@/components/TransactionList';
import ParallaxScrollView from '@/components/ParallaxScrollView';
import { ThemedText } from '@/components/ThemedText';
import { ThemedView } from '@/components/ThemedView';
import { Card } from '@/components/ui/Card';

export default function HomeScreen() {
  return (
    <ParallaxScrollView
      headerBackgroundColor={{ light: '#A1CEDC', dark: '#1D3D47' }}
      headerImage={<BalanceCard />}>
      <ThemedView style={styles.container}>
        <Card variant="primary" style={styles.summaryCard}>
          <ThemedText type="subtitle">This Month</ThemedText>
          <ThemedView style={styles.summaryRow}>
            <ThemedView>
              <ThemedText type="defaultSemiBold">Income</ThemedText>
              <ThemedText type="title" style={styles.income}>+$2,450.00</ThemedText>
            </ThemedView>
            <ThemedView>
              <ThemedText type="defaultSemiBold">Expenses</ThemedText>
              <ThemedText type="title" style={styles.expense}>-$1,850.00</ThemedText>
            </ThemedView>
          </ThemedView>
        </Card>

        <ThemedView style={styles.transactionsContainer}>
          <ThemedText type="subtitle">Recent Transactions</ThemedText>
          <TransactionList />
        </ThemedView>
      </ThemedView>
    </ParallaxScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    gap: 24,
  },
  summaryCard: {
    padding: 16,
    gap: 16,
  },
  summaryRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  income: {
    color: '#34C759',
  },
  expense: {
    color: '#FF3B30',
  },
  transactionsContainer: {
    gap: 16,
  },
});
