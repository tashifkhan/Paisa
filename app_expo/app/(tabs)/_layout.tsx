import type { BottomTabBarProps } from "@react-navigation/bottom-tabs";
import { Tabs, router, useSegments } from "expo-router";
import { Plus } from "lucide-react-native";
import React from "react";
import { Pressable, StyleSheet, View } from "react-native";
import { BottomNavigation, useTheme } from "react-native-paper";
import { useSafeAreaInsets } from "react-native-safe-area-context";

/**
 * Map expo-router route names → Paper BottomNavigation.Bar route descriptors.
 * Icons are Material Community Icons names (bundled with Paper).
 */
const ROUTE_CONFIG: Record<
	string,
	{ title: string; focusedIcon: string; unfocusedIcon: string }
> = {
	index: {
		title: "Home",
		focusedIcon: "home",
		unfocusedIcon: "home-outline",
	},
	stats: {
		title: "Stats",
		focusedIcon: "view-grid",
		unfocusedIcon: "view-grid-outline",
	},
	wallets: {
		title: "Wallets",
		focusedIcon: "wallet",
		unfocusedIcon: "wallet-outline",
	},
	social: {
		title: "Social",
		focusedIcon: "account-group",
		unfocusedIcon: "account-group-outline",
	},
	profile: {
		title: "Profile",
		focusedIcon: "account",
		unfocusedIcon: "account-outline",
	},
};

const HREF_MAP: Record<string, string> = {
	index: "/",
	stats: "/stats",
	wallets: "/wallets",
	social: "/social",
	profile: "/profile",
};

const NAV_BAR_H = 80;

/**
 * Custom tab bar using react-native-paper's BottomNavigation.Bar.
 * This gives us the real M3 navigation bar: animated pill indicators,
 * proper platform ripple, label typography, shifting/compact modes,
 * and safe-area handling — all out of the box.
 */
function PaperTabBar({ state }: BottomTabBarProps) {
	// Build the Paper navigation state, skipping the hidden "add" route
	const visibleRoutes: Array<{
		key: string;
		title: string;
		focusedIcon: string;
		unfocusedIcon: string;
	}> = [];
	const expoIndexMap: number[] = []; // maps Paper index → expo state index

	state.routes.forEach((route, expoIdx) => {
		const cfg = ROUTE_CONFIG[route.name];
		if (!cfg) return; // skip "add"
		expoIndexMap.push(expoIdx);
		visibleRoutes.push({ key: route.name, ...cfg });
	});

	// Translate the expo state.index to our filtered Paper index
	let paperIndex = expoIndexMap.indexOf(state.index);
	if (paperIndex === -1) paperIndex = 0; // fallback to Home

	return (
		<BottomNavigation.Bar
			navigationState={{ index: paperIndex, routes: visibleRoutes }}
			onTabPress={({ route }) => {
				const href = HREF_MAP[route.key];
				if (href) router.navigate(href as any);
			}}
			shifting={false}
			labeled={true}
			compact={false}
		/>
	);
}

export default function TabLayout() {
	const theme = useTheme();
	const insets = useSafeAreaInsets();
	const segments = useSegments();

	const currentSegment = segments[segments.length - 1];
	const isWallets = currentSegment === "wallets";
	const isSocial = currentSegment === "social";
	const isProfile = currentSegment === "profile";

	return (
		<View style={{ flex: 1 }}>
			<Tabs
				screenOptions={{ headerShown: false }}
				tabBar={(props) => <PaperTabBar {...props} />}
			>
				<Tabs.Screen name="index" />
				<Tabs.Screen name="stats" />
				<Tabs.Screen
					name="add"
					listeners={{
						tabPress: (e) => {
							e.preventDefault();
							router.push("/add-expense");
						},
					}}
				/>
				<Tabs.Screen name="wallets" />
				<Tabs.Screen name="social" />
				<Tabs.Screen name="profile" />
			</Tabs>

			{/* M3 FAB — floating above the navigation bar, bottom-right */}
			{!isProfile && (
				<Pressable
					onPress={() => router.push(isSocial ? "/add-debt" : isWallets ? "/add-wallet" : "/add-expense")}
					android_ripple={{
						color: theme.colors.onPrimary + "30",
						borderless: true,
					}}
					style={[
						styles.fab,
						{
							backgroundColor: theme.colors.primaryContainer,
							bottom: NAV_BAR_H + (insets.bottom || 0),
							shadowColor: theme.colors.shadow,
						},
					]}
				>
					<Plus
						size={26}
						color={theme.colors.onPrimaryContainer}
						strokeWidth={2.5}
					/>
				</Pressable>
			)}
		</View>
	);
}

const styles = StyleSheet.create({
	fab: {
		position: "absolute",
		right: 16,
		width: 56,
		height: 56,
		borderRadius: 16,
		alignItems: "center",
		justifyContent: "center",
		elevation: 6,
		shadowOffset: { width: 0, height: 3 },
		shadowOpacity: 0.25,
		shadowRadius: 8,
		zIndex: 100,
	},
});
