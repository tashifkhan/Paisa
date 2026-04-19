import { useRouter } from 'expo-router';
import React, { useCallback, useEffect, useState } from 'react';
import { RefreshControl, ScrollView, StyleSheet, View } from 'react-native';
import { ActivityIndicator, Button, Card, Chip, Dialog, FAB, Portal, Snackbar, Text, TextInput as PaperTextInput, useTheme } from 'react-native-paper';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { CustomSegmentedTabs } from '../../components/CustomSegmentedTabs';
import { debtService } from '../../services/debtService';
import { groupService } from '../../services/groupService';
import type { BackendDebt, BackendDebtSummary, BackendGroup } from '../../services/types';

const TextInput = PaperTextInput as unknown as React.ComponentType<any> & { Icon: typeof PaperTextInput.Icon; Affix: typeof PaperTextInput.Affix };

type Tab = 'debts' | 'groups';

function formatAmount(amount: number) {
  return `₹${Math.abs(amount).toLocaleString('en-IN', { maximumFractionDigits: 0 })}`;
}

function formatDueDate(dateStr?: string) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  const now = new Date();
  const diff = Math.ceil((d.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
  if (diff < 0) return 'Overdue';
  if (diff === 0) return 'Due today';
  if (diff === 1) return 'Due tomorrow';
  return `Due in ${diff} days`;
}

export default function SocialScreen() {
  const router = useRouter();
  const theme = useTheme();
  const [activeTab, setActiveTab] = useState<Tab>('debts');
  const [debts, setDebts] = useState<BackendDebt[]>([]);
  const [debtSummary, setDebtSummary] = useState<BackendDebtSummary | null>(null);
  const [groups, setGroups] = useState<BackendGroup[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [snack, setSnack] = useState('');

  const loadData = useCallback(async () => {
    try {
      const [debtsData, summaryData, groupsData] = await Promise.all([
        debtService.getDebts(),
        debtService.getSummary(),
        groupService.getGroups(),
      ]);
      setDebts(debtsData);
      setDebtSummary(summaryData);
      setGroups(groupsData);
    } catch {
      // silently fail
    }
  }, []);

  useEffect(() => {
    loadData().finally(() => setLoading(false));
  }, [loadData]);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await loadData();
    setRefreshing(false);
  }, [loadData]);

  const handleSettle = async (id: string) => {
    try {
      await debtService.settleDebt(id);
      setSnack('Debt settled!');
      await loadData();
    } catch {
      setSnack('Failed to settle debt');
    }
  };

  const netBalance = debtSummary ? debtSummary.net : 0;
  const isCredit = netBalance >= 0;

  return (
    <View style={{ flex: 1, backgroundColor: theme.colors.background }}>
      {/* Header */}
      <View style={styles.header}>
        <View>
          <Text variant="headlineMedium" style={styles.title}>Social</Text>
          <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>Friends & Shared Expenses</Text>
        </View>
      </View>

      <CustomSegmentedTabs
        tabs={[
          { value: 'debts', label: 'Friends' },
          { value: 'groups', label: 'Groups' },
        ]}
        value={activeTab}
        onValueChange={(v) => setActiveTab(v as Tab)}
        containerStyle={{ paddingHorizontal: 20, marginBottom: 16 }}
      />

      <ScrollView
        contentContainerStyle={{ paddingHorizontal: 20, paddingBottom: 100, gap: 12 }}
        showsVerticalScrollIndicator={false}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} colors={['#8a79ab']} />}
      >
        {loading ? (
          <ActivityIndicator style={{ marginTop: 60 }} size="large" />
        ) : activeTab === 'debts' ? (
          <>
            {/* Net Balance Card */}
            <View style={{ marginBottom: 16 }}>
              <Card
                style={[styles.netCard, { backgroundColor: isCredit ? '#14532d' : '#7f1d1d' }]}
                elevation={0}
              >
                <Card.Content style={{ padding: 24 }}>
                  <Text variant="titleSmall" style={{ color: 'rgba(255,255,255,0.7)', textTransform: 'uppercase', letterSpacing: 1, marginBottom: 4 }}>Net Balance</Text>
                  <Text variant="displayMedium" style={{ fontWeight: '800', color: isCredit ? '#4ade80' : '#f87171', marginBottom: 16, letterSpacing: -1 }}>
                    {isCredit ? '+' : '-'}{formatAmount(netBalance)}
                  </Text>
                  
                  <View style={[styles.netRow, { backgroundColor: isCredit ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.15)', padding: 16, borderRadius: 16 }]}>
                    <View style={{ flex: 1, borderRightWidth: 1, borderColor: 'rgba(255,255,255,0.1)', paddingRight: 12 }}>
                      <Text variant="labelMedium" style={{ color: 'rgba(255,255,255,0.6)', marginBottom: 2 }}>Owed to you</Text>
                      <Text variant="titleLarge" style={{ color: '#fff', fontWeight: '700' }}>
                        +{formatAmount(debtSummary?.owed_to_me || 0)}
                      </Text>
                    </View>
                    <View style={{ flex: 1, paddingLeft: 16 }}>
                      <Text variant="labelMedium" style={{ color: 'rgba(255,255,255,0.6)', marginBottom: 2 }}>You owe</Text>
                      <Text variant="titleLarge" style={{ color: '#fff', fontWeight: '700' }}>
                        -{formatAmount(debtSummary?.owed_by_me || 0)}
                      </Text>
                    </View>
                  </View>
                </Card.Content>
              </Card>
            </View>

            {/* Debts List */}
            {debts.length === 0 ? (
              <View style={{ alignItems: 'center', paddingTop: 60 }}>
                <View style={{ width: 80, height: 80, borderRadius: 40, backgroundColor: theme.colors.surfaceVariant, alignItems: 'center', justifyContent: 'center', marginBottom: 16 }}>
                   <MaterialCommunityIcons name="account-group" size={40} color={theme.colors.onSurfaceVariant} />
                </View>
                <Text variant="titleMedium" style={{ fontWeight: '700', marginBottom: 4 }}>You're all settled up!</Text>
                <Text variant="bodyMedium" style={{ color: theme.colors.onSurfaceVariant, textAlign: 'center', marginHorizontal: 32, marginBottom: 24 }}>Keep track of who owes you and who you owe directly from here.</Text>
                <Button mode="contained" onPress={() => router.push('/add-debt')} style={{ borderRadius: 24 }} contentStyle={{ paddingHorizontal: 16, height: 48 }}>
                  Add First Debt
                </Button>
              </View>
            ) : (
              debts.map((debt) => (
                <Card
                  key={debt.id}
                  style={[styles.debtCard, { backgroundColor: theme.colors.surface }]}
                  elevation={0}
                  onPress={() => router.push({ pathname: '/user-detail', params: { debtId: debt.id } })}
                >
                  <Card.Content style={styles.debtContent}>
                    <View style={[styles.avatar, { backgroundColor: debt.type === 'owed_to_me' ? '#dcfce7' : '#fee2e2' }]}>
                      <Text style={{ fontSize: 18, fontWeight: '700', color: debt.type === 'owed_to_me' ? '#166534' : '#991b1b' }}>
                        {debt.counterparty_name.charAt(0).toUpperCase()}
                      </Text>
                    </View>
                    <View style={{ flex: 1 }}>
                      <Text variant="titleSmall" style={{ fontWeight: '700' }}>{debt.counterparty_name}</Text>
                      <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>
                        {formatDueDate(debt.due_date) || (debt.type === 'owed_to_me' ? 'Owes you' : 'You owe')}
                      </Text>
                    </View>
                    <View style={{ alignItems: 'flex-end', gap: 4 }}>
                      <Text variant="titleSmall" style={{ fontWeight: '700', color: debt.type === 'owed_to_me' ? '#16a34a' : '#dc2626' }}>
                        {debt.type === 'owed_to_me' ? '+' : '-'}{formatAmount(debt.amount)}
                      </Text>
                      <Button
                        mode="text"
                        compact
                        onPress={() => handleSettle(debt.id)}
                        labelStyle={{ fontSize: 11 }}
                      >
                        Settle
                      </Button>
                    </View>
                  </Card.Content>
                </Card>
              ))
            )}
          </>
        ) : (
          <>
            {/* Groups List */}
            {groups.length === 0 ? (
              <View style={{ alignItems: 'center', paddingTop: 60 }}>
                <View style={{ width: 80, height: 80, borderRadius: 40, backgroundColor: theme.colors.surfaceVariant, alignItems: 'center', justifyContent: 'center', marginBottom: 16 }}>
                   <MaterialCommunityIcons name="account-group" size={40} color={theme.colors.onSurfaceVariant} />
                </View>
                <Text variant="titleMedium" style={{ fontWeight: '700', marginBottom: 4 }}>No groups yet</Text>
                <Text variant="bodyMedium" style={{ color: theme.colors.onSurfaceVariant, textAlign: 'center', marginHorizontal: 32, marginBottom: 24 }}>Share expenses natively with roommates, trips, and partners.</Text>
                <Button mode="contained" onPress={() => router.push('/create-group')} style={{ borderRadius: 24 }} contentStyle={{ paddingHorizontal: 16, height: 48 }}>
                  Create First Group
                </Button>
              </View>
            ) : (
              groups.map((group) => (
                <Card
                  key={group.id}
                  style={[styles.groupCard, { backgroundColor: theme.colors.surface }]}
                  elevation={0}
                  onPress={() => router.push({ pathname: '/group-detail', params: { groupId: group.id } })}
                >
                  <Card.Content style={styles.groupContent}>
                    <View style={[styles.groupIcon, { backgroundColor: group.color || '#8a79ab' }]}>
                      {group.icon ? <Text style={{ fontSize: 20 }}>{group.icon}</Text> : <MaterialCommunityIcons name="account-multiple" size={20} color="#fff" />}
                    </View>
                    <View style={{ flex: 1 }}>
                      <Text variant="titleSmall" style={{ fontWeight: '700' }}>{group.name}</Text>
                      <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>
                        {group.base_currency || 'INR'}
                      </Text>
                    </View>
                    <Button mode="text" compact icon="chevron-right" onPress={() => router.push({ pathname: '/group-detail', params: { groupId: group.id } })}>
                      View
                    </Button>
                  </Card.Content>
                </Card>
              ))
            )}
          </>
        )}
      </ScrollView>

      <Snackbar visible={!!snack} onDismiss={() => setSnack('')} duration={2500}>{snack}</Snackbar>
    </View>
  );
}

const styles = StyleSheet.create({
  header: { paddingHorizontal: 20, paddingTop: 52, paddingBottom: 8 },
  title: { fontWeight: '700' },
  netCard: { borderRadius: 28, marginBottom: 4 },
  netRow: { flexDirection: 'row', justifyContent: 'space-between', marginTop: 8 },
  debtCard: { borderRadius: 20 },
  debtContent: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 12 },
  avatar: { width: 48, height: 48, borderRadius: 24, alignItems: 'center', justifyContent: 'center' },
  groupCard: { borderRadius: 20 },
  groupContent: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 12 },
  groupIcon: { width: 48, height: 48, borderRadius: 24, alignItems: 'center', justifyContent: 'center' },
  createGroupBtn: { borderRadius: 28, marginTop: 8 },
  fab: { position: 'absolute', right: 20, bottom: 100, borderRadius: 20 },
});
