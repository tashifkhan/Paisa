import React from "react";
import { StyleSheet, View, TouchableOpacity } from "react-native";
import { Card } from "@/components/ui/Card";
import { ThemedText } from "@/components/ThemedText";
import { IconSymbol } from "@/components/ui/IconSymbol";
import { useThemeColor } from "@/hooks/useThemeColor";

interface BalanceCardProps {
	balance?: number; // Make balance optional
	currency?: string;
	onSendPress?: () => void;
	onReceivePress?: () => void;
	onAddPress?: () => void;
}

export function BalanceCard({
	balance = 0, // Add default value
	currency = "USD",
	onSendPress,
	onReceivePress,
	onAddPress,
}: BalanceCardProps) {
	const iconColor = useThemeColor(
		{ light: "#808080", dark: "#A0A0A0" },
		"text"
	);

	// Format balance with safety check
	const formattedBalance =
		typeof balance === "number"
			? balance.toLocaleString("en-US", { style: "currency", currency })
			: "0";

	return (
		<Card elevation style={styles.container}>
			<View style={styles.balanceContainer}>
				<ThemedText style={styles.label}>Total Balance</ThemedText>
				<ThemedText style={styles.balance}>{formattedBalance}</ThemedText>
			</View>

			<View style={styles.actions}>
				<TouchableOpacity style={styles.action} onPress={onSendPress}>
					<IconSymbol name="arrow.up.circle.fill" size={24} color={iconColor} />
					<ThemedText style={styles.actionText}>Send</ThemedText>
				</TouchableOpacity>

				<TouchableOpacity style={styles.action} onPress={onReceivePress}>
					<IconSymbol
						name="arrow.down.circle.fill"
						size={24}
						color={iconColor}
					/>
					<ThemedText style={styles.actionText}>Receive</ThemedText>
				</TouchableOpacity>

				<TouchableOpacity style={styles.action} onPress={onAddPress}>
					<IconSymbol name="plus.circle.fill" size={24} color={iconColor} />
					<ThemedText style={styles.actionText}>Add</ThemedText>
				</TouchableOpacity>
			</View>
		</Card>
	);
}

const styles = StyleSheet.create({
	container: {
		margin: 16,
	},
	balanceContainer: {
		alignItems: "center",
		marginBottom: 24,
	},
	label: {
		fontSize: 16,
		marginBottom: 8,
		opacity: 0.7,
	},
	balance: {
		fontSize: 32,
		fontWeight: "bold",
	},
	actions: {
		flexDirection: "row",
		justifyContent: "space-around",
	},
	action: {
		alignItems: "center",
	},
	actionText: {
		marginTop: 4,
		fontSize: 12,
	},
});
