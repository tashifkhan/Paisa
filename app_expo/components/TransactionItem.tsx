import React from "react";
import { StyleSheet, View } from "react-native";
import { List, Surface, Text, TouchableRipple, useTheme } from "react-native-paper";

interface TransactionItemProps {
	icon: string; // Material Community Icon name
	title: string;
	subtitle: string;
	amount: string;
	percent?: string;
	onPress?: () => void;
}

const TransactionItem = ({
	icon,
	title,
	subtitle,
	amount,
	percent,
	onPress,
}: TransactionItemProps) => {
	const theme = useTheme();

	return (
		<TouchableRipple
			onPress={onPress}
			borderless
			style={styles.container}
			rippleColor={theme.colors.primary + "14"}
		>
			<View style={styles.row}>
				<View style={[styles.iconBox, { backgroundColor: theme.colors.primaryContainer }]}>
					<List.Icon icon={icon} color={theme.colors.primary} style={styles.icon} />
				</View>
				<View style={styles.content}>
					<Text variant="titleSmall" style={{ fontWeight: "600" }}>
						{title}
					</Text>
					<Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>
						{subtitle}
					</Text>
				</View>
				<View style={styles.trailing}>
					<Text variant="titleSmall" style={{ fontWeight: "700" }}>
						{amount.startsWith("₹") ? amount : `₹${amount}`}
					</Text>
					{percent && (
						<Text variant="labelSmall" style={{ color: theme.colors.onSurfaceVariant }}>
							{percent}%
						</Text>
					)}
				</View>
			</View>
		</TouchableRipple>
	);
};

const styles = StyleSheet.create({
	container: {
		borderRadius: 16,
		overflow: "hidden",
	},
	row: {
		flexDirection: "row",
		alignItems: "center",
		paddingVertical: 12,
		paddingHorizontal: 12,
		gap: 12,
	},
	iconBox: {
		width: 44,
		height: 44,
		borderRadius: 14,
		alignItems: "center",
		justifyContent: "center",
	},
	icon: {
		margin: 0,
	},
	content: {
		flex: 1,
	},
	trailing: {
		alignItems: "flex-end",
	},
});

export default TransactionItem;
