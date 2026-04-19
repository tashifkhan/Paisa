import {
	Banknote,
	Bell,
	Car,
	Eye,
	EyeOff,
	Film,
	Loader2,
	Moon,
	Plus,
	ShoppingBag,
	Sun,
	Utensils,
	Wifi,
	Zap,
} from "lucide-react";
import { useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { useCategories } from "@/hooks/useCategories";
import { useProfile } from "@/hooks/useProfile";
import { useStatsComparison } from "@/hooks/useStats";
import { useTransactions } from "@/hooks/useTransactions";
import { useWalletTotal, useWallets } from "@/hooks/useWallets";
import type { BackendTransaction } from "../../services/types";
import { CreditCardComponent } from "../shared/CreditCardComponent";
import { EditExpenseModal } from "../shared/EditExpenseModal";
import { TransactionItem } from "../shared/TransactionItem";

interface HomeViewProps {
	isDarkMode: boolean;
	toggleTheme: () => void;
}

// Map category names to icons
const getCategoryIcon = (categoryName: string) => {
	const name = categoryName.toLowerCase();
	if (name.includes("food") || name.includes("dining")) return Utensils;
	if (name.includes("shopping")) return ShoppingBag;
	if (name.includes("transport")) return Car;
	if (name.includes("entertainment")) return Film;
	if (name.includes("bill") || name.includes("util")) return Zap;
	return ShoppingBag;
};

export const HomeView = ({
	isDarkMode,
	toggleTheme,
}: HomeViewProps) => {
	const queryClient = useQueryClient();
	const [showBalance, setShowBalance] = useState(true);
	const [editingTransaction, setEditingTransaction] =
		useState<BackendTransaction | null>(null);
	const { data: profile } = useProfile();
	const { data: allCategories = [] } = useCategories();
	const { data: wallets = [], isLoading: walletsLoading } = useWallets();
	const { data: totalData, isLoading: totalLoading } = useWalletTotal("INR");
	const { data: transactions = [], isLoading: txLoading } = useTransactions({
		limit: 50,
	});
	const { data: comparison } = useStatsComparison(30);

	const loading = walletsLoading || totalLoading || txLoading;
	const totalBalance = totalData?.total_balance ?? 0;
	const monthlyIncome = comparison?.income.current ?? 0;
	const monthlyExpense = comparison?.expense.current ?? 0;
	const userName = useMemo(
		() => profile?.name?.split(" ")[0] ?? "There",
		[profile?.name],
	);

	const formatCurrency = (amount: number, compact: boolean = false) => {
		if (compact && amount >= 100000) {
			return `₹${(amount / 100000).toFixed(2)}L`;
		}
		return new Intl.NumberFormat("en-IN", {
			style: "currency",
			currency: "INR",
			minimumFractionDigits: 0,
			maximumFractionDigits: 0,
		}).format(amount);
	};

	const getWalletGradient = (index: number) => {
		const gradients = [
			"from-(--chart-2) to-(--chart-1)",
			"from-green-600 to-teal-700",
			"from-(--chart-3) to-(--chart-5)",
		];
		return gradients[index % gradients.length];
	};

	return (
		<div className="flex flex-col h-full bg-(--background) overflow-y-auto hide-scrollbar transition-colors duration-300">
			<div className="max-w-7xl mx-auto w-full pb-24 md:pb-6">
				{/* Header */}
				<header className="flex justify-between items-center p-6 bg-transparent">
					<div className="flex flex-col">
						<h1 className="text-3xl font-bold text-(--foreground)">
							Hi, {userName}
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

				<div className="md:grid md:grid-cols-12 md:gap-8 md:px-6">
					<div className="md:col-span-7 lg:col-span-8">
						{/* Total Balance Card */}
						<div className="px-6 md:px-0 mb-8">
							<div className="bg-[#FFF6F1] dark:bg-(--card) text-[#3E2E28] dark:text-(--foreground) p-8 rounded-[2.5rem] shadow-sm border border-[#F5E6DE] dark:border-(--border) relative overflow-hidden transition-colors duration-300">
								{/* Gradient Blobs */}
								<div className="absolute -top-24 -right-24 w-64 h-64 bg-[#FADAC9] dark:bg-(--primary)/10 rounded-full blur-3xl opacity-60"></div>
								<div className="absolute -bottom-24 -left-24 w-64 h-64 bg-[#FADAC9] dark:bg-(--primary)/10 rounded-full blur-3xl opacity-60"></div>

								<div className="relative z-10">
									<div className="flex justify-between items-start mb-2">
										<div className="text-lg font-medium opacity-80">
											Total balance
										</div>
										<button
											onClick={() => setShowBalance(!showBalance)}
											className="opacity-60 hover:opacity-100 transition-opacity"
										>
											{showBalance ? <EyeOff size={24} /> : <Eye size={24} />}
										</button>
									</div>

									<div className="text-5xl font-bold mb-8 tracking-tight text-[#2D1F16] dark:text-(--foreground)">
										{loading ? (
											<Loader2 className="animate-spin" size={40} />
										) : showBalance ? (
											formatCurrency(totalBalance)
										) : (
											"₹••••••"
										)}
									</div>

									<div className="mb-6 text-lg font-semibold text-[#3E2E28] dark:text-(--foreground)">
										This month
									</div>

									<div className="grid grid-cols-2 gap-8">
										{/* Income */}
										<div>
											<div className="text-sm opacity-70 mb-1">Income</div>
											<div className="flex items-center flex-wrap gap-2 mb-1">
												<span className="text-xl font-bold text-[#2D1F16] dark:text-(--foreground)">
													{showBalance
														? formatCurrency(monthlyIncome, true)
														: "₹••••"}
												</span>
												{comparison && (
													<span
														className={`text-xs font-medium flex items-center ${
															comparison.income.change_percent >= 0
																? "text-emerald-600 dark:text-emerald-400"
																: "text-rose-600 dark:text-rose-400"
														}`}
													>
														{comparison.income.change_percent >= 0 ? "↑" : "↓"}{" "}
														{Math.abs(comparison.income.change_percent).toFixed(
															1,
														)}
														%
													</span>
												)}
											</div>
											{comparison && (
												<div className="text-xs opacity-60 leading-relaxed">
													Compared to{" "}
													{formatCurrency(comparison.income.previous, true)}{" "}
													last month
												</div>
											)}
										</div>

										{/* Expense */}
										<div>
											<div className="text-sm opacity-70 mb-1">Expense</div>
											<div className="flex items-center flex-wrap gap-2 mb-1">
												<span className="text-xl font-bold text-[#2D1F16] dark:text-(--foreground)">
													{showBalance
														? formatCurrency(monthlyExpense, true)
														: "₹••••"}
												</span>
												{comparison && (
													<span
														className={`text-xs font-medium flex items-center ${
															comparison.expense.change_percent <= 0
																? "text-emerald-600 dark:text-emerald-400"
																: "text-rose-600 dark:text-rose-400"
														}`}
													>
														{comparison.expense.change_percent >= 0 ? "↑" : "↓"}{" "}
														{Math.abs(
															comparison.expense.change_percent,
														).toFixed(1)}
														%
													</span>
												)}
											</div>
											{comparison && (
												<div className="text-xs opacity-60 leading-relaxed">
													Compared to{" "}
													{formatCurrency(comparison.expense.previous, true)}{" "}
													last month
												</div>
											)}
										</div>
									</div>
								</div>
							</div>
						</div>

						{/* Cards Section */}
						<div className="pl-6 md:pl-0 pb-2 mb-6 md:mb-0">
							<div className="flex justify-between items-center mb-4 pr-6">
								<h2 className="text-xl font-bold text-(--foreground)">
									My Cards
								</h2>
								<button
									onClick={() => (window.location.href = "/wallets")}
									className="text-sm font-medium text-(--muted-foreground) bg-(--card) border border-(--border) px-3 py-1 rounded-lg shadow-sm"
								>
									View All
								</button>
							</div>
							<div className="flex overflow-x-auto hide-scrollbar pb-4 pr-6 md:grid md:grid-cols-2 lg:grid-cols-3 md:gap-4 md:overflow-visible">
								{loading ? (
									<div className="flex items-center justify-center w-full py-8">
										<Loader2
											className="animate-spin text-(--muted-foreground)"
											size={32}
										/>
									</div>
								) : wallets.length > 0 ? (
									<>
									{wallets.slice(0, 3).map((wallet, index) => (
											<div
												key={wallet.id}
												className="mr-4 md:mr-0 flex-shrink-0"
											>
												<CreditCardComponent
													type={wallet.name}
													number={
														showBalance
															? formatCurrency(wallet.balance)
															: "₹••••"
													}
													holder={wallet.type || "Personal"}
													exp={wallet.currency}
													gradient={getWalletGradient(index)}
													icon={
														wallet.type?.toLowerCase() === "cash"
															? Banknote
															: Wifi
													}
													isCash={wallet.type?.toLowerCase() === "cash"}
												/>
											</div>
										))}
										<div
											onClick={() => (window.location.href = "/add-wallet")}
											className="w-16 h-56 md:w-full md:h-56 bg-(--muted) rounded-4xl flex items-center justify-center flex-shrink-0 border-2 border-dashed border-(--border) cursor-pointer hover:bg-(--muted)/80 transition-colors"
										>
											<Plus size={24} className="text-(--muted-foreground)" />
										</div>
									</>
								) : (
									<>
										<div
											onClick={() => (window.location.href = "/add-wallet")}
											className="w-16 h-56 md:w-full md:h-56 bg-(--muted) rounded-4xl flex items-center justify-center flex-shrink-0 border-2 border-dashed border-(--border) cursor-pointer hover:bg-(--muted)/80 transition-colors"
										>
											<Plus size={24} className="text-(--muted-foreground)" />
										</div>
									</>
								)}
							</div>
						</div>
					</div>

					<div className="md:col-span-5 lg:col-span-4">
						{/* Spending Analysis Title & List */}
						<div className="px-6 md:px-0 pb-24 md:pb-0">
							<div className="flex justify-between items-center mb-4">
								<h2 className="text-xl font-bold text-(--foreground)">
									Spending Analysis
								</h2>
								<button
									onClick={() => (window.location.href = "/stats")}
									className="text-sm font-medium text-(--muted-foreground) bg-(--card) border border-(--border) px-3 py-1 rounded-lg shadow-sm"
								>
									See All
								</button>
							</div>

							<div className="space-y-1">
								{transactions.length > 0 ? (
									transactions.slice(0, 5).map((t) => {
										const category = allCategories.find(
											(c) => c.id === t.category_id,
										);
										return (
											<TransactionItem
												key={t.id}
												icon={getCategoryIcon(
													t.note || category?.name || t.type || "Expense",
												)}
												title={t.note || category?.name || t.type || "Expense"}
												subtitle={t.currency}
												amount={t.amount.toString()}
												percent="0"
												onClick={() => setEditingTransaction(t)}
											/>
										);
									})
								) : (
									<div className="text-center py-8 text-(--muted-foreground)">
										No transactions yet
									</div>
								)}
							</div>
						</div>
					</div>
				</div>
			</div>

			{/* Edit Expense Modal */}
			{editingTransaction && (
				<EditExpenseModal
					transaction={editingTransaction}
					categories={allCategories}
					wallets={wallets}
					onClose={() => setEditingTransaction(null)}
					onSave={() => {
						setEditingTransaction(null);
						queryClient.invalidateQueries({ queryKey: ["transactions"] });
						queryClient.invalidateQueries({ queryKey: ["wallets"] });
						queryClient.invalidateQueries({ queryKey: ["wallets", "total"] });
						queryClient.invalidateQueries({
							queryKey: ["stats", "comparison", 30],
						});
					}}
					onDelete={() => {
						setEditingTransaction(null);
						queryClient.invalidateQueries({ queryKey: ["transactions"] });
						queryClient.invalidateQueries({ queryKey: ["wallets"] });
						queryClient.invalidateQueries({ queryKey: ["wallets", "total"] });
						queryClient.invalidateQueries({
							queryKey: ["stats", "comparison", 30],
						});
					}}
				/>
			)}
		</div>
	);
};
