import { useLocalSearchParams, useRouter } from 'expo-router';
import React, { useCallback, useEffect, useState } from 'react';
import { RefreshControl, ScrollView, StyleSheet, View } from 'react-native';
import { ActivityIndicator, Button, Card, Dialog, Divider, IconButton, Portal, Snackbar, Text, TextInput as PaperTextInput, useTheme } from 'react-native-paper';
import { CustomSegmentedTabs } from '../components/CustomSegmentedTabs';
import { groupService } from '../services/groupService';
import type { BackendGroup, BackendGroupBalanceSummary, BackendSimplifyDebtsResponse, BackendTransaction } from '../services/types';

const TextInput = PaperTextInput as unknown as React.ComponentType<any> & { Icon: typeof PaperTextInput.Icon; Affix: typeof PaperTextInput.Affix };

type Tab = 'expenses' | 'balances';

function formatAmount(amount: number, currency = 'INR') {
  const symbol = currency === 'INR' ? '₹' : currency;
  return `${symbol}${Math.abs(amount).toLocaleString('en-IN', { maximumFractionDigits: 0 })}`;
}

function formatDate(dateStr: string) {
  const d = new Date(dateStr);
  return d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });
}

export default function GroupDetailScreen() {
  const router = useRouter();
  const theme = useTheme();
  const { groupId } = useLocalSearchParams<{ groupId: string }>();

  const [activeTab, setActiveTab] = useState<Tab>('expenses');
  const [group, setGroup] = useState<BackendGroup | null>(null);
  const [expenses, setExpenses] = useState<BackendTransaction[]>([]);
  const [balances, setBalances] = useState<BackendGroupBalanceSummary | null>(null);
  const [simplified, setSimplified] = useState<BackendSimplifyDebtsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [snack, setSnack] = useState('');

  // Add expense dialog
  const [showAddExpense, setShowAddExpense] = useState(false);
  const [expenseAmount, setExpenseAmount] = useState('');
  const [expenseNote, setExpenseNote] = useState('');
  const [savingExpense, setSavingExpense] = useState(false);

  // Simplify dialog
  const [showSimplify, setShowSimplify] = useState(false);
  const [simplifying, setSimplifying] = useState(false);

  const loadData = useCallback(async () => {
    if (!groupId) return;
    try {
      const [g, exps, bals] = await Promise.all([
        groupService.getGroup(groupId),
        groupService.getGroupExpenses(groupId),
        groupService.getGroupBalances(groupId),
      ]);
      setGroup(g);
      setExpenses(exps);
      setBalances(bals);
    } catch {
      // silently fail
    }
  }, [groupId]);

  useEffect(() => {
    loadData().finally(() => setLoading(false));
  }, [loadData]);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await loadData();
    setRefreshing(false);
  }, [loadData]);

  const handleAddExpense = async () => {
    const amt = parseFloat(expenseAmount);
    if (!amt || amt <= 0) return;
    setSavingExpense(true);
    try {
      await groupService.addGroupExpense(groupId!, {
        amount: amt,
        currency: group?.base_currency || 'INR',
        type: 'expense',
        note: expenseNote || undefined,
        split_strategy: 'equal',
      });
      setSnack('Expense added!');
      setShowAddExpense(false);
      setExpenseAmount('');
      setExpenseNote('');
      await loadData();
    } catch {
      setSnack('Failed to add expense');
    } finally {
      setSavingExpense(false);
    }
  };

  const handleSimplify = async () => {
    if (!groupId) return;
    setSimplifying(true);
    try {
      const result = await groupService.simplifyDebts(groupId);
      setSimplified(result);
      setShowSimplify(true);
    } catch {
      setSnack('Failed to simplify debts');
    } finally {
      setSimplifying(false);
    }
  };

  if (!groupId) return (
    <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center' }}>
      <Text>Group not found</Text>
      <Button onPress={() => router.back()}>Go Back</Button>
    </View>
  );

  return (
    <View style={{ flex: 1, backgroundColor: theme.colors.background }}>
      {/* Header */}
      <View style={styles.header}>
        <IconButton icon="arrow-left" onPress={() => router.back()} />
        <Text variant="titleMedium" style={styles.headerTitle}>{group?.name || 'Group'}</Text>
        <IconButton icon="dots-horizontal" onPress={() => {}} />
      </View>

      {/* Group Summary Card */}
      {!loading && group && balances && (
        <View style={{ paddingHorizontal: 20, marginBottom: 12 }}>
          <Card style={[styles.summaryCard, { backgroundColor: theme.colors.surface }]} elevation={0}>
            <Card.Content style={{ padding: 20 }}>
              <View style={styles.summaryHeader}>
                <View style={[styles.groupIcon, { backgroundColor: group.color || theme.colors.primaryContainer }]}>
                  <Text style={{ fontSize: 24 }}>{group.icon || '👥'}</Text>
                </View>
                <View style={{ flex: 1 }}>
                  <Text variant="titleLarge" style={styles.groupName}>{group.name}</Text>
                  <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>
                    {balances.balances.length} members • {group.base_currency}
                  </Text>
                </View>
              </View>
              <Divider style={{ marginVertical: 12 }} />
              <View style={styles.totalsRow}>
                <View style={{ alignItems: 'center' }}>
                  <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>Total Expenses</Text>
                  <Text variant="titleMedium" style={styles.totalAmount}>
                    {formatAmount(balances.total_expenses, group.base_currency)}
                  </Text>
                </View>
                <Button
                  mode="outlined"
                  compact
                  onPress={handleSimplify}
                  loading={simplifying}
                  style={styles.simplifyBtn}
                  icon="lightning-bolt"
                >
                  Simplify
                </Button>
              </View>
            </Card.Content>
          </Card>
        </View>
      )}

      <CustomSegmentedTabs
        tabs={[
          { value: 'expenses', label: 'Expenses' },
          { value: 'balances', label: 'Balances' },
        ]}
        value={activeTab}
        onValueChange={(v) => setActiveTab(v as Tab)}
        containerStyle={{ paddingHorizontal: 20, marginBottom: 12 }}
      />

      <ScrollView
        contentContainerStyle={{ paddingHorizontal: 20, paddingBottom: 100, gap: 10 }}
        showsVerticalScrollIndicator={false}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} colors={['#8a79ab']} />}
      >
        {loading ? (
          <ActivityIndicator style={{ marginTop: 60 }} size="large" />
        ) : activeTab === 'expenses' ? (
          <>
            {expenses.length === 0 ? (
              <Card style={{ borderRadius: 24, marginTop: 20 }}>
                <Card.Content style={{ alignItems: 'center', paddingVertical: 32 }}>
                  <Text variant="bodyLarge" style={{ opacity: 0.5, marginBottom: 16 }}>No expenses yet</Text>
                  <Button mode="contained" onPress={() => setShowAddExpense(true)} style={{ borderRadius: 20 }}>
                    Add First Expense
                  </Button>
                </Card.Content>
              </Card>
            ) : (
              expenses.map((exp) => (
                <Card key={exp.id} style={[styles.expenseCard, { backgroundColor: theme.colors.surface }]} elevation={0}>
                  <Card.Content style={styles.expenseContent}>
                    <View style={[styles.expenseIcon, { backgroundColor: theme.colors.surfaceVariant }]}>
                      <Text style={{ fontSize: 18 }}>💸</Text>
                    </View>
                    <View style={{ flex: 1 }}>
                      <Text variant="titleSmall" style={{ fontWeight: '600' }}>
                        {exp.note || 'Group Expense'}
                      </Text>
                      <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>
                        {formatDate(exp.date)}
                      </Text>
                    </View>
                    <Text variant="titleSmall" style={{ fontWeight: '700', color: theme.colors.onSurface }}>
                      {formatAmount(exp.amount, exp.currency)}
                    </Text>
                  </Card.Content>
                </Card>
              ))
            )}
          </>
        ) : (
          <>
            {balances?.balances.map((b) => (
              <Card key={b.user_id} style={[styles.expenseCard, { backgroundColor: theme.colors.surface }]} elevation={0}>
                <Card.Content style={styles.expenseContent}>
                  <View style={[styles.balanceAvatar, { backgroundColor: theme.colors.primaryContainer }]}>
                    <Text style={{ fontWeight: '700', color: theme.colors.primary, fontSize: 16 }}>
                      {(b.user_name || 'U').charAt(0).toUpperCase()}
                    </Text>
                  </View>
                  <Text variant="titleSmall" style={{ flex: 1, fontWeight: '600' }}>
                    {b.user_name || 'Member'}
                  </Text>
                  <Text
                    variant="titleSmall"
                    style={{ fontWeight: '700', color: b.balance >= 0 ? '#16a34a' : '#dc2626' }}
                  >
                    {b.balance >= 0 ? '+' : '-'}{formatAmount(b.balance, group?.base_currency)}
                  </Text>
                </Card.Content>
              </Card>
            ))}
          </>
        )}
      </ScrollView>

      {/* FAB */}
      <View style={styles.fab}>
        <Button
          mode="contained"
          icon="plus"
          onPress={() => setShowAddExpense(true)}
          style={{ borderRadius: 24 }}
          contentStyle={{ height: 52, paddingHorizontal: 8 }}
          labelStyle={{ fontWeight: '700' }}
        >
          Add Expense
        </Button>
      </View>

      {/* Add Expense Dialog */}
      <Portal>
        <Dialog visible={showAddExpense} onDismiss={() => setShowAddExpense(false)} style={{ borderRadius: 24 }}>
          <Dialog.Title>Add Group Expense</Dialog.Title>
          <Dialog.Content style={{ gap: 12 }}>
            <TextInput
              mode="outlined"
              label="Amount"
              value={expenseAmount}
              onChangeText={setExpenseAmount}
              keyboardType="numeric"
              left={<TextInput.Affix text={group?.base_currency === 'USD' ? '$' : '₹'} />}
            />
            <TextInput
              mode="outlined"
              label="Description (optional)"
              value={expenseNote}
              onChangeText={setExpenseNote}
            />
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setShowAddExpense(false)}>Cancel</Button>
            <Button mode="contained" onPress={handleAddExpense} loading={savingExpense} disabled={savingExpense}>
              Add
            </Button>
          </Dialog.Actions>
        </Dialog>

        {/* Simplify Dialog */}
        <Dialog visible={showSimplify} onDismiss={() => setShowSimplify(false)} style={{ borderRadius: 24 }}>
          <Dialog.Title>Simplified Debts</Dialog.Title>
          <Dialog.Content>
            {simplified?.simplified_debts.length === 0 ? (
              <Text>All settled! No debts remaining.</Text>
            ) : (
              simplified?.simplified_debts.map((d, i) => (
                <Card key={i} style={{ borderRadius: 16, marginBottom: 8 }} elevation={0}>
                  <Card.Content>
                    <Text variant="bodyMedium">
                      <Text style={{ fontWeight: '700' }}>{d.from_user_name}</Text> owes{' '}
                      <Text style={{ fontWeight: '700' }}>{d.to_user_name}</Text>
                    </Text>
                    <Text variant="titleMedium" style={{ fontWeight: '800', color: '#dc2626' }}>
                      {formatAmount(d.amount, group?.base_currency)}
                    </Text>
                  </Card.Content>
                </Card>
              ))
            )}
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setShowSimplify(false)}>Close</Button>
          </Dialog.Actions>
        </Dialog>
      </Portal>

      <Snackbar visible={!!snack} onDismiss={() => setSnack('')} duration={2500}>{snack}</Snackbar>
    </View>
  );
}

const styles = StyleSheet.create({
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 16, paddingTop: 52, paddingBottom: 8 },
  headerTitle: { fontWeight: '700' },
  summaryCard: { borderRadius: 24 },
  summaryHeader: { flexDirection: 'row', alignItems: 'center', gap: 16 },
  groupIcon: { width: 56, height: 56, borderRadius: 28, alignItems: 'center', justifyContent: 'center' },
  groupName: { fontWeight: '700' },
  totalsRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  totalAmount: { fontWeight: '800', marginTop: 2 },
  simplifyBtn: { borderRadius: 16 },
  expenseCard: { borderRadius: 20 },
  expenseContent: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 12 },
  expenseIcon: { width: 44, height: 44, borderRadius: 22, alignItems: 'center', justifyContent: 'center' },
  balanceAvatar: { width: 44, height: 44, borderRadius: 22, alignItems: 'center', justifyContent: 'center' },
  fab: { position: 'absolute', bottom: 20, right: 20, left: 20 },
});
