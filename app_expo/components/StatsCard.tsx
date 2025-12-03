import React from "react";
import { Text, View } from "react-native";

interface StatsCardProps {
	title: string;
	amount: string;
}

const StatsCard = ({ title, amount }: StatsCardProps) => (
	<View className="bg-[var(--card)] p-5 rounded-[2rem] shadow-sm border border-[var(--border)] flex-col items-center justify-center min-w-[30%]">
		<Text className="text-xs text-[var(--muted-foreground)] mb-1">{title}</Text>
		<Text className="text-lg font-bold text-[var(--card-foreground)]">
			₹{amount}
		</Text>
	</View>
);

export default StatsCard;
