import { MaterialCommunityIcons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import React, { useState } from 'react';
import { Keyboard, Pressable, ScrollView, StyleSheet, View } from 'react-native';
import { Button, Text, TextInput, useTheme } from 'react-native-paper';
import { CustomSegmentedTabs } from '../../components/CustomSegmentedTabs';
import { debtService } from '../../services/debtService';
import { useQueryClient } from '@tanstack/react-query';

const InputField = ({ placeholder, value, onChangeText, leftIcon, style, keyboardType, prefix, theme }: any) => (
  <View style={[styles.inputContainer, { backgroundColor: theme.colors.surfaceVariant }, style]}>
    {leftIcon && (
      <View style={styles.leftIconWrapper}>
        <MaterialCommunityIcons name={leftIcon} size={20} color={theme.colors.primary} />
      </View>
    )}
    {prefix && (
      <Text style={{ fontSize: 24, fontWeight: '500', marginLeft: 16, marginRight: 4, color: theme.colors.onSurface }}>{prefix}</Text>
    )}
    <TextInput
      mode="outlined"
      placeholder={placeholder}
      value={value}
      onChangeText={onChangeText}
      keyboardType={keyboardType}
      outlineColor="transparent"
      activeOutlineColor="transparent"
      style={[{ backgroundColor: 'transparent', flex: 1, fontSize: prefix ? 24 : 16 }]}
      textColor={theme.colors.onSurface}
    />
  </View>
);

export default function AddDebtScreen() {
  const router = useRouter();
  const theme = useTheme();
  const queryClient = useQueryClient();

  const [counterpartyName, setCounterpartyName] = useState('');
  const [amount, setAmount] = useState('');
  const [debtType, setDebtType] = useState<'owed_to_me' | 'owed_by_me'>('owed_to_me');
  const [dueDate, setDueDate] = useState(''); // Mapped conditionally later if needed
  const [saving, setSaving] = useState(false);

  const handleSubmit = async () => {
    if (!counterpartyName.trim() || !amount.trim()) return;
    setSaving(true);
    try {
      await debtService.addDebt({
        counterparty_name: counterpartyName,
        amount: parseFloat(amount),
        type: debtType,
      });
      // Invalidate debts queries
      router.back();
    } catch {
      //
    } finally {
      setSaving(false);
    }
  };

  return (
    <View style={[styles.container, { backgroundColor: theme.colors.background }]}>
      {/* Header */}
      <View style={styles.header}>
        <Pressable onPress={() => router.back()} style={{ padding: 8 }}>
          <MaterialCommunityIcons name="arrow-left" size={24} color={theme.colors.onSurface} />
        </Pressable>
        <Text variant="titleMedium" style={styles.headerTitle}>Record Debt</Text>
        <View style={{ width: 40 }} />
      </View>

      <ScrollView contentContainerStyle={{ paddingHorizontal: 16, paddingBottom: 120 }} showsVerticalScrollIndicator={false}>
        {/* Tabs */}
        <CustomSegmentedTabs
          tabs={[
            { value: 'owed_to_me', label: 'They owe me' },
            { value: 'owed_by_me', label: 'I owe them' },
          ]}
          value={debtType}
          onValueChange={(v) => setDebtType(v as any)}
          containerStyle={{ marginBottom: 24 }}
        />

        {/* Inputs */}
        <InputField 
          placeholder="Person's Name" 
          value={counterpartyName} 
          onChangeText={setCounterpartyName} 
          leftIcon="account-outline"
          theme={theme}
        />

        <InputField 
          placeholder="0.00" 
          value={amount} 
          onChangeText={setAmount}
          keyboardType="decimal-pad"
          prefix="₹"
          theme={theme}
        />

        {/* Mock for Due Date */}
        <Pressable style={styles.listRow}>
          <MaterialCommunityIcons name="calendar-outline" size={24} color={theme.colors.onSurfaceVariant} style={styles.rowIcon} />
          <View style={{ flex: 1 }}>
             <Text style={{ fontSize: 14, fontWeight: '600', color: theme.colors.onSurface }}>Due Date</Text>
             <Text style={{ fontSize: 12, color: theme.colors.onSurfaceVariant, marginTop: 2 }}>No due date set</Text>
          </View>
          <MaterialCommunityIcons name="chevron-right" size={24} color={theme.colors.onSurfaceVariant} />
        </Pressable>

        {/* Informational Text */}
        <View style={[styles.infoBox, { backgroundColor: theme.colors.surfaceVariant, marginTop: 24 }]}>
          <MaterialCommunityIcons name="information-outline" size={20} color={theme.colors.onSurfaceVariant} style={{ marginRight: 12 }} />
          <Text style={{ flex: 1, fontSize: 13, color: theme.colors.onSurfaceVariant }}>
            {debtType === 'owed_to_me' 
              ? "This will add a positive balance to your net worth." 
              : "This will deduct from your net balance as a liability."}
          </Text>
        </View>

      </ScrollView>

      {/* Fixed Bottom Button */}
      <View style={[styles.bottomBar, { backgroundColor: theme.colors.background }]}>
        <Button 
          mode="contained" 
          onPress={handleSubmit} 
          loading={saving}
          disabled={!counterpartyName.trim() || !amount.trim() || saving}
          style={{ borderRadius: 16, backgroundColor: debtType === 'owed_to_me' ? '#16a34a' : theme.colors.primary }}
          labelStyle={{ color: '#fff', fontWeight: '700', fontSize: 16, paddingVertical: 8 }}
          icon="check-circle-outline"
        >
          Save Debt
        </Button>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 12, paddingTop: 52, paddingBottom: 16 },
  headerTitle: { fontWeight: '700', fontSize: 18, flex: 1, textAlign: 'center' },
  
  inputContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    borderRadius: 16,
    marginBottom: 16,
    paddingHorizontal: 8,
  },
  leftIconWrapper: {
    padding: 8,
    marginRight: 4,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.1)',
  },
  
  listRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 16,
  },
  rowIcon: {
    marginRight: 16,
  },
  infoBox: {
    padding: 16,
    borderRadius: 16,
    flexDirection: 'row',
    alignItems: 'center',
  },
  bottomBar: {
    position: 'absolute',
    bottom: 0, left: 0, right: 0,
    paddingHorizontal: 16,
    paddingBottom: 32,
    paddingTop: 12,
  },
});
