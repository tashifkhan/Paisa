import { LucideIcon } from "lucide-react-native";
import React from "react";
import { Text, TouchableOpacity, View } from "react-native";

interface TransactionItemProps {
	icon: LucideIcon;
	title: string;
	subtitle: string;
	amount: string;
	percent?: string;
}

const TransactionItem = ({
	icon: Icon,
	title,
	subtitle,
	amount,
	percent,
}: TransactionItemProps) => (
	<TouchableOpacity className="flex-row items-center justify-between py-4 px-3 rounded-3xl active:bg-[var(--muted)]">
		<View className="flex-row items-center gap-4">
			<View className="w-12 h-12 rounded-full items-center justify-center bg-[var(--muted)]">
				<Icon
					size={20}
					className="text-[var(--foreground)]"
					color="var(--foreground)"
				/>
			</View>
			<View>
				<Text className="font-bold text-[var(--foreground)] text-base">
					{title}
				</Text>
				<Text className="text-sm text-[var(--muted-foreground)]">
					{subtitle}
				</Text>
			</View>
		</View>
		<View className="items-end">
			<Text className="font-bold text-[var(--foreground)] text-base">
				₹{amount}
			</Text>
			{percent && (
				<Text className="text-xs text-[var(--muted-foreground)]">
					{percent}%
				</Text>
			)}
		</View>
	</TouchableOpacity>
);

export default TransactionItem;
