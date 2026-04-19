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
      <View style={[styles.container, { justifyContent: 'center', alignItems: 'center' }]}>
        <ActivityIndicator size="large" color={theme.colors.primary} />
      </View>
    );
  }

  const txDate = new Date(transaction.date || new Date());
  const dateStr = txDate.toLocaleDateString('en-US', { weekday: 'short', day: '2-digit', month: 'short', year: 'numeric' });
  const timeStr = txDate.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
  const isExpense = transaction.type === 'expense';

  return (
    <View style={styles.container}>
      <Pressable style={StyleSheet.absoluteFill} onPress={() => router.back()} />
      <View style={[styles.sheet, { backgroundColor: theme.colors.background }]}>
        
        {/* Drag handle */}
        <View style={styles.handleContainer}>
          <View style={[styles.handle, { backgroundColor: theme.colors.outlineVariant }]} />
        </View>

        {/* Header card */}
        <View style={styles.heroRow}>
           <View style={[styles.iconLarge, { backgroundColor: isExpense ? '#3f1f25' : '#1f3f2d' }]}>
             <MaterialCommunityIcons name={transaction.category_id ? "car" : "gift"} size={28} color={isExpense ? '#ef4444' : '#10b981'} />
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
            <MaterialCommunityIcons name="shape-outline" size={24} color={theme.colors.onSurfaceVariant} style={{ width: 40 }} />
            <View>
              <Text style={{ fontSize: 13, color: theme.colors.onSurfaceVariant }}>Category</Text>
              <Text style={{ fontSize: 16, fontWeight: '600', color: theme.colors.onSurface, marginTop: 2 }}>General</Text>
            </View>
          </View>

          <View style={styles.infoRow}>
            <MaterialCommunityIcons name="file-document-outline" size={24} color={theme.colors.onSurfaceVariant} style={{ width: 40 }} />
            <View flex={1}>
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
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'flex-end',
  },
  sheet: {
    borderTopLeftRadius: 32,
    borderTopRightRadius: 32,
    paddingTop: 8,
    maxHeight: '90%',
  },
  handleContainer: {
    alignItems: 'center',
    paddingVertical: 12,
  },
  handle: {
    width: 40,
    height: 4,
    borderRadius: 2,
    opacity: 0.5,
  },
  heroRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 16,
    paddingHorizontal: 24,
    paddingBottom: 24,
    paddingTop: 8,
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
