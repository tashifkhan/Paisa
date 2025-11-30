import { ChevronDown, Home, Smartphone, ArrowUpRight } from "lucide-react";
import { ExpenseChart } from "../shared/ExpenseChart";
import { StatsCard } from "../shared/StatsCard";
import { CircularProgress } from "../shared/CircularProgress";
import { TransactionItem } from "../shared/TransactionItem";

export const StatsView = () => (
	<div className="flex flex-col h-full bg-(--background) pb-24 overflow-y-auto hide-scrollbar transition-colors duration-300">
		<header className="flex justify-between items-start p-6 bg-(--background) pb-4 transition-colors duration-300">
			<div className="flex flex-col">
				<h1 className="text-2xl font-bold text-(--foreground) transition-colors duration-300">
					Analysis
				</h1>
				<p className="text-sm text-(--muted-foreground)">
					Detailed Breakdown
				</p>
			</div>
			<div className="flex items-center gap-2">
				<button className="flex items-center gap-1 px-4 py-2 bg-(--card) rounded-full text-sm font-medium text-(--foreground) border border-(--border) transition-colors duration-300 shadow-sm">
					June <ChevronDown size={14} />
				</button>
			</div>
		</header>

		{/* Chart Section */}
		<div className="px-6 bg-(--card) mx-6 p-4 rounded-[2.5rem] shadow-sm border border-(--border) z-10 transition-colors duration-300 mb-6">
			<ExpenseChart />
		</div>

		{/* Stats Cards */}
		<div className="px-6 mb-8">
			<div className="flex justify-between gap-3">
				<StatsCard title="Day" amount="52" />
				<StatsCard title="Week" amount="403" />
				<StatsCard title="Month" amount="1,612" />
			</div>
		</div>

		{/* Bills / Due Section (Integrated here) */}
		<div className="px-6">
			<h2 className="text-xl font-bold text-(--foreground) mb-4">
				Bills & Payments
			</h2>

			{/* Insight Card */}
			<div className="bg-(--card) p-4 rounded-4xl shadow-sm flex items-center justify-between border border-(--border) mb-6">
				<div className="flex items-center gap-4">
					<div className="w-12 h-12 rounded-full bg-(--muted) flex items-center justify-center text-(--primary) font-bold">
						<ArrowUpRight size={20} />
					</div>
					<div>
						<p className="text-sm text-(--muted-foreground)">
							You paid{" "}
							<span className="font-bold text-(--foreground)">
								₹50 more
							</span>{" "}
							on
							<br />
							your cell phone bill
						</p>
					</div>
				</div>
				<button className="px-3 py-1.5 bg-(--muted) text-(--muted-foreground) text-xs font-bold rounded-lg">
					Check
				</button>
			</div>

			{/* Amount Paid Circle */}
			<div className="bg-(--card) p-6 rounded-[2.5rem] shadow-sm border border-(--border) flex items-center gap-6 mb-6">
				<CircularProgress
					value={75}
					max={100}
					size={80}
					color="stroke-(--chart-4)"
					strokeWidth={8}
				/>
				<div>
					<div className="text-(--muted-foreground) text-sm mb-1">
						Total Paid
					</div>
					<div className="text-2xl font-bold text-(--foreground)">
						₹883
					</div>
					<div className="text-(--muted-foreground) text-xs mt-1">
						of ₹2,340 bills
					</div>
				</div>
			</div>

			{/* Upcoming Dues */}
			<h3 className="text-sm font-semibold text-(--muted-foreground) uppercase tracking-wider mb-2 ml-2">
				Upcoming Dues
			</h3>
			<div className="space-y-1">
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
			</div>
		</div>
	</div>
);
