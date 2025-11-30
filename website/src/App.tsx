// @ts-nocheck
import { useState } from "react";
import { Shirt, Gift, Pizza, Wallet, Smartphone } from "lucide-react";
import { themeStyles } from "./styles/theme";
import type { Transaction } from "./types";
import { HomeView } from "./components/views/HomeView";
import { StatsView } from "./components/views/StatsView";
import { WalletsView } from "./components/views/WalletsView";
import { AddExpenseView } from "./components/views/AddExpenseView";
import { ProfileView } from "./components/views/ProfileView";
import { BottomNav } from "./components/shared/BottomNav";

// --- Main App Component ---

export default function App() {
	const [currentView, setCurrentView] = useState("home");
	const [amount, setAmount] = useState("25.00");
	const [isDarkMode, setIsDarkMode] = useState(false);
	const [notifications, setNotifications] = useState(true);
	const [currency, setCurrency] = useState("INR");
	const [language, setLanguage] = useState("English");
	const [activeWalletTab, setActiveWalletTab] = useState("Cards");

	const [transactions, setTransactions] = useState<Transaction[]>([
		{
			id: 1,
			title: "Shopping",
			subtitle: "Cash",
			amount: "498.50",
			percent: "32",
			icon: Shirt,
		},
		{
			id: 2,
			title: "Gifts",
			subtitle: "Cash - Card",
			amount: "344.45",
			percent: "21",
			icon: Gift,
		},
		{
			id: 3,
			title: "Food",
			subtitle: "Cash",
			amount: "230.50",
			percent: "12",
			icon: Pizza,
		},
		{
			id: 4,
			title: "Taxi",
			subtitle: "Card",
			amount: "45.00",
			percent: "5",
			icon: Wallet,
		},
		{
			id: 5,
			title: "Mobile Bill",
			subtitle: "Online",
			amount: "55.00",
			percent: "6",
			icon: Smartphone,
		},
	]);

	const toggleTheme = () => setIsDarkMode(!isDarkMode);

	// Keypad Logic
	const handleKeyPress = (key) => {
		if (key === "backspace") {
			setAmount((prev) => (prev.length > 1 ? prev.slice(0, -1) : "0"));
		} else if (key === "check") {
			const newTransaction = {
				id: Date.now(),
				title: "New Expense",
				subtitle: "Cash",
				amount: amount,
				percent: "10",
				icon: Shirt,
			};
			setTransactions([newTransaction, ...transactions]);
			setAmount("0");
			setCurrentView("stats");
		} else {
			setAmount((prev) => {
				if (prev === "0" || prev === "25.00") return key;
				return prev + key;
			});
		}
	};

	return (
		<>
			<style>{themeStyles}</style>
			<div
				className={`${
					isDarkMode ? "dark" : ""
				} w-full min-h-screen bg-(--background) transition-colors duration-500`}
			>
				<div className="relative w-full h-screen bg-(--background) overflow-hidden transition-colors duration-300">
					{currentView === "addExpense" ? (
						<AddExpenseView
							amount={amount}
							isDarkMode={isDarkMode}
							toggleTheme={toggleTheme}
							handleKeyPress={handleKeyPress}
							setCurrentView={setCurrentView}
						/>
					) : (
						<>
							{currentView === "home" && (
								<HomeView
									transactions={transactions}
									isDarkMode={isDarkMode}
									toggleTheme={toggleTheme}
								/>
							)}
							{currentView === "stats" && <StatsView />}
							{currentView === "wallets" && (
								<WalletsView
									activeWalletTab={activeWalletTab}
									setActiveWalletTab={setActiveWalletTab}
								/>
							)}
							{currentView === "profile" && (
								<ProfileView
									isDarkMode={isDarkMode}
									toggleTheme={toggleTheme}
									notifications={notifications}
									setNotifications={setNotifications}
									currency={currency}
									setCurrency={setCurrency}
									language={language}
									setLanguage={setLanguage}
									setCurrentView={setCurrentView}
								/>
							)}
							<BottomNav
								currentView={currentView}
								setCurrentView={setCurrentView}
							/>
						</>
					)}
				</div>
			</div>
		</>
	);
}
