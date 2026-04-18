import React from "react";
import { StyleSheet, View } from "react-native";
import { List, Switch, Text, TouchableRipple, useTheme } from "react-native-paper";

interface SettingItemProps {
	icon: string; // Material Community Icon name
	title: string;
	value?: string;
	type?: "arrow" | "toggle";
	onClick?: () => void;
	isToggled?: boolean;
}

const SettingItem = ({
	icon,
	title,
	value,
	type = "arrow",
	onClick,
	isToggled,
}: SettingItemProps) => {
	const theme = useTheme();

	return (
		<TouchableRipple
			onPress={type === "toggle" ? undefined : onClick}
			borderless
			style={[styles.container, { backgroundColor: theme.colors.surface }]}
			rippleColor={theme.colors.primary + "14"}
		>
			<View style={styles.row}>
				<View style={[styles.iconBox, { backgroundColor: theme.colors.primaryContainer }]}>
					<List.Icon icon={icon} color={theme.colors.primary} style={styles.icon} />
				</View>
				<Text variant="bodyLarge" style={[styles.title, { color: theme.colors.onSurface }]}>
					{title}
				</Text>
				<View style={styles.trailing}>
					{value && (
						<Text variant="bodyMedium" style={{ color: theme.colors.onSurfaceVariant }}>
							{value}
						</Text>
					)}
					{type === "arrow" && (
						<List.Icon icon="chevron-right" color={theme.colors.onSurfaceVariant} style={styles.icon} />
					)}
					{type === "toggle" && (
						<Switch
							value={isToggled}
							onValueChange={onClick}
							color={theme.colors.primary}
						/>
					)}
				</View>
			</View>
		</TouchableRipple>
	);
};

const styles = StyleSheet.create({
	container: {
		borderRadius: 16,
		marginBottom: 8,
		overflow: "hidden",
	},
	row: {
		flexDirection: "row",
		alignItems: "center",
		paddingVertical: 12,
		paddingHorizontal: 16,
	},
	iconBox: {
		width: 40,
		height: 40,
		borderRadius: 12,
		alignItems: "center",
		justifyContent: "center",
		marginRight: 16,
	},
	icon: {
		margin: 0,
	},
	title: {
		flex: 1,
		fontWeight: "500",
	},
	trailing: {
		flexDirection: "row",
		alignItems: "center",
		gap: 4,
	},
});

export default SettingItem;
