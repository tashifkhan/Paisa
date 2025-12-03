// @ts-nocheck
import { useState } from "react";
import {
	BrowserRouter as Router,
	Routes,
	Route,
	useNavigate,
	useLocation,
} from "react-router-dom";
import {
	Shirt,
	Gift,
	Pizza,
	Wallet,
	Smartphone,
	Plane,
	Home as HomeIcon,
	Banknote,
} from "lucide-react";
import { themeStyles } from "./styles/theme";
import type { Transaction } from "./types";
import { HomeView } from "./components/views/HomeView";
import { StatsView } from "./components/views/StatsView";
import { WalletsView } from "./components/views/WalletsView";
import { AddExpenseView } from "./components/views/AddExpenseView";
import { ProfileView } from "./components/views/ProfileView";
import { SocialView } from "./components/views/SocialView";
import { GroupDetailView } from "./components/views/GroupDetailView";
import { CreateGroupView } from "./components/views/CreateGroupView";
import {
	SignInView,
	SignUpView,
	ForgotPasswordView,
	OTPView,
} from "./components/views/AuthViews";
import { NotFoundView } from "./components/views/NotFoundView";
import { BottomNav } from "./components/shared/BottomNav";
import { UserDetailView } from "./components/views/UserDetailView";
import { SideNav } from "./components/shared/SideNav";

// --- App Shell (with global state and Router) ---
export default function App() {
	return (
		<Router>
			<AppContent />
		</Router>
	);
}

// --- Main App Content Component ---
function AppContent() {
	const navigate = useNavigate();
	const location = useLocation();
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

	const [debts, setDebts] = useState([
		{
			id: 1,
			name: "Rahul Sharma",
			amount: 500,
			type: "owed_to_me",
			date: "Due in 3 days",
		},
		{
			id: 2,
			name: "Anita Roy",
			amount: 1200,
			type: "owed_by_me",
			date: "Due tomorrow",
		},
		{
			id: 3,
			name: "John Doe",
			amount: 250,
			type: "owed_to_me",
			date: "Due in 1 week",
		},
	]);

	const [groups, setGroups] = useState([
		{
			id: 1,
			name: "Goa Trip",
			members: 5,
			balance: -2000,
			type: "owe",
			icon: Plane,
			color: "bg-orange-500",
		},
		{
			id: 2,
			name: "Flat 302 Rent",
			members: 3,
			balance: 5000,
			type: "owed",
			icon: HomeIcon,
			color: "bg-indigo-500",
		},
	]);

	const [groupExpenses, setGroupExpenses] = useState([
		{
			id: 1,
			title: "Dinner at Thalassa",
			amount: 4500,
			paidBy: "Tashif",
			date: "Yesterday",
			icon: Pizza,
		},
		{
			id: 2,
			title: "Scooty Rental",
			amount: 1200,
			paidBy: "Rahul",
			date: "Today",
			icon: Banknote,
		},
		{
			id: 3,
			title: "Villa Advance",
			amount: 15000,
			paidBy: "Anita",
			date: "2 days ago",
			icon: HomeIcon,
		},
	]);

	const [socialTab, setSocialTab] = useState("debts");
	const [groupDetailTab, setGroupDetailTab] = useState("expenses");
	const [selectedUser, setSelectedUser] = useState(null);

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
			navigate("/stats");
		} else {
			setAmount((prev) => {
				if (prev === "0" || prev === "25.00") return key;
				return prev + key;
			});
		}
	};

	// Check if we're on a tab-based page (pages that have internal tab navigation)
	const tabBasedPages = [
		"/",
		"/stats",
		"/wallets",
		"/debts",
		"/group-detail",
		"/user-detail",
		"/profile",
	];
	const showBottomNav = tabBasedPages.includes(location.pathname);

	const handleAuthNavigation = (view: string) => {
		navigate(view === "home" ? "/" : `/${view}`);
	};

	return (
		<>
			<style>{themeStyles}</style>
			<div
				className={`${
					isDarkMode ? "dark" : ""
				} w-full min-h-screen bg-(--background) transition-colors duration-500 flex`}
			>
				{showBottomNav && <SideNav isDarkMode={isDarkMode} />}
				<div className="relative flex-1 h-screen bg-(--background) overflow-hidden transition-colors duration-300">
					<Routes>
						{/* Auth Routes */}
						<Route
							path="/signin"
							element={
								<SignInView
									setCurrentView={handleAuthNavigation}
									isDarkMode={isDarkMode}
								/>
							}
						/>
						<Route
							path="/signup"
							element={
								<SignUpView
									setCurrentView={handleAuthNavigation}
									isDarkMode={isDarkMode}
								/>
							}
						/>
						<Route
							path="/forgot-password"
							element={
								<ForgotPasswordView
									setCurrentView={handleAuthNavigation}
									isDarkMode={isDarkMode}
								/>
							}
						/>
						<Route
							path="/otp"
							element={
								<OTPView
									setCurrentView={handleAuthNavigation}
									isDarkMode={isDarkMode}
								/>
							}
						/>

						{/* Main Routes */}
						<Route
							path="/"
							element={
								<HomeView
									transactions={transactions}
									isDarkMode={isDarkMode}
									toggleTheme={toggleTheme}
								/>
							}
						/>
						<Route path="/stats" element={<StatsView />} />
						<Route
							path="/wallets"
							element={
								<WalletsView
									activeWalletTab={activeWalletTab}
									setActiveWalletTab={setActiveWalletTab}
								/>
							}
						/>
						<Route
							path="/add-expense"
							element={
								<AddExpenseView
									amount={amount}
									isDarkMode={isDarkMode}
									toggleTheme={toggleTheme}
									handleKeyPress={handleKeyPress}
									setCurrentView={() => navigate("/stats")}
								/>
							}
						/>
						<Route
							path="/debts"
							element={
								<SocialView
									debts={debts}
									groups={groups}
									socialTab={socialTab}
									setSocialTab={setSocialTab}
									setCurrentView={(view) => navigate(view)}
									setSelectedUser={(user) => {
										setSelectedUser(user);
										navigate("/user-detail");
									}}
								/>
							}
						/>
						<Route
							path="/user-detail"
							element={
								<UserDetailView
									user={selectedUser}
									setCurrentView={() => navigate("/debts")}
								/>
							}
						/>
						<Route
							path="/group-detail"
							element={
								<GroupDetailView
									groupExpenses={groupExpenses}
									groupDetailTab={groupDetailTab}
									setGroupDetailTab={setGroupDetailTab}
									setCurrentView={() => navigate("/debts")}
								/>
							}
						/>
						<Route
							path="/create-group"
							element={
								<CreateGroupView setCurrentView={() => navigate("/debts")} />
							}
						/>
						<Route
							path="/profile"
							element={
								<ProfileView
									isDarkMode={isDarkMode}
									toggleTheme={toggleTheme}
									notifications={notifications}
									setNotifications={setNotifications}
									currency={currency}
									setCurrency={setCurrency}
									language={language}
									setLanguage={setLanguage}
									setCurrentView={() => navigate("/")}
								/>
							}
						/>

						{/* 404 Not Found */}
						<Route
							path="*"
							element={<NotFoundView onGoHome={() => navigate("/")} />}
						/>
					</Routes>

					{showBottomNav && (
						<BottomNav
							currentView={location.pathname.slice(1) || "home"}
							setCurrentView={(view) =>
								navigate(`/${view === "home" ? "" : view}`)
							}
						/>
					)}
				</div>
			</div>
		</>
	);
}
