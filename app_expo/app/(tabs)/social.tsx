import { useRouter } from "expo-router";
import { MoreHorizontal, Plus, User, Users } from "lucide-react-native";
import { useColorScheme } from "nativewind";
import React, { useState } from "react";
import { ScrollView, Text, TouchableOpacity, View } from "react-native";

const DEBTS = [
	{
		id: 1,
		name: "Rahul Sharma",
		amount: 500,
		type: "owed_to_me",
		date: "Due in 3 days",
	},
	{
		id: 2,
		name: "Anita Roy",
		amount: 1200,
		type: "owed_by_me",
		date: "Due tomorrow",
	},
	{
		id: 3,
		name: "John Doe",
		amount: 250,
		type: "owed_to_me",
		date: "Due in 1 week",
	},
];

const GROUPS = [
	{
		id: 1,
		name: "Goa Trip",
		members: 5,
		balance: -2000,
		type: "owe",
		icon: "Plane",
		color: "bg-orange-500",
	},
	{
		id: 2,
		name: "Flat 302 Rent",
		members: 3,
		balance: 5000,
		type: "owed",
		icon: "Home",
		color: "bg-indigo-500",
	},
];

export default function SocialScreen() {
	const router = useRouter();
	const { colorScheme } = useColorScheme();
	const isDarkMode = colorScheme === "dark";
	const [activeTab, setActiveTab] = useState("debts");

	const netBalance = DEBTS.reduce(
		(acc, curr) =>
			curr.type === "owed_to_me" ? acc + curr.amount : acc - curr.amount,
		0
	);

	return (
		<View className="flex-1 bg-[var(--background)] pt-12">
			<View className="px-6 flex-row justify-between items-center mb-6">
				<View>
					<Text className="text-3xl font-bold text-[var(--foreground)]">
						Social
					</Text>
					<Text className="text-[var(--muted-foreground)] text-sm">
						Friends & Shared Expenses
					</Text>
				</View>
				<TouchableOpacity className="p-2 bg-[var(--card)] border border-[var(--border)] rounded-full shadow-sm">
					<MoreHorizontal size={24} color="var(--foreground)" />
				</TouchableOpacity>
			</View>

			{/* Tabs */}
			<View className="px-6 mb-6">
				<View className="flex-row bg-[var(--muted)] rounded-full p-1">
					<TouchableOpacity
						onPress={() => setActiveTab("debts")}
						className={`flex-1 py-3 rounded-full items-center ${
							activeTab === "debts" ? "bg-[var(--primary)] shadow-md" : ""
						}`}
					>
						<Text
							className={`font-medium ${
								activeTab === "debts"
									? "text-[var(--primary-foreground)]"
									: "text-[var(--muted-foreground)]"
							}`}
						>
							Friends
						</Text>
					</TouchableOpacity>
					<TouchableOpacity
						onPress={() => setActiveTab("groups")}
						className={`flex-1 py-3 rounded-full items-center ${
							activeTab === "groups" ? "bg-[var(--primary)] shadow-md" : ""
						}`}
					>
						<Text
							className={`font-medium ${
								activeTab === "groups"
									? "text-[var(--primary-foreground)]"
									: "text-[var(--muted-foreground)]"
							}`}
						>
							Groups
						</Text>
					</TouchableOpacity>
				</View>
			</View>

			<ScrollView
				className="flex-1 px-6"
				contentContainerStyle={{ paddingBottom: 100 }}
				showsVerticalScrollIndicator={false}
			>
				{activeTab === "debts" ? (
					<>
						{/* Net Balance Card */}
						<View
							className={`p-6 rounded-[32px] shadow-lg mb-6 overflow-hidden ${
								netBalance >= 0 ? "bg-[var(--chart-4)]" : "bg-[var(--chart-2)]"
							}`}
						>
							<View>
								<Text className="text-white/90 font-medium mb-1">
									Net Balance
								</Text>
								<Text className="text-4xl font-bold text-white mb-2">
									{netBalance >= 0 ? "+" : "-"}₹{Math.abs(netBalance)}
								</Text>
								<Text className="text-white/80 text-xs">
									{netBalance >= 0
										? "You are overall in credit"
										: "You are overall in debt"}
								</Text>
							</View>
						</View>

						{/* Debts List */}
						<View className="gap-3">
							{DEBTS.map((debt) => (
								<TouchableOpacity
									key={debt.id}
									onPress={() => router.push("/user-detail")}
									className="flex-row items-center justify-between p-4 bg-[var(--card)] border border-[var(--border)] rounded-[24px]"
								>
									<View className="flex-row items-center gap-4">
										<View
											className={`w-12 h-12 rounded-full items-center justify-center ${
												debt.type === "owed_to_me"
													? "bg-green-100 dark:bg-green-900/30"
													: "bg-red-100 dark:bg-red-900/30"
											}`}
										>
											<User
												size={20}
												color={
													debt.type === "owed_to_me" ? "#16a34a" : "#dc2626"
												}
											/>
										</View>
										<View>
											<Text className="font-bold text-[var(--foreground)] text-lg">
												{debt.name}
											</Text>
											<Text className="text-xs text-[var(--muted-foreground)]">
												{debt.date}
											</Text>
										</View>
									</View>
									<View className="items-end">
										<Text
											className={`font-bold text-lg ${
												debt.type === "owed_to_me"
													? "text-green-500"
													: "text-red-500"
											}`}
										>
											{debt.type === "owed_to_me" ? "+" : "-"}₹{debt.amount}
										</Text>
										<Text className="text-xs text-[var(--muted-foreground)]">
											{debt.type === "owed_to_me" ? "Credit" : "Debt"}
										</Text>
									</View>
								</TouchableOpacity>
							))}

							<TouchableOpacity className="w-full py-4 border-2 border-dashed border-[var(--border)] rounded-[32px] flex-row items-center justify-center gap-2 mt-4 active:bg-[var(--muted)]">
								<View className="w-6 h-6 rounded-full bg-[var(--primary)] items-center justify-center">
									<Plus size={16} color="white" />
								</View>
								<Text className="text-[var(--muted-foreground)] font-medium">
									Add New Contact
								</Text>
							</TouchableOpacity>
						</View>
					</>
				) : (
					/* Groups Tab */
					<View className="gap-4">
						{GROUPS.map((group) => (
							<TouchableOpacity
								key={group.id}
								onPress={() => router.push("/group-detail")}
								className="bg-[var(--card)] border border-[var(--border)] rounded-[32px] p-5"
							>
								<View className="flex-row justify-between items-start mb-4">
									<View className="flex-row items-center gap-4">
										<View
											className={`w-12 h-12 rounded-full ${group.color} items-center justify-center shadow-md`}
										>
											<Users size={20} color="white" />
										</View>
										<View>
											<Text className="font-bold text-[var(--foreground)] text-lg">
												{group.name}
											</Text>
											<View className="flex-row items-center gap-1">
												<Users size={12} color="var(--muted-foreground)" />
												<Text className="text-[var(--muted-foreground)] text-xs">
													{group.members} Members
												</Text>
											</View>
										</View>
									</View>
									<TouchableOpacity>
										<MoreHorizontal size={20} color="var(--muted-foreground)" />
									</TouchableOpacity>
								</View>
								<View className="flex-row justify-between items-center p-3 bg-[var(--muted)] rounded-2xl">
									<Text className="text-sm text-[var(--muted-foreground)]">
										Your share
									</Text>
									<Text
										className={`font-bold ${
											group.type === "owed" ? "text-green-500" : "text-red-500"
										}`}
									>
										{group.type === "owed" ? "+" : "-"}₹
										{Math.abs(group.balance)}
									</Text>
								</View>
							</TouchableOpacity>
						))}

						<TouchableOpacity
							onPress={() => router.push("/create-group")}
							className="w-full py-4 bg-[var(--primary)] rounded-[32px] shadow-lg flex-row items-center justify-center gap-2 mt-4"
						>
							<Users size={20} color="white" />
							<Text className="text-white font-bold text-lg">
								Create New Group
							</Text>
						</TouchableOpacity>
					</View>
				)}
			</ScrollView>
		</View>
	);
}
