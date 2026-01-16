import {
	ArrowUpRight,
	Calendar,
	Edit3,
	TrendingDown,
	TrendingUp,
} from "lucide-react";
import { useEffect, useState } from "react";
import { categoryService } from "../../services/categoryService";
import { debtService } from "../../services/debtService";
import { expenseService } from "../../services/expenseService";
import { statsService } from "../../services/statsService";
import type {
	BackendCategory,
	BackendTransaction,
	BackendWallet,
} from "../../services/types";
import { walletService } from "../../services/walletService";
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
	const [activeTab, setActiveTab] = useState("Overview");
	const [period, setPeriod] = useState("30");
	const [loading, setLoading] = useState(true);

	// Stats data
	const [totalExpense, setTotalExpense] = useState(0);
	const [totalIncome, setTotalIncome] = useState(0);
	const [categories, setCategories] = useState<CategoryStat[]>([]);
	const [comparison, setComparison] = useState<{
		income: { change_percent: number };
		expense: { change_percent: number };
	} | null>(null);
	const [debtSummary, setDebtSummary] = useState<{
		owed_to_me: number;
		owed_by_me: number;
	} | null>(null);
	const [upcomingDues, setUpcomingDues] = useState<
		Array<{
			id: string;
			counterparty_name: string;
			amount: number;
			due_date?: string;
		}>
	>([]);

	// Transaction list state
	const [transactions, setTransactions] = useState<BackendTransaction[]>([]);
	const [allCategories, setAllCategories] = useState<BackendCategory[]>([]);
	const [allWallets, setAllWallets] = useState<BackendWallet[]>([]);
	const [editingTransaction, setEditingTransaction] =
		useState<BackendTransaction | null>(null);
	const [transactionTypeFilter, setTransactionTypeFilter] = useState<
		"all" | "expense" | "income"
	>("all");

	useEffect(() => {
		const fetchStats = async () => {
			setLoading(true);
			try {
				const days = parseInt(period);

				// Fetch full stats
				const fullStats = await statsService.getFullStats(days);
				setTotalExpense(fullStats.total_expense);
				setTotalIncome(fullStats.total_income);
				setCategories(fullStats.by_category);

				// Fetch comparison
				const comparisonData = await statsService.getComparison(days);
				setComparison(comparisonData);

				// Fetch debt summary
				const debts = await debtService.getSummary();
				setDebtSummary(debts);

				// Fetch upcoming dues (debts with future due dates)
				const allDebts = await debtService.getDebts();
				const now = new Date();
				const upcoming = allDebts
					.filter((d) => d.due_date && new Date(d.due_date) >= now)
					.sort(
						(a, b) =>
							new Date(a.due_date!).getTime() - new Date(b.due_date!).getTime()
					)
					.slice(0, 5);
				setUpcomingDues(upcoming);

				// Fetch transactions and related data for the Transactions tab
				const [txns, cats, wallets] = await Promise.all([
					expenseService.getTransactions({ limit: 100 }),
					categoryService.getCategories(),
					walletService.getWallets(),
				]);
				setTransactions(txns);
				setAllCategories(cats);
				setAllWallets(wallets);
			} catch (error) {
				console.error("Failed to fetch stats:", error);
			} finally {
				setLoading(false);
			}
		};

		fetchStats();
	}, [period]);

	const refreshTransactions = async () => {
		try {
			const txns = await expenseService.getTransactions({ limit: 100 });
			setTransactions(txns);
		} catch (error) {
			console.error("Failed to refresh transactions:", error);
		}
	};

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
									)
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
											""
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
														1
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
								<div className="space-y-2 max-h-[500px] overflow-y-auto">
									{filteredTransactions.length === 0 ? (
										<div className="text-center py-8 text-(--muted-foreground)">
											<p>No transactions found</p>
										</div>
									) : (
										filteredTransactions.map((txn) => {
											const category = getCategoryById(txn.category_id);
											const wallet = getWalletById(txn.wallet_id);
											return (
												<div
													key={txn.id}
													className="flex items-center justify-between p-4 bg-(--card) rounded-2xl border border-(--border) hover:border-(--primary)/50 transition-colors"
												>
													<div className="flex items-center gap-3">
														<div
															className="w-10 h-10 rounded-full flex items-center justify-center"
															style={{
																backgroundColor: category?.color || "#6b7280",
															}}
														>
															<span className="text-white text-sm font-bold">
																{category?.name?.charAt(0) ||
																	txn.type?.charAt(0)?.toUpperCase() ||
																	"T"}
															</span>
														</div>
														<div>
															<div className="font-medium text-(--foreground)">
																{txn.note ||
																	category?.name ||
																	(txn.type === "income"
																		? "Income"
																		: "Expense")}
															</div>
															<div className="text-xs text-(--muted-foreground) flex items-center gap-2">
																<span>
																	{txn.date
																		? new Date(txn.date).toLocaleDateString(
																				"en-IN",
																				{
																					month: "short",
																					day: "numeric",
																				}
																		  )
																		: "No date"}
																</span>
																{wallet && (
																	<>
																		<span>•</span>
																		<span>{wallet.name}</span>
																	</>
																)}
															</div>
														</div>
													</div>
													<div className="flex items-center gap-3">
														<div className="text-right">
															<div
																className={`font-bold ${
																	txn.type === "income"
																		? "text-green-500"
																		: "text-(--foreground)"
																}`}
															>
																{txn.type === "income" ? "+" : "-"}₹
																{txn.amount.toLocaleString("en-IN")}
															</div>
															<div className="text-xs text-(--muted-foreground) capitalize">
																{txn.type}
															</div>
														</div>
														<button
															onClick={() => setEditingTransaction(txn)}
															className="p-2 hover:bg-(--muted) rounded-full transition-colors"
														>
															<Edit3
																size={16}
																className="text-(--muted-foreground)"
															/>
														</button>
													</div>
												</div>
											);
										})
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
								<div className="bg-(--card) p-6 rounded-[2.5rem] shadow-sm border border-(--border) flex items-center gap-6 mb-6">
									<CircularProgress
										value={debtSummary.owed_to_me}
										max={debtSummary.owed_to_me + debtSummary.owed_by_me || 1}
										size={80}
										color="stroke-(--chart-4)"
										strokeWidth={8}
									/>
									<div>
										<div className="text-(--muted-foreground) text-sm mb-1">
											Owed to You
										</div>
										<div className="text-2xl font-bold text-green-500">
											{formatCurrency(debtSummary.owed_to_me)}
										</div>
										<div className="text-(--muted-foreground) text-xs mt-1">
											You owe: {formatCurrency(debtSummary.owed_by_me)}
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
															comparison?.expense.change_percent || 0
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
															{ month: "short", day: "numeric" }
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
						refreshTransactions();
					}}
					onDelete={() => {
						setEditingTransaction(null);
						refreshTransactions();
					}}
				/>
			)}
		</div>
	);
};
