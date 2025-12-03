import SettingItem from "@/components/SettingItem";
import { useRouter } from "expo-router";
import {
	ArrowLeft,
	Bell,
	CircleHelp,
	DollarSign,
	Languages,
	LogOut,
	Moon,
	Shield,
	User,
} from "lucide-react-native";
import { useColorScheme } from "nativewind";
import React, { useState } from "react";
import { ScrollView, Text, TouchableOpacity, View } from "react-native";

export default function ProfileScreen() {
	const router = useRouter();
	const { colorScheme, toggleColorScheme } = useColorScheme();
	const isDarkMode = colorScheme === "dark";

	const [currency, setCurrency] = useState("INR");
	const [language, setLanguage] = useState("English");
	const [notifications, setNotifications] = useState(true);

	return (
		<ScrollView
			className="flex-1 bg-[var(--background)]"
			contentContainerStyle={{ paddingBottom: 100 }}
		>
			<View className="flex-row justify-between items-center p-6 pt-12">
				<TouchableOpacity
					onPress={() => router.back()}
					className="p-2 bg-[var(--card)] border border-[var(--border)] rounded-full shadow-sm"
				>
					<ArrowLeft size={20} color="var(--foreground)" />
				</TouchableOpacity>
				<Text className="text-xl font-bold text-[var(--foreground)]">
					Profile
				</Text>
				<View className="w-10" />
			</View>

			{/* Avatar Section */}
			<View className="items-center justify-center mb-8">
				<View className="w-28 h-28 rounded-full bg-[var(--chart-2)] p-1 mb-4 shadow-lg">
					<View className="w-full h-full rounded-full bg-[var(--card)] items-center justify-center overflow-hidden">
						<User
							size={48}
							className="text-[var(--foreground)] opacity-50"
							color="var(--foreground)"
						/>
					</View>
				</View>
				<Text className="text-2xl font-bold text-[var(--foreground)]">
					Tashif Ahmad Khan
				</Text>
				<Text className="text-[var(--muted-foreground)]">
					admin@tashif.codes
				</Text>
			</View>

			{/* Settings List */}
			<View className="px-6 gap-8">
				{/* General Section */}
				<View>
					<Text className="text-sm font-semibold text-[var(--muted-foreground)] uppercase tracking-wider mb-4 ml-1">
						General
					</Text>
					<SettingItem
						icon={Languages}
						title="Language"
						value={language}
						onClick={() =>
							setLanguage(language === "English" ? "Hindi" : "English")
						}
					/>
					<SettingItem
						icon={DollarSign}
						title="Currency"
						value={currency}
						onClick={() => setCurrency(currency === "INR" ? "USD" : "INR")}
					/>
					<SettingItem
						icon={Moon}
						title="Dark Mode"
						type="toggle"
						isToggled={isDarkMode}
						onClick={toggleColorScheme}
					/>
				</View>

				{/* Notifications Section */}
				<View>
					<Text className="text-sm font-semibold text-[var(--muted-foreground)] uppercase tracking-wider mb-4 ml-1">
						Notifications
					</Text>
					<SettingItem
						icon={Bell}
						title="Push Notifications"
						type="toggle"
						isToggled={notifications}
						onClick={() => setNotifications(!notifications)}
					/>
					<SettingItem icon={Shield} title="Security Alerts" type="arrow" />
				</View>

				{/* Support Section */}
				<View>
					<Text className="text-sm font-semibold text-[var(--muted-foreground)] uppercase tracking-wider mb-4 ml-1">
						Support
					</Text>
					<SettingItem icon={CircleHelp} title="Help & Support" />
					<TouchableOpacity className="w-full flex-row items-center justify-start p-4 gap-4 bg-[var(--destructive)]/10 rounded-2xl mt-4 active:bg-[var(--destructive)]/20">
						<View className="w-10 h-10 rounded-full bg-[var(--destructive)]/20 items-center justify-center">
							<LogOut size={20} color="var(--destructive)" />
						</View>
						<Text className="font-bold text-[var(--destructive)]">Log Out</Text>
					</TouchableOpacity>
				</View>
			</View>
		</ScrollView>
	);
}
