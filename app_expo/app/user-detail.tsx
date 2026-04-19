import { useLocalSearchParams, useRouter } from 'expo-router';
import React, { useCallback, useEffect, useState } from 'react';
import { RefreshControl, ScrollView, StyleSheet, View } from 'react-native';
import { ActivityIndicator, Button, Card, Dialog, Portal, Snackbar, Text, useTheme } from 'react-native-paper';
import { debtService } from '../services/debtService';
import type { BackendDebt } from '../services/types';

function formatAmount(amount: number) {
  return `₹${Math.abs(amount).toLocaleString('en-IN', { maximumFractionDigits: 0 })}`;
}

function formatDueDate(dateStr?: string) {
  if (!dateStr) return null;
  const d = new Date(dateStr);
  return d.toLocaleDateString('en-IN', { day: 'numeric', month: 'long', year: 'numeric' });
}

export default function UserDetailScreen() {
  const router = useRouter();
  const theme = useTheme();
  const { debtId } = useLocalSearchParams<{ debtId: string }>();

  const [debt, setDebt] = useState<BackendDebt | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [snack, setSnack] = useState('');
  const [showSettleDialog, setShowSettleDialog] = useState(false);
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const [settling, setSettling] = useState(false);

  const loadData = useCallback(async () => {
    if (!debtId) return;
    try {
      const d = await debtService.getDebt(debtId);
      setDebt(d);
    } catch {
      setSnack('Failed to load debt details');
    }
  }, [debtId]);

  useEffect(() => {
    loadData().finally(() => setLoading(false));
  }, [loadData]);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await loadData();
    setRefreshing(false);
  }, [loadData]);

  const handleSettle = async () => {
    if (!debtId) return;
    setSettling(true);
    try {
      await debtService.settleDebt(debtId);
      setSnack('Debt settled!');
      setShowSettleDialog(false);
      router.back();
    } catch {
      setSnack('Failed to settle debt');
    } finally {
      setSettling(false);
    }
  };

  const handleDelete = async () => {
    if (!debtId) return;
    try {
      await debtService.deleteDebt(debtId);
      setShowDeleteDialog(false);
      router.back();
    } catch {
      setSnack('Failed to delete debt');
    }
  };

  if (!debtId) return (
    <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center' }}>
      <Text>Debt not found</Text>
      <Button onPress={() => router.back()}>Go Back</Button>
    </View>
  );

  const isCredit = debt?.type === 'owed_to_me';
  const color = isCredit ? '#16a34a' : '#dc2626';
  const bgColor = isCredit ? '#dcfce7' : '#fee2e2';

  return (
    <View style={{ flex: 1, backgroundColor: theme.colors.background }}>
      {/* Header */}
      <View style={styles.header}>
        <Button mode="text" icon="arrow-left" onPress={() => router.back()} compact>Back</Button>
        <Text variant="titleMedium" style={styles.headerTitle}>Debt Details</Text>
        <View style={{ width: 80 }} />
      </View>

      <ScrollView
        contentContainerStyle={{ paddingHorizontal: 20, paddingBottom: 100 }}
        showsVerticalScrollIndicator={false}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} colors={['#8a79ab']} />}
      >
        {loading ? (
          <ActivityIndicator style={{ marginTop: 60 }} size="large" />
        ) : !debt ? (
          <Card style={{ borderRadius: 24, marginTop: 20 }}>
            <Card.Content style={{ alignItems: 'center', paddingVertical: 32 }}>
              <Text variant="bodyLarge" style={{ opacity: 0.5 }}>Debt not found</Text>
            </Card.Content>
          </Card>
        ) : (
          <>
            {/* Profile Card */}
            <Card style={[styles.profileCard, { backgroundColor: theme.colors.surface }]} elevation={0}>
              <Card.Content style={{ alignItems: 'center', paddingVertical: 28 }}>
                <View style={[styles.avatar, { backgroundColor: bgColor }]}>
                  <Text style={{ fontSize: 32, fontWeight: '800', color }}>
                    {debt.counterparty_name.charAt(0).toUpperCase()}
                  </Text>
                </View>
                <Text variant="headlineSmall" style={styles.name}>{debt.counterparty_name}</Text>
                <View style={[styles.typeBadge, { backgroundColor: bgColor }]}>
                  <Text variant="labelMedium" style={{ color, fontWeight: '700' }}>
                    {isCredit ? '↑ Owes You' : '↓ You Owe'}
                  </Text>
                </View>
              </Card.Content>
            </Card>

            {/* Amount Card */}
            <Card style={[styles.amountCard, { backgroundColor: bgColor, borderColor: color + '30', borderWidth: 1 }]} elevation={0}>
              <Card.Content style={{ alignItems: 'center', paddingVertical: 24 }}>
                <Text variant="bodyMedium" style={{ color, opacity: 0.8, marginBottom: 4 }}>Amount</Text>
                <Text variant="displaySmall" style={{ fontWeight: '800', color }}>
                  {isCredit ? '+' : '-'}{formatAmount(debt.amount)}
                </Text>
                {debt.due_date && (
                  <Text variant="bodySmall" style={{ color, opacity: 0.7, marginTop: 8 }}>
                    Due: {formatDueDate(debt.due_date)}
                  </Text>
                )}
              </Card.Content>
            </Card>

            {/* Actions */}
            <View style={styles.actionsRow}>
              <Button
                mode="contained"
                icon="check-circle"
                onPress={() => setShowSettleDialog(true)}
                style={[styles.actionBtn, { flex: 1 }]}
                contentStyle={{ height: 52 }}
                labelStyle={styles.actionLabel}
                buttonColor={color}
              >
                Mark Settled
              </Button>
            </View>

            <Button
              mode="outlined"
              icon="delete-outline"
              onPress={() => setShowDeleteDialog(true)}
              style={[styles.deleteBtn, { borderColor: theme.colors.error }]}
              textColor={theme.colors.error}
              contentStyle={{ height: 48 }}
            >
              Delete Debt
            </Button>
          </>
        )}
      </ScrollView>

      {/* Dialogs */}
      <Portal>
        <Dialog visible={showSettleDialog} onDismiss={() => setShowSettleDialog(false)} style={{ borderRadius: 24 }}>
          <Dialog.Title>Settle Debt</Dialog.Title>
          <Dialog.Content>
            <Text variant="bodyMedium">
              Mark this debt as settled?{' '}
              <Text style={{ fontWeight: '700' }}>{debt?.counterparty_name}</Text> —{' '}
              {formatAmount(debt?.amount || 0)}
            </Text>
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setShowSettleDialog(false)}>Cancel</Button>
            <Button mode="contained" onPress={handleSettle} loading={settling} disabled={settling}>
              Settle
            </Button>
          </Dialog.Actions>
        </Dialog>

        <Dialog visible={showDeleteDialog} onDismiss={() => setShowDeleteDialog(false)} style={{ borderRadius: 24 }}>
          <Dialog.Title>Delete Debt</Dialog.Title>
          <Dialog.Content>
            <Text variant="bodyMedium">Are you sure you want to delete this debt record?</Text>
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setShowDeleteDialog(false)}>Cancel</Button>
            <Button onPress={handleDelete} textColor={theme.colors.error}>Delete</Button>
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
  profileCard: { borderRadius: 24, marginTop: 8, marginBottom: 16 },
  avatar: { width: 88, height: 88, borderRadius: 44, alignItems: 'center', justifyContent: 'center', marginBottom: 12 },
  name: { fontWeight: '700', marginBottom: 8 },
  typeBadge: { paddingHorizontal: 16, paddingVertical: 6, borderRadius: 16 },
  amountCard: { borderRadius: 24, marginBottom: 24 },
  actionsRow: { flexDirection: 'row', gap: 12, marginBottom: 12 },
  actionBtn: { borderRadius: 24 },
  actionLabel: { fontWeight: '700', fontSize: 15 },
  deleteBtn: { borderRadius: 24 },
});
