// @ts-nocheck
import { Plane, Shirt } from "lucide-react";
import { useEffect, useState } from "react";
import {
	Route,
	BrowserRouter as Router,
	Routes,
	useLocation,
	useNavigate,
} from "react-router-dom";
import { BottomNav } from "./components/shared/BottomNav";
import { SideNav } from "./components/shared/SideNav";
import { AddExpenseView } from "./components/views/AddExpenseView";
import {
	ForgotPasswordView,
	OTPView,
	SignInView,
	SignUpView,
} from "./components/views/AuthViews";
import { CreateGroupView } from "./components/views/CreateGroupView";
import { GroupDetailView } from "./components/views/GroupDetailView";
import { HomeView } from "./components/views/HomeView";
import { NotFoundView } from "./components/views/NotFoundView";
import { ProfileView } from "./components/views/ProfileView";
import { SocialView } from "./components/views/SocialView";
import { StatsView } from "./components/views/StatsView";
import { UserDetailView } from "./components/views/UserDetailView";
import { WalletsView } from "./components/views/WalletsView";
import { themeStyles } from "./styles/theme";
import type { Transaction } from "./types";

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
	const [isDarkMode, setIsDarkMode] = useState(() => {
		const savedTheme = localStorage.getItem("theme");
		return savedTheme === "dark";
	});

	useEffect(() => {
		localStorage.setItem("theme", isDarkMode ? "dark" : "light");
	}, [isDarkMode]);

	// Check Backend Connection
	useEffect(() => {
		import("./lib/api").then(({ default: api }) => {
			api
				.get("/")
				.then((res) => console.log("Backend Connected:", res.data))
				.catch((err) => console.error("Backend Connection Failed:", err));
		});
	}, []);
	const [notifications, setNotifications] = useState(true);
	const [currency, setCurrency] = useState("INR");
	const [language, setLanguage] = useState("English");
	const [activeWalletTab, setActiveWalletTab] = useState("Cards");

	// --- API State ---
	const [transactions, setTransactions] = useState<Transaction[]>([]);
	const [groups, setGroups] = useState<any[]>([]);
	const [debts, setDebts] = useState<any[]>([]);
	const [groupExpenses, setGroupExpenses] = useState<any[]>([]);

	useEffect(() => {
		const checkAuthAndFetch = async () => {
			const { authService } = await import("./services/authService");

			// Check Authentication
			const isAuth = authService.isAuthenticated();
			const publicRoutes = ["/signin", "/signup", "/otp", "/forgot-password"];

			if (!isAuth) {
				if (!publicRoutes.includes(location.pathname)) {
					navigate("/signin");
				}
				return;
			}

			try {
				const { expenseService } = await import("./services/expenseService");
				const { groupService } = await import("./services/groupService");
				const { debtService } = await import("./services/debtService");

				// Fetch Transactions
				const txnsData = await expenseService.getTransactions();
				const mappedTxns = txnsData.map((t: any) => ({
					id: t.id,
					title: t.note || t.type || "Expense",
					subtitle: t.currency,
					amount: t.amount.toString(),
					percent: "0",
					icon: Shirt,
				}));
				setTransactions(mappedTxns as any);

				// Fetch Groups
				const groupsData = await groupService.getGroups();
				const mappedGroups = groupsData.map((g: any) => ({
					id: g.id,
					name: g.name,
					members: 1,
					balance: 0,
					type: "owe",
					icon: Plane,
					color: "bg-blue-500",
				}));
				setGroups(mappedGroups);

				// Fetch Debts
				const debtsData = await debtService.getDebts();
				setDebts(
					debtsData.map((d: any) => ({
						id: d.id,
						name: d.counterparty_name,
						amount: d.amount,
						type: d.type,
						date: d.due_date || "No due date",
					}))
				);
			} catch (error) {
				console.error("Failed to fetch data", error);
				if ((error as any)?.response?.status === 401) {
					authService.logout();
					navigate("/signin");
				}
			}
		};

		checkAuthAndFetch();
	}, [navigate, location.pathname]);

	const [socialTab, setSocialTab] = useState("debts");
	const [groupDetailTab, setGroupDetailTab] = useState("expenses");
	const [selectedUser, setSelectedUser] = useState(null);
	const [selectedGroup, setSelectedGroup] = useState<any>(null); // Add selectedGroup state

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
									// Pass setSelectedGroup
									setSelectedGroup={(group) => {
										setSelectedGroup(group);
										navigate("/group-detail");
									}}
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
									group={selectedGroup} // Pass selectedGroup
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
