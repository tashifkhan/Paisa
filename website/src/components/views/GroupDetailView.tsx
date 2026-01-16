import {
	ArrowLeft,
	ArrowRight,
	Loader2,
	MoreHorizontal,
	Plane,
	Plus,
	RefreshCw,
	Share2,
	ShoppingBag,
	Users,
	X,
} from "lucide-react";
import { useEffect, useState } from "react";
import { groupService } from "../../services/groupService";
import type {
	BackendGroupBalanceSummary,
	BackendGroupMember,
	BackendTransaction,
	BackendUserBalance,
} from "../../services/types";

interface GroupDetailViewProps {
	group: any;
	groupExpenses: any[];
	groupDetailTab: string;
	setGroupDetailTab: (tab: string) => void;
	setCurrentView: (view: string) => void;
}

interface SimplifiedDebt {
	from: string;
	fromName: string;
	to: string;
	toName: string;
	amount: number;
}

export const GroupDetailView = ({
	group,
	groupExpenses: _initialExpenses, // Kept for prop compatibility but using API data
	groupDetailTab,
	setGroupDetailTab,
	setCurrentView,
}: GroupDetailViewProps) => {
	const [loading, setLoading] = useState(true);
	const [expenses, setExpenses] = useState<BackendTransaction[]>([]);
	const [members, setMembers] = useState<BackendGroupMember[]>([]);
	const [balances, setBalances] = useState<BackendGroupBalanceSummary | null>(
		null,
	);
	const [totalExpenses, setTotalExpenses] = useState(0);

	// Simplify Debts State
	const [showSimplifyModal, setShowSimplifyModal] = useState(false);
	const [simplifiedDebts, setSimplifiedDebts] = useState<SimplifiedDebt[]>([]);
	const [isSimplifying, setIsSimplifying] = useState(false);

	useEffect(() => {
		if (!group?.id) return;

		const fetchGroupData = async () => {
			setLoading(true);
			try {
				const [expensesData, membersData, balancesData] = await Promise.all([
					groupService.getGroupExpenses(group.id),
					groupService.getMembers(group.id),
					groupService.getGroupBalances(group.id),
				]);

				setExpenses(expensesData);
				setMembers(membersData);
				setBalances(balancesData);
				setTotalExpenses(balancesData.total_expenses);
			} catch (error) {
				console.error("Failed to fetch group data:", error);
			} finally {
				setLoading(false);
			}
		};

		fetchGroupData();
	}, [group?.id]);

	if (!group) return null;

	const formatCurrency = (amount: number) => {
		return new Intl.NumberFormat("en-IN", {
			style: "currency",
			currency: "INR",
			minimumFractionDigits: 0,
			maximumFractionDigits: 0,
		}).format(amount);
	};

	const formatDate = (dateStr: string) => {
		return new Date(dateStr).toLocaleDateString("en-IN", {
			month: "short",
			day: "numeric",
		});
	};

	// Get current user's balance from balances
	const getCurrentUserBalance = (): number => {
		if (!balances?.balances?.length) return 0;
		// Assuming first balance might be current user, or find by matching user_id
		// For now, just return the first negative balance (money owed)
		const negativeBalance = balances.balances.find((b) => b.balance < 0);
		return negativeBalance?.balance || 0;
	};

	const userBalance = getCurrentUserBalance();

	// Simplify Debts Algorithm (now uses backend API)
	const simplifyDebts = async () => {
		if (!group?.id) return;

		setIsSimplifying(true);
		try {
			const response = await groupService.simplifyDebts(group.id);

			// Map backend response to frontend format
			const transactions: SimplifiedDebt[] = response.simplified_debts.map(
				(debt) => ({
					from: debt.from_user_id,
					fromName: debt.from_user_name,
					to: debt.to_user_id,
					toName: debt.to_user_name,
					amount: debt.amount,
				}),
			);

			setSimplifiedDebts(transactions);
			setShowSimplifyModal(true);
		} catch (error) {
			console.error("Failed to simplify debts:", error);
			alert("Failed to calculate simplified settlements. Please try again.");
		} finally {
			setIsSimplifying(false);
		}
	};

	return (
		<div className="flex flex-col h-full bg-(--background) overflow-y-auto hide-scrollbar transition-colors duration-300">
			{/* Header with Group Info */}
			<div className="bg-(--card) rounded-b-[3rem] shadow-sm border-b border-(--border) pb-6">
				<header className="flex justify-between items-center p-6">
					<button
						onClick={() => setCurrentView("debts")}
						className="p-2 bg-(--muted) rounded-full text-(--foreground)"
					>
						<ArrowLeft size={20} />
					</button>
					<div className="flex items-center gap-2">
						<button className="p-2 text-(--foreground)">
							<Share2 size={20} />
						</button>
						<button className="p-2 text-(--foreground)">
							<MoreHorizontal size={20} />
						</button>
					</div>
				</header>

				<div className="px-6 text-center">
					<div
						className={`w-20 h-20 ${
							group.color || "bg-orange-500"
						} rounded-3xl mx-auto flex items-center justify-center text-white shadow-lg mb-4 rotate-3`}
					>
						<Plane size={32} />
					</div>
					<h1 className="text-2xl font-bold text-(--foreground)">
						{group.name}
					</h1>
					<p className="text-(--muted-foreground) text-sm mb-6">
						{loading ? "Loading..." : `${members.length} members`}
					</p>

					<div className="bg-(--muted) rounded-2xl p-4 inline-flex items-center gap-4 border border-(--border)">
						<div className="text-left">
							<div className="text-xs text-(--muted-foreground)">
								Total Expenses
							</div>
							<div className="text-lg font-bold text-(--foreground)">
								{loading ? "..." : formatCurrency(totalExpenses)}
							</div>
						</div>
						<div className="h-8 w-px bg-(--border)"></div>
						<div className="text-left">
							<div className="text-xs text-(--muted-foreground)">
								{userBalance < 0 ? "You Owe" : "You're Owed"}
							</div>
							<div
								className={`text-lg font-bold ${
									userBalance < 0 ? "text-red-500" : "text-green-500"
								}`}
							>
								{loading ? "..." : formatCurrency(Math.abs(userBalance))}
							</div>
						</div>
					</div>
				</div>

				{/* Inner Tabs */}
				<div className="px-6 mt-6">
					<div className="flex justify-between items-center bg-(--muted) rounded-[2rem] p-1 text-sm font-medium">
						{["Expenses", "Balances", "Members"].map((tab) => (
							<button
								key={tab}
								onClick={() => setGroupDetailTab(tab.toLowerCase())}
								className={`flex-1 py-3 rounded-[2rem] transition-all ${
									groupDetailTab === tab.toLowerCase()
										? "bg-(--primary) text-(--primary-foreground) shadow-md"
										: "text-(--muted-foreground)"
								}`}
							>
								{tab}
							</button>
						))}
					</div>
				</div>
			</div>

			<div className="flex-1 px-6 pt-6 pb-24">
				{loading ? (
					<div className="flex items-center justify-center py-12">
						<Loader2
							className="animate-spin text-(--muted-foreground)"
							size={32}
						/>
					</div>
				) : (
					<>
						{groupDetailTab === "expenses" && (
							<div className="space-y-1">
								{expenses.length > 0 ? (
									expenses.map((exp) => (
										<div
											key={exp.id}
											className="flex items-center justify-between py-4 border-b border-(--border) last:border-0"
										>
											<div className="flex items-center gap-4">
												<div className="w-10 h-10 bg-(--muted) rounded-full flex items-center justify-center text-(--foreground)">
													<ShoppingBag size={18} />
												</div>
												<div>
													<div className="font-bold text-(--foreground)">
														{exp.note || "Expense"}
													</div>
													<div className="text-xs text-(--muted-foreground)">
														{formatDate(exp.date)}
													</div>
												</div>
											</div>
											<div className="text-right">
												<div className="font-bold text-(--foreground)">
													{formatCurrency(exp.amount)}
												</div>
												<div className="text-xs text-(--muted-foreground)">
													{exp.type}
												</div>
											</div>
										</div>
									))
								) : (
									<div className="text-center py-12 text-(--muted-foreground)">
										<ShoppingBag
											size={48}
											className="mx-auto mb-4 opacity-50"
										/>
										<p>No expenses yet</p>
										<p className="text-sm">Add your first group expense</p>
									</div>
								)}
							</div>
						)}

						{groupDetailTab === "balances" && (
							<div className="space-y-4">
								{balances?.balances && balances.balances.length > 0 ? (
									<>
										{balances.balances.map(
											(balance: BackendUserBalance, index: number) => (
												<div
													key={balance.user_id || index}
													className="bg-(--card) border border-(--border) rounded-2xl p-4 flex items-center justify-between"
												>
													<div className="flex items-center gap-3">
														<div
															className={`w-10 h-10 rounded-full flex items-center justify-center text-sm font-bold ${
																balance.balance >= 0
																	? "bg-green-100 text-green-600"
																	: "bg-red-100 text-red-600"
															}`}
														>
															{balance.user_name?.charAt(0) || "?"}
														</div>
														<div>
															<div className="font-medium text-(--foreground)">
																{balance.user_name || "Unknown"}
															</div>
															<div className="text-xs text-(--muted-foreground)">
																{balance.balance >= 0 ? "is owed" : "owes"}
															</div>
														</div>
													</div>
													<div
														className={`font-bold ${
															balance.balance >= 0
																? "text-green-500"
																: "text-red-500"
														}`}
													>
														{formatCurrency(Math.abs(balance.balance))}
													</div>
												</div>
											),
										)}

										<div className="bg-(--muted) rounded-2xl p-6 text-center mt-8">
											<RefreshCw
												size={32}
												className="mx-auto text-(--muted-foreground) mb-3"
											/>
											<h3 className="font-bold text-(--foreground)">
												Simplify Debts?
											</h3>
											<p className="text-xs text-(--muted-foreground) mb-4">
												Minimize the number of transactions required to settle
												up.
											</p>
											<button
												onClick={simplifyDebts}
												disabled={isSimplifying}
												className="px-6 py-2 bg-(--foreground) text-(--background) rounded-xl text-sm font-bold disabled:opacity-50"
											>
												{isSimplifying ? "Calculating..." : "Simplify Now"}
											</button>
										</div>
									</>
								) : (
									<div className="text-center py-12 text-(--muted-foreground)">
										<p>No balances to show</p>
									</div>
								)}
							</div>
						)}

						{groupDetailTab === "members" && (
							<div className="space-y-2">
								{members.map((member) => (
									<div
										key={member.id}
										className="bg-(--card) border border-(--border) rounded-2xl p-4 flex items-center justify-between"
									>
										<div className="flex items-center gap-3">
											<div
												className={`w-10 h-10 rounded-full flex items-center justify-center text-sm font-bold ${
													member.role === "admin"
														? "bg-purple-100 text-purple-600"
														: "bg-blue-100 text-blue-600"
												}`}
											>
												{member.user_name?.charAt(0) || "?"}
											</div>
											<div>
												<div className="font-medium text-(--foreground)">
													{member.user_name || member.user_email || "Unknown"}
												</div>
												<div className="text-xs text-(--muted-foreground)">
													{member.role === "admin" ? "👑 Admin" : "Member"}
												</div>
											</div>
										</div>
										<div className="text-xs text-(--muted-foreground)">
											Joined {formatDate(member.joined_at)}
										</div>
									</div>
								))}

								<button className="w-full py-4 border-2 border-dashed border-(--border) rounded-2xl text-(--muted-foreground) font-medium hover:bg-(--muted) transition-colors flex items-center justify-center gap-2 mt-4">
									<Users size={20} />
									Add Member
								</button>
							</div>
						)}
					</>
				)}
			</div>

			{/* Floating Add for Group */}
			<div className="absolute bottom-6 right-6">
				<button
					onClick={() => setCurrentView("addExpense")}
					className="h-14 w-14 bg-(--primary) text-(--primary-foreground) rounded-full shadow-lg flex items-center justify-center"
				>
					<Plus size={24} />
				</button>
			</div>

			{/* Simplify Debts Modal */}
			{showSimplifyModal && (
				<div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-6">
					<div className="bg-(--card) border border-(--border) rounded-3xl p-6 w-full max-w-md shadow-xl max-h-[80vh] overflow-y-auto">
						<div className="flex justify-between items-center mb-4">
							<h3 className="text-lg font-bold text-(--foreground)">
								Simplified Settlements
							</h3>
							<button
								onClick={() => setShowSimplifyModal(false)}
								className="p-1 text-(--muted-foreground) hover:text-(--foreground)"
							>
								<X size={20} />
							</button>
						</div>

						{simplifiedDebts.length > 0 ? (
							<div className="space-y-3">
								<p className="text-sm text-(--muted-foreground) mb-4">
									Minimized to {simplifiedDebts.length} transaction
									{simplifiedDebts.length > 1 ? "s" : ""}:
								</p>
								{simplifiedDebts.map((debt, index) => (
									<div
										key={index}
										className="flex items-center justify-between p-4 bg-(--muted) rounded-2xl"
									>
										<div className="flex items-center gap-2">
											<div className="w-8 h-8 rounded-full bg-red-100 text-red-600 flex items-center justify-center text-xs font-bold">
												{debt.fromName.charAt(0)}
											</div>
											<span className="text-sm font-medium text-(--foreground)">
												{debt.fromName}
											</span>
											<ArrowRight
												size={16}
												className="text-(--muted-foreground)"
											/>
											<div className="w-8 h-8 rounded-full bg-green-100 text-green-600 flex items-center justify-center text-xs font-bold">
												{debt.toName.charAt(0)}
											</div>
											<span className="text-sm font-medium text-(--foreground)">
												{debt.toName}
											</span>
										</div>
										<span className="font-bold text-(--primary)">
											{formatCurrency(debt.amount)}
										</span>
									</div>
								))}
							</div>
						) : (
							<div className="text-center py-8 text-(--muted-foreground)">
								<p>All balances are already settled!</p>
							</div>
						)}

						<button
							onClick={() => setShowSimplifyModal(false)}
							className="w-full mt-6 py-3 bg-(--primary) text-(--primary-foreground) rounded-2xl font-medium"
						>
							Got it
						</button>
					</div>
				</div>
			)}
		</div>
	);
};
