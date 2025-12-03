import { useRouter } from "expo-router";
import {
	Calendar,
	Check,
	ChevronDown,
	Delete,
	Edit3,
	Moon,
	Shirt,
	Sun,
	Wallet,
	X,
} from "lucide-react-native";
import { useColorScheme } from "nativewind";
import React, { useState } from "react";
import { Text, TextInput, TouchableOpacity, View } from "react-native";

export default function AddExpenseScreen() {
	const router = useRouter();
	const { colorScheme, toggleColorScheme } = useColorScheme();
	const isDarkMode = colorScheme === "dark";
	const [amount, setAmount] = useState("25.00");

	const handleKeyPress = (key: string) => {
		if (key === "backspace") {
			setAmount((prev) => (prev.length > 1 ? prev.slice(0, -1) : "0"));
		} else if (key === "check") {
			// Logic to add expense would go here
			router.back();
		} else {
			setAmount((prev) => {
				if (prev === "0" || prev === "25.00") return key;
				return prev + key;
			});
		}
	};

	return (
		<View className="flex-1 bg-[var(--background)]">
			<View className="flex-row justify-between items-start p-6 pt-12">
				<TouchableOpacity onPress={() => router.back()} className="p-2">
					<X size={24} color="var(--foreground)" />
				</TouchableOpacity>
				<View className="items-center opacity-50">
					<Text className="text-sm font-bold text-[var(--foreground)]">
						₹32,500.00
					</Text>
					<Text className="text-xs text-[var(--muted-foreground)]">
						Total Balance
					</Text>
				</View>
				<View className="flex-row items-center gap-1">
					<TouchableOpacity
						onPress={toggleColorScheme}
						className="p-2 rounded-full active:bg-[var(--muted)]"
					>
						{isDarkMode ? (
							<Sun size={20} color="var(--foreground)" />
						) : (
							<Moon size={20} color="var(--foreground)" />
						)}
					</TouchableOpacity>
					<TouchableOpacity className="p-2">
						<Edit3 size={20} color="var(--foreground)" />
					</TouchableOpacity>
				</View>
			</View>

			<View className="flex-1 items-center px-8 pt-4">
				<View className="flex-row gap-4 w-full justify-between mb-8">
					<TouchableOpacity className="flex-1 flex-row items-center justify-between bg-[var(--muted)] px-4 py-3 rounded-2xl">
						<View className="flex-row items-center gap-2">
							<Wallet size={18} color="var(--foreground)" />
							<Text className="font-medium text-[var(--foreground)]">Cash</Text>
						</View>
						<ChevronDown size={16} color="var(--foreground)" />
					</TouchableOpacity>
					<TouchableOpacity className="flex-1 flex-row items-center justify-between bg-[var(--muted)] px-4 py-3 rounded-2xl">
						<View className="flex-row items-center gap-2">
							<Shirt size={18} color="var(--foreground)" />
							<Text className="font-medium text-[var(--foreground)]">
								Shopping
							</Text>
						</View>
						<ChevronDown size={16} color="var(--foreground)" />
					</TouchableOpacity>
				</View>

				<View className="items-center justify-center flex-1 w-full mb-8">
					<Text className="text-[var(--muted-foreground)] text-sm mb-2">
						Expenses
					</Text>
					<View className="flex-row items-center">
						<Text className="text-[var(--muted-foreground)] text-4xl mr-1">
							₹
						</Text>
						<Text className="text-6xl font-bold text-[var(--foreground)] tracking-tight">
							{amount}
						</Text>
						<View className="w-0.5 h-12 bg-[var(--foreground)] ml-1" />
					</View>
					<TextInput
						placeholder="Add comment..."
						placeholderTextColor="var(--muted-foreground)"
						className="mt-6 text-center w-full text-[var(--muted-foreground)] font-medium text-lg"
					/>
				</View>
			</View>

			<View className="bg-[var(--card)] rounded-t-[3rem] p-8 pb-10 shadow-lg border-t border-[var(--border)]">
				<View className="flex-row flex-wrap justify-between gap-y-4">
					{[1, 2, 3].map((num) => (
						<TouchableOpacity
							key={num}
							onPress={() => handleKeyPress(num.toString())}
							className="w-[30%] items-center py-2 rounded-full active:bg-[var(--muted)]"
						>
							<Text className="text-2xl font-medium text-[var(--foreground)]">
								{num}
							</Text>
						</TouchableOpacity>
					))}

					{[4, 5, 6].map((num) => (
						<TouchableOpacity
							key={num}
							onPress={() => handleKeyPress(num.toString())}
							className="w-[30%] items-center py-2 rounded-full active:bg-[var(--muted)]"
						>
							<Text className="text-2xl font-medium text-[var(--foreground)]">
								{num}
							</Text>
						</TouchableOpacity>
					))}

					{[7, 8, 9].map((num) => (
						<TouchableOpacity
							key={num}
							onPress={() => handleKeyPress(num.toString())}
							className="w-[30%] items-center py-2 rounded-full active:bg-[var(--muted)]"
						>
							<Text className="text-2xl font-medium text-[var(--foreground)]">
								{num}
							</Text>
						</TouchableOpacity>
					))}

					<TouchableOpacity className="w-[30%] items-center justify-center py-2 bg-[var(--muted)] rounded-full active:bg-[var(--muted)]/80">
						<Text className="text-2xl font-medium text-[var(--foreground)]">
							.
						</Text>
					</TouchableOpacity>

					<TouchableOpacity
						onPress={() => handleKeyPress("0")}
						className="w-[30%] items-center py-2 rounded-full active:bg-[var(--muted)]"
					>
						<Text className="text-2xl font-medium text-[var(--foreground)]">
							0
						</Text>
					</TouchableOpacity>

					<TouchableOpacity
						onPress={() => handleKeyPress("backspace")}
						className="w-[30%] items-center justify-center py-2 bg-[var(--destructive)] rounded-full active:opacity-90"
					>
						<Delete size={24} color="var(--destructive-foreground)" />
					</TouchableOpacity>
				</View>

				<View className="flex-row justify-between mt-4">
					<TouchableOpacity className="flex-1 items-center justify-center py-4 bg-[var(--muted)] rounded-full mr-2">
						<Calendar size={24} color="var(--primary)" />
					</TouchableOpacity>
					<TouchableOpacity
						onPress={() => handleKeyPress("check")}
						className="flex-[2] items-center justify-center py-4 bg-[var(--primary)] rounded-[2rem] shadow-xl active:opacity-90 ml-2"
					>
						<Check size={32} color="var(--primary-foreground)" />
					</TouchableOpacity>
				</View>
			</View>
		</View>
	);
}
