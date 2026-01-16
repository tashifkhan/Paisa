import {
	AlertCircle,
	Calendar,
	Check,
	Loader2,
	Trash2,
	Wallet,
	X,
} from "lucide-react";
import { useState } from "react";
import { expenseService } from "../../services/expenseService";
import type {
	BackendCategory,
	BackendTransaction,
	BackendWallet,
} from "../../services/types";
import {
	Select,
	SelectContent,
	SelectItem,
	SelectTrigger,
	SelectValue,
} from "../ui/select";

interface EditExpenseModalProps {
	transaction: BackendTransaction;
	categories: BackendCategory[];
	wallets: BackendWallet[];
	onClose: () => void;
	onSave: () => void;
	onDelete: () => void;
}

export const EditExpenseModal = ({
	transaction,
	categories,
	wallets,
	onClose,
	onSave,
	onDelete,
}: EditExpenseModalProps) => {
	const [amount, setAmount] = useState(transaction.amount.toString());
	const [transactionType, setTransactionType] = useState<"expense" | "income">(
		transaction.type as "expense" | "income"
	);
	const [selectedCategory, setSelectedCategory] = useState(
		transaction.category_id || ""
	);
	const [selectedWallet, setSelectedWallet] = useState(
		transaction.wallet_id || ""
	);
	const [note, setNote] = useState(transaction.note || "");
	const [date, setDate] = useState(
		transaction.date
			? transaction.date.split("T")[0]
			: new Date().toISOString().split("T")[0]
	);
	const [saving, setSaving] = useState(false);
	const [deleting, setDeleting] = useState(false);
	const [error, setError] = useState<string | null>(null);

	const filteredCategories = categories.filter(
		(cat) => cat.type === transactionType || cat.type === "both"
	);

	const handleSave = async () => {
		const amountNum = parseFloat(amount);
		if (isNaN(amountNum) || amountNum <= 0) {
			setError("Please enter a valid amount");
			return;
		}

		setSaving(true);
		setError(null);
		try {
			await expenseService.updateTransaction(transaction.id, {
				amount: amountNum,
				type: transactionType,
				date: new Date(date).toISOString(),
				note: note || undefined,
				wallet_id: selectedWallet || undefined,
				category_id: selectedCategory || undefined,
			});
			onSave();
		} catch (err: unknown) {
			console.error("Failed to update transaction:", err);
			setError("Failed to update transaction. Please try again.");
		} finally {
			setSaving(false);
		}
	};

	const handleDelete = async () => {
		if (!confirm("Are you sure you want to delete this transaction?")) {
			return;
		}

		setDeleting(true);
		setError(null);
		try {
			await expenseService.deleteTransaction(transaction.id);
			onDelete();
		} catch (err: unknown) {
			console.error("Failed to delete transaction:", err);
			setError("Failed to delete transaction. Please try again.");
		} finally {
			setDeleting(false);
		}
	};

	return (
		<div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm">
			<div className="bg-(--card) rounded-3xl shadow-2xl w-full max-w-md mx-4 overflow-hidden border border-(--border)">
				{/* Header */}
				<div className="flex items-center justify-between p-4 border-b border-(--border)">
					<h2 className="text-lg font-bold text-(--foreground)">
						Edit Transaction
					</h2>
					<button
						onClick={onClose}
						className="p-2 hover:bg-(--muted) rounded-full transition-colors"
					>
						<X size={20} className="text-(--foreground)" />
					</button>
				</div>

				{/* Body */}
				<div className="p-6 space-y-4">
					{/* Error message */}
					{error && (
						<div className="flex items-center gap-2 p-3 bg-red-100 dark:bg-red-900/30 rounded-xl text-red-600 dark:text-red-400 text-sm">
							<AlertCircle size={16} />
							{error}
						</div>
					)}

					{/* Type Toggle */}
					<div className="flex bg-(--muted) rounded-full p-1">
						<button
							onClick={() => setTransactionType("expense")}
							className={`flex-1 py-2 px-4 rounded-full text-sm font-medium transition-all ${
								transactionType === "expense"
									? "bg-(--destructive) text-(--destructive-foreground)"
									: "text-(--muted-foreground)"
							}`}
						>
							Expense
						</button>
						<button
							onClick={() => setTransactionType("income")}
							className={`flex-1 py-2 px-4 rounded-full text-sm font-medium transition-all ${
								transactionType === "income"
									? "bg-green-500 text-white"
									: "text-(--muted-foreground)"
							}`}
						>
							Income
						</button>
					</div>

					{/* Amount */}
					<div>
						<label className="block text-sm font-medium text-(--foreground) mb-2">
							Amount
						</label>
						<div className="relative">
							<span className="absolute left-4 top-1/2 -translate-y-1/2 text-(--muted-foreground)">
								₹
							</span>
							<input
								type="number"
								value={amount}
								onChange={(e) => setAmount(e.target.value)}
								className="w-full pl-8 pr-4 py-3 bg-(--background) border border-(--border) rounded-xl text-(--foreground) focus:outline-none focus:ring-2 focus:ring-(--primary)"
								step="0.01"
								min="0"
							/>
						</div>
					</div>

					{/* Date */}
					<div>
						<label className="block text-sm font-medium text-(--foreground) mb-2">
							Date
						</label>
						<div className="relative">
							<Calendar
								size={18}
								className="absolute left-4 top-1/2 -translate-y-1/2 text-(--muted-foreground)"
							/>
							<input
								type="date"
								value={date}
								onChange={(e) => setDate(e.target.value)}
								className="w-full pl-12 pr-4 py-3 bg-(--background) border border-(--border) rounded-xl text-(--foreground) focus:outline-none focus:ring-2 focus:ring-(--primary)"
							/>
						</div>
					</div>

					{/* Wallet & Category */}
					<div className="grid grid-cols-2 gap-3">
						<div>
							<label className="block text-sm font-medium text-(--foreground) mb-2">
								Wallet
							</label>
							<Select value={selectedWallet} onValueChange={setSelectedWallet}>
								<SelectTrigger>
									<div className="flex items-center gap-2">
										<Wallet size={16} />
										<SelectValue placeholder="Select wallet" />
									</div>
								</SelectTrigger>
								<SelectContent>
									<SelectItem value="">No wallet</SelectItem>
									{wallets.map((wallet) => (
										<SelectItem key={wallet.id} value={wallet.id}>
											{wallet.name}
										</SelectItem>
									))}
								</SelectContent>
							</Select>
						</div>

						<div>
							<label className="block text-sm font-medium text-(--foreground) mb-2">
								Category
							</label>
							<Select
								value={selectedCategory}
								onValueChange={setSelectedCategory}
							>
								<SelectTrigger>
									<div className="flex items-center gap-2">
										<span
											className="w-3 h-3 rounded-full"
											style={{
												backgroundColor:
													categories.find((c) => c.id === selectedCategory)
														?.color || "#888",
											}}
										/>
										<SelectValue placeholder="Category" />
									</div>
								</SelectTrigger>
								<SelectContent>
									{filteredCategories.map((cat) => (
										<SelectItem key={cat.id} value={cat.id}>
											<div className="flex items-center gap-2">
												<span
													className="w-3 h-3 rounded-full"
													style={{ backgroundColor: cat.color || "#888" }}
												/>
												{cat.name}
											</div>
										</SelectItem>
									))}
								</SelectContent>
							</Select>
						</div>
					</div>

					{/* Note */}
					<div>
						<label className="block text-sm font-medium text-(--foreground) mb-2">
							Note
						</label>
						<input
							type="text"
							value={note}
							onChange={(e) => setNote(e.target.value)}
							placeholder="Add a note..."
							className="w-full px-4 py-3 bg-(--background) border border-(--border) rounded-xl text-(--foreground) placeholder-(--muted-foreground) focus:outline-none focus:ring-2 focus:ring-(--primary)"
						/>
					</div>
				</div>

				{/* Footer */}
				<div className="flex items-center gap-3 p-4 border-t border-(--border)">
					<button
						onClick={handleDelete}
						disabled={deleting || saving}
						className="flex items-center justify-center gap-2 px-4 py-3 bg-(--destructive) text-(--destructive-foreground) rounded-xl font-medium hover:opacity-90 transition-all disabled:opacity-50"
					>
						{deleting ? (
							<Loader2 size={18} className="animate-spin" />
						) : (
							<Trash2 size={18} />
						)}
						Delete
					</button>
					<button
						onClick={handleSave}
						disabled={saving || deleting}
						className="flex-1 flex items-center justify-center gap-2 px-4 py-3 bg-(--primary) text-(--primary-foreground) rounded-xl font-medium hover:opacity-90 transition-all disabled:opacity-50"
					>
						{saving ? (
							<Loader2 size={18} className="animate-spin" />
						) : (
							<Check size={18} />
						)}
						Save Changes
					</button>
				</div>
			</div>
		</div>
	);
};
