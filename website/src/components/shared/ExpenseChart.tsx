import { Loader2 } from "lucide-react";
import { useEffect, useState } from "react";
import { statsService } from "../../services/statsService";

interface DailyData {
	date: string;
	day: number;
	amount: number;
	percentage: number;
}

interface ExpenseChartProps {
	period?: number;
}

const CHART_COLORS = [
	"bg-(--chart-1)",
	"bg-(--chart-2)",
	"bg-(--chart-3)",
	"bg-(--chart-4)",
	"bg-(--chart-5)",
];

export const ExpenseChart = ({ period = 30 }: ExpenseChartProps) => {
	const [loading, setLoading] = useState(true);
	const [dailyData, setDailyData] = useState<DailyData[]>([]);

	useEffect(() => {
		const fetchData = async () => {
			setLoading(true);
			try {
				const response = await statsService.getDailyBreakdown(period);
				// Only show days with data or sample evenly across the period
				const data = response.daily;
				// Sample 8 data points for display
				const step = Math.max(1, Math.floor(data.length / 8));
				const sampled = data.filter((_, idx) => idx % step === 0).slice(0, 8);
				setDailyData(sampled);
			} catch (error) {
				console.error("Failed to fetch daily breakdown:", error);
				setDailyData([]);
			} finally {
				setLoading(false);
			}
		};

		fetchData();
	}, [period]);

	if (loading) {
		return (
			<div className="w-full h-64 flex items-center justify-center">
				<Loader2 className="animate-spin text-(--muted-foreground)" size={32} />
			</div>
		);
	}

	if (dailyData.length === 0 || dailyData.every((d) => d.amount === 0)) {
		return (
			<div className="w-full h-64 flex items-center justify-center text-(--muted-foreground)">
				No expense data for this period
			</div>
		);
	}

	// Calculate max percentage for scaling
	const maxPercentage = Math.max(...dailyData.map((d) => d.percentage), 1);

	return (
		<div className="w-full h-64 flex items-end justify-between px-2 mt-6 mb-2">
			{dailyData.map((item, index) => {
				const heightPercent = (item.percentage / maxPercentage) * 80 + 10; // Min 10% height
				return (
					<div
						key={item.date}
						className="flex flex-col items-center justify-end h-full w-8 gap-2"
					>
						<span className="text-xs font-semibold text-(--muted-foreground) mb-1">
							{item.percentage > 0 ? `${item.percentage.toFixed(0)}%` : ""}
						</span>
						<div
							className={`w-4 rounded-full ${
								CHART_COLORS[index % CHART_COLORS.length]
							} transition-colors duration-300`}
							style={{
								height: `${heightPercent}%`,
								transition: "height 0.5s ease-in-out",
							}}
						></div>
						<span className="text-xs text-(--muted-foreground) mt-2">
							{item.day}
						</span>
					</div>
				);
			})}
		</div>
	);
};
