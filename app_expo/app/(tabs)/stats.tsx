import CircularProgress from "@/components/CircularProgress";
import ExpenseChart from "@/components/ExpenseChart";
import StatsCard from "@/components/StatsCard";
import TransactionItem from "@/components/TransactionItem";
import {
	ArrowUpRight,
	ChevronDown,
	Home,
	Smartphone,
} from "lucide-react-native";
import React from "react";
import { ScrollView, Text, TouchableOpacity, View } from "react-native";

export default function StatsScreen() {
	return (
		<ScrollView
			className="flex-1 bg-[var(--background)]"
			contentContainerStyle={{ paddingBottom: 100 }}
		>
			<View className="flex-row justify-between items-start p-6 pt-12">
				<View>
					<Text className="text-2xl font-bold text-[var(--foreground)]">
						Analysis
					</Text>
					<Text className="text-sm text-[var(--muted-foreground)]">
						Detailed Breakdown
					</Text>
				</View>
				<TouchableOpacity className="flex-row items-center gap-1 px-4 py-2 bg-[var(--card)] rounded-full border border-[var(--border)] shadow-sm">
					<Text className="text-sm font-medium text-[var(--foreground)]">
						June
					</Text>
					<ChevronDown size={14} color="var(--foreground)" />
				</TouchableOpacity>
			</View>

			{/* Chart Section */}
			<View className="px-6 mx-6 p-4 bg-[var(--card)] rounded-[2.5rem] shadow-sm border border-[var(--border)] mb-6">
				<ExpenseChart />
			</View>

			{/* Stats Cards */}
			<View className="px-6 mb-8 flex-row justify-between gap-3">
				<StatsCard title="Day" amount="52" />
				<StatsCard title="Week" amount="403" />
				<StatsCard title="Month" amount="1,612" />
			</View>

			{/* Bills / Due Section */}
			<View className="px-6">
				<Text className="text-xl font-bold text-[var(--foreground)] mb-4">
					Bills & Payments
				</Text>

				{/* Insight Card */}
				<View className="bg-[var(--card)] p-4 rounded-[2rem] shadow-sm flex-row items-center justify-between border border-[var(--border)] mb-6">
					<View className="flex-row items-center gap-4">
						<View className="w-12 h-12 rounded-full bg-[var(--muted)] items-center justify-center">
							<ArrowUpRight size={20} color="var(--primary)" />
						</View>
						<View>
							<Text className="text-sm text-[var(--muted-foreground)]">
								You paid{" "}
								<Text className="font-bold text-[var(--foreground)]">
									₹50 more
								</Text>{" "}
								on{"\n"}your cell phone bill
							</Text>
						</View>
					</View>
					<TouchableOpacity className="px-3 py-1.5 bg-[var(--muted)] rounded-lg">
						<Text className="text-[var(--muted-foreground)] text-xs font-bold">
							Check
						</Text>
					</TouchableOpacity>
				</View>

				{/* Amount Paid Circle */}
				<View className="bg-[var(--card)] p-6 rounded-[2.5rem] shadow-sm border border-[var(--border)] flex-row items-center gap-6 mb-6">
					<CircularProgress
						value={75}
						max={100}
						size={80}
						color="stroke-[var(--chart-4)]"
						strokeWidth={8}
					/>
					<View>
						<Text className="text-[var(--muted-foreground)] text-sm mb-1">
							Total Paid
						</Text>
						<Text className="text-2xl font-bold text-[var(--foreground)]">
							₹883
						</Text>
						<Text className="text-[var(--muted-foreground)] text-xs mt-1">
							of ₹2,340 bills
						</Text>
					</View>
				</View>

				{/* Upcoming Dues */}
				<Text className="text-sm font-semibold text-[var(--muted-foreground)] uppercase tracking-wider mb-2 ml-2">
					Upcoming Dues
				</Text>
				<View className="gap-1">
					<TransactionItem
						icon={Home}
						title="Home Rent"
						subtitle="Due date: Mar 25"
						amount="339.30"
					/>
					<TransactionItem
						icon={Smartphone}
						title="Mobile Bill"
						subtitle="Due date: Mar 28"
						amount="55.00"
					/>
				</View>
			</View>
		</ScrollView>
	);
}
