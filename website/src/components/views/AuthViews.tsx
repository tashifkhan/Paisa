import { GoogleLogin } from "@react-oauth/google";
import { ArrowLeft, ArrowRight, Lock, Mail, Timer, User } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { authService } from "../../services/authService";
import { InputField } from "../shared/InputField";

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID || "";

interface AuthViewProps {
	setCurrentView: (view: string) => void;
	isDarkMode?: boolean;
}

// --- OTP View ---
export const OTPView = () => {
	const navigate = useNavigate();
	const location = useLocation();
	const { email, name, password } = location.state || {}; // Get state passed from SignUp

	const [otp, setOtp] = useState(["", "", "", "", "", ""]);
	const [timer, setTimer] = useState(30);
	const [loading, setLoading] = useState(false);
	const inputRefs = useRef<(HTMLInputElement | null)[]>([]);

	useEffect(() => {
		if (!email) {
			// If no email in state, redirect back to signup
			navigate("/signup");
		}
	}, [email, navigate]);

	useEffect(() => {
		const interval = setInterval(() => {
			setTimer((prev) => (prev > 0 ? prev - 1 : 0));
		}, 1000);
		return () => clearInterval(interval);
	}, []);

	const handleChange = (index: number, value: string) => {
		// If pasting multiple characters, distribute them across inputs
		if (value.length > 1) {
			const digits = value.replace(/\D/g, "").slice(0, 6).split("");
			const newOtp = [...otp];
			digits.forEach((digit, i) => {
				if (index + i < 6) {
					newOtp[index + i] = digit;
				}
			});
			setOtp(newOtp);
			// Focus the last filled input or the next empty one
			const focusIndex = Math.min(index + digits.length, 5);
			inputRefs.current[focusIndex]?.focus();
			return;
		}

		const newOtp = [...otp];
		newOtp[index] = value;
		setOtp(newOtp);

		// Auto-focus next input
		if (value && index < 5) {
			inputRefs.current[index + 1]?.focus();
		}
	};

	const handlePaste = (e: React.ClipboardEvent<HTMLInputElement>) => {
		e.preventDefault();
		const pastedData = e.clipboardData.getData("text");
		const digits = pastedData.replace(/\D/g, "").slice(0, 6).split("");

		if (digits.length > 0) {
			const newOtp = ["", "", "", "", "", ""];
			digits.forEach((digit, i) => {
				if (i < 6) newOtp[i] = digit;
			});
			setOtp(newOtp);
			// Focus the last filled input
			const focusIndex = Math.min(digits.length - 1, 5);
			inputRefs.current[focusIndex]?.focus();
		}
	};

	const handleKeyDown = (
		index: number,
		e: React.KeyboardEvent<HTMLInputElement>,
	) => {
		if (e.key === "Backspace" && !otp[index] && index > 0) {
			inputRefs.current[index - 1]?.focus();
		}
	};

	const handleVerify = async () => {
		setLoading(true);
		try {
			const code = otp.join("");
			await authService.verifyOtp(email, code, name, password);
			// Assume success updates token in localStorage
			// App.tsx auth check will pass now
			navigate("/");
		} catch (error) {
			alert("Verification failed. Please try again.");
			console.error(error);
		} finally {
			setLoading(false);
		}
	};

	return (
		<div className="flex flex-col h-full bg-(--background) px-6 pt-12 pb-6 overflow-y-auto animate-in fade-in slide-in-from-right duration-300">
			<button
				onClick={() => navigate("/signin")}
				className="absolute top-6 left-6 p-3 bg-(--card) border border-(--border) rounded-full text-(--foreground) shadow-sm hover:shadow-md transition-all"
			>
				<ArrowLeft size={20} />
			</button>

			<div className="flex-1 flex flex-col justify-center items-center max-w-md mx-auto w-full">
				<div className="mb-8 text-center">
					<div className="w-16 h-16 bg-(--primary)/10 rounded-full mx-auto flex items-center justify-center text-(--primary) mb-6">
						<Lock size={32} />
					</div>
					<h1 className="text-3xl font-bold text-(--foreground) mb-3">
						Verification
					</h1>
					<p className="text-(--muted-foreground)">
						Enter the 6-digit code sent to {email}.
					</p>
				</div>

				<div className="flex gap-4 mb-8 justify-center w-full">
					{otp.map((digit, index) => (
						<input
							key={index}
							ref={(el) => {
								inputRefs.current[index] = el;
							}}
							type="text"
							maxLength={1}
							value={digit}
							onChange={(e) => handleChange(index, e.target.value)}
							onKeyDown={(e) => handleKeyDown(index, e)}
							onPaste={handlePaste}
							className="w-12 h-14 text-center text-xl font-bold bg-(--card) border-2 border-(--border) rounded-xl focus:border-(--primary) focus:ring-4 focus:ring-(--primary)/10 outline-none transition-all caret-(--primary)"
						/>
					))}
				</div>

				<div className="w-full space-y-6">
					<button
						onClick={handleVerify}
						disabled={loading}
						className="w-full py-4 bg-(--primary) text-(--primary-foreground) rounded-4xl font-bold text-lg shadow-lg hover:shadow-xl hover:scale-[1.02] active:scale-[0.98] transition-all flex items-center justify-center gap-2 disabled:opacity-50"
					>
						{loading ? "Verifying..." : "Verify & Proceed"}
						{!loading && <ArrowRight size={20} />}
					</button>

					<div className="text-center">
						{timer > 0 ? (
							<p className="text-(--muted-foreground) flex items-center justify-center gap-2 font-medium">
								<Timer size={16} />
								Resend code in 00:{timer.toString().padStart(2, "0")}
							</p>
						) : (
							<button
								onClick={() => setTimer(30)} // Implement resend logic
								className="text-(--primary) font-bold hover:underline"
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
export const SignInView = ({ setCurrentView, isDarkMode }: AuthViewProps) => {
	const navigate = useNavigate();
	const [email, setEmail] = useState("");
	const [password, setPassword] = useState("");
	const [loading, setLoading] = useState(false);
	const [showGoogleLogin, setShowGoogleLogin] = useState(
		Boolean(GOOGLE_CLIENT_ID),
	);

	const handleLogin = async () => {
		setLoading(true);
		try {
			await authService.login(email, password);
			navigate("/");
		} catch (error) {
			alert("Login failed. Check your credentials.");
			console.error(error);
		} finally {
			setLoading(false);
		}
	};

	return (
		<div className="flex flex-col h-full bg-(--background) px-6 pt-12 pb-6 overflow-y-auto animate-in fade-in duration-500">
			<div className="flex-1 flex flex-col justify-center max-w-md mx-auto w-full">
				<div className="mb-10 text-center">
					<div className="relative w-24 h-24 mx-auto mb-6">
						<div className="absolute inset-0 bg-(--primary)/20 rounded-4xl rotate-6 blur-sm"></div>
						<div className="relative w-full h-full bg-(--card) rounded-4xl flex items-center justify-center shadow-xl overflow-hidden p-4">
							<img
								src={isDarkMode ? "/logo-light.png" : "/logo-dark.png"}
								alt="Paisa Logo"
								className="w-full h-full object-contain"
							/>
						</div>
					</div>
					<h1 className="text-4xl font-bold text-(--foreground) mb-3 tracking-tight">
						Welcome Back
					</h1>
					<p className="text-(--muted-foreground) text-lg">
						Sign in to manage your finances
					</p>
				</div>

				<div className="space-y-5">
					<InputField
						icon={Mail}
						type="email"
						placeholder="Email Address"
						value={email}
						onChange={(e) => setEmail(e.target.value)}
					/>
					<div className="space-y-1">
						<InputField
							icon={Lock}
							type="password"
							placeholder="Password"
							value={password}
							onChange={(e) => setPassword(e.target.value)}
						/>
						<div className="flex justify-end px-1">
							<button
								onClick={() => setCurrentView("forgot-password")}
								className="text-sm text-(--primary) font-semibold hover:text-(--primary)/80 transition-colors"
							>
								Forgot Password?
							</button>
						</div>
					</div>

					<button
						onClick={handleLogin}
						disabled={loading}
						className="w-full py-4 bg-(--primary) text-(--primary-foreground) rounded-4xl font-bold text-lg shadow-lg hover:shadow-xl hover:scale-[1.02] active:scale-[0.98] transition-all mt-4 flex items-center justify-center gap-2 disabled:opacity-50"
					>
						{loading ? "Signing In..." : "Sign In"}
						{!loading && <ArrowRight size={20} />}
					</button>

					{showGoogleLogin && (
						<>
							{/* Google Sign-In Divider */}
							<div className="relative my-6">
								<div className="absolute inset-0 flex items-center">
									<div className="w-full border-t border-(--border)"></div>
								</div>
								<div className="relative flex justify-center text-sm">
									<span className="px-4 bg-(--background) text-(--muted-foreground)">
										Or continue with
									</span>
								</div>
							</div>

							{/* Google Sign-In Button */}
							<div className="flex justify-center">
								<GoogleLogin
									onSuccess={async (response) => {
										if (!response.credential) return;
										setLoading(true);
										try {
											await authService.googleLogin(response.credential);
											navigate("/");
										} catch (error) {
											alert("Google login failed. Please try again.");
											console.error(error);
										} finally {
											setLoading(false);
										}
									}}
									onError={() => {
										setShowGoogleLogin(false);
									}}
									theme="outline"
									size="large"
									text="signin_with"
									shape="pill"
									width="300"
								/>
							</div>
						</>
					)}
				</div>
			</div>

			<div className="text-center mt-8 pb-4">
				<p className="text-(--muted-foreground)">
					Don't have an account?{" "}
					<button
						onClick={() => setCurrentView("signup")}
						className="text-(--primary) font-bold hover:underline transition-all"
					>
						Create Account
					</button>
				</p>
			</div>
		</div>
	);
};

// --- Sign Up View ---
export const SignUpView = ({ setCurrentView }: AuthViewProps) => {
	const navigate = useNavigate();
	const [name, setName] = useState("");
	const [email, setEmail] = useState("");
	const [password, setPassword] = useState("");
	const [loading, setLoading] = useState(false);

	const handleSignUp = async () => {
		if (!name || !email || !password) return alert("Please fill all fields");
		setLoading(true);
		try {
			// Request OTP
			await authService.requestOtp(email);
			// Navigate to OTP view with state
			navigate("/otp", { state: { email, name, password } });
		} catch (error) {
			alert("Signup initiation failed.");
			console.error(error);
		} finally {
			setLoading(false);
		}
	};

	return (
		<div className="flex flex-col h-full bg-(--background) px-6 pt-12 pb-6 overflow-y-auto animate-in fade-in slide-in-from-right duration-300">
			<button
				onClick={() => setCurrentView("signin")}
				className="absolute top-6 left-6 p-3 bg-(--card) border border-(--border) rounded-full text-(--foreground) shadow-sm hover:shadow-md transition-all"
			>
				<ArrowLeft size={20} />
			</button>

			<div className="flex-1 flex flex-col justify-center max-w-md mx-auto w-full">
				<div className="mb-10 text-center">
					<h1 className="text-4xl font-bold text-(--foreground) mb-3 tracking-tight">
						Create Account
					</h1>
					<p className="text-(--muted-foreground) text-lg">
						Start your financial journey
					</p>
				</div>

				<div className="space-y-5">
					<InputField
						icon={User}
						type="text"
						placeholder="Full Name"
						value={name}
						onChange={(e) => setName(e.target.value)}
					/>
					<InputField
						icon={Mail}
						type="email"
						placeholder="Email Address"
						value={email}
						onChange={(e) => setEmail(e.target.value)}
					/>
					<InputField
						icon={Lock}
						type="password"
						placeholder="Password"
						value={password}
						onChange={(e) => setPassword(e.target.value)}
					/>

					<button
						onClick={handleSignUp}
						disabled={loading}
						className="w-full py-4 bg-(--primary) text-(--primary-foreground) rounded-4xl font-bold text-lg shadow-lg hover:shadow-xl hover:scale-[1.02] active:scale-[0.98] transition-all mt-6 flex items-center justify-center gap-2 disabled:opacity-50"
					>
						{loading ? "Sending Code..." : "Sign Up"}
						{!loading && <ArrowRight size={20} />}
					</button>

					{/* Google Sign-Up Divider */}
					<div className="relative my-6">
						<div className="absolute inset-0 flex items-center">
							<div className="w-full border-t border-(--border)"></div>
						</div>
						<div className="relative flex justify-center text-sm">
							<span className="px-4 bg-(--background) text-(--muted-foreground)">
								Or sign up with
							</span>
						</div>
					</div>

					{/* Google Sign-Up Button */}
					<div className="flex justify-center">
						<GoogleLogin
							onSuccess={async (response) => {
								if (!response.credential) return;
								setLoading(true);
								try {
									await authService.googleLogin(response.credential);
									navigate("/");
								} catch (error) {
									alert("Google sign up failed. Please try again.");
									console.error(error);
								} finally {
									setLoading(false);
								}
							}}
							onError={() => {
								alert("Google sign up failed. Please try again.");
							}}
							theme="outline"
							size="large"
							text="signup_with"
							shape="pill"
							width="300"
						/>
					</div>
				</div>
			</div>

			<div className="text-center mt-8 pb-4">
				<p className="text-(--muted-foreground)">
					Already have an account?{" "}
					<button
						onClick={() => setCurrentView("signin")}
						className="text-(--primary) font-bold hover:underline transition-all"
					>
						Sign In
					</button>
				</p>
			</div>
		</div>
	);
};

// --- Forgot Password View ---
export const ForgotPasswordView = ({ setCurrentView }: AuthViewProps) => (
	<div className="flex flex-col h-full bg-(--background) px-6 pt-12 pb-6 overflow-y-auto animate-in fade-in slide-in-from-right duration-300">
		<button
			onClick={() => setCurrentView("signin")}
			className="absolute top-6 left-6 p-3 bg-(--card) border border-(--border) rounded-full text-(--foreground) shadow-sm hover:shadow-md transition-all"
		>
			<ArrowLeft size={20} />
		</button>

		<div className="flex-1 flex flex-col justify-center max-w-md mx-auto w-full">
			<div className="mb-10 text-center">
				<div className="w-20 h-20 bg-(--muted) rounded-full mx-auto flex items-center justify-center text-(--muted-foreground) mb-6 ring-8 ring-(--muted)/20">
					<Lock size={36} />
				</div>
				<h1 className="text-3xl font-bold text-(--foreground) mb-3">
					Reset Password
				</h1>
				<p className="text-(--muted-foreground) px-4">
					Enter your email and we'll send you a code to reset your password.
				</p>
			</div>

			<div className="space-y-5">
				<InputField icon={Mail} type="email" placeholder="Email Address" />
				<button
					onClick={() => setCurrentView("otp")}
					className="w-full py-4 bg-(--primary) text-(--primary-foreground) rounded-4xl font-bold text-lg shadow-lg hover:shadow-xl hover:scale-[1.02] active:scale-[0.98] transition-all mt-4"
				>
					Send Reset Code
				</button>
			</div>
		</div>
	</div>
);
