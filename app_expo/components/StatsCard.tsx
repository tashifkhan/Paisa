import React from "react";
import { StyleSheet } from "react-native";
import { Card, Text, useTheme } from "react-native-paper";

interface StatsCardProps {
	title: string;
	amount: string;
}

const StatsCard = ({ title, amount }: StatsCardProps) => {
	const theme = useTheme();

	return (
		<Card style={styles.card} elevation={0}>
			<Card.Content style={styles.content}>
				<Text variant="labelMedium" style={{ color: theme.colors.onSurfaceVariant, marginBottom: 4 }}>
					{title}
				</Text>
				<Text variant="titleLarge" style={{ fontWeight: "700" }}>
					{amount.startsWith("₹") ? amount : `₹${amount}`}
				</Text>
			</Card.Content>
		</Card>
	);
};

const styles = StyleSheet.create({
	card: {
		borderRadius: 20,
		flex: 1,
		minWidth: "30%",
	},
	content: {
		alignItems: "center",
		justifyContent: "center",
		paddingVertical: 16,
	},
});

export default StatsCard;
