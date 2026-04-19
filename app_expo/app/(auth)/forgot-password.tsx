import { useRouter } from 'expo-router';
import React, { useState } from 'react';
import { KeyboardAvoidingView, Platform, StyleSheet, View } from 'react-native';
import { ActivityIndicator, Button, Snackbar, Text, TextInput as PaperTextInput } from 'react-native-paper';
import { useAuth } from '../../context/AuthContext';

const TextInput = PaperTextInput as unknown as React.ComponentType<any> & { Icon: typeof PaperTextInput.Icon; Affix: typeof PaperTextInput.Affix };

export default function ForgotPasswordScreen() {
  const router = useRouter();
  const { requestOtp } = useAuth();
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleReset = async () => {
    if (!email) { setError('Please enter your email'); return; }
    setLoading(true);
    try {
      await requestOtp(email);
      router.push({ pathname: '/(auth)/otp', params: { email, flow: 'password-reset' } });
    } catch (e: any) {
      setError(e?.response?.data?.detail || 'Failed to send reset code.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <KeyboardAvoidingView style={styles.container} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <Button mode="text" icon="arrow-left" onPress={() => router.back()} style={styles.backBtn} compact>
        Back
      </Button>

      <View style={styles.header}>
        <View style={styles.iconBox}>
          <Text style={styles.iconText}>🔒</Text>
        </View>
        <Text variant="headlineMedium" style={styles.title}>Forgot Password</Text>
        <Text variant="bodyMedium" style={styles.subtitle}>
          Enter your email to receive a reset code
        </Text>
      </View>

      <TextInput
        mode="outlined"
        label="Email Address"
        value={email}
        onChangeText={setEmail}
        keyboardType="email-address"
        autoCapitalize="none"
        left={<TextInput.Icon icon="email-outline" />}
        style={styles.input}
      />

      <Button
        mode="contained"
        onPress={handleReset}
        disabled={loading}
        style={styles.primaryBtn}
        contentStyle={styles.primaryBtnContent}
        labelStyle={styles.primaryBtnLabel}
        icon={loading ? undefined : 'send'}
      >
        {loading ? <ActivityIndicator color="#f8f7fa" size={20} /> : 'Send Reset Code'}
      </Button>

      <Snackbar visible={!!error} onDismiss={() => setError('')} duration={3000}>
        {error}
      </Snackbar>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 24, paddingTop: 48 },
  backBtn: { alignSelf: 'flex-start', marginBottom: 24 },
  header: { alignItems: 'center', marginBottom: 36 },
  iconBox: {
    width: 72, height: 72, borderRadius: 20,
    backgroundColor: '#8a79ab20',
    alignItems: 'center', justifyContent: 'center',
    marginBottom: 16,
  },
  iconText: { fontSize: 32 },
  title: { fontWeight: '700', marginBottom: 8 },
  subtitle: { textAlign: 'center', opacity: 0.7 },
  input: { borderRadius: 16, marginBottom: 24 },
  primaryBtn: { borderRadius: 28 },
  primaryBtnContent: { height: 52 },
  primaryBtnLabel: { fontSize: 16, fontWeight: '700' },
});
