import React, { useEffect } from "react";
import { Text, View } from "react-native";
import Animated, {
	useAnimatedStyle,
	useSharedValue,
	withTiming,
} from "react-native-reanimated";

const ExpenseChart = () => {
	const data = [
		{ label: "1", height: "60%", color: "bg-[var(--chart-1)]", text: "12%" },
		{ label: "5", height: "20%", color: "bg-[var(--chart-2)]", text: "3%" },
		{ label: "10", height: "30%", color: "bg-[var(--chart-3)]", text: "5%" },
		{ label: "15", height: "90%", color: "bg-[var(--chart-4)]", text: "32%" },
		{ label: "20", height: "70%", color: "bg-[var(--chart-5)]", text: "21%" },
		{ label: "25", height: "40%", color: "bg-[var(--chart-1)]", text: "7%" },
		{ label: "31", height: "50%", color: "bg-[var(--chart-2)]", text: "13%" },
		{ label: "1", height: "35%", color: "bg-[var(--chart-3)]", text: "5%" },
	];

	return (
		<View className="w-full h-64 flex-row items-end justify-between px-2 mt-6 mb-2">
			{data.map((item, index) => (
				<Bar key={index} item={item} />
			))}
		</View>
	);
};

const Bar = ({ item }: { item: any }) => {
	const height = useSharedValue(0);

	useEffect(() => {
		// Parse percentage string to number for animation
		const h = parseFloat(item.height);
		height.value = withTiming(h, { duration: 500 });
	}, []);

	const animatedStyle = useAnimatedStyle(() => {
		return {
			height: `${height.value}%`,
		};
	});

	return (
		<View className="flex-col items-center justify-end h-full w-8 gap-2">
			<Text className="text-xs font-semibold text-[var(--muted-foreground)] mb-1">
				{item.text}
			</Text>
			<Animated.View
				className={`w-4 rounded-full ${item.color}`}
				style={animatedStyle}
			/>
			<Text className="text-xs text-[var(--muted-foreground)] mt-2">
				{item.label}
			</Text>
		</View>
	);
};

export default ExpenseChart;
