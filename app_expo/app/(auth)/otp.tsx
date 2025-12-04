import { useRouter } from "expo-router";
import { ArrowLeft, Check } from "lucide-react-native";
import React, { useRef, useState } from "react";
import { Text, TextInput, TouchableOpacity, View } from "react-native";

export default function OTPScreen() {
	const router = useRouter();
	const [otp, setOtp] = useState(["", "", "", ""]);
	const inputRefs = useRef<Array<TextInput | null>>([]);

	const handleVerify = () => {
		// Implement verify logic here
		router.replace("/(tabs)");
	};

	const handleOtpChange = (value: string, index: number) => {
		const newOtp = [...otp];
		newOtp[index] = value;
		setOtp(newOtp);

		if (value && index < 3) {
			inputRefs.current[index + 1]?.focus();
		}
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
					Verification
				</Text>
				<Text className="text-[var(--muted-foreground)] text-center mt-2">
					Enter the 4-digit code sent to your email
				</Text>
			</View>

			<View className="flex-row justify-center gap-4 mb-8">
				{otp.map((digit, index) => (
					<TextInput
						key={index}
						ref={(ref) => (inputRefs.current[index] = ref)}
						className="w-16 h-16 bg-[var(--card)] border border-[var(--border)] rounded-2xl text-center text-2xl font-bold text-[var(--foreground)]"
						value={digit}
						onChangeText={(value) => handleOtpChange(value, index)}
						keyboardType="number-pad"
						maxLength={1}
					/>
				))}
			</View>

			<TouchableOpacity
				onPress={handleVerify}
				className="w-full bg-[var(--primary)] py-4 rounded-[24px] flex-row items-center justify-center gap-2 shadow-lg mb-6"
			>
				<Text className="text-white font-bold text-lg">Verify</Text>
				<Check size={20} color="white" />
			</TouchableOpacity>

			<View className="flex-row justify-center gap-1">
				<Text className="text-[var(--muted-foreground)]">
					Didn't receive code?
				</Text>
				<TouchableOpacity>
					<Text className="text-[var(--primary)] font-bold">Resend</Text>
				</TouchableOpacity>
			</View>
		</View>
	);
}
