import { useRouter } from 'expo-router';
import { useColorScheme } from 'nativewind';
import React, { useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { Button, Dialog, List, Portal, Snackbar, Text, useTheme, IconButton } from 'react-native-paper';
import { useAuth } from '../../context/AuthContext';
import { userService } from '../../services/userService';
import { MD3Switch } from '../../components/MD3Switch';

const CURRENCIES = ['INR', 'USD', 'EUR', 'GBP', 'AED', 'JPY'];
const LANGUAGES = ['English', 'Hindi'];

export default function ProfileScreen() {
  const router = useRouter();
  const theme = useTheme();
  const { colorScheme, toggleColorScheme } = useColorScheme();
  const isDark = colorScheme === 'dark';
  const { user, logout, refreshUser } = useAuth();

  const [currency, setCurrency] = useState(user?.currency || 'INR');
  const [language, setLanguage] = useState(user?.language || 'English');
  const [notifications, setNotifications] = useState(true);
  const [saving, setSaving] = useState(false);
  const [snack, setSnack] = useState('');

  // Dialogs
  const [showCurrencyDialog, setShowCurrencyDialog] = useState(false);
  const [showLanguageDialog, setShowLanguageDialog] = useState(false);
  const [showLogoutDialog, setShowLogoutDialog] = useState(false);

  const handleUpdateCurrency = async (c: string) => {
    setCurrency(c);
    setShowCurrencyDialog(false);
    setSaving(true);
    try {
      await userService.updateCurrentUser({ currency: c });
      await refreshUser();
      setSnack('Currency updated');
    } catch {
      setSnack('Failed to update currency');
    } finally {
      setSaving(false);
    }
  };

  const handleUpdateLanguage = async (l: string) => {
    setLanguage(l);
    setShowLanguageDialog(false);
    setSaving(true);
    try {
      await userService.updateCurrentUser({ language: l });
      await refreshUser();
      setSnack('Language updated');
    } catch {
      setSnack('Failed to update language');
    } finally {
      setSaving(false);
    }
  };

  const handleLogout = async () => {
    setShowLogoutDialog(false);
    await logout();
    router.replace('/(auth)/signin');
  };

  const displayName = user?.name || 'User';

  const renderItem = ({ title, icon, value, onPress, rightContent }: any) => (
    <View style={[styles.listItemContainer, { backgroundColor: theme.colors.elevation.level1 }]}>
      <List.Item
        title={title}
        titleStyle={{ fontWeight: '600', fontSize: 16 }}
        left={(props) => <List.Icon {...props} icon={icon} color={theme.colors.onSurfaceVariant} />}
        right={(props) => (
          <View style={{ flexDirection: 'row', alignItems: 'center' }}>
            {rightContent}
            {value ? (
              <>
                <Text style={{ color: theme.colors.onSurfaceVariant, marginRight: 2 }}>{value}</Text>
                <List.Icon {...props} icon="chevron-right" color={theme.colors.onSurfaceVariant} />
              </>
            ) : null}
            {!rightContent && !value ? (
              <List.Icon {...props} icon="chevron-right" color={theme.colors.onSurfaceVariant} />
            ) : null}
          </View>
        )}
        onPress={onPress}
        style={styles.listItem}
      />
    </View>
  );

  return (
    <ScrollView
      style={{ flex: 1, backgroundColor: theme.colors.background }}
      contentContainerStyle={{ paddingBottom: 100 }}
      showsVerticalScrollIndicator={false}
    >
      {/* Header */}
      <View style={styles.header}>
        <IconButton
          icon="arrow-left"
          size={24}
          onPress={() => router.back()}
          style={styles.backButton}
        />
        <Text variant="titleMedium" style={styles.title}>Profile</Text>
        <View style={{ width: 48 }} />
      </View>

      {/* Avatar */}
      <View style={styles.avatarSection}>
        <View style={[styles.avatarCircle, { borderColor: theme.colors.primary, backgroundColor: 'transparent' }]}>
          <List.Icon icon="account-outline" color={theme.colors.onSurfaceVariant} style={styles.avatarIcon} />
        </View>
        <Text variant="titleLarge" style={styles.name}>{displayName}</Text>
        <Text variant="bodyMedium" style={{ color: theme.colors.onSurfaceVariant }}>{user?.email}</Text>
      </View>

      {/* General Settings */}
      <View style={styles.section}>
        <Text variant="labelMedium" style={[styles.sectionLabel, { color: theme.colors.onSurfaceVariant }]}>
          GENERAL
        </Text>
        
        {renderItem({
          title: 'Language',
          icon: 'translate',
          value: language,
          onPress: () => setShowLanguageDialog(true)
        })}
        {renderItem({
          title: 'Currency',
          icon: 'currency-usd',
          value: currency,
          onPress: () => setShowCurrencyDialog(true)
        })}
        {renderItem({
          title: 'Dark Mode',
          icon: 'weather-night',
          rightContent: (
            <MD3Switch
              value={isDark}
              onValueChange={toggleColorScheme}
            />
          )
        })}
        {renderItem({
          title: 'Data Management',
          icon: 'database-outline',
          value: 'Import/Export'
        })}
      </View>

      {/* Notifications */}
      <View style={styles.section}>
        <Text variant="labelMedium" style={[styles.sectionLabel, { color: theme.colors.onSurfaceVariant }]}>
          NOTIFICATIONS
        </Text>
        
        {renderItem({
          title: 'Push Notifications',
          icon: 'bell-outline',
          rightContent: (
            <MD3Switch
              value={notifications}
              onValueChange={setNotifications}
            />
          )
        })}
        {renderItem({
          title: 'Security Alerts',
          icon: 'shield-outline'
        })}
      </View>

      {/* Support */}
      <View style={styles.section}>
        <Text variant="labelMedium" style={[styles.sectionLabel, { color: theme.colors.onSurfaceVariant }]}>
          SUPPORT
        </Text>
        
        {renderItem({
          title: 'Help & Support',
          icon: 'help-circle-outline'
        })}
      </View>

      {/* Logout */}
      <View style={[styles.section, { marginBottom: 24, marginTop: 12 }]}>
        <Button
          mode="contained"
          icon="logout"
          onPress={() => setShowLogoutDialog(true)}
          style={styles.logoutBtn}
          buttonColor={theme.colors.errorContainer}
          textColor={theme.colors.error}
          contentStyle={{ height: 52 }}
          labelStyle={{ fontWeight: '700', fontSize: 16 }}
        >
          Log Out
        </Button>
      </View>

      {/* Dialogs */}
      <Portal>
        {/* Currency Dialog */}
        <Dialog visible={showCurrencyDialog} onDismiss={() => setShowCurrencyDialog(false)} style={{ borderRadius: 24 }}>
          <Dialog.Title>Select Currency</Dialog.Title>
          <Dialog.Content>
            {CURRENCIES.map((c) => (
              <List.Item
                key={c}
                title={c}
                onPress={() => handleUpdateCurrency(c)}
                right={() => currency === c ? <List.Icon icon="check" color={theme.colors.primary} /> : null}
                style={{ borderRadius: 12 }}
              />
            ))}
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setShowCurrencyDialog(false)}>Cancel</Button>
          </Dialog.Actions>
        </Dialog>

        {/* Language Dialog */}
        <Dialog visible={showLanguageDialog} onDismiss={() => setShowLanguageDialog(false)} style={{ borderRadius: 24 }}>
          <Dialog.Title>Select Language</Dialog.Title>
          <Dialog.Content>
            {LANGUAGES.map((l) => (
              <List.Item
                key={l}
                title={l}
                onPress={() => handleUpdateLanguage(l)}
                right={() => language === l ? <List.Icon icon="check" color={theme.colors.primary} /> : null}
                style={{ borderRadius: 12 }}
              />
            ))}
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setShowLanguageDialog(false)}>Cancel</Button>
          </Dialog.Actions>
        </Dialog>

        {/* Logout Confirmation */}
        <Dialog visible={showLogoutDialog} onDismiss={() => setShowLogoutDialog(false)} style={{ borderRadius: 24 }}>
          <Dialog.Title>Log Out</Dialog.Title>
          <Dialog.Content>
            <Text variant="bodyMedium">Are you sure you want to log out?</Text>
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setShowLogoutDialog(false)}>Cancel</Button>
            <Button onPress={handleLogout} textColor={theme.colors.error}>Log Out</Button>
          </Dialog.Actions>
        </Dialog>
      </Portal>

      <Snackbar visible={!!snack} onDismiss={() => setSnack('')} duration={2500}>{snack}</Snackbar>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  header: { 
    flexDirection: 'row', 
    alignItems: 'center', 
    justifyContent: 'space-between',
    paddingHorizontal: 8, 
    paddingTop: 52, 
    paddingBottom: 8 
  },
  backButton: { margin: 0 },
  title: { fontWeight: '700', fontSize: 18 },
  avatarSection: { alignItems: 'center', paddingVertical: 12, paddingBottom: 24 },
  avatarCircle: {
    width: 88, 
    height: 88, 
    borderRadius: 44,
    borderWidth: 2,
    alignItems: 'center', 
    justifyContent: 'center', 
    marginBottom: 16,
  },
  avatarIcon: {
    width: 48,
    height: 48,
    margin: 0,
    backgroundColor: 'transparent'
  },
  name: { fontWeight: '700', marginBottom: 4 },
  section: { paddingHorizontal: 16, marginBottom: 20 },
  sectionLabel: { marginBottom: 12, fontWeight: '700', letterSpacing: 1, marginLeft: 8 },
  listItemContainer: { 
    borderRadius: 16, 
    marginBottom: 12,
    overflow: 'hidden'
  },
  listItem: { paddingVertical: 6, paddingHorizontal: 8 },
  logoutBtn: { borderRadius: 16, marginHorizontal: 8 },
});
