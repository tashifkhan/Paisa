import { useRouter } from "expo-router";
import { ArrowLeft, DollarSign, Mail, Phone } from "lucide-react-native";
import React from "react";
import { ScrollView, Text, TouchableOpacity, View } from "react-native";

export default function UserDetailScreen() {
	const router = useRouter();

	// Mock data - in a real app this would come from params or store
	const user = {
		name: "Rahul Sharma",
		email: "rahul.sharma@example.com",
		phone: "+91 98765 43210",
		balance: 500,
		type: "owed_to_me",
		history: [
			{
				id: 1,
				title: "Dinner",
				amount: 800,
				date: "Yesterday",
				type: "owed_to_me",
			},
			{
				id: 2,
				title: "Movie Tickets",
				amount: 300,
				date: "Last Week",
				type: "owed_by_me",
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
					Friend Details
				</Text>
				<View className="w-10" />
			</View>

			<ScrollView className="flex-1 px-6">
				<View className="items-center mb-8">
					<View className="w-24 h-24 rounded-full bg-green-100 dark:bg-green-900/30 items-center justify-center mb-4">
						<Text className="text-3xl font-bold text-green-600 dark:text-green-400">
							{user.name.charAt(0)}
						</Text>
					</View>
					<Text className="text-2xl font-bold text-[var(--foreground)]">
						{user.name}
					</Text>
					<Text className="text-[var(--muted-foreground)]">
						Added 2 months ago
					</Text>
				</View>

				<View className="bg-[var(--card)] border border-[var(--border)] rounded-[24px] p-6 mb-6">
					<View className="flex-row items-center gap-4 mb-4">
						<View className="w-10 h-10 rounded-full bg-[var(--muted)] items-center justify-center">
							<Mail size={20} color="var(--foreground)" />
						</View>
						<View>
							<Text className="text-xs text-[var(--muted-foreground)]">
								Email
							</Text>
							<Text className="text-[var(--foreground)] font-medium">
								{user.email}
							</Text>
						</View>
					</View>
					<View className="flex-row items-center gap-4">
						<View className="w-10 h-10 rounded-full bg-[var(--muted)] items-center justify-center">
							<Phone size={20} color="var(--foreground)" />
						</View>
						<View>
							<Text className="text-xs text-[var(--muted-foreground)]">
								Phone
							</Text>
							<Text className="text-[var(--foreground)] font-medium">
								{user.phone}
							</Text>
						</View>
					</View>
				</View>

				<Text className="text-lg font-bold text-[var(--foreground)] mb-4">
					Transaction History
				</Text>
				<View className="gap-3 mb-8">
					{user.history.map((item) => (
						<View
							key={item.id}
							className="flex-row justify-between items-center p-4 bg-[var(--card)] border border-[var(--border)] rounded-2xl"
						>
							<View className="flex-row items-center gap-3">
								<View className="w-10 h-10 rounded-full bg-[var(--muted)] items-center justify-center">
									<DollarSign size={20} color="var(--foreground)" />
								</View>
								<View>
									<Text className="font-bold text-[var(--foreground)]">
										{item.title}
									</Text>
									<Text className="text-xs text-[var(--muted-foreground)]">
										{item.date}
									</Text>
								</View>
							</View>
							<Text
								className={`font-bold ${
									item.type === "owed_to_me" ? "text-green-500" : "text-red-500"
								}`}
							>
								{item.type === "owed_to_me" ? "+" : "-"}₹{item.amount}
							</Text>
						</View>
					))}
				</View>

				<TouchableOpacity className="w-full py-4 bg-[var(--destructive)] rounded-[32px] items-center mb-8">
					<Text className="text-white font-bold">Remove Friend</Text>
				</TouchableOpacity>
			</ScrollView>
		</View>
	);
}
