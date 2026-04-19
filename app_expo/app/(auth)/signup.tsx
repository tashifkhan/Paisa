import { useRouter } from 'expo-router';
import React, { useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, View } from 'react-native';
import { ActivityIndicator, Button, Snackbar, Text, TextInput as PaperTextInput } from 'react-native-paper';
import { useAuth } from '../../context/AuthContext';

const TextInput = PaperTextInput as unknown as React.ComponentType<any> & { Icon: typeof PaperTextInput.Icon; Affix: typeof PaperTextInput.Affix };

export default function SignUpScreen() {
  const router = useRouter();
  const { requestOtp, setPendingSignup } = useAuth();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSignUp = async () => {
    if (!name || !email || !password) {
      setError('Please fill in all fields');
      return;
    }
    setLoading(true);
    try {
      await requestOtp(email);
      setPendingSignup({ email, name, password });
      router.push({ pathname: '/(auth)/otp', params: { email, flow: 'signup' } });
    } catch (e: any) {
      setError(e?.response?.data?.detail || 'Failed to send verification code.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <KeyboardAvoidingView style={styles.container} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
        <Button
          mode="text"
          icon="arrow-left"
          onPress={() => router.back()}
          style={styles.backBtn}
          compact
        >
          Back
        </Button>

        <View style={styles.header}>
          <Text variant="headlineMedium" style={styles.title}>Create Account</Text>
          <Text variant="bodyMedium" style={styles.subtitle}>Start your journey to financial freedom</Text>
        </View>

        <View style={styles.inputs}>
          <TextInput
            mode="outlined"
            label="Full Name"
            value={name}
            onChangeText={setName}
            left={<TextInput.Icon icon="account-outline" />}
            style={styles.input}
          />
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
          <TextInput
            mode="outlined"
            label="Password"
            value={password}
            onChangeText={setPassword}
            secureTextEntry={!showPassword}
            left={<TextInput.Icon icon="lock-outline" />}
            right={
              <TextInput.Icon
                icon={showPassword ? 'eye-off-outline' : 'eye-outline'}
                onPress={() => setShowPassword(!showPassword)}
              />
            }
            style={styles.input}
          />
        </View>

        <Button
          mode="contained"
          onPress={handleSignUp}
          disabled={loading}
          style={styles.primaryBtn}
          contentStyle={styles.primaryBtnContent}
          labelStyle={styles.primaryBtnLabel}
          icon={loading ? undefined : 'arrow-right'}
        >
          {loading ? <ActivityIndicator color="#f8f7fa" size={20} /> : 'Continue'}
        </Button>

        <View style={styles.footerRow}>
          <Text variant="bodyMedium" style={styles.footerText}>Already have an account? </Text>
          <Button mode="text" onPress={() => router.push('/(auth)/signin')} compact labelStyle={styles.linkLabel}>
            Sign In
          </Button>
        </View>
      </ScrollView>

      <Snackbar visible={!!error} onDismiss={() => setError('')} duration={3000}>
        {error}
      </Snackbar>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  scroll: { flexGrow: 1, padding: 24, paddingTop: 48 },
  backBtn: { alignSelf: 'flex-start', marginBottom: 24 },
  header: { alignItems: 'center', marginBottom: 36 },
  title: { fontWeight: '700', textAlign: 'center', marginBottom: 8 },
  subtitle: { textAlign: 'center', opacity: 0.7 },
  inputs: { gap: 12, marginBottom: 24 },
  input: { borderRadius: 16 },
  primaryBtn: { borderRadius: 28, marginBottom: 24 },
  primaryBtnContent: { height: 52 },
  primaryBtnLabel: { fontSize: 16, fontWeight: '700', letterSpacing: 0.5 },
  footerRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center' },
  footerText: { opacity: 0.7 },
  linkLabel: { fontWeight: '700', fontSize: 14 },
});
