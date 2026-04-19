import { useRouter } from 'expo-router';
import React, { useCallback, useEffect, useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { ActivityIndicator, Button, Card, Chip, Snackbar, Text, TextInput, useTheme } from 'react-native-paper';
import { groupService } from '../../services/groupService';
import { userService } from '../../services/userService';
import type { BackendUser } from '../../services/types';

const CURRENCIES = ['INR', 'USD', 'EUR', 'GBP'];

export default function CreateGroupScreen() {
  const router = useRouter();
  const theme = useTheme();

  const [groupName, setGroupName] = useState('');
  const [currency, setCurrency] = useState('INR');
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<BackendUser[]>([]);
  const [selectedUsers, setSelectedUsers] = useState<BackendUser[]>([]);
  const [searching, setSearching] = useState(false);
  const [saving, setSaving] = useState(false);
  const [snack, setSnack] = useState('');

  const searchUsers = useCallback(async (q: string) => {
    if (!q.trim()) { setSearchResults([]); return; }
    setSearching(true);
    try {
      const results = await userService.searchUsers(q);
      setSearchResults(results);
    } catch {
      setSearchResults([]);
    } finally {
      setSearching(false);
    }
  }, []);

  useEffect(() => {
    const t = setTimeout(() => searchUsers(searchQuery), 400);
    return () => clearTimeout(t);
  }, [searchQuery, searchUsers]);

  const toggleUser = (u: BackendUser) => {
    setSelectedUsers((prev) =>
      prev.find((x) => x.id === u.id) ? prev.filter((x) => x.id !== u.id) : [...prev, u]
    );
  };

  const handleCreate = async () => {
    if (!groupName.trim()) {
      setSnack('Please enter a group name');
      return;
    }
    setSaving(true);
    try {
      const result = await groupService.createGroup({ name: groupName, base_currency: currency });
      // Add selected members
      for (const u of selectedUsers) {
        try {
          await groupService.addMember(result.id, { user_id: u.id });
        } catch {
          // ignore individual member add failures
        }
      }
      router.replace({ pathname: '/group-detail', params: { groupId: result.id } });
    } catch (e: any) {
      setSnack(e?.response?.data?.detail || 'Failed to create group');
    } finally {
      setSaving(false);
    }
  };

  return (
    <View style={[styles.container, { backgroundColor: theme.colors.background }]}>
      {/* Header */}
      <View style={styles.header}>
        <Button mode="text" icon="close" onPress={() => router.back()} compact>Close</Button>
        <Text variant="titleMedium" style={styles.headerTitle}>Create Group</Text>
        <Button
          mode="contained"
          onPress={handleCreate}
          loading={saving}
          disabled={saving || !groupName.trim()}
          style={styles.createBtn}
          compact
        >
          Create
        </Button>
      </View>

      <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
        {/* Group Name */}
        <TextInput
          mode="outlined"
          label="Group Name"
          value={groupName}
          onChangeText={setGroupName}
          placeholder="e.g. Goa Trip"
          left={<TextInput.Icon icon="account-group-outline" />}
          style={styles.input}
        />

        {/* Currency */}
        <View style={styles.field}>
          <Text variant="labelLarge" style={[styles.fieldLabel, { color: theme.colors.onSurfaceVariant }]}>
            Base Currency
          </Text>
          <View style={styles.chipsRow}>
            {CURRENCIES.map((c) => (
              <Chip
                key={c}
                selected={currency === c}
                onPress={() => setCurrency(c)}
                style={currency === c ? { backgroundColor: theme.colors.primaryContainer } : { backgroundColor: theme.colors.surfaceVariant }}
                textStyle={currency === c ? { color: theme.colors.primary, fontWeight: '700' } : {}}
              >
                {c}
              </Chip>
            ))}
          </View>
        </View>

        {/* Member Search */}
        <View style={styles.field}>
          <Text variant="labelLarge" style={[styles.fieldLabel, { color: theme.colors.onSurfaceVariant }]}>
            Add Members
          </Text>
          <TextInput
            mode="outlined"
            label="Search by name or email"
            value={searchQuery}
            onChangeText={setSearchQuery}
            left={<TextInput.Icon icon="magnify" />}
            right={searching ? <TextInput.Icon icon="loading" /> : undefined}
            style={styles.input}
          />

          {/* Search Results */}
          {searchResults.length > 0 && (
            <Card style={{ borderRadius: 16, marginBottom: 12 }} elevation={1}>
              {searchResults.map((u) => (
                <Card.Content key={u.id} style={styles.userRow}>
                  <View style={[styles.userAvatar, { backgroundColor: theme.colors.primaryContainer }]}>
                    <Text style={{ fontWeight: '700', color: theme.colors.primary }}>
                      {(u.name || u.email).charAt(0).toUpperCase()}
                    </Text>
                  </View>
                  <View style={{ flex: 1 }}>
                    <Text variant="titleSmall" style={{ fontWeight: '600' }}>{u.name || 'User'}</Text>
                    <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>{u.email}</Text>
                  </View>
                  <Button
                    mode={selectedUsers.find((x) => x.id === u.id) ? 'contained' : 'outlined'}
                    compact
                    onPress={() => toggleUser(u)}
                    style={{ borderRadius: 12 }}
                  >
                    {selectedUsers.find((x) => x.id === u.id) ? 'Added' : 'Add'}
                  </Button>
                </Card.Content>
              ))}
            </Card>
          )}

          {/* Selected Members */}
          {selectedUsers.length > 0 && (
            <View>
              <Text variant="labelMedium" style={{ color: theme.colors.onSurfaceVariant, marginBottom: 8 }}>
                Selected ({selectedUsers.length})
              </Text>
              <View style={styles.selectedChips}>
                {selectedUsers.map((u) => (
                  <Chip
                    key={u.id}
                    onClose={() => toggleUser(u)}
                    style={{ backgroundColor: theme.colors.primaryContainer }}
                    textStyle={{ color: theme.colors.primary }}
                  >
                    {u.name || u.email}
                  </Chip>
                ))}
              </View>
            </View>
          )}
        </View>
      </ScrollView>

      <Snackbar visible={!!snack} onDismiss={() => setSnack('')} duration={3000}>{snack}</Snackbar>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 16, paddingTop: 52, paddingBottom: 12 },
  headerTitle: { fontWeight: '700' },
  createBtn: { borderRadius: 20 },
  scroll: { padding: 20, paddingBottom: 40 },
  input: { borderRadius: 16, marginBottom: 16 },
  field: { marginBottom: 20 },
  fieldLabel: { marginBottom: 8, fontWeight: '600' },
  chipsRow: { flexDirection: 'row', gap: 8, flexWrap: 'wrap' },
  userRow: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 10 },
  userAvatar: { width: 40, height: 40, borderRadius: 20, alignItems: 'center', justifyContent: 'center' },
  selectedChips: { flexDirection: 'row', gap: 8, flexWrap: 'wrap' },
});
