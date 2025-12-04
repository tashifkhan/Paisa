import { useRouter } from "expo-router";
import { ArrowLeft, Plus, Receipt, Settings, Users } from "lucide-react-native";
import React, { useState } from "react";
import { ScrollView, Text, TouchableOpacity, View } from "react-native";

export default function GroupDetailScreen() {
	const router = useRouter();
	const [activeTab, setActiveTab] = useState("expenses");

	const group = {
		name: "Goa Trip",
		members: 5,
		balance: -2000,
		expenses: [
			{
				id: 1,
				title: "Dinner at Thalassa",
				amount: 4500,
				paidBy: "Tashif",
				date: "Yesterday",
			},
			{
				id: 2,
				title: "Scooty Rental",
				amount: 1200,
				paidBy: "Rahul",
				date: "Today",
			},
		],
	};

	return (
		<View className="flex-1 bg-[var(--background)]">
			<View className="flex-row justify-between items-center p-6 pt-12">
				<TouchableOpacity
					onPress={() => router.back()}
					className="p-2 bg-[var(--card)] border border-[var(--border)] rounded-full shadow-sm"
				>
					<ArrowLeft size={20} color="var(--foreground)" />
				</TouchableOpacity>
				<Text className="text-xl font-bold text-[var(--foreground)]">
					Group Details
				</Text>
				<TouchableOpacity className="p-2">
					<Settings size={24} color="var(--foreground)" />
				</TouchableOpacity>
			</View>

			<View className="px-6 mb-6">
				<View className="bg-[var(--card)] border border-[var(--border)] rounded-[32px] p-6 items-center">
					<View className="w-16 h-16 rounded-full bg-orange-500 items-center justify-center mb-3">
						<Users size={32} color="white" />
					</View>
					<Text className="text-2xl font-bold text-[var(--foreground)] mb-1">
						{group.name}
					</Text>
					<Text className="text-[var(--muted-foreground)] mb-4">
						{group.members} Members
					</Text>

					<View className="w-full h-[1px] bg-[var(--border)] mb-4" />

					<View className="flex-row justify-between w-full px-4">
						<View className="items-center">
							<Text className="text-xs text-[var(--muted-foreground)] mb-1">
								Total Expenses
							</Text>
							<Text className="text-lg font-bold text-[var(--foreground)]">
								₹15,400
							</Text>
						</View>
						<View className="items-center">
							<Text className="text-xs text-[var(--muted-foreground)] mb-1">
								Your Share
							</Text>
							<Text className="text-lg font-bold text-red-500">-₹2,000</Text>
						</View>
					</View>
				</View>
			</View>

			<View className="px-6 mb-4">
				<View className="flex-row bg-[var(--muted)] rounded-full p-1">
					<TouchableOpacity
						onPress={() => setActiveTab("expenses")}
						className={`flex-1 py-3 rounded-full items-center ${
							activeTab === "expenses" ? "bg-[var(--primary)] shadow-md" : ""
						}`}
					>
						<Text
							className={`font-medium ${
								activeTab === "expenses"
									? "text-[var(--primary-foreground)]"
									: "text-[var(--muted-foreground)]"
							}`}
						>
							Expenses
						</Text>
					</TouchableOpacity>
					<TouchableOpacity
						onPress={() => setActiveTab("balances")}
						className={`flex-1 py-3 rounded-full items-center ${
							activeTab === "balances" ? "bg-[var(--primary)] shadow-md" : ""
						}`}
					>
						<Text
							className={`font-medium ${
								activeTab === "balances"
									? "text-[var(--primary-foreground)]"
									: "text-[var(--muted-foreground)]"
							}`}
						>
							Balances
						</Text>
					</TouchableOpacity>
				</View>
			</View>

			<ScrollView className="flex-1 px-6">
				{activeTab === "expenses" ? (
					<View className="gap-3 pb-24">
						{group.expenses.map((expense) => (
							<View
								key={expense.id}
								className="flex-row justify-between items-center p-4 bg-[var(--card)] border border-[var(--border)] rounded-2xl"
							>
								<View className="flex-row items-center gap-3">
									<View className="w-10 h-10 rounded-full bg-[var(--muted)] items-center justify-center">
										<Receipt size={20} color="var(--foreground)" />
									</View>
									<View>
										<Text className="font-bold text-[var(--foreground)]">
											{expense.title}
										</Text>
										<Text className="text-xs text-[var(--muted-foreground)]">
											Paid by {expense.paidBy} • {expense.date}
										</Text>
									</View>
								</View>
								<Text className="font-bold text-[var(--foreground)]">
									₹{expense.amount}
								</Text>
							</View>
						))}
					</View>
				) : (
					<View className="items-center justify-center py-10">
						<Text className="text-[var(--muted-foreground)]">
							Balances view coming soon
						</Text>
					</View>
				)}
			</ScrollView>

			<View className="absolute bottom-10 right-6">
				<TouchableOpacity className="w-14 h-14 bg-[var(--primary)] rounded-full items-center justify-center shadow-lg">
					<Plus size={24} color="white" />
				</TouchableOpacity>
			</View>
		</View>
	);
}
