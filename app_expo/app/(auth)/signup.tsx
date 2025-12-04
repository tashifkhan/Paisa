import { useRouter } from "expo-router";
import { ArrowLeft, ArrowRight, Lock, Mail, User } from "lucide-react-native";
import React, { useState } from "react";
import { Text, TextInput, TouchableOpacity, View } from "react-native";

export default function SignUpScreen() {
	const router = useRouter();
	const [name, setName] = useState("");
	const [email, setEmail] = useState("");
	const [password, setPassword] = useState("");

	const handleSignUp = () => {
		// Implement sign up logic here
		router.push("/otp");
	};

	return (
		<View className="flex-1 bg-[var(--background)] p-6 justify-center">
			<TouchableOpacity
				onPress={() => router.back()}
				className="absolute top-12 left-6 p-2 bg-[var(--card)] border border-[var(--border)] rounded-full shadow-sm"
			>
				<ArrowLeft size={20} color="var(--foreground)" />
			</TouchableOpacity>

			<View className="items-center mb-10 mt-10">
				<Text className="text-3xl font-bold text-[var(--foreground)]">
					Create Account
				</Text>
				<Text className="text-[var(--muted-foreground)] text-center mt-2">
					Start your journey to financial freedom
				</Text>
			</View>

			<View className="gap-4 mb-6">
				<View className="bg-[var(--card)] border border-[var(--border)] rounded-2xl p-4 flex-row items-center gap-3">
					<User size={20} color="var(--muted-foreground)" />
					<TextInput
						className="flex-1 text-[var(--foreground)] text-base"
						placeholder="Full Name"
						placeholderTextColor="var(--muted-foreground)"
						value={name}
						onChangeText={setName}
					/>
				</View>

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
			</View>

			<TouchableOpacity
				onPress={handleSignUp}
				className="w-full bg-[var(--primary)] py-4 rounded-[24px] flex-row items-center justify-center gap-2 shadow-lg mb-6"
			>
				<Text className="text-white font-bold text-lg">Sign Up</Text>
				<ArrowRight size={20} color="white" />
			</TouchableOpacity>

			<View className="flex-row justify-center gap-1">
				<Text className="text-[var(--muted-foreground)]">
					Already have an account?
				</Text>
				<TouchableOpacity onPress={() => router.push("/signin")}>
					<Text className="text-[var(--primary)] font-bold">Sign In</Text>
				</TouchableOpacity>
			</View>
		</View>
	);
}
