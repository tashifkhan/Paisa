import {
	ArrowLeft,
	Check,
	CreditCard,
	DollarSign,
	Loader2,
	Wallet,
} from "lucide-react";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { walletService } from "../../services/walletService";
import {
	Select,
	SelectContent,
	SelectItem,
	SelectTrigger,
	SelectValue,
} from "../ui/select";

export const AddWalletView = () => {
	const navigate = useNavigate();
	const [name, setName] = useState("");
	const [type, setType] = useState("personal"); // personal (card), virtual, cash
	const [currency, setCurrency] = useState("INR");
	const [initialBalance, setInitialBalance] = useState("");
	const [loading, setLoading] = useState(false);

	const handleSubmit = async () => {
		if (!name) {
			alert("Please enter a wallet name");
			return;
		}

		setLoading(true);
		try {
			// 1. Create Wallet
			const response = await walletService.createWallet({
				name,
				type,
				currency,
			});

			// 2. Adjust Balance if needed
			const balance = parseFloat(initialBalance);
			if (!isNaN(balance) && balance !== 0) {
				await walletService.adjustBalance(response.id, balance);
			}

			navigate("/wallets");
		} catch (error) {
			console.error("Failed to create wallet:", error);
			alert("Failed to create wallet. Please try again.");
		} finally {
			setLoading(false);
		}
	};

	return (
		<div className="flex flex-col h-full bg-(--background) animate-in fade-in slide-in-from-right duration-300 md:max-w-2xl md:mx-auto md:my-8 md:border md:border-(--border) md:rounded-3xl md:shadow-xl md:h-[calc(100vh-4rem)]">
			{/* Header */}
			<div className="p-6 flex items-center gap-4">
				<button
					onClick={() => navigate("/wallets")}
					className="p-2 -ml-2 hover:bg-(--muted) rounded-full transition-colors text-(--foreground)"
				>
					<ArrowLeft size={24} />
				</button>
				<h1 className="text-2xl font-bold text-(--foreground)">
					Add New Wallet
				</h1>
			</div>

			<div className="flex-1 px-6 space-y-6 overflow-y-auto">
				{/* Wallet Name */}
				<div className="space-y-2">
					<label className="text-sm font-medium text-(--muted-foreground)">
						Wallet Name
					</label>
					<div className="relative">
						<Wallet
							className="absolute left-4 top-1/2 -translate-y-1/2 text-(--muted-foreground)"
							size={20}
						/>
						<input
							type="text"
							value={name}
							onChange={(e) => setName(e.target.value)}
							placeholder="e.g. HDFC Credit Card"
							className="w-full bg-(--card) text-(--foreground) pl-12 pr-4 py-4 rounded-2xl border border-(--border) focus:border-(--primary) outline-none transition-all placeholder:text-(--muted-foreground)/50"
						/>
					</div>
				</div>

				{/* Wallet Type */}
				<div className="space-y-2">
					<label className="text-sm font-medium text-(--muted-foreground)">
						Type
					</label>
					<Select value={type} onValueChange={setType}>
						<SelectTrigger className="w-full h-14 bg-(--card) border-(--border) rounded-2xl px-4 text-(--foreground)">
							<div className="flex items-center gap-3">
								<CreditCard size={20} className="text-(--muted-foreground)" />
								<SelectValue placeholder="Select Type" />
							</div>
						</SelectTrigger>
						<SelectContent>
							<SelectItem value="personal">Physical Card / Bank</SelectItem>
							<SelectItem value="virtual">Virtual Wallet</SelectItem>
							<SelectItem value="cash">Cash</SelectItem>
						</SelectContent>
					</Select>
				</div>

				{/* Currency */}
				<div className="space-y-2">
					<label className="text-sm font-medium text-(--muted-foreground)">
						Currency
					</label>
					<Select value={currency} onValueChange={setCurrency}>
						<SelectTrigger className="w-full h-14 bg-(--card) border-(--border) rounded-2xl px-4 text-(--foreground)">
							<div className="flex items-center gap-3">
								<DollarSign size={20} className="text-(--muted-foreground)" />
								<SelectValue placeholder="Select Currency" />
							</div>
						</SelectTrigger>
						<SelectContent>
							<SelectItem value="INR">Indian Rupee (INR)</SelectItem>
							<SelectItem value="USD">US Dollar (USD)</SelectItem>
							<SelectItem value="EUR">Euro (EUR)</SelectItem>
						</SelectContent>
					</Select>
				</div>

				{/* Initial Balance */}
				<div className="space-y-2">
					<label className="text-sm font-medium text-(--muted-foreground)">
						Initial Balance
					</label>
					<div className="relative">
						<span className="absolute left-4 top-1/2 -translate-y-1/2 text-(--muted-foreground) font-bold text-lg">
							{currency === "INR" ? "₹" : currency === "EUR" ? "€" : "$"}
						</span>
						<input
							type="number"
							value={initialBalance}
							onChange={(e) => setInitialBalance(e.target.value)}
							placeholder="0.00"
							className="w-full bg-(--card) text-(--foreground) pl-10 pr-4 py-4 rounded-2xl border border-(--border) focus:border-(--primary) outline-none transition-all placeholder:text-(--muted-foreground)/50"
						/>
					</div>
					<p className="text-xs text-(--muted-foreground) px-1">
						Current balance in the wallet
					</p>
				</div>
			</div>

			{/* Footer Action */}
			<div className="p-6 bg-(--background)">
				<button
					onClick={handleSubmit}
					disabled={loading}
					className="w-full py-4 bg-(--primary) text-(--primary-foreground) rounded-4xl font-bold text-lg shadow-lg hover:shadow-xl hover:scale-[1.02] active:scale-[0.98] transition-all flex items-center justify-center gap-2 disabled:opacity-50 disabled:hover:scale-100"
				>
					{loading ? (
						<>
							<Loader2 size={24} className="animate-spin" />
							Creating...
						</>
					) : (
						<>
							<Check size={24} />
							Create Wallet
						</>
					)}
				</button>
			</div>
		</div>
	);
};
