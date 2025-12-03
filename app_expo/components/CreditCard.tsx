import { LinearGradient } from "expo-linear-gradient";
import { Banknote, Cpu, LucideIcon, Nfc, Wifi } from "lucide-react-native";
import React from "react";
import { Text, View } from "react-native";

interface CreditCardProps {
	type?: string;
	number?: string;
	holder?: string;
	exp?: string;
	gradient?: string;
	icon?: LucideIcon;
	isVirtual?: boolean;
	isCash?: boolean;
}

const CreditCardComponent = ({
	type = "VISA",
	number = "9038 4061 **** ****",
	holder = "Tashif Ahmad Khan",
	exp = "02/02",
	gradient = "from-[var(--chart-2)] to-[var(--chart-1)]",
	icon: Icon = Wifi,
	isVirtual = false,
	isCash = false,
}: CreditCardProps) => {
	// Map gradient classes to colors for LinearGradient
	const getColors = (): [string, string] => {
		if (isCash) return ["#16a34a", "#0f766e"]; // green-600 to teal-700
		if (isVirtual) return ["#1f2937", "#111827"]; // gray-800 to gray-900
		if (gradient.includes("chart-3"))
			return ["#2dd4bf", "#a78bfa"]; // chart-3 to chart-5 (approx)
		return ["#f472b6", "#c084fc"]; // chart-2 to chart-1 (approx)
	};

	return (
		<LinearGradient
			colors={getColors()}
			start={{ x: 0, y: 0 }}
			end={{ x: 1, y: 1 }}
			className={`relative w-full h-56 rounded-[2rem] p-6 shadow-md flex-col justify-between overflow-hidden mb-6 ${
				isVirtual ? "border-2 border-white/30" : ""
			}`}
		>
			{!isCash && (
				<View className="absolute top-0 right-0 w-32 h-32 bg-white/20 rounded-full -mr-10 -mt-10 blur-2xl" />
			)}

			<View className="flex-row justify-between items-start z-10">
				<View className="flex-col">
					<Text className="text-white font-bold text-lg tracking-wider opacity-90">
						{type}
					</Text>
					{isVirtual && (
						<Text className="text-white/70 text-xs font-medium">
							Virtual Card
						</Text>
					)}
				</View>
				<View className="flex-row items-center gap-2">
					{isVirtual && (
						<Cpu size={20} className="text-white opacity-80" color="white" />
					)}
					<Icon size={24} className="text-white opacity-80" color="white" />
				</View>
			</View>

			<View className="z-10">
				{isCash ? (
					<>
						<Text className="text-xs text-white/70 uppercase mb-1 font-medium">
							Total Cash
						</Text>
						<Text className="text-3xl font-bold text-white tracking-widest mb-4 shadow-sm">
							₹4,500.00
						</Text>
					</>
				) : (
					<Text className="text-2xl font-bold text-white tracking-widest mb-4 shadow-sm">
						{number}
					</Text>
				)}

				<View className="flex-row justify-between items-end">
					<View>
						<Text className="text-xs text-white/70 uppercase mb-1 font-medium">
							Card Holder
						</Text>
						<Text className="font-semibold text-white tracking-wide">
							{holder}
						</Text>
					</View>
					<View className="items-end">
						<Text className="text-xs text-white/70 uppercase mb-1 font-medium">
							Exp Date
						</Text>
						<Text className="font-semibold text-white tracking-wide">
							{exp}
						</Text>
					</View>
				</View>
			</View>

			{(type === "VISA" || type === "Mastercard") && !isVirtual && (
				<View className="absolute bottom-6 right-6 z-10 flex-row items-center gap-2">
					<Nfc
						size={20}
						className="text-white/70"
						color="rgba(255,255,255,0.7)"
					/>
					<View className="w-12 h-8 bg-white/20 backdrop-blur-sm rounded flex-row overflow-hidden border border-white/10">
						<View className="w-1/2 h-full border-r border-white/20" />
					</View>
				</View>
			)}

			{isCash && (
				<View className="absolute bottom-4 right-6 z-10 opacity-20">
					<Banknote size={64} className="text-white" color="white" />
				</View>
			)}
		</LinearGradient>
	);
};

export default CreditCardComponent;
