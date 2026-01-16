import {
	Calendar,
	Check,
	Delete,
	Edit3,
	Loader2,
	Moon,
	Split,
	Sun,
	Users,
	Wallet,
	X,
} from "lucide-react";
import { useEffect, useRef, useState, type MouseEvent } from "react";
import { useLocation } from "react-router-dom";
import { categoryService } from "../../services/categoryService";
import { debtService } from "../../services/debtService";
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
import { Switch } from "../ui/switch";

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
	const location = useLocation();
	const [categories, setCategories] = useState<BackendCategory[]>([]);
	const [wallets, setWallets] = useState<BackendWallet[]>([]);
	const [people, setPeople] = useState<any[]>([]); // Friends/Contacts
	const [selectedCategory, setSelectedCategory] = useState<string>("");
	const [selectedWallet, setSelectedWallet] = useState<string>("");
	const [transactionType, setTransactionType] = useState<"expense" | "income">(
		"expense",
	);
	const [isSplit, setIsSplit] = useState(false);
	const [splitWith, setSplitWith] = useState<string>("");

	// New states for enhanced splitting
	const [paidBy, setPaidBy] = useState<"me" | "them">("me");
	const [splitType, setSplitType] = useState<"equal" | "percentage" | "exact">(
		"equal",
	);
	const [myPercentage, setMyPercentage] = useState(50);
	const [myExactAmount, setMyExactAmount] = useState(0);

	const [note, setNote] = useState("");
	const [loading, setLoading] = useState(true);
	const [submitting, setSubmitting] = useState(false);
	const [totalBalance, setTotalBalance] = useState(0);

	useEffect(() => {
		const fetchData = async () => {
			setLoading(true);
			try {
				const [catsData, walletsData, balanceData, debtsData] =
					await Promise.all([
						categoryService.getCategories(),
						walletService.getWallets(),
						walletService.getTotalBalance("INR"),
						debtService.getDebts(),
					]);

				setCategories(catsData);
				setWallets(walletsData);
				setTotalBalance(balanceData.total_balance);

				// Process unique people from debts for "Split With"
				const uniquePeople = Array.from(
					new Map(debtsData.map((d: any) => [d.counterparty_name, d])).values(),
				).filter(
					(p: any) => p.counterparty_name && p.counterparty_name.trim() !== "",
				);
				setPeople(uniquePeople);

				// If no categories exist, try to seed defaults
				if (catsData.length === 0) {
					try {
						await categoryService.seedDefaults();
						const newCats = await categoryService.getCategories();
						setCategories(newCats);
						if (newCats.length > 0) {
							const defaultCat = newCats.find(
								(c) => c.type === "expense" || c.type === "both",
							);
							if (defaultCat) setSelectedCategory(defaultCat.id);
						}
					} catch (seedError) {
						console.error("Failed to seed default categories:", seedError);
					}
				} else {
					// Set defaults
					const defaultCat = catsData.find(
						(c) => c.type === "expense" || c.type === "both",
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

	// Handle pre-filled split data from location state
	useEffect(() => {
		if (location.state?.splitWith) {
			setIsSplit(true);
			setSplitWith(location.state.splitWith.name); // Using name for now as ID might be for debt record
		}
	}, [location.state]);

	// Calculate split amounts based on type
	const calculateSplitAmount = (
		totalAmount: number,
	): { myShare: number; theirShare: number } => {
		switch (splitType) {
			case "equal":
				return { myShare: totalAmount / 2, theirShare: totalAmount / 2 };
			case "percentage":
				return {
					myShare: (totalAmount * myPercentage) / 100,
					theirShare: (totalAmount * (100 - myPercentage)) / 100,
				};
			case "exact":
				return {
					myShare: myExactAmount,
					theirShare: totalAmount - myExactAmount,
				};
			default:
				return { myShare: totalAmount / 2, theirShare: totalAmount / 2 };
		}
	};

	const handleSubmit = async () => {
		const amountNum = parseFloat(amount);
		if (isNaN(amountNum) || amountNum <= 0) {
			alert("Please enter a valid amount");
			return;
		}

		if (isSplit && !splitWith) {
			alert("Please select who to split with");
			return;
		}

		setSubmitting(true);
		try {
			const { myShare, theirShare } = calculateSplitAmount(amountNum);

			if (isSplit) {
				if (paidBy === "me") {
					// I paid the full amount
					// Record my expense
					await expenseService.addTransaction({
						amount: amountNum,
						currency: "INR",
						type: transactionType,
						date: new Date().toISOString(),
						note: note || `Split with ${splitWith}`,
						wallet_id: selectedWallet || undefined,
						category_id: selectedCategory || undefined,
					});
					// They owe me their share
					await debtService.addDebt({
						counterparty_name: splitWith,
						amount: theirShare,
						type: "owed_to_me",
					});
				} else {
					// They paid the full amount
					// I owe them my share
					await debtService.addDebt({
						counterparty_name: splitWith,
						amount: myShare,
						type: "owed_by_me",
					});
				}
			} else {
				// No split, just a regular transaction
				await expenseService.addTransaction({
					amount: amountNum,
					currency: "INR",
					type: transactionType,
					date: new Date().toISOString(),
					note: note || undefined,
					wallet_id: selectedWallet || undefined,
					category_id: selectedCategory || undefined,
				});
			}

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
		(cat) => cat.type === transactionType || cat.type === "both",
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

					<div className="flex gap-4 w-full justify-between mb-4">
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

					{/* Split Toggle */}
					<div className="w-full mb-6">
						<div className="flex items-center justify-between mb-2">
							<div className="flex items-center gap-2 text-(--muted-foreground)">
								<Split size={18} />
								<span className="text-sm font-medium">Split Expense</span>
							</div>
							<Switch
								checked={isSplit}
								onCheckedChange={setIsSplit}
								className="data-[state=checked]:bg-(--primary)"
							/>
						</div>

						{isSplit && (
							<div className="space-y-3 mt-3">
								{/* Split With Person */}
								<Select value={splitWith} onValueChange={setSplitWith}>
									<SelectTrigger className="w-full">
										<div className="flex items-center gap-2">
											<Users size={18} />
											<SelectValue placeholder="Split with..." />
										</div>
									</SelectTrigger>
									<SelectContent>
										{people
											.filter(
												(person) =>
													person.counterparty_name &&
													person.counterparty_name.trim() !== "",
											)
											.map((person) => (
												<SelectItem
													key={person.id || person.counterparty_name}
													value={person.counterparty_name}
												>
													<div className="flex items-center gap-2">
														<Users size={18} />
														{person.counterparty_name}
													</div>
												</SelectItem>
											))}
										{people.length === 0 && (
											<SelectItem value="none" disabled>
												No contacts
											</SelectItem>
										)}
									</SelectContent>
								</Select>

								{/* Paid By Toggle */}
								{splitWith && (
									<div className="flex bg-(--muted) rounded-full p-1 w-full">
										<button
											onClick={() => setPaidBy("me")}
											className={`flex-1 py-2 px-3 rounded-full text-xs font-medium transition-all ${
												paidBy === "me"
													? "bg-(--primary) text-(--primary-foreground)"
													: "text-(--muted-foreground)"
											}`}
										>
											You Paid
										</button>
										<button
											onClick={() => setPaidBy("them")}
											className={`flex-1 py-2 px-3 rounded-full text-xs font-medium transition-all ${
												paidBy === "them"
													? "bg-(--primary) text-(--primary-foreground)"
													: "text-(--muted-foreground)"
											}`}
										>
											{splitWith} Paid
										</button>
									</div>
								)}

								{/* Split Type Selector */}
								{splitWith && (
									<div className="flex bg-(--muted) rounded-xl p-1 w-full">
										{(["equal", "percentage", "exact"] as const).map((type) => (
											<button
												key={type}
												onClick={() => setSplitType(type)}
												className={`flex-1 py-2 px-2 rounded-lg text-xs font-medium transition-all capitalize ${
													splitType === type
														? "bg-(--card) text-(--foreground) shadow-sm"
														: "text-(--muted-foreground)"
												}`}
											>
												{type}
											</button>
										))}
									</div>
								)}

								{/* Custom Split Inputs */}
								{splitWith && splitType === "percentage" && (
									<div className="flex items-center gap-2 text-sm">
										<span className="text-(--muted-foreground)">You:</span>
										<input
											type="number"
											min="0"
											max="100"
											value={myPercentage}
											onChange={(e) =>
												setMyPercentage(
													Math.min(100, Math.max(0, Number(e.target.value))),
												)
											}
											className="w-16 px-2 py-1 rounded-lg bg-(--muted) text-(--foreground) text-center border border-(--border)"
										/>
										<span className="text-(--muted-foreground)">%</span>
										<span className="mx-2 text-(--muted-foreground)">|</span>
										<span className="text-(--muted-foreground)">
											{splitWith}:
										</span>
										<span className="font-medium text-(--foreground)">
											{100 - myPercentage}%
										</span>
									</div>
								)}

								{splitWith && splitType === "exact" && (
									<div className="flex items-center gap-2 text-sm">
										<span className="text-(--muted-foreground)">You:</span>
										<span className="text-(--muted-foreground)">₹</span>
										<input
											type="number"
											min="0"
											value={myExactAmount}
											onChange={(e) =>
												setMyExactAmount(Math.max(0, Number(e.target.value)))
											}
											className="w-20 px-2 py-1 rounded-lg bg-(--muted) text-(--foreground) text-center border border-(--border)"
										/>
										<span className="mx-2 text-(--muted-foreground)">|</span>
										<span className="text-(--muted-foreground)">
											{splitWith}:
										</span>
										<span className="font-medium text-(--foreground)">
											₹
											{Math.max(0, parseFloat(amount) - myExactAmount).toFixed(
												2,
											)}
										</span>
									</div>
								)}
							</div>
						)}
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
