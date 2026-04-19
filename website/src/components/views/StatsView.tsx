import { ArrowUpRight, Calendar, TrendingDown, TrendingUp } from "lucide-react";
import { useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { useCategories } from "@/hooks/useCategories";
import { useDebtSummary, useDebts } from "@/hooks/useDebts";
import { useStatsComparison, useStatsFull } from "@/hooks/useStats";
import { useTransactions } from "@/hooks/useTransactions";
import { useWallets } from "@/hooks/useWallets";
import type {
	BackendTransaction,
} from "../../services/types";
import { CircularProgress } from "../shared/CircularProgress";
import { EditExpenseModal } from "../shared/EditExpenseModal";
import { ExpenseChart } from "../shared/ExpenseChart";
import { StatsCard } from "../shared/StatsCard";
import { TransactionItem } from "../shared/TransactionItem";
import {
	Select,
	SelectContent,
	SelectItem,
	SelectTrigger,
	SelectValue,
} from "../ui/select";

interface CategoryStat {
	category_id?: string;
	category_name: string;
	total: number;
	percentage: number;
	count: number;
}

export const StatsView = () => {
	const queryClient = useQueryClient();
	const [activeTab, setActiveTab] = useState("Overview");
	const [period, setPeriod] = useState("30");
	const days = useMemo(() => parseInt(period), [period]);
	const { data: fullStats, isLoading: fullLoading } = useStatsFull(days);
	const { data: comparison, isLoading: comparisonLoading } = useStatsComparison(days);
	const { data: debtSummary, isLoading: debtSummaryLoading } = useDebtSummary();
	const { data: allDebts = [], isLoading: debtsLoading } = useDebts();
	const { data: transactions = [], isLoading: txLoading } = useTransactions({
		limit: 100,
	});
	const { data: allCategories = [], isLoading: categoriesLoading } = useCategories();
	const { data: allWallets = [], isLoading: walletsLoading } = useWallets();
	const loading =
		fullLoading ||
		comparisonLoading ||
		debtSummaryLoading ||
		debtsLoading ||
		txLoading ||
		categoriesLoading ||
		walletsLoading;
	const totalExpense = fullStats?.total_expense ?? 0;
	const totalIncome = fullStats?.total_income ?? 0;
	const categories: CategoryStat[] = fullStats?.by_category ?? [];
	const upcomingDues = useMemo(() => {
		const now = new Date();
		return allDebts
			.filter((d) => d.due_date && new Date(d.due_date) >= now)
			.sort(
				(a, b) =>
					new Date(a.due_date || "").getTime() -
					new Date(b.due_date || "").getTime(),
			)
			.slice(0, 5);
	}, [allDebts]);

	// Transaction list state
	const [editingTransaction, setEditingTransaction] =
		useState<BackendTransaction | null>(null);
	const [transactionTypeFilter, setTransactionTypeFilter] = useState<
		"all" | "expense" | "income"
	>("all");

	const getCategoryById = (id?: string) => {
		if (!id) return null;
		return allCategories.find((c) => c.id === id);
	};

	const getWalletById = (id?: string) => {
		if (!id) return null;
		return allWallets.find((w) => w.id === id);
	};

	const filteredTransactions = transactions.filter((t) => {
		if (transactionTypeFilter === "all") return true;
		return t.type === transactionTypeFilter;
	});

	const formatCurrency = (amount: number) => {
		return new Intl.NumberFormat("en-IN", {
			style: "currency",
			currency: "INR",
			minimumFractionDigits: 0,
			maximumFractionDigits: 0,
		}).format(amount);
	};

	return (
		<div className="flex flex-col h-full bg-(--background) pb-24 md:pb-6 overflow-y-auto hide-scrollbar transition-colors duration-300">
			<div className="max-w-7xl mx-auto w-full">
				<header className="flex justify-between items-start p-6 bg-(--background) pb-4 transition-colors duration-300">
					<div className="flex flex-col">
						<h1 className="text-2xl font-bold text-(--foreground) transition-colors duration-300">
							Analysis
						</h1>
						<p className="text-sm text-(--muted-foreground)">
							{loading ? "Loading..." : "Detailed Breakdown"}
						</p>
					</div>
					<div className="flex items-center gap-2">
						<Select value={period} onValueChange={setPeriod}>
							<SelectTrigger className="w-[120px]">
								<SelectValue placeholder="Select period" />
							</SelectTrigger>
							<SelectContent>
								<SelectItem value="7">7 days</SelectItem>
								<SelectItem value="30">30 days</SelectItem>
								<SelectItem value="90">90 days</SelectItem>
								<SelectItem value="180">6 months</SelectItem>
								<SelectItem value="365">1 year</SelectItem>
							</SelectContent>
						</Select>
					</div>
				</header>

				<div className="md:grid md:grid-cols-12 md:gap-8 md:px-6">
					<div className="md:col-span-8">
						{/* Chart Section */}
						<div className="px-6 md:px-0 bg-(--card) mx-6 md:mx-0 p-4 rounded-[2.5rem] shadow-sm border border-(--border) z-10 transition-colors duration-300 mb-6">
							<ExpenseChart period={parseInt(period)} />
						</div>

						{/* Stats Cards */}
						<div className="px-6 md:px-0 mb-8">
							<div className="flex justify-between items-center bg-(--muted) rounded-4xl p-1 text-sm font-medium mb-4">
								{["Overview", "Expenses", "Income", "Transactions"].map(
									(tab) => (
										<button
											key={tab}
											onClick={() => setActiveTab(tab)}
											className={`flex-1 py-3 rounded-4xl transition-all text-xs md:text-sm ${
												activeTab === tab
													? "bg-(--primary) text-(--primary-foreground) shadow-md"
													: "text-(--muted-foreground)"
											}`}
										>
											{tab}
										</button>
									),
								)}
							</div>

							{activeTab === "Overview" && (
								<div className="flex justify-between gap-3">
									<StatsCard
										title="Income"
										amount={formatCurrency(totalIncome).replace("₹", "")}
									/>
									<StatsCard
										title="Expense"
										amount={formatCurrency(totalExpense).replace("₹", "")}
									/>
									<StatsCard
										title="Net"
										amount={formatCurrency(totalIncome - totalExpense).replace(
											"₹",
											"",
										)}
									/>
								</div>
							)}

							{activeTab === "Expenses" && (
								<div className="space-y-3">
									<div className="flex justify-between gap-3 mb-4">
										<StatsCard
											title="Total"
											amount={formatCurrency(totalExpense).replace("₹", "")}
										/>
										{comparison && (
											<div className="flex-1 bg-(--card) p-4 rounded-3xl shadow-sm border border-(--border)">
												<div className="text-xs text-(--muted-foreground) mb-1">
													vs Previous
												</div>
												<div
													className={`flex items-center gap-1 font-bold ${
														comparison.expense.change_percent > 0
															? "text-red-500"
															: "text-green-500"
													}`}
												>
													{comparison.expense.change_percent > 0 ? (
														<TrendingUp size={16} />
													) : (
														<TrendingDown size={16} />
													)}
													{Math.abs(comparison.expense.change_percent).toFixed(
														1,
													)}
													%
												</div>
											</div>
										)}
									</div>

									{/* Category breakdown */}
									<div className="space-y-2">
										{categories.slice(0, 5).map((cat, idx) => (
											<div
												key={cat.category_id || idx}
												className="flex items-center justify-between p-3 bg-(--card) rounded-2xl border border-(--border)"
											>
												<div className="flex items-center gap-3">
													<div className="w-10 h-10 rounded-full bg-(--muted) flex items-center justify-center">
														<span className="text-sm font-bold">
															{cat.category_name.charAt(0)}
														</span>
													</div>
													<div>
														<div className="font-medium text-(--foreground)">
															{cat.category_name}
														</div>
														<div className="text-xs text-(--muted-foreground)">
															{cat.count} transactions
														</div>
													</div>
												</div>
												<div className="text-right">
													<div className="font-bold text-(--foreground)">
														{formatCurrency(cat.total)}
													</div>
													<div className="text-xs text-(--muted-foreground)">
														{cat.percentage.toFixed(1)}%
													</div>
												</div>
											</div>
										))}
									</div>
								</div>
							)}

							{activeTab === "Income" && (
								<div className="flex justify-between gap-3">
									<StatsCard
										title="Total"
										amount={formatCurrency(totalIncome).replace("₹", "")}
									/>
									{comparison && (
										<div className="flex-1 bg-(--card) p-4 rounded-3xl shadow-sm border border-(--border)">
											<div className="text-xs text-(--muted-foreground) mb-1">
												vs Previous
											</div>
											<div
												className={`flex items-center gap-1 font-bold ${
													comparison.income.change_percent > 0
														? "text-green-500"
														: "text-red-500"
												}`}
											>
												{comparison.income.change_percent > 0 ? (
													<TrendingUp size={16} />
												) : (
													<TrendingDown size={16} />
												)}
												{Math.abs(comparison.income.change_percent).toFixed(1)}%
											</div>
										</div>
									)}
								</div>
							)}
						</div>

						{activeTab === "Transactions" && (
							<div className="space-y-4">
								{/* Filter Row */}
								<div className="flex items-center gap-3">
									<div className="flex bg-(--muted) rounded-xl p-1 text-xs font-medium">
										{(["all", "expense", "income"] as const).map((type) => (
											<button
												key={type}
												onClick={() => setTransactionTypeFilter(type)}
												className={`px-3 py-1.5 rounded-lg transition-all capitalize ${
													transactionTypeFilter === type
														? "bg-(--card) text-(--foreground) shadow-sm"
														: "text-(--muted-foreground)"
												}`}
											>
												{type}
											</button>
										))}
									</div>
									<span className="text-xs text-(--muted-foreground)">
										{filteredTransactions.length} transactions
									</span>
								</div>

								{/* Transaction List */}
								<div className="space-y-6 max-h-[600px] overflow-y-auto pr-2">
									{loading ? (
										// Skeleton Loading State
										Array.from({ length: 3 }).map((_, i) => (
											<div key={i} className="space-y-3">
												<div className="h-4 w-24 bg-(--muted) rounded animate-pulse mb-2" />
												{Array.from({ length: 2 }).map((_, j) => (
													<div
														key={j}
														className="flex items-center justify-between p-4 bg-(--card) rounded-2xl border border-(--border)"
													>
														<div className="flex items-center gap-3">
															<div className="w-12 h-12 rounded-full bg-(--muted) animate-pulse" />
															<div className="space-y-2">
																<div className="h-4 w-32 bg-(--muted) rounded animate-pulse" />
																<div className="h-3 w-24 bg-(--muted) rounded animate-pulse" />
															</div>
														</div>
														<div className="space-y-2">
															<div className="h-5 w-20 bg-(--muted) rounded animate-pulse ml-auto" />
															<div className="h-3 w-12 bg-(--muted) rounded animate-pulse ml-auto" />
														</div>
													</div>
												))}
											</div>
										))
									) : filteredTransactions.length === 0 ? (
										<div className="text-center py-12 text-(--muted-foreground)">
											<p>No transactions found</p>
										</div>
									) : (
										// Grouped Transactions
										Object.entries(
											filteredTransactions.reduce(
												(groups, txn) => {
													const date = txn.date
														? new Date(txn.date).toISOString().split("T")[0]
														: "Unknown";
													if (!groups[date]) groups[date] = [];
													groups[date].push(txn);
													return groups;
												},
												{} as Record<string, BackendTransaction[]>,
											),
										)
											.sort((a, b) => b[0].localeCompare(a[0])) // Sort by date descending
											.map(([date, txns]) => (
												<div key={date}>
													<h3 className="text-xs font-bold text-(--muted-foreground) mb-3 sticky top-0 bg-(--background)/80 backdrop-blur-md py-3 z-10 uppercase tracking-widest pl-2">
														{(() => {
															if (date === "Unknown") return "Unknown Date";
															const today = new Date()
																.toISOString()
																.split("T")[0];
															const yesterday = new Date(Date.now() - 86400000)
																.toISOString()
																.split("T")[0];
															if (date === today) return "Today";
															if (date === yesterday) return "Yesterday";
															return new Date(date).toLocaleDateString(
																"en-IN",
																{
																	weekday: "short",
																	day: "numeric",
																	month: "short",
																},
															);
														})()}
													</h3>
													<div className="space-y-3">
														{txns.map((txn) => {
															const category = getCategoryById(txn.category_id);
															const wallet = getWalletById(txn.wallet_id);
															return (
																<div
																	key={txn.id}
																	onClick={() => setEditingTransaction(txn)}
																	className="group cursor-pointer flex items-center justify-between p-4 bg-(--card) rounded-2xl border border-transparent hover:border-(--border)/50 hover:shadow-lg hover:-translate-y-0.5 transition-all duration-300 relative"
																>
																	<div className="flex items-center gap-4">
																		<div
																			className="w-12 h-12 rounded-full flex items-center justify-center shadow-xs transition-transform group-hover:scale-110"
																			style={{
																				backgroundColor: category?.color
																					? `${category.color}20`
																					: "#f3f4f6", // 20 hex = ~12% opacity
																				color: category?.color || "#6b7280",
																			}}
																		>
																			{/* You typically want icons here, fallback to initial */}
																			{/* Using CSS safe check charAt */}
																			<span className="text-lg font-bold">
																				{category?.name?.charAt(0) ||
																					txn.type?.charAt(0)?.toUpperCase()}
																			</span>
																		</div>
																		<div>
																			<div className="font-semibold text-(--foreground) text-base group-hover:text-(--primary) transition-colors">
																				{txn.note ||
																					category?.name ||
																					(txn.type === "income"
																						? "Income"
																						: "Expense")}
																			</div>
																			<div className="text-xs text-(--muted-foreground) flex items-center gap-2 mt-0.5">
																				{wallet && (
																					<span className="flex items-center gap-1 bg-(--muted) px-2 py-0.5 rounded-full">
																						<div className="w-1.5 h-1.5 rounded-full bg-(--muted-foreground)/50" />
																						{wallet.name}
																					</span>
																				)}
																				{txn.type === "income" && (
																					<span className="text-green-500 bg-green-500/10 px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider">
																						Income
																					</span>
																				)}
																			</div>
																		</div>
																	</div>
																	<div className="text-right">
																		<div
																			className={`text-lg font-bold tracking-tight ${
																				txn.type === "income"
																					? "text-green-500"
																					: "text-(--foreground)"
																			}`}
																		>
																			{txn.type === "income" ? "+" : "-"}
																			{formatCurrency(txn.amount).replace(
																				"₹",
																				"",
																			)}
																		</div>
																		<div className="text-xs text-(--muted-foreground) font-medium mt-1">
																			{formatCurrency(txn.amount).split(".")[0]}
																		</div>
																	</div>
																	<ArrowUpRight
																		size={16}
																		className="text-(--muted-foreground) opacity-0 group-hover:opacity-100 transition-opacity absolute right-4 top-1/2 -translate-y-1/2"
																	/>
																</div>
															);
														})}
													</div>
												</div>
											))
									)}
								</div>
							</div>
						)}
					</div>

					<div className="md:col-span-4">
						{/* Debts Overview */}
						<div className="px-6 md:px-0">
							<h2 className="text-xl font-bold text-(--foreground) mb-4">
								Debts Overview
							</h2>

							{debtSummary && (
								<div className="bg-linear-to-br from-(--card) to-(--muted)/30 p-6 rounded-[2.5rem] shadow-sm border border-(--border)/50 flex items-center gap-8 mb-6 relative overflow-hidden">
									{/* Decorative background element */}
									<div className="absolute top-0 right-0 w-32 h-32 bg-(--primary)/5 rounded-full blur-3xl -mr-10 -mt-10 pointer-events-none" />

									<div className="relative z-10">
										<CircularProgress
											value={debtSummary.owed_to_me}
											max={debtSummary.owed_to_me + debtSummary.owed_by_me || 1}
											size={80}
											color="stroke-(--primary)"
											strokeWidth={8}
										/>
									</div>
									<div className="flex-1 relative z-10 grid grid-cols-2 gap-4">
										<div>
											<div className="text-(--muted-foreground) text-xs uppercase tracking-wider font-semibold mb-1">
												Owed to You
											</div>
											<div className="text-2xl font-black text-green-500 tracking-tight">
												{formatCurrency(debtSummary.owed_to_me)}
											</div>
										</div>
										<div>
											<div className="text-(--muted-foreground) text-xs uppercase tracking-wider font-semibold mb-1">
												You Owe
											</div>
											<div className="text-xl font-bold text-(--foreground)">
												{formatCurrency(debtSummary.owed_by_me)}
											</div>
										</div>
									</div>
								</div>
							)}

							{/* Insight Card */}
							<div className="bg-(--card) p-4 rounded-4xl shadow-sm flex items-center justify-between border border-(--border) mb-6">
								<div className="flex items-center gap-4">
									<div className="w-12 h-12 rounded-full bg-(--muted) flex items-center justify-center text-(--primary) font-bold">
										<ArrowUpRight size={20} />
									</div>
									<div>
										<p className="text-sm text-(--muted-foreground)">
											{comparison && comparison.expense.change_percent > 0 ? (
												<>
													You spent{" "}
													<span className="font-bold text-(--foreground)">
														{comparison.expense.change_percent.toFixed(0)}% more
													</span>{" "}
													than
													<br />
													last period
												</>
											) : (
												<>
													You saved{" "}
													<span className="font-bold text-(--foreground)">
														{Math.abs(
															comparison?.expense.change_percent || 0,
														).toFixed(0)}
														%
													</span>{" "}
													compared to
													<br />
													last period
												</>
											)}
										</p>
									</div>
								</div>
							</div>

							{/* Upcoming Dues */}
							<h3 className="text-sm font-semibold text-(--muted-foreground) uppercase tracking-wider mb-2 ml-2">
								Upcoming Dues
							</h3>
							<div className="space-y-1">
								{upcomingDues.length > 0 ? (
									upcomingDues.map((debt) => (
										<TransactionItem
											key={debt.id}
											icon={Calendar}
											title={debt.counterparty_name}
											subtitle={
												debt.due_date
													? `Due: ${new Date(debt.due_date).toLocaleDateString(
															"en-IN",
															{ month: "short", day: "numeric" },
														)}`
													: "No due date"
											}
											amount={debt.amount.toFixed(2)}
										/>
									))
								) : (
									<div className="text-center py-4 text-(--muted-foreground) text-sm">
										No upcoming dues
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
					wallets={allWallets}
					onClose={() => setEditingTransaction(null)}
					onSave={() => {
						setEditingTransaction(null);
						queryClient.invalidateQueries({ queryKey: ["transactions"] });
						queryClient.invalidateQueries({ queryKey: ["wallets"] });
						queryClient.invalidateQueries({ queryKey: ["categories"] });
						queryClient.invalidateQueries({
							queryKey: ["stats", "full", days],
						});
						queryClient.invalidateQueries({
							queryKey: ["stats", "comparison", days],
						});
					}}
					onDelete={() => {
						setEditingTransaction(null);
						queryClient.invalidateQueries({ queryKey: ["transactions"] });
						queryClient.invalidateQueries({ queryKey: ["wallets"] });
						queryClient.invalidateQueries({ queryKey: ["categories"] });
						queryClient.invalidateQueries({
							queryKey: ["stats", "full", days],
						});
						queryClient.invalidateQueries({
							queryKey: ["stats", "comparison", days],
						});
					}}
				/>
			)}
		</div>
	);
};
