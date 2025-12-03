import { User, Users, MoreHorizontal, Plus } from "lucide-react";
import React from "react";

interface Debt {
	id: number;
	name: string;
	amount: number;
	type: "owed_to_me" | "owed_by_me";
	date: string;
}

interface Group {
	id: number;
	name: string;
	members: number;
	balance: number;
	type: "owe" | "owed";
	icon: any;
	color: string;
}

interface SocialViewProps {
	debts: Debt[];
	groups: Group[];
	socialTab: string;
	setSocialTab: (tab: string) => void;
	setCurrentView: (view: string) => void;
	setSelectedUser: (user: any) => void;
}

export const SocialView = ({
	debts,
	groups,
	socialTab,
	setSocialTab,
	setCurrentView,
	setSelectedUser,
}: SocialViewProps) => {
	const netBalance = debts.reduce(
		(acc, curr) =>
			curr.type === "owed_to_me" ? acc + curr.amount : acc - curr.amount,
		0
	);

	return (
		<div className="flex flex-col h-full bg-(--background) pb-24 overflow-y-auto hide-scrollbar transition-colors duration-300">
			<header className="flex justify-between items-center p-6">
				<div className="flex flex-col">
					<h1 className="text-3xl font-bold text-(--foreground)">Social</h1>
					<p className="text-(--muted-foreground) text-sm">
						Friends & Shared Expenses
					</p>
				</div>
				<button className="p-2 bg-(--card) border border-(--border) rounded-full text-(--foreground) shadow-sm">
					<MoreHorizontal size={20} />
				</button>
			</header>

			{/* Tabs */}
			<div className="px-6 mb-6">
				<div className="flex justify-between items-center bg-(--muted) rounded-[2rem] p-1 text-sm font-medium">
					<button
						onClick={() => setSocialTab("debts")}
						className={`flex-1 py-3 rounded-[2rem] transition-all ${
							socialTab === "debts"
								? "bg-(--primary) text-(--primary-foreground) shadow-md"
								: "text-(--muted-foreground)"
						}`}
					>
						Friends
					</button>
					<button
						onClick={() => setSocialTab("groups")}
						className={`flex-1 py-3 rounded-[2rem] transition-all ${
							socialTab === "groups"
								? "bg-(--primary) text-(--primary-foreground) shadow-md"
								: "text-(--muted-foreground)"
						}`}
					>
						Groups
					</button>
				</div>
			</div>

			{socialTab === "debts" ? (
				<>
					{/* Net Balance Card */}
					<div className="px-6 mb-6">
						<div
							className={`p-6 rounded-[2rem] shadow-lg relative overflow-hidden ${
								netBalance >= 0 ? "bg-(--chart-4)" : "bg-(--chart-2)"
							} text-white`}
						>
							<div className="relative z-10 text-center">
								<div className="text-sm font-medium opacity-90 mb-1">
									Net Balance
								</div>
								<div className="text-4xl font-bold mb-2">
									{netBalance >= 0 ? "+" : "-"}₹{Math.abs(netBalance)}
								</div>
								<div className="text-xs opacity-80">
									{netBalance >= 0
										? "You are overall in credit"
										: "You are overall in debt"}
								</div>
							</div>
						</div>
					</div>

					{/* Debts List */}
					<div className="px-6 space-y-3">
						{debts.map((debt) => (
							<div
								key={debt.id}
								onClick={() => setSelectedUser(debt)}
								className="flex items-center justify-between p-4 bg-(--card) border border-(--border) rounded-3xl hover:bg-(--muted)/50 transition-all cursor-pointer"
							>
								<div className="flex items-center gap-4">
									<div
										className={`w-12 h-12 rounded-full flex items-center justify-center ${
											debt.type === "owed_to_me"
												? "bg-green-100 text-green-600 dark:bg-green-900/30 dark:text-green-400"
												: "bg-red-100 text-red-600 dark:bg-red-900/30 dark:text-red-400"
										}`}
									>
										<User size={20} />
									</div>
									<div>
										<h3 className="font-bold text-(--foreground)">
											{debt.name}
										</h3>
										<p className="text-xs text-(--muted-foreground)">
											{debt.date}
										</p>
									</div>
								</div>
								<div className="text-right">
									<p
										className={`font-bold ${
											debt.type === "owed_to_me"
												? "text-green-500"
												: "text-red-500"
										}`}
									>
										{debt.type === "owed_to_me" ? "+" : "-"}₹{debt.amount}
									</p>
									<p className="text-xs text-(--muted-foreground)">
										{debt.type === "owed_to_me" ? "Credit" : "Debt"}
									</p>
								</div>
							</div>
						))}

						<button className="w-full py-4 border-2 border-dashed border-(--border) rounded-[2rem] text-(--muted-foreground) font-medium hover:bg-(--muted) transition-colors flex items-center justify-center gap-2 mt-4">
							<div className="w-6 h-6 rounded-full bg-(--primary) text-(--primary-foreground) flex items-center justify-center text-lg leading-none pb-1">
								+
							</div>
							Add New Contact
						</button>
					</div>
				</>
			) : (
				/* Groups Tab */
				<div className="px-6 space-y-4">
					{groups.map((group) => (
						<div
							key={group.id}
							onClick={() => setCurrentView("group-detail")}
							className="bg-(--card) border border-(--border) rounded-[2rem] p-5 hover:bg-(--muted)/50 transition-all cursor-pointer"
						>
							<div className="flex justify-between items-start mb-4">
								<div className="flex items-center gap-4">
									<div
										className={`w-12 h-12 rounded-full ${group.color} flex items-center justify-center text-white font-bold text-lg shadow-md`}
									>
										{React.createElement(group.icon || Users, { size: 20 })}
									</div>
									<div>
										<h3 className="font-bold text-(--foreground) text-lg">
											{group.name}
										</h3>
										<div className="flex items-center gap-1 text-(--muted-foreground) text-xs">
											<Users size={12} /> {group.members} Members
										</div>
									</div>
								</div>
								<button className="p-2 text-(--muted-foreground) hover:text-(--foreground)">
									<MoreHorizontal size={20} />
								</button>
							</div>
							<div className="flex justify-between items-center p-3 bg-(--muted) rounded-2xl">
								<span className="text-sm text-(--muted-foreground)">
									Your share
								</span>
								<span
									className={`font-bold ${
										group.type === "owed" ? "text-green-500" : "text-red-500"
									}`}
								>
									{group.type === "owed" ? "+" : "-"}₹{Math.abs(group.balance)}
								</span>
							</div>
						</div>
					))}

					<button
						onClick={() => setCurrentView("create-group")}
						className="w-full py-4 bg-(--primary) text-(--primary-foreground) rounded-[2rem] font-bold text-lg shadow-lg hover:opacity-90 active:scale-95 transition-all mt-4 flex items-center justify-center gap-2"
					>
						<Users size={20} /> Create New Group
					</button>
				</div>
			)}
		</div>
	);
};
