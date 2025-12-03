import TransactionItem from "@/components/TransactionItem";
import {
	ArrowDownLeft,
	ArrowUpRight,
	Bell,
	Gift,
	Moon,
	Pizza,
	Shirt,
	Smartphone,
	Sun,
	Wallet,
} from "lucide-react-native";
import { useColorScheme } from "nativewind";
import React from "react";
import { ScrollView, Text, TouchableOpacity, View } from "react-native";

export default function HomeScreen() {
	const { colorScheme, toggleColorScheme } = useColorScheme();
	const isDarkMode = colorScheme === "dark";

	const transactions = [
		{
			id: 1,
			title: "Shopping",
			subtitle: "Cash",
			amount: "498.50",
			percent: "32",
			icon: Shirt,
		},
		{
			id: 2,
			title: "Gifts",
			subtitle: "Cash - Card",
			amount: "344.45",
			percent: "21",
			icon: Gift,
		},
		{
			id: 3,
			title: "Food",
			subtitle: "Cash",
			amount: "230.50",
			percent: "12",
			icon: Pizza,
		},
		{
			id: 4,
			title: "Taxi",
			subtitle: "Card",
			amount: "45.00",
			percent: "5",
			icon: Wallet,
		},
		{
			id: 5,
			title: "Mobile Bill",
			subtitle: "Online",
			amount: "55.00",
			percent: "6",
			icon: Smartphone,
		},
	];

	return (
		<ScrollView
			className="flex-1 bg-[var(--background)]"
			contentContainerStyle={{ paddingBottom: 100 }}
		>
			{/* Header */}
			<View className="flex-row justify-between items-center p-6 pt-12">
				<View>
					<Text className="text-3xl font-bold text-[var(--foreground)]">
						Hi, There
					</Text>
				</View>
				<View className="flex-row gap-3">
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
					<TouchableOpacity className="p-2 relative bg-[var(--card)] rounded-full border border-[var(--border)]">
						<Bell size={20} color="var(--foreground)" />
						<View className="absolute top-1.5 right-2 w-2 h-2 bg-[var(--destructive)] rounded-full" />
					</TouchableOpacity>
				</View>
			</View>

			{/* Total Balance Card */}
			<View className="px-6 mb-8">
				<View className="bg-[var(--primary)] p-6 rounded-[2rem] shadow-lg relative overflow-hidden">
					{/* Abstract blobs */}
					<View className="absolute -top-10 -right-10 w-32 h-32 bg-white/20 rounded-full blur-2xl" />
					<View className="absolute bottom-0 left-0 w-24 h-24 bg-black/10 rounded-full blur-2xl" />

					<View className="relative z-10">
						<Text className="text-sm font-medium text-[var(--primary-foreground)] opacity-90 mb-1">
							Total Balance
						</Text>
						<Text className="text-4xl font-bold text-[var(--primary-foreground)] mb-8">
							₹32,500.00
						</Text>

						<View className="flex-row gap-4">
							<View className="flex-1 bg-black/20 rounded-2xl p-3 backdrop-blur-sm">
								<View className="flex-row items-center gap-1 mb-1 opacity-90">
									<View className="w-5 h-5 rounded-full bg-white/20 items-center justify-center">
										<ArrowDownLeft size={12} color="white" />
									</View>
									<Text className="text-xs text-white">Income</Text>
								</View>
								<Text className="font-semibold text-lg text-white">₹4,200</Text>
							</View>
							<View className="flex-1 bg-white/20 rounded-2xl p-3 backdrop-blur-sm">
								<View className="flex-row items-center gap-1 mb-1 opacity-90">
									<View className="w-5 h-5 rounded-full bg-black/10 items-center justify-center">
										<ArrowUpRight size={12} color="white" />
									</View>
									<Text className="text-xs text-white">Expense</Text>
								</View>
								<Text className="font-semibold text-lg text-white">₹1,612</Text>
							</View>
						</View>
					</View>
				</View>
			</View>

			{/* Spending Analysis */}
			<View className="px-6 pb-6">
				<View className="flex-row justify-between items-center mb-4">
					<Text className="text-xl font-bold text-[var(--foreground)]">
						Spending Analysis
					</Text>
					<TouchableOpacity className="bg-[var(--card)] border border-[var(--border)] px-3 py-1 rounded-lg">
						<Text className="text-sm font-medium text-[var(--muted-foreground)]">
							See All
						</Text>
					</TouchableOpacity>
				</View>

				<View className="gap-1">
					{transactions.map((t) => (
						<TransactionItem
							key={t.id}
							icon={t.icon}
							title={t.title}
							subtitle={t.subtitle}
							amount={t.amount}
							percent={t.percent}
						/>
					))}
				</View>
			</View>
		</ScrollView>
	);
}
