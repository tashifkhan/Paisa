import { useRouter } from 'expo-router';
import React, { useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, View } from 'react-native';
import { ActivityIndicator, Button, Snackbar, Text, TextInput as PaperTextInput } from 'react-native-paper';
import { useAuth } from '../../context/AuthContext';

// Paper TextInput passes through all RN TextInput props but types are incomplete
const TextInput = PaperTextInput as unknown as React.ComponentType<any> & { Icon: typeof PaperTextInput.Icon; Affix: typeof PaperTextInput.Affix };

export default function SignInScreen() {
  const router = useRouter();
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSignIn = async () => {
    if (!email || !password) {
      setError('Please enter your email and password');
      return;
    }
    setLoading(true);
    try {
      await login(email, password);
      router.replace('/(tabs)');
    } catch (e: any) {
      setError(e?.response?.data?.detail || 'Sign in failed. Please check your credentials.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <KeyboardAvoidingView style={styles.container} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
        {/* Logo */}
        <View style={styles.logoWrap}>
          <View style={styles.logoBox}>
            <Text style={styles.logoText}>P</Text>
          </View>
          <Text variant="headlineMedium" style={styles.title}>Welcome Back</Text>
          <Text variant="bodyMedium" style={styles.subtitle}>Sign in to continue managing your finances</Text>
        </View>

        {/* Inputs */}
        <View style={styles.inputs}>
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
          <Button
            mode="text"
            onPress={() => router.push('/(auth)/forgot-password')}
            style={styles.forgotBtn}
            labelStyle={styles.forgotLabel}
          >
            Forgot Password?
          </Button>
        </View>

        {/* Sign In Button */}
        <Button
          mode="contained"
          onPress={handleSignIn}
          disabled={loading}
          style={styles.primaryBtn}
          contentStyle={styles.primaryBtnContent}
          labelStyle={styles.primaryBtnLabel}
          icon={loading ? undefined : 'arrow-right'}
        >
          {loading ? <ActivityIndicator color="#f8f7fa" size={20} /> : 'Sign In'}
        </Button>

        {/* Sign Up Link */}
        <View style={styles.footerRow}>
          <Text variant="bodyMedium" style={styles.footerText}>Don't have an account? </Text>
          <Button mode="text" onPress={() => router.push('/(auth)/signup')} compact labelStyle={styles.linkLabel}>
            Sign Up
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
  scroll: { flexGrow: 1, justifyContent: 'center', padding: 24 },
  logoWrap: { alignItems: 'center', marginBottom: 40 },
  logoBox: {
    width: 80, height: 80,
    backgroundColor: '#8a79ab',
    borderRadius: 24,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 16,
    transform: [{ rotate: '3deg' }],
    shadowColor: '#8a79ab',
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.35,
    shadowRadius: 16,
    elevation: 8,
  },
  logoText: { fontSize: 36, fontWeight: '800', color: '#fff' },
  title: { fontWeight: '700', textAlign: 'center', marginBottom: 8 },
  subtitle: { textAlign: 'center', opacity: 0.7 },
  inputs: { gap: 12, marginBottom: 8 },
  input: { borderRadius: 16 },
  forgotBtn: { alignSelf: 'flex-end' },
  forgotLabel: { fontSize: 13 },
  primaryBtn: { borderRadius: 28, marginTop: 16, marginBottom: 24 },
  primaryBtnContent: { height: 52 },
  primaryBtnLabel: { fontSize: 16, fontWeight: '700', letterSpacing: 0.5 },
  footerRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center' },
  footerText: { opacity: 0.7 },
  linkLabel: { fontWeight: '700', fontSize: 14 },
});
