import { useLocalSearchParams, useRouter } from 'expo-router';
import React, { useEffect, useRef, useState } from 'react';
import { StyleSheet, TextInput as RNTextInput, View } from 'react-native';
import { ActivityIndicator, Button, Snackbar, Text, TextInput as PaperTextInput, useTheme } from 'react-native-paper';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { useAuth } from '../../context/AuthContext';

const TextInput = PaperTextInput as unknown as React.ComponentType<any> & { Icon: typeof PaperTextInput.Icon; Affix: typeof PaperTextInput.Affix };

export default function OTPScreen() {
  const router = useRouter();
  const params = useLocalSearchParams<{ email: string; flow?: 'signup' | 'password-reset' }>();
  const theme = useTheme();
  const { verifyOtp, requestOtp, pendingSignup } = useAuth();
  const [otp, setOtp] = useState(['', '', '', '', '', '']);
  const [newPassword, setNewPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const inputRefs = useRef<Array<any>>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [resendTimer, setResendTimer] = useState(60);
  const flow = params.flow || 'signup';
  const isPasswordReset = flow === 'password-reset';

  useEffect(() => {
    if (resendTimer <= 0) return;
    const t = setTimeout(() => setResendTimer(resendTimer - 1), 1000);
    return () => clearTimeout(t);
  }, [resendTimer]);

  const handleOtpChange = (value: string, index: number) => {
    // Handle paste: distribute all digits starting from current index
    const digits = value.replace(/\D/g, '');
    if (digits.length > 1) {
      const newOtp = [...otp];
      for (let i = 0; i < digits.length && index + i < 6; i++) {
        newOtp[index + i] = digits[i];
      }
      setOtp(newOtp);
      const nextFocus = Math.min(index + digits.length, 5);
      inputRefs.current[nextFocus]?.focus();
      return;
    }
    const newOtp = [...otp];
    newOtp[index] = digits;
    setOtp(newOtp);
    if (digits && index < 5) {
      inputRefs.current[index + 1]?.focus();
    }
  };

  const handleBackspace = (key: string, index: number) => {
    if (key === 'Backspace' && !otp[index] && index > 0) {
      inputRefs.current[index - 1]?.focus();
    }
  };

  const handleVerify = async () => {
    const code = otp.join('');
    if (code.length < 6) {
      setError('Please enter the complete 6-digit code');
      return;
    }

    if (isPasswordReset && newPassword.length < 6) {
      setError('Please enter a password with at least 6 characters');
      return;
    }

    if (!isPasswordReset && (!pendingSignup || pendingSignup.email !== params.email)) {
      setError('Signup session expired. Please go back and sign up again.');
      return;
    }

    setLoading(true);
    try {
      if (isPasswordReset) {
        await verifyOtp(params.email, code, undefined, newPassword);
      } else {
        await verifyOtp(params.email, code, pendingSignup?.name, pendingSignup?.password);
      }
      router.replace('/(tabs)');
    } catch (e: any) {
      setError(e?.response?.data?.detail || 'Invalid verification code.');
      setOtp(['', '', '', '', '', '']);
      inputRefs.current[0]?.focus();
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async () => {
    try {
      await requestOtp(params.email);
      setResendTimer(60);
    } catch {
      setError('Failed to resend code.');
    }
  };

  return (
    <View style={styles.container}>
      <Button mode="text" icon="arrow-left" onPress={() => router.back()} style={styles.backBtn} compact>
        Back
      </Button>

      <View style={styles.header}>
        <View style={styles.iconBox}>
          <MaterialCommunityIcons name={isPasswordReset ? 'lock' : 'email'} size={32} color={theme.colors.primary} />
        </View>
        <Text variant="headlineMedium" style={styles.title}>{isPasswordReset ? 'Reset Password' : 'Verify Email'}</Text>
        <Text variant="bodyMedium" style={styles.subtitle}>
          {isPasswordReset ? 'Enter the code and set a new password for' : 'Enter the 6-digit code sent to'}{'\n'}
          <Text style={styles.emailText}>{params.email}</Text>
        </Text>
      </View>

      <View style={styles.otpRow}>
        {otp.map((digit, index) => (
          <RNTextInput
            key={index}
            ref={(ref: any) => (inputRefs.current[index] = ref)}
            style={[styles.otpInput, digit ? styles.otpInputFilled : null]}
            value={digit}
            onChangeText={(v: string) => handleOtpChange(v, index)}
            onKeyPress={({ nativeEvent }: any) => handleBackspace(nativeEvent.key, index)}
            keyboardType="number-pad"
            maxLength={1}
            textAlign="center"
          />
        ))}
      </View>

      {isPasswordReset && (
        <TextInput
          mode="outlined"
          label="New Password"
          value={newPassword}
          onChangeText={setNewPassword}
          secureTextEntry={!showPassword}
          left={<TextInput.Icon icon="lock-outline" />}
          right={
            <TextInput.Icon
              icon={showPassword ? 'eye-off-outline' : 'eye-outline'}
              onPress={() => setShowPassword(!showPassword)}
            />
          }
          style={styles.passwordInput}
        />
      )}

      <Button
        mode="contained"
        onPress={handleVerify}
        disabled={loading}
        style={styles.primaryBtn}
        contentStyle={styles.primaryBtnContent}
        labelStyle={styles.primaryBtnLabel}
      >
        {loading ? <ActivityIndicator color="#f8f7fa" size={20} /> : (isPasswordReset ? 'Reset & Continue' : 'Verify & Continue')}
      </Button>

      <View style={styles.resendRow}>
        {resendTimer > 0 ? (
          <Text variant="bodySmall" style={styles.timerText}>Resend code in {resendTimer}s</Text>
        ) : (
          <Button mode="text" onPress={handleResend} compact labelStyle={styles.resendLabel}>
            Resend Code
          </Button>
        )}
      </View>

      <Snackbar visible={!!error} onDismiss={() => setError('')} duration={3000}>
        {error}
      </Snackbar>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 24, paddingTop: 48 },
  backBtn: { alignSelf: 'flex-start', marginBottom: 24 },
  header: { alignItems: 'center', marginBottom: 40 },
  iconBox: {
    width: 72, height: 72,
    borderRadius: 20,
    backgroundColor: '#8a79ab20',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 16,
  },
  iconText: { fontSize: 32 },
  title: { fontWeight: '700', marginBottom: 8 },
  subtitle: { textAlign: 'center', opacity: 0.7, lineHeight: 22 },
  emailText: { fontWeight: '600', opacity: 1 },
  otpRow: {
    flexDirection: 'row',
    justifyContent: 'center',
    gap: 10,
    marginBottom: 36,
  },
  otpInput: {
    width: 48, height: 56,
    borderRadius: 16,
    borderWidth: 1.5,
    borderColor: '#cec9d9',
    fontSize: 22,
    fontWeight: '700',
    color: '#3d3c4f',
    backgroundColor: '#ffffff',
  },
  otpInputFilled: {
    borderColor: '#8a79ab',
    backgroundColor: '#f5f2fa',
  },
  passwordInput: { borderRadius: 16, marginBottom: 16 },
  primaryBtn: { borderRadius: 28, marginBottom: 20 },
  primaryBtnContent: { height: 52 },
  primaryBtnLabel: { fontSize: 16, fontWeight: '700' },
  resendRow: { alignItems: 'center' },
  timerText: { opacity: 0.6 },
  resendLabel: { fontWeight: '700' },
});
