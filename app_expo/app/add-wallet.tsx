import { MaterialCommunityIcons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import React, { useState } from 'react';
import { Keyboard, Pressable, ScrollView, StyleSheet, View } from 'react-native';
import { Button, Surface, Text, TextInput, TouchableRipple, useTheme } from 'react-native-paper';
import { CustomSegmentedTabs } from '../components/CustomSegmentedTabs';
import { MD3Switch } from '../components/MD3Switch';
import { useCreateWallet } from '../hooks/useWallets';

export default function AddWalletScreen() {
  const router = useRouter();
  const theme = useTheme();
  const createWalletMutation = useCreateWallet();

  const [walletType, setWalletType] = useState<'bank' | 'cash' | 'credit card'>('bank');
  const [walletName, setWalletName] = useState('');
  const [initialAmount, setInitialAmount] = useState('');
  const [accountNumber, setAccountNumber] = useState('');
  const [isDefault, setIsDefault] = useState(false);
  const [isExcluded, setIsExcluded] = useState(false);
  
  // Mocks
  const [expandedCurrency, setExpandedCurrency] = useState(false);
  const [expandedParent, setExpandedParent] = useState(false);

  // Colors
  const [selectedColor, setSelectedColor] = useState('#22d3ee'); // Default wheel color
  const MOCK_COLORS = ['#ef4444', '#ec4899', '#a855f7', '#6366f1', '#3b82f6', '#0ea5e9', '#06b6d4'];

  const handleSubmit = async () => {
    if (!walletName.trim()) return;
    try {
      await createWalletMutation.mutateAsync({
        name: walletName,
        type: walletType,
        currency: 'INR', // From mock
      });
      router.back();
    } catch {
      // Handle error gracefully
    }
  };

  const InputField = ({ placeholder, value, onChangeText, leftIcon, style, keyboardType }: any) => (
    <View style={[styles.inputContainer, { backgroundColor: theme.colors.surfaceVariant }, style]}>
      {leftIcon && (
        <View style={styles.leftIconWrapper}>
          <MaterialCommunityIcons name={leftIcon} size={20} color={selectedColor} />
        </View>
      )}
      <TextInput
        mode="outlined"
        placeholder={placeholder}
        value={value}
        onChangeText={onChangeText}
        keyboardType={keyboardType}
        outlineColor="transparent"
        activeOutlineColor="transparent"
        style={[{ backgroundColor: 'transparent', flex: 1 }]}
        textColor={theme.colors.onSurface}
      />
    </View>
  );

  return (
    <View style={[styles.container, { backgroundColor: theme.colors.background }]}>
      {/* Header */}
      <View style={styles.header}>
        <Pressable onPress={() => router.back()} style={{ padding: 8 }}>
          <MaterialCommunityIcons name="arrow-left" size={24} color={theme.colors.onSurface} />
        </Pressable>
        <Text variant="titleMedium" style={styles.headerTitle}>Account</Text>
        <View style={{ width: 40 }} />
      </View>

      <ScrollView contentContainerStyle={{ paddingHorizontal: 16, paddingBottom: 120 }} showsVerticalScrollIndicator={false}>
        {/* Tabs */}
        <CustomSegmentedTabs
          tabs={[
            { value: 'bank', label: 'Bank' },
            { value: 'cash', label: 'Cash' },
            { value: 'credit card', label: 'Credit Card' },
          ]}
          value={walletType}
          onValueChange={(v) => setWalletType(v as any)}
          containerStyle={{ marginBottom: 24 }}
        />

        {/* Inputs */}
        <InputField 
          placeholder="Enter account name" 
          value={walletName} 
          onChangeText={setWalletName} 
          leftIcon="credit-card-outline"
        />
        
        <InputField 
          placeholder="Enter name" 
          value={walletName} // Visually mapping to mock input
          onChangeText={setWalletName} 
        />

        <View style={{ flexDirection: 'row', gap: 12 }}>
          <InputField 
            placeholder="Enter amount" 
            value={initialAmount} 
            onChangeText={setInitialAmount}
            keyboardType="decimal-pad"
            style={{ flex: 1 }}
          />
          <InputField 
            placeholder="Account number" 
            value={accountNumber} 
            onChangeText={setAccountNumber}
            keyboardType="number-pad"
            style={{ flex: 1 }}
          />
        </View>

        {/* Currency Picker Mock */}
        <Pressable style={styles.listRow} onPress={() => setExpandedCurrency(!expandedCurrency)}>
          <MaterialCommunityIcons name="currency-inr" size={24} color={theme.colors.onSurfaceVariant} style={styles.rowIcon} />
          <View style={{ flex: 1 }}>
             <Text style={{ fontSize: 14, fontWeight: '600', color: theme.colors.onSurface }}>Account currency <MaterialCommunityIcons name="lock-outline" size={12} /></Text>
             <Text style={{ fontSize: 12, color: theme.colors.onSurfaceVariant, marginTop: 2 }}>INR (₹)</Text>
          </View>
          <MaterialCommunityIcons name="chevron-right" size={24} color={theme.colors.onSurfaceVariant} />
        </Pressable>

        {/* Parent Account Mock */}
        <Pressable style={styles.listRow} onPress={() => setExpandedParent(!expandedParent)}>
          <MaterialCommunityIcons name="bank-outline" size={24} color={theme.colors.onSurfaceVariant} style={styles.rowIcon} />
          <View style={{ flex: 1 }}>
             <Text style={{ fontSize: 14, fontWeight: '600', color: theme.colors.onSurface }}>Parent Account</Text>
             <Text style={{ fontSize: 12, color: theme.colors.onSurfaceVariant, marginTop: 2 }}>Select account</Text>
          </View>
          <MaterialCommunityIcons name="chevron-down" size={24} color={theme.colors.onSurfaceVariant} />
        </Pressable>

        {/* Switches */}
        <View style={styles.listRow}>
          <MaterialCommunityIcons name="star-outline" size={24} color={theme.colors.onSurfaceVariant} style={styles.rowIcon} />
          <View style={{ flex: 1, paddingRight: 16 }}>
             <Text style={{ fontSize: 14, fontWeight: '600', color: theme.colors.onSurface }}>Set default account</Text>
             <Text style={{ fontSize: 12, color: theme.colors.onSurfaceVariant, marginTop: 2 }}>Default account will be selected while adding transaction</Text>
          </View>
          <MD3Switch value={isDefault} onValueChange={setIsDefault} />
        </View>

        <View style={styles.listRow}>
          <MaterialCommunityIcons name="cancel" size={24} color={theme.colors.onSurfaceVariant} style={styles.rowIcon} />
          <View style={{ flex: 1, paddingRight: 16 }}>
             <Text style={{ fontSize: 14, fontWeight: '600', color: theme.colors.onSurface }}>Exclude account</Text>
             <Text style={{ fontSize: 12, color: theme.colors.onSurfaceVariant, marginTop: 2 }}>Transactions are not calculated in the balance and other places</Text>
          </View>
          <MD3Switch value={isExcluded} onValueChange={setIsExcluded} />
        </View>

        {/* Colors Picker */}
        <View style={{ marginTop: 16 }}>
          <Text style={{ fontSize: 14, fontWeight: '600', color: theme.colors.onSurface, marginBottom: 12 }}>Colors</Text>
          
          <CustomSegmentedTabs
            tabs={[
              { value: 'primary', label: 'Primary' },
              { value: 'accent', label: 'Accent' },
              { value: 'wheel', label: 'Wheel' },
            ]}
            value={'primary'}
            onValueChange={() => {}}
            containerStyle={{ marginBottom: 16 }}
          />

          <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: 12, paddingBottom: 16 }}>
            {MOCK_COLORS.map(c => (
              <Pressable 
                key={c} 
                onPress={() => setSelectedColor(c)}
                style={[styles.colorCircle, { backgroundColor: c }]}
              >
                {selectedColor === c && <MaterialCommunityIcons name="check" size={20} color="#fff" />}
              </Pressable>
            ))}
          </ScrollView>
        </View>

      </ScrollView>

      {/* Fixed Bottom Button */}
      <View style={[styles.bottomBar, { backgroundColor: theme.colors.background }]}>
        <Button 
          mode="contained" 
          onPress={handleSubmit} 
          loading={createWalletMutation.isPending}
          disabled={!walletName.trim() || createWalletMutation.isPending}
          style={{ borderRadius: 16, backgroundColor: theme.colors.primary }}
          labelStyle={{ color: theme.colors.onPrimary, fontWeight: '700', fontSize: 16, paddingVertical: 8 }}
          icon="content-save-outline"
        >
          Add Account
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
  
  colorCircle: {
    width: 48,
    height: 48,
    borderRadius: 24,
    alignItems: 'center',
    justifyContent: 'center',
  },
  
  bottomBar: {
    position: 'absolute',
    bottom: 0, left: 0, right: 0,
    paddingHorizontal: 16,
    paddingBottom: 32,
    paddingTop: 12,
  },
});
