import { useState, useEffect, useRef } from 'react';
import { ArrowLeft, Landmark, Mail, Lock, User, ArrowRight, Timer } from 'lucide-react';
import { InputField } from '../shared/InputField';

interface AuthViewProps {
  setCurrentView: (view: string) => void;
}

// --- OTP View ---
export const OTPView = ({ setCurrentView }: AuthViewProps) => {
  const [otp, setOtp] = useState(['', '', '', '']);
  const [timer, setTimer] = useState(30);
  const inputRefs = useRef<(HTMLInputElement | null)[]>([]);

  useEffect(() => {
    const interval = setInterval(() => {
      setTimer((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);
    return () => clearInterval(interval);
  }, []);

  const handleChange = (index: number, value: string) => {
    if (value.length > 1) return; // Prevent multiple chars
    const newOtp = [...otp];
    newOtp[index] = value;
    setOtp(newOtp);

    // Auto-focus next input
    if (value && index < 3) {
      inputRefs.current[index + 1]?.focus();
    }
  };

  const handleKeyDown = (index: number, e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Backspace' && !otp[index] && index > 0) {
      inputRefs.current[index - 1]?.focus();
    }
  };

  return (
    <div className="flex flex-col h-full bg-[var(--background)] px-6 pt-12 pb-6 overflow-y-auto animate-in fade-in slide-in-from-right duration-300">
      <button 
        onClick={() => setCurrentView('signin')} 
        className="absolute top-6 left-6 p-3 bg-[var(--card)] border border-[var(--border)] rounded-full text-[var(--foreground)] shadow-sm hover:shadow-md transition-all"
      >
        <ArrowLeft size={20} />
      </button>
      
      <div className="flex-1 flex flex-col justify-center items-center max-w-md mx-auto w-full">
        <div className="mb-8 text-center">
          <div className="w-16 h-16 bg-[var(--primary)]/10 rounded-full mx-auto flex items-center justify-center text-[var(--primary)] mb-6">
            <Lock size={32} />
          </div>
          <h1 className="text-3xl font-bold text-[var(--foreground)] mb-3">Verification</h1>
          <p className="text-[var(--muted-foreground)]">
            Enter the 4-digit code sent to your email.
          </p>
        </div>

        <div className="flex gap-4 mb-8 justify-center w-full">
          {otp.map((digit, index) => (
            <input
              key={index}
              ref={(el) => { inputRefs.current[index] = el; }}
              type="text"
              maxLength={1}
              value={digit}
              onChange={(e) => handleChange(index, e.target.value)}
              onKeyDown={(e) => handleKeyDown(index, e)}
              className="w-14 h-16 text-center text-2xl font-bold bg-[var(--card)] border-2 border-[var(--border)] rounded-2xl focus:border-[var(--primary)] focus:ring-4 focus:ring-[var(--primary)]/10 outline-none transition-all caret-[var(--primary)]"
            />
          ))}
        </div>

        <div className="w-full space-y-6">
          <button 
            onClick={() => setCurrentView('home')} 
            className="w-full py-4 bg-[var(--primary)] text-[var(--primary-foreground)] rounded-[2rem] font-bold text-lg shadow-lg hover:shadow-xl hover:scale-[1.02] active:scale-[0.98] transition-all flex items-center justify-center gap-2"
          >
            Verify & Proceed
            <ArrowRight size={20} />
          </button>

          <div className="text-center">
            {timer > 0 ? (
              <p className="text-[var(--muted-foreground)] flex items-center justify-center gap-2 font-medium">
                <Timer size={16} />
                Resend code in 00:{timer.toString().padStart(2, '0')}
              </p>
            ) : (
              <button 
                onClick={() => setTimer(30)} 
                className="text-[var(--primary)] font-bold hover:underline"
              >
                Resend Code
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

// --- Sign In View ---
export const SignInView = ({ setCurrentView }: AuthViewProps) => (
  <div className="flex flex-col h-full bg-[var(--background)] px-6 pt-12 pb-6 overflow-y-auto animate-in fade-in duration-500">
    <div className="flex-1 flex flex-col justify-center max-w-md mx-auto w-full">
      <div className="mb-10 text-center">
        <div className="relative w-24 h-24 mx-auto mb-6">
          <div className="absolute inset-0 bg-[var(--primary)]/20 rounded-[2rem] rotate-6 blur-sm"></div>
          <div className="relative w-full h-full bg-[var(--primary)] rounded-[2rem] flex items-center justify-center text-[var(--primary-foreground)] shadow-xl">
            <Landmark size={44} />
          </div>
        </div>
        <h1 className="text-4xl font-bold text-[var(--foreground)] mb-3 tracking-tight">Welcome Back</h1>
        <p className="text-[var(--muted-foreground)] text-lg">Sign in to manage your finances</p>
      </div>

      <div className="space-y-5">
        <InputField icon={Mail} type="email" placeholder="Email Address" />
        <div className="space-y-1">
          <InputField icon={Lock} type="password" placeholder="Password" />
          <div className="flex justify-end px-1">
            <button 
              onClick={() => setCurrentView('forgot-password')} 
              className="text-sm text-[var(--primary)] font-semibold hover:text-[var(--primary)]/80 transition-colors"
            >
              Forgot Password?
            </button>
          </div>
        </div>

        <button 
          onClick={() => setCurrentView('home')} 
          className="w-full py-4 bg-[var(--primary)] text-[var(--primary-foreground)] rounded-[2rem] font-bold text-lg shadow-lg hover:shadow-xl hover:scale-[1.02] active:scale-[0.98] transition-all mt-4 flex items-center justify-center gap-2"
        >
          Sign In
          <ArrowRight size={20} />
        </button>
      </div>
    </div>

    <div className="text-center mt-8 pb-4">
      <p className="text-[var(--muted-foreground)]">
        Don't have an account?{' '}
        <button 
          onClick={() => setCurrentView('signup')} 
          className="text-[var(--primary)] font-bold hover:underline transition-all"
        >
          Create Account
        </button>
      </p>
    </div>
  </div>
);

// --- Sign Up View ---
export const SignUpView = ({ setCurrentView }: AuthViewProps) => (
  <div className="flex flex-col h-full bg-[var(--background)] px-6 pt-12 pb-6 overflow-y-auto animate-in fade-in slide-in-from-right duration-300">
    <button 
      onClick={() => setCurrentView('signin')} 
      className="absolute top-6 left-6 p-3 bg-[var(--card)] border border-[var(--border)] rounded-full text-[var(--foreground)] shadow-sm hover:shadow-md transition-all"
    >
      <ArrowLeft size={20} />
    </button>

    <div className="flex-1 flex flex-col justify-center max-w-md mx-auto w-full">
      <div className="mb-10 text-center">
        <h1 className="text-4xl font-bold text-[var(--foreground)] mb-3 tracking-tight">Create Account</h1>
        <p className="text-[var(--muted-foreground)] text-lg">Start your financial journey</p>
      </div>

      <div className="space-y-5">
        <InputField icon={User} type="text" placeholder="Full Name" />
        <InputField icon={Mail} type="email" placeholder="Email Address" />
        <InputField icon={Lock} type="password" placeholder="Password" />
        
        <button 
          onClick={() => setCurrentView('otp')} 
          className="w-full py-4 bg-[var(--primary)] text-[var(--primary-foreground)] rounded-[2rem] font-bold text-lg shadow-lg hover:shadow-xl hover:scale-[1.02] active:scale-[0.98] transition-all mt-6 flex items-center justify-center gap-2"
        >
          Sign Up
          <ArrowRight size={20} />
        </button>
      </div>
    </div>

    <div className="text-center mt-8 pb-4">
      <p className="text-[var(--muted-foreground)]">
        Already have an account?{' '}
        <button 
          onClick={() => setCurrentView('signin')} 
          className="text-[var(--primary)] font-bold hover:underline transition-all"
        >
          Sign In
        </button>
      </p>
    </div>
  </div>
);

// --- Forgot Password View ---
export const ForgotPasswordView = ({ setCurrentView }: AuthViewProps) => (
  <div className="flex flex-col h-full bg-[var(--background)] px-6 pt-12 pb-6 overflow-y-auto animate-in fade-in slide-in-from-right duration-300">
    <button 
      onClick={() => setCurrentView('signin')} 
      className="absolute top-6 left-6 p-3 bg-[var(--card)] border border-[var(--border)] rounded-full text-[var(--foreground)] shadow-sm hover:shadow-md transition-all"
    >
      <ArrowLeft size={20} />
    </button>

    <div className="flex-1 flex flex-col justify-center max-w-md mx-auto w-full">
      <div className="mb-10 text-center">
        <div className="w-20 h-20 bg-[var(--muted)] rounded-full mx-auto flex items-center justify-center text-[var(--muted-foreground)] mb-6 ring-8 ring-[var(--muted)]/20">
          <Lock size={36} />
        </div>
        <h1 className="text-3xl font-bold text-[var(--foreground)] mb-3">Reset Password</h1>
        <p className="text-[var(--muted-foreground)] px-4">
          Enter your email and we'll send you a code to reset your password.
        </p>
      </div>

      <div className="space-y-5">
        <InputField icon={Mail} type="email" placeholder="Email Address" />
        <button 
          onClick={() => setCurrentView('otp')} 
          className="w-full py-4 bg-[var(--primary)] text-[var(--primary-foreground)] rounded-[2rem] font-bold text-lg shadow-lg hover:shadow-xl hover:scale-[1.02] active:scale-[0.98] transition-all mt-4"
        >
          Send Reset Code
        </button>
      </div>
    </div>
  </div>
);

