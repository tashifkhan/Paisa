import { useRouter } from "expo-router";
import { ArrowRight, Lock, Mail } from "lucide-react-native";
import React, { useState } from "react";
import { Text, TextInput, TouchableOpacity, View } from "react-native";

export default function SignInScreen() {
	const router = useRouter();
	const [email, setEmail] = useState("");
	const [password, setPassword] = useState("");

	const handleSignIn = () => {
		// Implement sign in logic here
		router.replace("/(tabs)");
	};

	return (
		<View className="flex-1 bg-[var(--background)] p-6 justify-center">
			<View className="items-center mb-10">
				<View className="w-20 h-20 bg-[var(--primary)] rounded-3xl items-center justify-center mb-4 shadow-lg rotate-3">
					<Text className="text-4xl font-bold text-white">P</Text>
				</View>
				<Text className="text-3xl font-bold text-[var(--foreground)]">
					Welcome Back
				</Text>
				<Text className="text-[var(--muted-foreground)] text-center mt-2">
					Sign in to continue managing your finances
				</Text>
			</View>

			<View className="gap-4 mb-6">
				<View className="bg-[var(--card)] border border-[var(--border)] rounded-2xl p-4 flex-row items-center gap-3">
					<Mail size={20} color="var(--muted-foreground)" />
					<TextInput
						className="flex-1 text-[var(--foreground)] text-base"
						placeholder="Email Address"
						placeholderTextColor="var(--muted-foreground)"
						value={email}
						onChangeText={setEmail}
						autoCapitalize="none"
						keyboardType="email-address"
					/>
				</View>

				<View className="bg-[var(--card)] border border-[var(--border)] rounded-2xl p-4 flex-row items-center gap-3">
					<Lock size={20} color="var(--muted-foreground)" />
					<TextInput
						className="flex-1 text-[var(--foreground)] text-base"
						placeholder="Password"
						placeholderTextColor="var(--muted-foreground)"
						value={password}
						onChangeText={setPassword}
						secureTextEntry
					/>
				</View>

				<TouchableOpacity onPress={() => router.push("/forgot-password")}>
					<Text className="text-[var(--primary)] text-right font-medium">
						Forgot Password?
					</Text>
				</TouchableOpacity>
			</View>

			<TouchableOpacity
				onPress={handleSignIn}
				className="w-full bg-[var(--primary)] py-4 rounded-[24px] flex-row items-center justify-center gap-2 shadow-lg mb-6"
			>
				<Text className="text-white font-bold text-lg">Sign In</Text>
				<ArrowRight size={20} color="white" />
			</TouchableOpacity>

			<View className="flex-row justify-center gap-1">
				<Text className="text-[var(--muted-foreground)]">
					Don't have an account?
				</Text>
				<TouchableOpacity onPress={() => router.push("/signup")}>
					<Text className="text-[var(--primary)] font-bold">Sign Up</Text>
				</TouchableOpacity>
			</View>
		</View>
	);
}
