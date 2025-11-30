import { Bell, Sun, Moon, ArrowDownLeft, ArrowUpRight } from "lucide-react";
import { TransactionItem } from "../shared/TransactionItem";
import type { Transaction } from "../../types";

interface HomeViewProps {
	transactions: Transaction[];
	isDarkMode: boolean;
	toggleTheme: () => void;
}

export const HomeView = ({ transactions, isDarkMode, toggleTheme }: HomeViewProps) => (
	<div className="flex flex-col h-full bg-(--background) overflow-y-auto hide-scrollbar transition-colors duration-300">
		{/* Header */}
		<header className="flex justify-between items-center p-6 bg-transparent">
			<div className="flex flex-col">
				<h1 className="text-3xl font-bold text-(--foreground)">
					Hi, There
				</h1>
			</div>
			<div className="flex gap-3">
				<button
					onClick={toggleTheme}
					className="p-2 text-(--foreground) hover:bg-(--muted) rounded-full transition-colors"
				>
					{isDarkMode ? <Sun size={20} /> : <Moon size={20} />}
				</button>
				<button className="p-2 relative bg-(--card) rounded-full shadow-sm text-(--foreground) border border-(--border)">
					<Bell size={20} />
					<span className="absolute top-1.5 right-2 w-2 h-2 bg-(--destructive) rounded-full"></span>
				</button>
			</div>
		</header>

		{/* Total Balance Card */}
		<div className="px-6 mb-8">
			<div className="bg-(--primary) text-(--primary-foreground) p-6 rounded-4xl shadow-lg relative overflow-hidden">
				{/* Abstract blobs for visual interest */}
				<div className="absolute -top-10 -right-10 w-32 h-32 bg-white/20 rounded-full blur-2xl"></div>
				<div className="absolute bottom-0 left-0 w-24 h-24 bg-black/10 rounded-full blur-2xl"></div>

				<div className="relative z-10">
					<div className="text-sm font-medium opacity-90 mb-1">
						Total Balance
					</div>
					<div className="text-4xl font-bold mb-8">₹32,500.00</div>

					<div className="flex gap-4">
						<div className="flex-1 bg-black/20 rounded-2xl p-3 backdrop-blur-sm">
							<div className="flex items-center gap-1 text-xs opacity-90 mb-1">
								<div className="w-5 h-5 rounded-full bg-white/20 flex items-center justify-center">
									<ArrowDownLeft size={12} />
								</div>
								Income
							</div>
							<div className="font-semibold text-lg">₹4,200</div>
						</div>
						<div className="flex-1 bg-white/20 rounded-2xl p-3 backdrop-blur-sm">
							<div className="flex items-center gap-1 text-xs opacity-90 mb-1">
								<div className="w-5 h-5 rounded-full bg-black/10 flex items-center justify-center">
									<ArrowUpRight size={12} />
								</div>
								Expense
							</div>
							<div className="font-semibold text-lg">₹1,612</div>
						</div>
					</div>
				</div>
			</div>
		</div>

		{/* Spending Analysis Title & List */}
		<div className="px-6 pb-24">
			<div className="flex justify-between items-center mb-4">
				<h2 className="text-xl font-bold text-(--foreground)">
					Spending Analysis
				</h2>
				<button className="text-sm font-medium text-(--muted-foreground) bg-(--card) border border-(--border) px-3 py-1 rounded-lg shadow-sm">
					See All
				</button>
			</div>

			<div className="space-y-1">
				{transactions.map((t) => (
					<TransactionItem
						key={t.id}
						icon={t.icon}
						title={t.title}
						subtitle={t.subtitle}
						amount={t.amount}
						percent={t.percent}
					/>
				))}
			</div>
		</div>
	</div>
);
