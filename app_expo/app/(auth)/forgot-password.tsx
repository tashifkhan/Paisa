import { useRouter } from "expo-router";
import { ArrowLeft, ArrowRight, Mail } from "lucide-react-native";
import React, { useState } from "react";
import { Text, TextInput, TouchableOpacity, View } from "react-native";

export default function ForgotPasswordScreen() {
	const router = useRouter();
	const [email, setEmail] = useState("");

	const handleReset = () => {
		// Implement reset logic here
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
					Forgot Password
				</Text>
				<Text className="text-[var(--muted-foreground)] text-center mt-2">
					Enter your email to receive a reset code
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
			</View>

			<TouchableOpacity
				onPress={handleReset}
				className="w-full bg-[var(--primary)] py-4 rounded-[24px] flex-row items-center justify-center gap-2 shadow-lg"
			>
				<Text className="text-white font-bold text-lg">Send Code</Text>
				<ArrowRight size={20} color="white" />
			</TouchableOpacity>
		</View>
	);
}
