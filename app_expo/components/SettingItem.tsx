import { ChevronRight, LucideIcon } from "lucide-react-native";
import React from "react";
import { Switch, Text, TouchableOpacity, View } from "react-native";

interface SettingItemProps {
	icon: LucideIcon;
	title: string;
	value?: string;
	type?: "arrow" | "toggle";
	onClick?: () => void;
	isToggled?: boolean;
}

const SettingItem = ({
	icon: Icon,
	title,
	value,
	type = "arrow",
	onClick,
	isToggled,
}: SettingItemProps) => (
	<TouchableOpacity
		onPress={onClick}
		activeOpacity={0.7}
		className="w-full flex-row items-center justify-between p-4 bg-[var(--card)] border border-[var(--border)] rounded-2xl mb-3 active:bg-[var(--muted)]"
	>
		<View className="flex-row items-center gap-4">
			<View className="w-10 h-10 rounded-full bg-[var(--muted)] items-center justify-center">
				<Icon
					size={20}
					className="text-[var(--primary)]"
					color="var(--primary)"
				/>
			</View>
			<View>
				<Text className="font-medium text-[var(--foreground)] text-base">
					{title}
				</Text>
			</View>
		</View>

		<View className="flex-row items-center gap-2">
			{value && (
				<Text className="text-sm text-[var(--muted-foreground)]">{value}</Text>
			)}

			{type === "arrow" && (
				<ChevronRight
					size={18}
					className="text-[var(--muted-foreground)]"
					color="var(--muted-foreground)"
				/>
			)}

			{type === "toggle" && (
				<Switch
					value={isToggled}
					onValueChange={onClick}
					trackColor={{
						false: "var(--muted-foreground)",
						true: "var(--primary)",
					}}
					thumbColor={"white"}
				/>
			)}
		</View>
	</TouchableOpacity>
);

export default SettingItem;
