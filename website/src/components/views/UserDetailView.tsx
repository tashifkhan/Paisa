import React from "react";
import {
	ArrowLeft,
	MoreHorizontal,
	Phone,
	Mail,
	Calendar,
	DollarSign,
	ArrowUpRight,
	ArrowDownLeft,
} from "lucide-react";

interface Transaction {
	id: number;
	title: string;
	amount: number;
	date: string;
	type: "paid" | "received";
}

interface UserDetailViewProps {
	user: {
		id: number;
		name: string;
		amount: number;
		type: "owed_to_me" | "owed_by_me";
		date: string;
	} | null;
	setCurrentView: (view: string) => void;
}

export const UserDetailView = ({
	user,
	setCurrentView,
}: UserDetailViewProps) => {
	if (!user) return null;

	// Mock transactions
	const transactions: Transaction[] = [
		{
			id: 1,
			title: "Dinner at Thalassa",
			amount: 1200,
			date: "Yesterday",
			type: "paid",
		},
		{
			id: 2,
			title: "Movie Tickets",
			amount: 500,
			date: "3 days ago",
			type: "received",
		},
		{
			id: 3,
			title: "Uber Split",
			amount: 250,
			date: "1 week ago",
			type: "paid",
		},
	];

	return (
		<div className="flex flex-col h-full bg-(--background) overflow-y-auto hide-scrollbar transition-colors duration-300">
			<header className="flex justify-between items-center p-6">
				<button
					onClick={() => setCurrentView("debts")}
					className="p-2 bg-(--card) border border-(--border) rounded-full text-(--foreground) shadow-sm"
				>
					<ArrowLeft size={20} />
				</button>
				<button className="p-2 text-(--foreground)">
					<MoreHorizontal size={20} />
				</button>
			</header>

			<div className="flex flex-col items-center px-6 mb-8">
				<div className="w-24 h-24 rounded-full bg-(--muted) flex items-center justify-center text-4xl font-bold text-(--foreground) mb-4 border-4 border-(--card) shadow-lg">
					{user.name.charAt(0)}
				</div>
				<h1 className="text-2xl font-bold text-(--foreground) mb-1">
					{user.name}
				</h1>
				<p className="text-(--muted-foreground) text-sm">+91 98765 43210</p>

				<div className="flex gap-4 mt-6 w-full">
					<button className="flex-1 py-3 bg-(--card) border border-(--border) rounded-2xl flex items-center justify-center gap-2 text-(--foreground) font-medium shadow-sm hover:bg-(--muted) transition-colors">
						<Phone size={18} /> Call
					</button>
					<button className="flex-1 py-3 bg-(--card) border border-(--border) rounded-2xl flex items-center justify-center gap-2 text-(--foreground) font-medium shadow-sm hover:bg-(--muted) transition-colors">
						<Mail size={18} /> Email
					</button>
				</div>
			</div>

			<div className="px-6 mb-8">
				<div
					className={`p-6 rounded-[2rem] shadow-lg relative overflow-hidden ${
						user.type === "owed_to_me" ? "bg-(--chart-4)" : "bg-(--chart-2)"
					} text-white`}
				>
					<div className="relative z-10 text-center">
						<div className="text-sm font-medium opacity-90 mb-1">
							{user.type === "owed_to_me" ? "You are owed" : "You owe"}
						</div>
						<div className="text-4xl font-bold mb-4">₹{user.amount}</div>

						<div className="flex gap-3 justify-center">
							<button className="px-6 py-2 bg-white/20 backdrop-blur-md rounded-xl text-sm font-bold hover:bg-white/30 transition-colors">
								Settle Up
							</button>
							{user.type === "owed_to_me" && (
								<button className="px-6 py-2 bg-white text-black rounded-xl text-sm font-bold hover:opacity-90 transition-opacity">
									Remind
								</button>
							)}
						</div>
					</div>
				</div>
			</div>

			<div className="flex-1 px-6 pb-24">
				<h2 className="text-lg font-bold text-(--foreground) mb-4">
					Transaction History
				</h2>
				<div className="space-y-4">
					{transactions.map((tx) => (
						<div
							key={tx.id}
							className="flex items-center justify-between p-4 bg-(--card) border border-(--border) rounded-2xl"
						>
							<div className="flex items-center gap-4">
								<div
									className={`w-10 h-10 rounded-full flex items-center justify-center ${
										tx.type === "paid"
											? "bg-red-100 text-red-600"
											: "bg-green-100 text-green-600"
									}`}
								>
									{tx.type === "paid" ? (
										<ArrowUpRight size={18} />
									) : (
										<ArrowDownLeft size={18} />
									)}
								</div>
								<div>
									<div className="font-bold text-(--foreground)">
										{tx.title}
									</div>
									<div className="text-xs text-(--muted-foreground)">
										{tx.date}
									</div>
								</div>
							</div>
							<div
								className={`font-bold ${
									tx.type === "paid" ? "text-red-500" : "text-green-500"
								}`}
							>
								{tx.type === "paid" ? "-" : "+"}₹{tx.amount}
							</div>
						</div>
					))}
				</div>
			</div>
		</div>
	);
};
