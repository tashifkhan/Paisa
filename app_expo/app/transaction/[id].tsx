import { MaterialCommunityIcons } from '@expo/vector-icons';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useLocalSearchParams, useRouter } from 'expo-router';
import React, { useState } from 'react';
import { ActivityIndicator, Alert, Pressable, ScrollView, StyleSheet, View } from 'react-native';
import { Button, Text, useTheme } from 'react-native-paper';
import { expenseService } from '@/services/expenseService';

export default function TransactionDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const router = useRouter();
  const theme = useTheme();
  const queryClient = useQueryClient();

  const { data: transaction, isLoading } = useQuery({
    queryKey: ['transaction', id],
    queryFn: () => expenseService.getTransaction(id),
    enabled: !!id,
  });

  const deleteMutation = useMutation({
    mutationFn: () => expenseService.deleteTransaction(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['transactions'] });
      queryClient.invalidateQueries({ queryKey: ['wallets'] });
      router.back();
    },
    onError: () => {
      Alert.alert('Error', 'Failed to delete transaction.');
    }
  });

  const handleDelete = () => {
    Alert.alert('Delete', 'Are you sure you want to delete this transaction?', [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Delete', style: 'destructive', onPress: () => deleteMutation.mutate() }
    ]);
  };

  if (isLoading || !transaction) {
    return (
      <View style={[styles.container, { backgroundColor: theme.colors.background, justifyContent: 'center', alignItems: 'center' }]}>
        <ActivityIndicator size="large" color={theme.colors.primary} />
      </View>
    );
  }

  const txDate = new Date(transaction.date || new Date());
  const dateStr = txDate.toLocaleDateString('en-US', { weekday: 'short', day: '2-digit', month: 'short', year: 'numeric' });
  const timeStr = txDate.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
  const isExpense = transaction.type === 'expense';

  return (
    <View style={[styles.container, { backgroundColor: theme.colors.background }]}>
      {/* Header card (matches modal-like header look) */}
      <View style={[styles.heroCard, { backgroundColor: theme.colors.surface }]}>
        {/* Back Button Placeholder */}
        <Pressable onPress={() => router.back()} style={{ position: 'absolute', top: 56, right: 24, zIndex: 10 }}>
           <MaterialCommunityIcons name="close-circle" size={28} color={theme.colors.onSurfaceVariant} />
        </Pressable>

        <View style={styles.heroRow}>
           <View style={[styles.iconLarge, { backgroundColor: isExpense ? '#1a2e1d' : '#3b0764' }]}>
             <MaterialCommunityIcons name={transaction.category_id ? "car" : "gift"} size={28} color={isExpense ? '#4ade80' : '#a855f7'} />
           </View>
           <View style={{ flex: 1 }}>
             <Text style={{ fontSize: 20, fontWeight: '700', color: theme.colors.onSurface }}>
               {transaction.title || transaction.note || 'Transaction'}
             </Text>
             <Text style={{ fontSize: 13, color: theme.colors.onSurfaceVariant, marginTop: 4 }}>
               {dateStr} at {timeStr}
             </Text>
           </View>
           <Text style={{ fontSize: 20, fontWeight: '700', color: isExpense ? '#ef4444' : '#10b981' }}>
             {isExpense ? '-' : ''}₹{transaction.amount.toFixed(2)}
           </Text>
        </View>
      </View>

      <ScrollView contentContainerStyle={{ paddingHorizontal: 24, paddingTop: 24 }}>
        <Text style={{ fontSize: 16, fontWeight: '700', color: theme.colors.primary, marginBottom: 24 }}>
          Transaction Details
        </Text>

        {/* Info Rows */}
        <View style={styles.infoRow}>
          <MaterialCommunityIcons name="bank-outline" size={24} color={theme.colors.onSurfaceVariant} style={{ width: 40 }} />
          <View>
            <Text style={{ fontSize: 13, color: theme.colors.onSurfaceVariant }}>Account</Text>
            <Text style={{ fontSize: 16, fontWeight: '600', color: theme.colors.onSurface, marginTop: 2 }}>Bank</Text>
          </View>
        </View>

        <View style={styles.infoRow}>
          <MaterialCommunityIcons name="car-outline" size={24} color={theme.colors.onSurfaceVariant} style={{ width: 40 }} />
          <View>
            <Text style={{ fontSize: 13, color: theme.colors.onSurfaceVariant }}>Category</Text>
            <Text style={{ fontSize: 16, fontWeight: '600', color: theme.colors.onSurface, marginTop: 2 }}>Transport</Text>
          </View>
        </View>

        <View style={styles.infoRow}>
          <MaterialCommunityIcons name="file-document-outline" size={24} color={theme.colors.onSurfaceVariant} style={{ width: 40 }} />
          <View>
            <Text style={{ fontSize: 13, color: theme.colors.onSurfaceVariant }}>Description</Text>
            <Text style={{ fontSize: 16, fontWeight: '600', color: theme.colors.onSurface, marginTop: 2 }}>
              {transaction.note || transaction.description || 'No description provided'}
            </Text>
          </View>
        </View>

        <Text style={{ fontSize: 12, color: theme.colors.onSurfaceVariant, marginTop: 24 }}>
          Created on {txDate.toLocaleDateString('en-US', { month: 'long', day: '2-digit', year: 'numeric' })}
        </Text>
      </ScrollView>

      {/* Action Footer */}
      <View style={[styles.footer, { borderTopColor: theme.colors.surfaceVariant, borderTopWidth: 1, backgroundColor: theme.colors.background }]}>
        <Button 
          onPress={handleDelete}
          mode="contained-tonal" 
          icon={() => <MaterialCommunityIcons name="trash-can-outline" size={20} color={theme.colors.error} />}
          labelStyle={{ color: theme.colors.error, fontWeight: '600' }}
          style={{ width: '90%' }}
        >
          Delete Transaction
        </Button>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  heroCard: {
    paddingTop: 100,
    paddingBottom: 24,
    paddingHorizontal: 24,
    borderBottomLeftRadius: 32,
    borderBottomRightRadius: 32,
    elevation: 4,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.1,
    shadowRadius: 12,
  },
  heroRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 16,
  },
  iconLarge: {
    width: 56,
    height: 56,
    borderRadius: 28,
    alignItems: 'center',
    justifyContent: 'center',
  },
  infoRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 24,
  },
  footer: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    paddingVertical: 16,
    paddingBottom: 32,
  }
});
