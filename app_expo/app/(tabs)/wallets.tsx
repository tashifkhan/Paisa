import CreditCardComponent from "@/components/CreditCard";
import {
	Banknote,
	Cpu,
	CreditCard as CreditCardIcon,
	MoreHorizontal,
	Wifi,
} from "lucide-react-native";
import React, { useState } from "react";
import { ScrollView, Text, TouchableOpacity, View } from "react-native";

export default function WalletsScreen() {
	const [activeWalletTab, setActiveWalletTab] = useState("Cards"); // Cards, Virtual, Cash

	return (
		<ScrollView
			className="flex-1 bg-[var(--background)]"
			contentContainerStyle={{ paddingBottom: 100 }}
		>
			<View className="flex-row justify-between items-center p-6 pt-12">
				<View>
					<Text className="text-3xl font-bold text-[var(--foreground)]">
						My Wallets
					</Text>
					<Text className="text-[var(--muted-foreground)] text-sm">
						Manage your cards & cash
					</Text>
				</View>
				<TouchableOpacity className="p-2 bg-[var(--card)] border border-[var(--border)] rounded-full shadow-sm">
					<MoreHorizontal size={20} color="var(--foreground)" />
				</TouchableOpacity>
			</View>

			{/* Tabs */}
			<View className="px-6 mb-6">
				<View className="flex-row justify-between items-center bg-[var(--muted)] rounded-[2rem] p-1">
					{["Cards", "Virtual", "Cash"].map((tab) => (
						<TouchableOpacity
							key={tab}
							onPress={() => setActiveWalletTab(tab)}
							className={`flex-1 py-3 rounded-[2rem] items-center justify-center ${
								activeWalletTab === tab ? "bg-[var(--primary)] shadow-md" : ""
							}`}
						>
							<Text
								className={`text-sm font-medium ${
									activeWalletTab === tab
										? "text-[var(--primary-foreground)]"
										: "text-[var(--muted-foreground)]"
								}`}
							>
								{tab}
							</Text>
						</TouchableOpacity>
					))}
				</View>
			</View>

			{/* Cards List */}
			<View className="px-6">
				{activeWalletTab === "Cards" && (
					<>
						<Text className="text-sm font-semibold text-[var(--muted-foreground)] uppercase tracking-wider mb-4 ml-2">
							Physical Cards
						</Text>
						<CreditCardComponent
							type="VISA"
							number="9038 4061 **** ****"
							holder="Tashif Ahmad Khan"
							exp="02/28"
							gradient="from-[var(--chart-2)] to-[var(--chart-1)]"
							icon={Wifi}
						/>
						<CreditCardComponent
							type="Mastercard"
							number="5500 1234 **** ****"
							holder="Tashif Ahmad Khan"
							exp="11/26"
							gradient="from-[var(--chart-3)] to-[var(--chart-5)]"
							icon={CreditCardIcon}
						/>
					</>
				)}

				{activeWalletTab === "Virtual" && (
					<>
						<Text className="text-sm font-semibold text-[var(--muted-foreground)] uppercase tracking-wider mb-4 ml-2">
							Virtual Cards
						</Text>
						<CreditCardComponent
							type="VISA Platinum"
							number="4111 1234 **** ****"
							holder="Tashif Ahmad Khan"
							exp="09/29"
							gradient="from-gray-800 to-gray-900"
							icon={Cpu}
							isVirtual={true}
						/>
					</>
				)}

				{activeWalletTab === "Cash" && (
					<>
						<Text className="text-sm font-semibold text-[var(--muted-foreground)] uppercase tracking-wider mb-4 ml-2">
							Cash on Hand
						</Text>
						<CreditCardComponent
							type="Cash Wallet"
							number="Physical Cash"
							holder="Tashif Ahmad Khan"
							exp="--"
							gradient="from-green-600 to-teal-700"
							icon={Banknote}
							isCash={true}
						/>
					</>
				)}

				{/* Add New Card Button */}
				<TouchableOpacity className="w-full py-4 border-2 border-dashed border-[var(--border)] rounded-[2rem] flex-row items-center justify-center gap-2 mt-4 active:bg-[var(--muted)]">
					<View className="w-6 h-6 rounded-full bg-[var(--primary)] items-center justify-center">
						<Text className="text-[var(--primary-foreground)] text-lg leading-none pb-1">
							+
						</Text>
					</View>
					<Text className="text-[var(--muted-foreground)] font-medium">
						Add New {activeWalletTab === "Cash" ? "Entry" : "Card"}
					</Text>
				</TouchableOpacity>
			</View>
		</ScrollView>
	);
}
