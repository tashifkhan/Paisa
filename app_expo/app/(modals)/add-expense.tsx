import { MaterialCommunityIcons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import React, { useCallback, useEffect, useState } from 'react';
import { Keyboard, Pressable, ScrollView, StyleSheet, View, Dimensions } from 'react-native';
import { ActivityIndicator, Button, Portal, Snackbar, Surface, Text, TextInput, TouchableRipple, useTheme } from 'react-native-paper';
import { CustomSegmentedTabs } from '../../components/CustomSegmentedTabs';
import { categoryService } from '../../services/categoryService';
import { expenseService } from '../../services/expenseService';
import type { BackendCategory, BackendWallet } from '../../services/types';
import { walletService } from '../../services/walletService';

const { height: SCREEN_HEIGHT } = Dimensions.get('window');

function evaluateMath(expression: string): string {
  try {
    const sanitized = expression.replace(/x/g, '*').replace(/÷/g, '/');
    if (/^[0-9+\-*/. ]+$/.test(sanitized)) {
      // eslint-disable-next-line no-new-func
      const result = new Function(`return ${sanitized}`)();
      if (!isFinite(result)) return '';
      return result.toString();
    }
    return expression;
  } catch {
    return expression;
  }
}

export default function AddExpenseScreen() {
  const router = useRouter();
  const theme = useTheme();

  const [txType, setTxType] = useState<'expense' | 'income' | 'transfer'>('expense');
  const [expenseName, setExpenseName] = useState('');
  const [amountStr, setAmountStr] = useState('');
  const [description, setDescription] = useState('');
  const [date, setDate] = useState(new Date().toISOString().split('T')[0]);

  const [wallets, setWallets] = useState<BackendWallet[]>([]);
  const [categories, setCategories] = useState<BackendCategory[]>([]);
  const [selectedWallet, setSelectedWallet] = useState<BackendWallet | null>(null);
  const [selectedWalletTo, setSelectedWalletTo] = useState<BackendWallet | null>(null);
  const [selectedCategory, setSelectedCategory] = useState<BackendCategory | null>(null);

  const [expandedAccount, setExpandedAccount] = useState(false);
  const [expandedAccountTo, setExpandedAccountTo] = useState(false);
  const [expandedCategory, setExpandedCategory] = useState(true);
  const [expandedSplit, setExpandedSplit] = useState(false);
  const [expandedPerson, setExpandedPerson] = useState(false);

  // Calculator State
  const [showCalculator, setShowCalculator] = useState(false);
  const [calcExpr, setCalcExpr] = useState('');
  const [calcResult, setCalcResult] = useState('');

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [snack, setSnack] = useState('');

  const loadData = useCallback(async () => {
    try {
      const [ws, cats] = await Promise.all([
        walletService.getWallets(),
        categoryService.getCategories(),
      ]);
      setWallets(ws);
      setCategories(cats);
      if (ws.length > 0) {
        setSelectedWallet(ws[0]);
        const transferTarget = ws.find((w) => w.id !== ws[0].id) || null;
        setSelectedWalletTo(transferTarget);
      }
      if (cats.length > 0) setSelectedCategory(cats[0]);
    } catch {
      // silently fail
    }
  }, []);

  useEffect(() => {
    loadData().finally(() => setLoading(false));
  }, [loadData]);

  useEffect(() => {
    if (txType !== 'transfer') return;
    if (!selectedWallet) return;

    if (!selectedWalletTo || selectedWalletTo.id === selectedWallet.id) {
      const fallback = wallets.find((w) => w.id !== selectedWallet.id) || null;
      setSelectedWalletTo(fallback);
    }
  }, [txType, selectedWallet, selectedWalletTo, wallets]);

  const filteredCategories = categories.filter((c) => c.type === txType || c.type === 'both');

  const handleSubmit = async () => {
    const finalAmount = parseFloat(amountStr || calcResult || '0');
    if (!finalAmount || finalAmount <= 0) {
      setSnack('Please enter a valid amount');
      return;
    }
    if (!selectedWallet) {
      setSnack('Please select an account');
      return;
    }

    if (txType === 'transfer') {
      if (!selectedWalletTo) {
        setSnack('Please select destination account');
        return;
      }
      if (selectedWallet.id === selectedWalletTo.id) {
        setSnack('Transfer accounts must be different');
        return;
      }
    }

    setSaving(true);
    try {
      const note = expenseName || description || undefined;

      await expenseService.addTransaction({
        amount: finalAmount,
        type: txType,
        date: new Date(date).toISOString(),
        title: expenseName || undefined,
        description: description || undefined,
        note,
        wallet_id: selectedWallet?.id,
        to_wallet_id: txType === 'transfer' ? selectedWalletTo?.id : undefined,
        category_id: txType === 'transfer' ? undefined : selectedCategory?.id,
        currency: selectedWallet?.currency || 'INR',
        split_strategy: txType === 'transfer' ? undefined : 'equal',
      });
      router.back();
    } catch (e: any) {
      setSnack(e?.response?.data?.detail || 'Failed to add transaction');
    } finally {
      setSaving(false);
    }
  };

  const namePlaceholder = txType === 'expense' ? 'Expense name' : txType === 'income' ? 'Income name' : 'Transfer name';

  const handleCalcKey = (key: string) => {
    if (key === 'AC') {
      setCalcExpr('');
      setCalcResult('');
    } else if (key === '⌫') {
      const newExpr = calcExpr.slice(0, -1);
      setCalcExpr(newExpr);
      setCalcResult(evaluateMath(newExpr));
    } else if (key === '=') {
      if (calcResult) {
        setAmountStr(calcResult);
        setShowCalculator(false);
      }
    } else {
      const newExpr = calcExpr + key;
      setCalcExpr(newExpr);
      setCalcResult(evaluateMath(newExpr));
    }
  };

  return (
    <View style={[styles.container, { backgroundColor: theme.colors.background }]}>
      {/* Header */}
      <View style={styles.header}>
        <Pressable onPress={() => router.back()} style={{ padding: 8 }}>
          <MaterialCommunityIcons name="arrow-left" size={24} color={theme.colors.onSurface} />
        </Pressable>
        <Text variant="titleMedium" style={styles.headerTitle}>Transaction</Text>
        <View style={{ width: 40 }} />
      </View>

      <ScrollView contentContainerStyle={{ paddingHorizontal: 16, paddingBottom: 120 }} showsVerticalScrollIndicator={false}>
        {/* Tabs */}
        <CustomSegmentedTabs
          tabs={[
            { value: 'expense', label: 'Expense' },
            { value: 'income', label: 'Income' },
            { value: 'transfer', label: 'Transfer' },
          ]}
          value={txType}
          onValueChange={(v) => setTxType(v as any)}
          containerStyle={{ marginBottom: 16 }}
        />

        {/* Inputs */}
        <TextInput
          mode="outlined"
          placeholder={namePlaceholder}
          value={expenseName}
          onChangeText={setExpenseName}
          outlineColor="transparent"
          activeOutlineColor={theme.colors.primary}
          outlineStyle={{ borderRadius: 16, borderWidth: 0 }}
          style={{ backgroundColor: theme.colors.surfaceVariant, marginBottom: 16 }}
          textColor={theme.colors.onSurface}
        />
        
        <TextInput
          mode="outlined"
          placeholder="Amount (e.g. 1000.00)"
          value={amountStr}
          onChangeText={setAmountStr}
          keyboardType="decimal-pad"
          right={<TextInput.Icon icon="calculator" color={theme.colors.onSurfaceVariant} onPress={() => { Keyboard.dismiss(); setShowCalculator(true); }} />}
          onFocus={() => { setShowCalculator(false); }}
          outlineColor="transparent"
          activeOutlineColor={theme.colors.primary}
          outlineStyle={{ borderRadius: 16, borderWidth: 0 }}
          style={{ backgroundColor: theme.colors.surfaceVariant, marginBottom: 16 }}
          textColor={theme.colors.onSurface}
        />

        <TextInput
          mode="outlined"
          placeholder="Description"
          value={description}
          onChangeText={setDescription}
          outlineColor="transparent"
          activeOutlineColor={theme.colors.primary}
          outlineStyle={{ borderRadius: 16, borderWidth: 0 }}
          style={{ backgroundColor: theme.colors.surfaceVariant, marginBottom: 16 }}
          textColor={theme.colors.onSurface}
        />

        {/* Date Time Row */}
        <View style={styles.dateTimeRow}>
          <Pressable style={[styles.dateTimeBox, { backgroundColor: theme.colors.surfaceVariant }]}>
            <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6, marginBottom: 4 }}>
              <MaterialCommunityIcons name="calendar-blank" size={14} color={theme.colors.onSurfaceVariant} />
              <Text style={{ fontSize: 12, color: theme.colors.onSurfaceVariant }}>Date</Text>
            </View>
            <Text style={{ fontSize: 16, fontWeight: '600' }}>{date}</Text>
          </Pressable>

          <Pressable style={[styles.dateTimeBox, { backgroundColor: theme.colors.surfaceVariant }]}>
            <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6, marginBottom: 4 }}>
              <MaterialCommunityIcons name="clock-outline" size={14} color={theme.colors.onSurfaceVariant} />
              <Text style={{ fontSize: 12, color: theme.colors.onSurfaceVariant }}>Time</Text>
            </View>
            <Text style={{ fontSize: 16, fontWeight: '600' }}>Now</Text>
          </Pressable>
        </View>

        {/* Account Section (From) */}
        <View style={[styles.sectionContainer, { borderBottomColor: theme.colors.surfaceVariant, borderBottomWidth: 1 }]}>
          <Pressable onPress={() => setExpandedAccount(!expandedAccount)} style={styles.sectionHeader}>
            <View style={{ flexDirection: 'row', alignItems: 'center', gap: 16 }}>
              <MaterialCommunityIcons name="wallet-outline" size={24} color={theme.colors.onSurfaceVariant} />
              <View>
                <Text style={{ fontSize: 14, fontWeight: '600' }}>{txType === 'transfer' ? 'Transfer account from' : 'Account'}</Text>
                <Text style={{ fontSize: 12, color: selectedWallet && !expandedAccount ? theme.colors.primary : theme.colors.onSurfaceVariant, marginTop: 2, fontWeight: selectedWallet && !expandedAccount ? '600' : '400' }}>
                  {selectedWallet && !expandedAccount ? <><MaterialCommunityIcons name="bank" size={12} /> {selectedWallet.name}</> : 'Select account'}
                </Text>
              </View>
            </View>
            <MaterialCommunityIcons name={expandedAccount ? "chevron-up" : "chevron-down"} size={24} color={theme.colors.onSurfaceVariant} />
          </Pressable>
          
          {expandedAccount && (
            <View style={styles.chipGrid}>
              {wallets.map(w => (
                <Surface
                  key={w.id}
                  style={[styles.chip, { backgroundColor: selectedWallet?.id === w.id ? theme.colors.primaryContainer : theme.colors.surfaceContainer }]}
                  elevation={0}
                >
                  <TouchableRipple onPress={() => setSelectedWallet(w)} style={styles.chipInner}>
                    <Text style={{ color: selectedWallet?.id === w.id ? theme.colors.primary : theme.colors.onSurfaceVariant, fontWeight: '600', fontSize: 13 }}>
                      <MaterialCommunityIcons name={w.type === 'cash' ? 'cash' : 'bank'} size={14} /> {w.name}
                    </Text>
                  </TouchableRipple>
                </Surface>
              ))}
              <Surface style={[styles.chip, { backgroundColor: theme.colors.surfaceContainer }]} elevation={0}>
                <TouchableRipple onPress={() => {}} style={styles.chipInner}>
                  <Text style={{ color: theme.colors.onSurfaceVariant, fontWeight: '600', fontSize: 13 }}>
                    <MaterialCommunityIcons name="plus-circle-outline" size={14} /> Add
                  </Text>
                </TouchableRipple>
              </Surface>
            </View>
          )}
        </View>

        {/* Account Section (To - Only for Transfers) */}
        {txType === 'transfer' && (
          <View style={[styles.sectionContainer, { borderBottomColor: theme.colors.surfaceVariant, borderBottomWidth: 1 }]}>
            <Pressable onPress={() => setExpandedAccountTo(!expandedAccountTo)} style={styles.sectionHeader}>
              <View style={{ flexDirection: 'row', alignItems: 'center', gap: 16 }}>
                <MaterialCommunityIcons name="wallet-outline" size={24} color={theme.colors.onSurfaceVariant} />
                <View>
                  <Text style={{ fontSize: 14, fontWeight: '600' }}>Transfer account to</Text>
                  <Text style={{ fontSize: 12, color: selectedWalletTo && !expandedAccountTo ? theme.colors.primary : theme.colors.onSurfaceVariant, marginTop: 2, fontWeight: selectedWalletTo && !expandedAccountTo ? '600' : '400' }}>
                    {selectedWalletTo && !expandedAccountTo ? <><MaterialCommunityIcons name="bank" size={12} /> {selectedWalletTo.name}</> : 'Select account'}
                  </Text>
                </View>
              </View>
              <MaterialCommunityIcons name={expandedAccountTo ? "chevron-up" : "chevron-down"} size={24} color={theme.colors.onSurfaceVariant} />
            </Pressable>
            
            {expandedAccountTo && (
              <View style={styles.chipGrid}>
                {wallets.filter((w) => w.id !== selectedWallet?.id).map(w => (
                  <Surface
                    key={`to-${w.id}`}
                    style={[styles.chip, { backgroundColor: selectedWalletTo?.id === w.id ? theme.colors.primaryContainer : theme.colors.surfaceContainer }]}
                    elevation={0}
                  >
                    <TouchableRipple onPress={() => setSelectedWalletTo(w)} style={styles.chipInner}>
                      <Text style={{ color: selectedWalletTo?.id === w.id ? theme.colors.primary : theme.colors.onSurfaceVariant, fontWeight: '600', fontSize: 13 }}>
                        <MaterialCommunityIcons name={w.type === 'cash' ? 'cash' : 'bank'} size={14} /> {w.name}
                      </Text>
                    </TouchableRipple>
                  </Surface>
                ))}
                <Surface style={[styles.chip, { backgroundColor: theme.colors.surfaceContainer }]} elevation={0}>
                  <TouchableRipple onPress={() => {}} style={styles.chipInner}>
                    <Text style={{ color: theme.colors.onSurfaceVariant, fontWeight: '600', fontSize: 13 }}>
                      <MaterialCommunityIcons name="plus-circle-outline" size={14} /> Add
                    </Text>
                  </TouchableRipple>
                </Surface>
              </View>
            )}
          </View>
        )}

        {/* Category Section */}
        {txType !== 'transfer' && (
        <View style={[styles.sectionContainer, { borderBottomColor: theme.colors.surfaceVariant, borderBottomWidth: 1 }]}> 
          <Pressable onPress={() => setExpandedCategory(!expandedCategory)} style={styles.sectionHeader}>
            <View style={{ flexDirection: 'row', alignItems: 'center', gap: 16 }}>
              <MaterialCommunityIcons name="shape-outline" size={24} color={theme.colors.onSurfaceVariant} />
              <View>
                <Text style={{ fontSize: 14, fontWeight: '600' }}>{filteredCategories.length === 0 ? 'No categories' : 'Category'}</Text>
                <Text style={{ fontSize: 12, color: theme.colors.onSurfaceVariant, marginTop: 2 }}>
                  {filteredCategories.length === 0 ? 'Add categories to select' : (selectedCategory && !expandedCategory ? selectedCategory.name : 'Select category')}
                </Text>
              </View>
            </View>
            <MaterialCommunityIcons name={expandedCategory ? "chevron-up" : (filteredCategories.length === 0 ? "chevron-right" : "chevron-down")} size={24} color={theme.colors.onSurfaceVariant} />
          </Pressable>
          
          {expandedCategory && filteredCategories.length > 0 && (
            <View style={styles.chipGrid}>
              {loading ? <ActivityIndicator size="small" /> : filteredCategories.map(c => (
                <Surface
                  key={c.id}
                  style={[styles.chip, { backgroundColor: selectedCategory?.id === c.id ? theme.colors.primaryContainer : theme.colors.surfaceContainer }]}
                  elevation={0}
                >
                  <TouchableRipple onPress={() => setSelectedCategory(c)} style={styles.chipInner}>
                    <Text style={{ color: selectedCategory?.id === c.id ? theme.colors.primary : theme.colors.onSurfaceVariant, fontWeight: '600', fontSize: 13 }}>
                      <MaterialCommunityIcons name={c.icon || 'star'} size={14} color={c.color} /> {c.name}
                    </Text>
                  </TouchableRipple>
                </Surface>
              ))}
              {!loading && (
                <Surface style={[styles.chip, { backgroundColor: theme.colors.surfaceContainer }]} elevation={0}>
                  <TouchableRipple onPress={() => {}} style={styles.chipInner}>
                    <Text style={{ color: theme.colors.onSurfaceVariant, fontWeight: '600', fontSize: 13 }}>
                      <MaterialCommunityIcons name="plus-circle-outline" size={14} /> Add
                    </Text>
                  </TouchableRipple>
                </Surface>
              )}
            </View>
          )}
        </View>
        )}

        {/* Person Section (Transfers usually) */}
        {txType === 'expense' && (
          <View style={[styles.sectionContainer, { borderBottomColor: theme.colors.surfaceVariant, borderBottomWidth: 1 }]}> 
            <Pressable onPress={() => setExpandedPerson(!expandedPerson)} style={styles.sectionHeader}>
               <View style={{ flexDirection: 'row', alignItems: 'center', gap: 16 }}>
                 <MaterialCommunityIcons name="account-outline" size={24} color={theme.colors.onSurfaceVariant} />
                 <View>
                   <Text style={{ fontSize: 14, fontWeight: '600' }}>Person</Text>
                   <Text style={{ fontSize: 12, color: theme.colors.onSurfaceVariant, marginTop: 2 }}>Select a person</Text>
                 </View>
               </View>
               <MaterialCommunityIcons name={expandedPerson ? "chevron-up" : "chevron-down"} size={24} color={theme.colors.onSurfaceVariant} />
            </Pressable>
          </View>
        )}

        {/* Split Section */}
        {txType === 'expense' && (
          <View style={styles.sectionContainer}>
            <Pressable onPress={() => setExpandedSplit(!expandedSplit)} style={styles.sectionHeader}>
              <View style={{ flexDirection: 'row', alignItems: 'center', gap: 16 }}>
                <MaterialCommunityIcons name="account-group-outline" size={24} color={theme.colors.onSurfaceVariant} />
                <View>
                  <Text style={{ fontSize: 14, fontWeight: '600' }}>Split Expense</Text>
                  {!expandedSplit && (
                    <Text style={{ fontSize: 12, color: theme.colors.onSurfaceVariant, marginTop: 2 }}>You paid, split equally</Text>
                  )}
                </View>
              </View>
              <MaterialCommunityIcons name={expandedSplit ? "chevron-up" : "chevron-down"} size={24} color={theme.colors.onSurfaceVariant} />
            </Pressable>
            
            {expandedSplit && (
              <View style={{ marginTop: 16, gap: 12 }}>
                <View style={{ flexDirection: 'row', gap: 8 }}>
                  <Button mode="contained-tonal" style={{ flex: 1, backgroundColor: theme.colors.primaryContainer }} labelStyle={{ fontSize: 12, color: theme.colors.primary }} onPress={() => {}}>Equal</Button>
                  <Button mode="outlined" style={{ flex: 1 }} labelStyle={{ fontSize: 12 }} onPress={() => {}}>Exact</Button>
                  <Button mode="outlined" style={{ flex: 1 }} labelStyle={{ fontSize: 12 }} onPress={() => {}}>Ratio</Button>
                </View>
                <View style={{ padding: 16, backgroundColor: theme.colors.surfaceVariant, borderRadius: 16 }}>
                  <Text style={{ color: theme.colors.onSurfaceVariant, textAlign: 'center' }}>
                    Advanced splitting logic will be hooked up here. Support for ratios, percentages, and exact amounts is planned.
                  </Text>
                </View>
              </View>
            )}
          </View>
        )}

        {/* Receipt Box */}
        <View style={{ marginTop: 16 }}>
          <Text style={{ color: theme.colors.onSurfaceVariant, fontWeight: '600', marginBottom: 12, marginLeft: 4 }}>
            Receipt or Bill (optional) <MaterialCommunityIcons name="lock-outline" size={14} />
          </Text>
          <Pressable style={[styles.receiptBox, { backgroundColor: theme.colors.surfaceVariant }]}>
            <MaterialCommunityIcons name="camera-outline" size={32} color={theme.colors.onSurfaceVariant} style={{ marginBottom: 8 }} />
            <Text style={{ fontWeight: '600', color: theme.colors.onSurfaceVariant }}>Add Receipt or Bill</Text>
            <Text style={{ fontSize: 12, color: theme.colors.onSurfaceVariant, opacity: 0.6, marginTop: 4 }}>Tap to select an image</Text>
          </Pressable>
        </View>

      </ScrollView>

      {/* Floating Bottom Button */}
      <View style={[styles.bottomBar, { backgroundColor: theme.colors.background }]}>
        <View style={{ flexDirection: 'row', gap: 8, flex: 1 }}>
          <Button 
            mode="contained" 
            onPress={handleSubmit} 
            loading={saving}
            style={{ flex: 1, borderRadius: 16, backgroundColor: theme.colors.primary }}
            labelStyle={{ color: theme.colors.onPrimary, fontWeight: '700', fontSize: 16, paddingVertical: 8 }}
          >
            Add transaction
          </Button>
          <Button 
            mode="contained" 
            onPress={() => {}} 
            style={{ width: 64, borderRadius: 16, backgroundColor: theme.colors.primary }}
            labelStyle={{ color: theme.colors.onPrimary, marginHorizontal: 0, paddingVertical: 8 }}
          >
            <MaterialCommunityIcons name="chevron-up" size={24} />
          </Button>
        </View>
      </View>

      {/* Calculator Modal */}
      <Portal>
        {showCalculator && (
          <Pressable style={styles.calculatorOverlay} onPress={() => setShowCalculator(false)}>
            <Pressable 
              style={[styles.calculatorSheet, { backgroundColor: theme.colors.surface }]}
              onPress={(e) => e.stopPropagation()}
            >
              {/* Calc Display */}
              <View style={styles.calcHeader}>
                <Pressable onPress={() => setShowCalculator(false)} style={{ padding: 8, marginHorizontal: -8 }}>
                  <MaterialCommunityIcons name="close" size={24} color={theme.colors.onSurface} />
                </Pressable>
                <View style={{ alignItems: 'flex-end', flex: 1 }}>
                  <Text style={{ color: theme.colors.onSurfaceVariant, fontSize: 16, opacity: 0.8, minHeight: 20 }}>{calcExpr}</Text>
                  <Text style={{ color: theme.colors.onSurface, fontSize: 28, fontWeight: '700' }}>{calcResult || amountStr || '0'}</Text>
                </View>
              </View>

              {/* Calc Grid */}
              <View style={styles.calcGrid}>
                {['AC', '⌫', '÷', '7', '8', '9', 'x', '4', '5', '6', '-', '1', '2', '3', '+', '.', '0', '='].map((key, i) => {
                  let btnColor = theme.colors.surfaceVariant;
                  let textColor = theme.colors.onSurface;
                  let isWide = false;

                  if (key === 'AC') {
                    btnColor = theme.colors.errorContainer;
                    textColor = theme.colors.onErrorContainer;
                    isWide = true;
                  } else if (['÷', 'x', '-', '+'].includes(key)) {
                    btnColor = theme.colors.secondaryContainer;
                    textColor = theme.colors.onSecondaryContainer;
                  } else if (key === '=') {
                    btnColor = theme.colors.primary;
                    textColor = theme.colors.onPrimary;
                    isWide = true;
                  }

                  // 4 columns grid logic: AC takes 2 cols, = takes 2 cols
                  const widthPct = isWide ? '48%' : '23%';

                  return (
                    <Pressable 
                      key={i} 
                      onPress={() => handleCalcKey(key)}
                      style={[styles.calcBtn, { backgroundColor: btnColor, width: widthPct as `${number}%` }]}
                    >
                      {key === '⌫' ? (
                        <MaterialCommunityIcons name="backspace-outline" size={24} color={textColor} />
                      ) : (
                        <Text style={{ color: textColor, fontSize: 24, fontWeight: '600' }}>{key}</Text>
                      )}
                    </Pressable>
                  );
                })}
              </View>
            </Pressable>
          </Pressable>
        )}
      </Portal>

      <Snackbar visible={!!snack} onDismiss={() => setSnack('')} duration={3000}>{snack}</Snackbar>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 12, paddingTop: 52, paddingBottom: 16 },
  headerTitle: { fontWeight: '700', fontSize: 18, flex: 1, textAlign: 'center' },
  
  dateTimeRow: { flexDirection: 'row', gap: 12, marginBottom: 24 },
  dateTimeBox: {
    flex: 1,
    padding: 16,
    borderRadius: 16,
  },
  
  sectionContainer: {
    paddingVertical: 16,
  },
  sectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  chipGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 10,
    marginTop: 16,
    paddingLeft: 40, 
  },
  chip: {
    borderRadius: 20,
    overflow: 'hidden',
  },
  chipInner: {
    paddingHorizontal: 14,
    paddingVertical: 8,
  },
  receiptBox: {
    borderRadius: 24,
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 40,
    borderWidth: 1,
    borderColor: 'transparent',
  },
  
  bottomBar: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    paddingHorizontal: 16,
    paddingBottom: 32,
    paddingTop: 12,
  },

  calculatorOverlay: {
    position: 'absolute',
    top: 0, left: 0, right: 0, bottom: 0,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'flex-end',
  },
  calculatorSheet: {
    borderTopLeftRadius: 32,
    borderTopRightRadius: 32,
    padding: 24,
    paddingBottom: 40,
    elevation: 20,
  },
  calcHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 24,
  },
  calcGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
    gap: 8,
  },
  calcBtn: {
    height: 64,
    borderRadius: 20,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 4,
  },
});
