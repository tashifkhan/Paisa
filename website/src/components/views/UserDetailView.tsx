import {
	ArrowLeft,
	Loader2,
	Mail,
	MoreHorizontal,
	Trash2,
	X,
} from "lucide-react";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { debtService } from "../../services/debtService";

interface UserDetailViewProps {
	user: {
		id: string | number;
		name: string;
		amount: number;
		type: "owed_to_me" | "owed_by_me";
		date: string;
		email?: string;
	} | null;
	setCurrentView: (view: string, options?: any) => void;
	onDebtDeleted?: () => void;
}

export const UserDetailView = ({
	user,
	setCurrentView,
	onDebtDeleted,
}: UserDetailViewProps) => {
	const navigate = useNavigate();
	const [isDeleting, setIsDeleting] = useState(false);
	const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
	const [isSettling, setIsSettling] = useState(false);

	if (!user) return null;

	const handleDelete = async () => {
		setIsDeleting(true);
		try {
			await debtService.deleteDebt(String(user.id));
			onDebtDeleted?.();
			setCurrentView("debts");
		} catch (error) {
			console.error("Failed to delete:", error);
		} finally {
			setIsDeleting(false);
			setShowDeleteConfirm(false);
		}
	};

	const handleSettle = async () => {
		setIsSettling(true);
		try {
			await debtService.settleDebt(String(user.id));
			onDebtDeleted?.();
			setCurrentView("debts");
		} catch (error) {
			console.error("Failed to settle:", error);
		} finally {
			setIsSettling(false);
		}
	};

	// Check if this is a manually added contact (no transactions, just a debt record)
	const isManualContact = user.date === "New Contact" || user.amount === 0;

	return (
		<div className="flex flex-col h-full bg-(--background) overflow-y-auto hide-scrollbar transition-colors duration-300">
			<header className="flex justify-between items-center p-6">
				<button
					onClick={() => setCurrentView("debts")}
					className="p-2 bg-(--card) border border-(--border) rounded-full text-(--foreground) shadow-sm"
				>
					<ArrowLeft size={20} />
				</button>
				<button
					onClick={() => setShowDeleteConfirm(true)}
					className="p-2 text-(--foreground) hover:text-red-500 transition-colors"
					title="Delete contact"
				>
					<MoreHorizontal size={20} />
				</button>
			</header>

			<div className="flex flex-col items-center px-6 mb-8">
				<div className="w-24 h-24 rounded-full bg-(--muted) flex items-center justify-center text-4xl font-bold text-(--foreground) mb-4 border-4 border-(--card) shadow-lg">
					{user.name.charAt(0).toUpperCase()}
				</div>
				<h1 className="text-2xl font-bold text-(--foreground) mb-1">
					{user.name}
				</h1>
				{user.email && (
					<p className="text-(--muted-foreground) text-sm">{user.email}</p>
				)}
				{isManualContact && (
					<span className="mt-2 text-xs bg-(--muted) text-(--muted-foreground) px-3 py-1 rounded-full">
						Manually Added
					</span>
				)}

				{user.email && (
					<div className="flex gap-4 mt-6 w-full">
						<a
							href={`mailto:${user.email}`}
							className="flex-1 py-3 bg-(--card) border border-(--border) rounded-2xl flex items-center justify-center gap-2 text-(--foreground) font-medium shadow-sm hover:bg-(--muted) transition-colors"
						>
							<Mail size={18} /> Email
						</a>
					</div>
				)}
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
							{user.amount > 0 && (
								<button
									onClick={handleSettle}
									disabled={isSettling}
									className="px-6 py-2 bg-white/20 backdrop-blur-md rounded-xl text-sm font-bold hover:bg-white/30 transition-colors disabled:opacity-50 flex items-center gap-2"
								>
									{isSettling && <Loader2 size={16} className="animate-spin" />}
									Settle Up
								</button>
							)}
							<button
								onClick={() =>
									navigate("/add-expense", { state: { splitWith: user } })
								}
								className="px-6 py-2 bg-white text-(--primary) rounded-xl text-sm font-bold hover:bg-white/90 transition-colors shadow-lg"
							>
								Add Transaction
							</button>
						</div>
					</div>
				</div>
			</div>

			{/* No hardcoded transaction history - show empty state or real data when available */}
			<div className="flex-1 px-6 pb-24">
				<div className="text-center py-8 text-(--muted-foreground)">
					<p className="text-sm">
						Transaction history will appear here when you add expenses with{" "}
						{user.name}.
					</p>
					<button
						onClick={() =>
							navigate("/add-expense", { state: { splitWith: user } })
						}
						className="mt-6 px-6 py-3 bg-(--primary) text-(--primary-foreground) rounded-full font-bold shadow-lg hover:opacity-90 transition-all flex items-center justify-center gap-2 mx-auto"
					>
						+ Add Transaction
					</button>
				</div>
			</div>

			{/* Delete action at bottom */}
			<div className="px-6 pb-24">
				<button
					onClick={() => setShowDeleteConfirm(true)}
					className="w-full py-3 border border-red-500/30 text-red-500 rounded-2xl font-medium hover:bg-red-500/10 transition-colors flex items-center justify-center gap-2"
				>
					<Trash2 size={18} /> Remove Contact
				</button>
			</div>

			{/* Delete Confirmation Modal */}
			{showDeleteConfirm && (
				<div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-6">
					<div className="bg-(--card) border border-(--border) rounded-3xl p-6 w-full max-w-sm shadow-xl">
						<div className="flex justify-between items-start mb-4">
							<h3 className="text-lg font-bold text-(--foreground)">
								Remove Contact?
							</h3>
							<button
								onClick={() => setShowDeleteConfirm(false)}
								className="p-1 text-(--muted-foreground) hover:text-(--foreground)"
							>
								<X size={20} />
							</button>
						</div>
						<p className="text-sm text-(--muted-foreground) mb-6">
							This will remove {user.name} from your contacts and delete any
							debt records. This action cannot be undone.
						</p>
						<div className="flex gap-3">
							<button
								onClick={() => setShowDeleteConfirm(false)}
								className="flex-1 py-3 bg-(--muted) text-(--foreground) rounded-2xl font-medium"
							>
								Cancel
							</button>
							<button
								onClick={handleDelete}
								disabled={isDeleting}
								className="flex-1 py-3 bg-red-500 text-white rounded-2xl font-medium hover:bg-red-600 transition-colors disabled:opacity-50 flex items-center justify-center gap-2"
							>
								{isDeleting ? (
									<Loader2 size={18} className="animate-spin" />
								) : (
									<Trash2 size={18} />
								)}
								Remove
							</button>
						</div>
					</div>
				</div>
			)}
		</div>
	);
};
