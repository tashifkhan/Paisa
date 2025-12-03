import { Tabs, router } from "expo-router";
import { Home, LayoutGrid, Plus, User, Wallet } from "lucide-react-native";
import React from "react";
import { TouchableOpacity, View, useColorScheme } from "react-native";

const Colors = {
	light: {
		card: "#ffffff",
		border: "#cec9d9",
		primary: "#8a79ab",
		mutedForeground: "#6b6880",
	},
	dark: {
		card: "#232030",
		border: "#302c40",
		primary: "#a995c9",
		mutedForeground: "#a09aad",
	},
};

export default function TabLayout() {
	const colorScheme = useColorScheme() ?? "light";
	const theme = Colors[colorScheme];

	return (
		<Tabs
			screenOptions={{
				headerShown: false,
				tabBarStyle: {
					position: "absolute",
					bottom: 0,
					left: 0,
					right: 0,
					backgroundColor: theme.card,
					borderTopWidth: 1,
					borderTopColor: theme.border,
					borderTopLeftRadius: 48, // 3rem
					borderTopRightRadius: 48, // 3rem
					height: 90,
					paddingBottom: 20,
					paddingHorizontal: 20,
					elevation: 0,
					shadowColor: "#000",
					shadowOffset: { width: 0, height: -5 },
					shadowOpacity: 0.03,
					shadowRadius: 20,
				},
				tabBarShowLabel: false,
				tabBarActiveTintColor: theme.primary,
				tabBarInactiveTintColor: theme.mutedForeground,
			}}
		>
			<Tabs.Screen
				name="index"
				options={{
					title: "Home",
					tabBarIcon: ({ color }: { color: string }) => (
						<Home size={24} color={color} />
					),
				}}
			/>
			<Tabs.Screen
				name="stats"
				options={{
					title: "Stats",
					tabBarIcon: ({ color }: { color: string }) => (
						<LayoutGrid size={24} color={color} />
					),
				}}
			/>
			<Tabs.Screen
				name="explore"
				options={{
					title: "Add",
					tabBarButton: (props: any) => (
						<TouchableOpacity
							{...props}
							onPress={() => router.push("/add-expense")}
							style={{
								top: -30,
								justifyContent: "center",
								alignItems: "center",
							}}
						>
							<View
								style={{
									backgroundColor: theme.primary,
									padding: 16,
									borderRadius: 9999,
									borderWidth: 4,
									borderColor: colorScheme === "dark" ? "#1c1923" : "#f9f7fc",
									shadowColor: "#000",
									shadowOffset: { width: 0, height: -4 },
									shadowOpacity: 0.3,
									shadowRadius: 8,
									elevation: 8,
								}}
							>
								<Plus size={24} color="white" />
							</View>
						</TouchableOpacity>
					),
				}}
				listeners={{
					tabPress: (e) => {
						e.preventDefault();
						router.push("/add-expense");
					},
				}}
			/>
			<Tabs.Screen
				name="wallets"
				options={{
					title: "Wallets",
					tabBarIcon: ({ color }: { color: string }) => (
						<Wallet size={24} color={color} />
					),
				}}
			/>
			<Tabs.Screen
				name="profile"
				options={{
					title: "Profile",
					tabBarIcon: ({ color }: { color: string }) => (
						<User size={24} color={color} />
					),
				}}
			/>
		</Tabs>
	);
}
