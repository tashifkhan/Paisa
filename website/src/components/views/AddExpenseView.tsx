import {
	Calendar,
	Check,
	Delete,
	Edit3,
	Loader2,
	Moon,
	Sun,
	Wallet,
	X,
} from "lucide-react";
import { useEffect, useRef, useState, type MouseEvent } from "react";
import { categoryService } from "../../services/categoryService";
import { expenseService } from "../../services/expenseService";
import type { BackendCategory, BackendWallet } from "../../services/types";
import { walletService } from "../../services/walletService";
import {
	Select,
	SelectContent,
	SelectItem,
	SelectTrigger,
	SelectValue,
} from "../ui/select";

interface AddExpenseViewProps {
	amount: string;
	isDarkMode: boolean;
	toggleTheme: () => void;
	handleKeyPress: (key: string) => void;
	setCurrentView: (view: string) => void;
}

interface Ripple {
	x: number;
	y: number;
	size: number;
	id: number;
}

const RippleButton = ({
	children,
	onClick,
	className = "",
	disabled = false,
	...props
}: {
	children: React.ReactNode;
	onClick?: () => void;
	className?: string;
	disabled?: boolean;
	[key: string]: any;
}) => {
	const [ripples, setRipples] = useState<Ripple[]>([]);
	const buttonRef = useRef<HTMLButtonElement>(null);

	const createRipple = (event: MouseEvent<HTMLButtonElement>) => {
		const button = buttonRef.current;
		if (!button) return;

		const rect = button.getBoundingClientRect();
		const size = Math.max(rect.width, rect.height);
		const x = event.clientX - rect.left - size / 2;
		const y = event.clientY - rect.top - size / 2;

		const newRipple = {
			x,
			y,
			size,
			id: Date.now(),
		};

		setRipples((prev) => [...prev, newRipple]);

		setTimeout(() => {
			setRipples((prev) => prev.filter((r) => r.id !== newRipple.id));
		}, 600);
	};

	const handleClick = (event: MouseEvent<HTMLButtonElement>) => {
		createRipple(event);
		onClick?.();
	};

	return (
		<button
			ref={buttonRef}
			onClick={handleClick}
			disabled={disabled}
			className={`relative overflow-hidden ${className} ${
				disabled ? "opacity-50" : ""
			}`}
			{...props}
		>
			{children}
			{ripples.map((ripple) => (
				<span
					key={ripple.id}
					style={{
						position: "absolute",
						left: ripple.x,
						top: ripple.y,
						width: ripple.size,
						height: ripple.size,
						borderRadius: "50%",
						background: "rgba(128, 128, 128, 0.4)",
						transform: "scale(0)",
						animation: "ripple 600ms ease-out",
						pointerEvents: "none",
					}}
				/>
			))}
			<style>
				{`
					@keyframes ripple {
						0% {
							transform: scale(0);
							opacity: 1;
						}
						100% {
							transform: scale(2.5);
							opacity: 0;
						}
					}
				`}
			</style>
		</button>
	);
};

export const AddExpenseView = ({
	amount,
	isDarkMode,
	toggleTheme,
	handleKeyPress,
	setCurrentView,
}: AddExpenseViewProps) => {
	const [categories, setCategories] = useState<BackendCategory[]>([]);
	const [wallets, setWallets] = useState<BackendWallet[]>([]);
	const [selectedCategory, setSelectedCategory] = useState<string>("");
	const [selectedWallet, setSelectedWallet] = useState<string>("");
	const [transactionType, setTransactionType] = useState<"expense" | "income">(
		"expense"
	);
	const [note, setNote] = useState("");
	const [loading, setLoading] = useState(true);
	const [submitting, setSubmitting] = useState(false);
	const [totalBalance, setTotalBalance] = useState(0);

	useEffect(() => {
		const fetchData = async () => {
			setLoading(true);
			try {
				const [catsData, walletsData, balanceData] = await Promise.all([
					categoryService.getCategories(),
					walletService.getWallets(),
					walletService.getTotalBalance("INR"),
				]);

				setCategories(catsData);
				setWallets(walletsData);
				setTotalBalance(balanceData.total_balance);

				// If no categories exist, try to seed defaults
				if (catsData.length === 0) {
					try {
						await categoryService.seedDefaults();
						const newCats = await categoryService.getCategories();
						setCategories(newCats);
						if (newCats.length > 0) {
							const defaultCat = newCats.find(
								(c) => c.type === "expense" || c.type === "both"
							);
							if (defaultCat) setSelectedCategory(defaultCat.id);
						}
					} catch (seedError) {
						console.error("Failed to seed default categories:", seedError);
					}
				} else {
					// Set defaults
					const defaultCat = catsData.find(
						(c) => c.type === "expense" || c.type === "both"
					);
					if (defaultCat) setSelectedCategory(defaultCat.id);
				}

				if (walletsData.length > 0) {
					setSelectedWallet(walletsData[0].id);
				}
			} catch (error) {
				console.error("Failed to fetch categories/wallets:", error);
			} finally {
				setLoading(false);
			}
		};
		fetchData();
	}, []);

	const handleSubmit = async () => {
		const amountNum = parseFloat(amount);
		if (isNaN(amountNum) || amountNum <= 0) {
			alert("Please enter a valid amount");
			return;
		}

		setSubmitting(true);
		try {
			await expenseService.addTransaction({
				amount: amountNum,
				currency: "INR",
				type: transactionType,
				date: new Date().toISOString(),
				note: note || undefined,
				wallet_id: selectedWallet || undefined,
				category_id: selectedCategory || undefined,
			});

			// Navigate back to stats view
			setCurrentView("stats");
		} catch (error: unknown) {
			console.error("Failed to add transaction:", error);
			const message =
				error instanceof Error
					? error.message
					: (error as { response?: { data?: { detail?: string } } })?.response
							?.data?.detail || "Failed to add transaction. Please try again.";
			alert(message);
		} finally {
			setSubmitting(false);
		}
	};

	const formatCurrency = (amount: number) => {
		return new Intl.NumberFormat("en-IN", {
			style: "currency",
			currency: "INR",
			minimumFractionDigits: 2,
			maximumFractionDigits: 2,
		}).format(amount);
	};

	// Filter categories based on transaction type
	const filteredCategories = categories.filter(
		(cat) => cat.type === transactionType || cat.type === "both"
	);

	return (
		<div className="flex flex-col h-full bg-(--background) transition-colors duration-300">
			<div className="max-w-md mx-auto w-full h-full flex flex-col shadow-2xl md:my-8 md:rounded-[3rem] md:h-auto md:min-h-[800px] overflow-hidden bg-(--background)">
				<header className="flex justify-between items-start p-6">
					<RippleButton
						onClick={() => setCurrentView("stats")}
						className="p-2 text-(--foreground)"
					>
						<X size={24} />
					</RippleButton>
					<div className="flex flex-col items-center opacity-50">
						<h1 className="text-sm font-bold text-(--foreground) transition-colors duration-300">
							{formatCurrency(totalBalance)}
						</h1>
						<div className="text-xs text-(--muted-foreground)">
							Total Balance
						</div>
					</div>
					<div className="flex items-center gap-1">
						<RippleButton
							onClick={toggleTheme}
							className="p-2 text-(--foreground) hover:bg-(--muted) rounded-full transition-colors"
						>
							{isDarkMode ? <Sun size={20} /> : <Moon size={20} />}
						</RippleButton>
						<RippleButton className="p-2 text-(--foreground)">
							<Edit3 size={20} />
						</RippleButton>
					</div>
				</header>

				<div className="flex-1 flex flex-col items-center px-8 pt-4">
					{/* Type Toggle */}
					<div className="flex bg-(--muted) rounded-full p-1 mb-6 w-full max-w-xs">
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

					<div className="flex gap-4 w-full justify-between mb-8">
						{/* Wallet Select */}
						<Select value={selectedWallet} onValueChange={setSelectedWallet}>
							<SelectTrigger className="flex-1">
								<div className="flex items-center gap-2">
									<Wallet size={18} />
									<SelectValue
										placeholder={loading ? "Loading..." : "Select wallet"}
									/>
								</div>
							</SelectTrigger>
							<SelectContent>
								{wallets.map((wallet) => (
									<SelectItem key={wallet.id} value={wallet.id}>
										<div className="flex items-center gap-2">
											<Wallet size={18} />
											{wallet.name}
										</div>
									</SelectItem>
								))}
								{wallets.length === 0 && (
									<SelectItem value="none" disabled>
										No wallets
									</SelectItem>
								)}
							</SelectContent>
						</Select>

						{/* Category Select */}
						<Select
							value={selectedCategory}
							onValueChange={setSelectedCategory}
						>
							<SelectTrigger className="flex-1">
								<div className="flex items-center gap-2">
									<span
										className="w-4 h-4 rounded-full"
										style={{
											backgroundColor:
												categories.find((c) => c.id === selectedCategory)
													?.color || "#888",
										}}
									/>
									<SelectValue
										placeholder={loading ? "Loading..." : "Category"}
									/>
								</div>
							</SelectTrigger>
							<SelectContent>
								{filteredCategories.map((cat) => (
									<SelectItem key={cat.id} value={cat.id}>
										<div className="flex items-center gap-2">
											<span
												className="w-4 h-4 rounded-full"
												style={{ backgroundColor: cat.color || "#888" }}
											/>
											{cat.name}
										</div>
									</SelectItem>
								))}
								{filteredCategories.length === 0 && (
									<SelectItem value="none" disabled>
										No categories
									</SelectItem>
								)}
							</SelectContent>
						</Select>
					</div>

					<div className="flex flex-col items-center justify-center flex-1 w-full mb-8">
						<span className="text-(--muted-foreground) text-sm mb-2">
							{transactionType === "expense" ? "Expense" : "Income"}
						</span>
						<div className="flex items-center text-6xl font-bold text-(--foreground) tracking-tight transition-colors duration-300">
							<span className="text-(--muted-foreground) text-4xl mr-1">₹</span>
							{amount}
							<span className="animate-pulse w-0.5 h-12 bg-(--foreground) ml-1"></span>
						</div>
						<input
							type="text"
							placeholder="Add note..."
							value={note}
							onChange={(e) => setNote(e.target.value)}
							className="mt-6 text-center w-full outline-none bg-transparent text-(--muted-foreground) placeholder-(--muted-foreground) font-medium transition-colors duration-300"
						/>
					</div>
				</div>

				<div className="bg-(--card) rounded-t-[3rem] p-8 pb-10 shadow-[0_-10px_40px_rgba(0,0,0,0.05)] border-t border-(--border) transition-colors duration-300">
					<div className="grid grid-cols-4 gap-4 h-80">
						{[1, 2, 3].map((num) => (
							<RippleButton
								key={num}
								onClick={() => handleKeyPress(num.toString())}
								className="text-2xl font-medium text-(--foreground) rounded-full hover:bg-(--muted) active:scale-95 transition-all"
							>
								{num}
							</RippleButton>
						))}
						<RippleButton
							onClick={() => handleKeyPress("backspace")}
							className="flex items-center justify-center bg-(--destructive) text-(--destructive-foreground) rounded-full hover:opacity-90 active:scale-95 transition-all"
						>
							<Delete size={24} />
						</RippleButton>

						{[4, 5, 6].map((num) => (
							<RippleButton
								key={num}
								onClick={() => handleKeyPress(num.toString())}
								className="text-2xl font-medium text-(--foreground) rounded-full hover:bg-(--muted) active:scale-95 transition-all"
							>
								{num}
							</RippleButton>
						))}
						<RippleButton className="flex items-center justify-center bg-(--muted) text-(--primary) rounded-full hover:bg-(--muted)/80 active:scale-95 transition-all">
							<Calendar size={24} />
						</RippleButton>

						{[7, 8, 9].map((num) => (
							<RippleButton
								key={num}
								onClick={() => handleKeyPress(num.toString())}
								className="text-2xl font-medium text-(--foreground) rounded-full hover:bg-(--muted) active:scale-95 transition-all"
							>
								{num}
							</RippleButton>
						))}

						<RippleButton
							onClick={handleSubmit}
							disabled={submitting}
							className="row-span-2 flex items-center justify-center bg-(--primary) text-(--primary-foreground) rounded-4xl shadow-xl hover:opacity-90 active:scale-95 transition-all"
						>
							{submitting ? (
								<Loader2 size={32} className="animate-spin" />
							) : (
								<Check size={32} />
							)}
						</RippleButton>

						<RippleButton className="text-2xl font-medium text-(--foreground) bg-(--muted) rounded-full hover:bg-(--muted)/80 active:scale-95 transition-all">
							₹
						</RippleButton>
						<RippleButton
							onClick={() => handleKeyPress("0")}
							className="text-2xl font-medium text-(--foreground) rounded-full hover:bg-(--muted) active:scale-95 transition-all"
						>
							0
						</RippleButton>
						<RippleButton
							onClick={() => handleKeyPress(".")}
							className="text-2xl font-medium text-(--foreground) rounded-full hover:bg-(--muted) active:scale-95 transition-all"
						>
							.
						</RippleButton>
					</div>
				</div>
			</div>
		</div>
	);
};
