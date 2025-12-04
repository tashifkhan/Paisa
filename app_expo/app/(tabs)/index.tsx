import CreditCardComponent from "@/components/CreditCard";
import TransactionItem from "@/components/TransactionItem";
import { useRouter } from "expo-router";
import {
	Banknote,
	Bell,
	EyeOff,
	Gift,
	Moon,
	Pizza,
	Plus,
	Shirt,
	Smartphone,
	Sun,
	Wallet,
	Wifi,
} from "lucide-react-native";
import { useColorScheme } from "nativewind";
import React from "react";
import { ScrollView, Text, TouchableOpacity, View } from "react-native";

export default function HomeScreen() {
	const { colorScheme, toggleColorScheme } = useColorScheme();
	const isDarkMode = colorScheme === "dark";
	const router = useRouter();

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
			showsVerticalScrollIndicator={false}
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
				<View className="bg-[#FFF6F1] dark:bg-[var(--card)] p-8 rounded-[2.5rem] shadow-sm border border-[#F5E6DE] dark:border-[var(--border)] relative overflow-hidden">
					{/* Gradient Blobs */}
					<View className="absolute -top-24 -right-24 w-64 h-64 bg-[#FADAC9] dark:bg-[var(--primary)]/10 rounded-full blur-3xl opacity-60" />
					<View className="absolute -bottom-24 -left-24 w-64 h-64 bg-[#FADAC9] dark:bg-[var(--primary)]/10 rounded-full blur-3xl opacity-60" />

					<View className="relative z-10">
						<View className="flex-row justify-between items-start mb-2">
							<Text className="text-lg font-medium opacity-80 text-[#3E2E28] dark:text-[var(--foreground)]">
								Total balance
							</Text>
							<TouchableOpacity className="opacity-60">
								<EyeOff
									size={24}
									color={isDarkMode ? "var(--foreground)" : "#3E2E28"}
								/>
							</TouchableOpacity>
						</View>

						<Text className="text-4xl font-bold mb-8 tracking-tight text-[#2D1F16] dark:text-[var(--foreground)]">
							₹6,64,472.00
						</Text>

						<Text className="mb-6 text-lg font-semibold text-[#3E2E28] dark:text-[var(--foreground)]">
							This month
						</Text>

						<View className="flex-row gap-8">
							{/* Income */}
							<View className="flex-1">
								<Text className="text-sm opacity-70 mb-1 text-[#3E2E28] dark:text-[var(--foreground)]">
									Income
								</Text>
								<View className="flex-row items-center flex-wrap gap-2 mb-1">
									<Text className="text-xl font-bold text-[#2D1F16] dark:text-[var(--foreground)]">
										₹4,99,100
									</Text>
									<Text className="text-xs font-medium text-emerald-600 dark:text-emerald-400">
										↑ 565.47%
									</Text>
								</View>
								<Text className="text-xs opacity-60 leading-relaxed text-[#3E2E28] dark:text-[var(--foreground)]">
									Compared to ₹75,000 last month
								</Text>
							</View>

							{/* Expense */}
							<View className="flex-1">
								<Text className="text-sm opacity-70 mb-1 text-[#3E2E28] dark:text-[var(--foreground)]">
									Expense
								</Text>
								<View className="flex-row items-center flex-wrap gap-2 mb-1">
									<Text className="text-xl font-bold text-[#2D1F16] dark:text-[var(--foreground)]">
										₹92,628
									</Text>
									<Text className="text-xs font-medium text-rose-600 dark:text-rose-400">
										↑ 92.97%
									</Text>
								</View>
								<Text className="text-xs opacity-60 leading-relaxed text-[#3E2E28] dark:text-[var(--foreground)]">
									Compared to ₹48,000 last month
								</Text>
							</View>
						</View>
					</View>
				</View>
			</View>

			{/* My Cards Section */}
			<View className="pl-6 mb-8">
				<View className="flex-row justify-between items-center mb-4 pr-6">
					<Text className="text-xl font-bold text-[var(--foreground)]">
						My Cards
					</Text>
					<TouchableOpacity
						onPress={() => router.push("/wallets")}
						className="bg-[var(--card)] border border-[var(--border)] px-3 py-1 rounded-lg shadow-sm"
					>
						<Text className="text-sm font-medium text-[var(--muted-foreground)]">
							View All
						</Text>
					</TouchableOpacity>
				</View>

				<ScrollView
					horizontal
					showsHorizontalScrollIndicator={false}
					contentContainerStyle={{ paddingRight: 24 }}
				>
					<View className="w-80 mr-4">
						<CreditCardComponent
							type="VISA"
							number="9038 4061 **** ****"
							holder="Tashif"
							exp="02/28"
							gradient="from-[var(--chart-2)] to-[var(--chart-1)]"
							icon={Wifi}
						/>
					</View>
					<View className="w-80 mr-4">
						<CreditCardComponent
							type="Cash"
							number="Physical"
							holder="Tashif"
							exp="--"
							gradient="from-green-600 to-teal-700"
							icon={Banknote}
							isCash={true}
						/>
					</View>
					<TouchableOpacity className="w-20 h-56 bg-[var(--muted)] rounded-[32px] items-center justify-center border-2 border-dashed border-[var(--border)] active:bg-[var(--muted)]/80">
						<Plus size={24} color="var(--muted-foreground)" />
					</TouchableOpacity>
				</ScrollView>
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
