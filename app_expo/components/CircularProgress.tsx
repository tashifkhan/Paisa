import React from "react";
import { View, useColorScheme } from "react-native";
import Svg, { Circle } from "react-native-svg";

interface CircularProgressProps {
	value: number;
	max: number;
	color: string;
	size?: number;
	strokeWidth?: number;
}

const CircularProgress = ({
	value,
	max,
	color,
	size = 60,
	strokeWidth = 6,
}: CircularProgressProps) => {
	const colorScheme = useColorScheme() ?? "light";
	const radius = (size - strokeWidth) / 2;
	const circumference = radius * 2 * Math.PI;
	const offset = circumference - (value / max) * circumference;

	// Map color classes to actual hex values based on theme
	const getStrokeColor = () => {
		if (colorScheme === "dark") {
			return "#d5cb8f"; // chart-4 dark
		}
		return "#cab35b"; // chart-4 light
	};

	const mutedColor = colorScheme === "dark" ? "#2e2b38" : "#e5e1ef";

	return (
		<View
			className="items-center justify-center"
			style={{ width: size, height: size }}
		>
			<Svg width={size} height={size} style={{ transform: [{ rotate: "-90deg" }] }}>
				<Circle
					cx={size / 2}
					cy={size / 2}
					r={radius}
					fill="transparent"
					stroke={mutedColor}
					strokeWidth={strokeWidth}
				/>
				<Circle
					cx={size / 2}
					cy={size / 2}
					r={radius}
					fill="transparent"
					stroke={getStrokeColor()}
					strokeWidth={strokeWidth}
					strokeDasharray={circumference}
					strokeDashoffset={offset}
					strokeLinecap="round"
				/>
			</Svg>
		</View>
	);
};

export default CircularProgress;
