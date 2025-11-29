// @ts-nocheck
import React, { useState } from "react";
import {
	Menu,
	Bell,
	ChevronDown,
	Home,
	LayoutGrid,
	BarChart2,
	Plus, // Import the Plus icon
	User,
	Shirt,
	Gift,
	Pizza,
	Edit3,
	Delete,
	Calendar,
	Check,
	Wallet,
	X,
	Moon,
	Sun,
	MoreHorizontal,
	ArrowLeft,
	Receipt,
	Smartphone,
	CreditCard as CreditCardIcon,
	Wifi,
	Globe,
	Languages,
	DollarSign,
	Shield,
	CircleHelp,
	LogOut,
	ChevronRight,
	ArrowUpRight,
	ArrowDownLeft,
	Banknote,
	Landmark,
	Cpu,
	Nfc,
} from "lucide-react";

// --- CSS Variables Injection ---
const themeStyles = `
:root {
  --background: oklch(0.9777 0.0041 301.4256);
  --foreground: oklch(0.3651 0.0325 287.0807);
  --card: oklch(1.0000 0 0);
  --card-foreground: oklch(0.3651 0.0325 287.0807);
  --popover: oklch(1.0000 0 0);
  --popover-foreground: oklch(0.3651 0.0325 287.0807);
  --primary: oklch(0.6104 0.0767 299.7335);
  --primary-foreground: oklch(0.9777 0.0041 301.4256);
  --secondary: oklch(0.8957 0.0265 300.2416);
  --secondary-foreground: oklch(0.3651 0.0325 287.0807);
  --muted: oklch(0.8906 0.0139 299.7754);
  --muted-foreground: oklch(0.5288 0.0375 290.7895);
  --accent: oklch(0.7889 0.0802 359.9375);
  --accent-foreground: oklch(0.3394 0.0441 1.7583);
  --destructive: oklch(0.6332 0.1578 22.6734);
  --destructive-foreground: oklch(0.9777 0.0041 301.4256);
  --border: oklch(0.8447 0.0226 300.1421);
  --input: oklch(0.9329 0.0124 301.2783);
  --ring: oklch(0.6104 0.0767 299.7335);
  --chart-1: oklch(0.6104 0.0767 299.7335);
  --chart-2: oklch(0.7889 0.0802 359.9375);
  --chart-3: oklch(0.7321 0.0749 169.8670);
  --chart-4: oklch(0.8540 0.0882 76.8292);
  --chart-5: oklch(0.7857 0.0645 258.0839);
  --radius: 0.5rem;
}

.dark {
  --background: oklch(0.2166 0.0215 292.8474);
  --foreground: oklch(0.9053 0.0245 293.5570);
  --card: oklch(0.2544 0.0301 292.7315);
  --card-foreground: oklch(0.9053 0.0245 293.5570);
  --popover: oklch(0.2544 0.0301 292.7315);
  --popover-foreground: oklch(0.9053 0.0245 293.5570);
  --primary: oklch(0.7058 0.0777 302.0489);
  --primary-foreground: oklch(0.2166 0.0215 292.8474);
  --secondary: oklch(0.4604 0.0472 295.5578);
  --secondary-foreground: oklch(0.9053 0.0245 293.5570);
  --muted: oklch(0.2560 0.0320 294.8380);
  --muted-foreground: oklch(0.6974 0.0282 300.0614);
  --accent: oklch(0.3181 0.0321 308.6149);
  --accent-foreground: oklch(0.8391 0.0692 2.6681);
  --destructive: oklch(0.6875 0.1420 21.4566);
  --destructive-foreground: oklch(0.2166 0.0215 292.8474);
  --border: oklch(0.3063 0.0359 293.3367);
  --input: oklch(0.2847 0.0346 291.2726);
  --ring: oklch(0.7058 0.0777 302.0489);
  --chart-1: oklch(0.7058 0.0777 302.0489);
  --chart-2: oklch(0.8391 0.0692 2.6681);
  --chart-3: oklch(0.7321 0.0749 169.8670);
  --chart-4: oklch(0.8540 0.0882 76.8292);
  --chart-5: oklch(0.7857 0.0645 258.0839);
}
`;

// --- Shared Components ---

// 1. Bar Chart Component
const ExpenseChart = () => {
	const data = [
		{ label: "1", height: "60%", color: "bg-[var(--chart-1)]", text: "12%" },
		{ label: "5", height: "20%", color: "bg-[var(--chart-2)]", text: "3%" },
		{ label: "10", height: "30%", color: "bg-[var(--chart-3)]", text: "5%" },
		{ label: "15", height: "90%", color: "bg-[var(--chart-4)]", text: "32%" },
		{ label: "20", height: "70%", color: "bg-[var(--chart-5)]", text: "21%" },
		{ label: "25", height: "40%", color: "bg-[var(--chart-1)]", text: "7%" },
		{ label: "31", height: "50%", color: "bg-[var(--chart-2)]", text: "13%" },
		{ label: "1", height: "35%", color: "bg-[var(--chart-3)]", text: "5%" },
	];

	return (
		<div className="w-full h-64 flex items-end justify-between px-2 mt-6 mb-2">
			{data.map((item, index) => (
				<div
					key={index}
					className="flex flex-col items-center justify-end h-full w-8 gap-2"
				>
					<span className="text-xs font-semibold text-[var(--muted-foreground)] mb-1">
						{item.text}
					</span>
					<div
						className={`w-4 rounded-full ${item.color} transition-colors duration-300`}
						style={{
							height: item.height,
							transition: "height 0.5s ease-in-out",
						}}
					></div>
					<span className="text-xs text-[var(--muted-foreground)] mt-2">
						{item.label}
					</span>
				</div>
			))}
		</div>
	);
};

// 2. Transaction Item Component
const TransactionItem = ({ icon: Icon, title, subtitle, amount, percent }) => (
	<div className="flex items-center justify-between py-4 group cursor-pointer hover:bg-[var(--muted)] rounded-3xl px-3 transition-all">
		<div className="flex items-center gap-4">
			<div
				className={`w-12 h-12 rounded-full flex items-center justify-center bg-[var(--muted)] transition-colors duration-300`}
			>
				<Icon size={20} className="text-[var(--foreground)]" />
			</div>
			<div>
				<h3 className="font-bold text-[var(--foreground)] transition-colors duration-300">
					{title}
				</h3>
				<p className="text-sm text-[var(--muted-foreground)]">{subtitle}</p>
			</div>
		</div>
		<div className="text-right">
			<p className="font-bold text-[var(--foreground)] transition-colors duration-300">
				₹{amount}
			</p>
			{percent && (
				<p className="text-xs text-[var(--muted-foreground)]">{percent}%</p>
			)}
		</div>
	</div>
);

// 3. Stats Card Component
const StatsCard = ({ title, amount }) => (
	<div className="bg-[var(--card)] p-5 rounded-[2rem] shadow-sm border border-[var(--border)] flex flex-col items-center justify-center min-w-[30%] transition-colors duration-300 hover:scale-105 transition-transform">
		<span className="text-xs text-[var(--muted-foreground)] mb-1">{title}</span>
		<span className="text-lg font-bold text-[var(--card-foreground)] transition-colors duration-300">
			₹{amount}
		</span>
	</div>
);

// 4. Setting Item Component
const SettingItem = ({
	icon: Icon,
	title,
	value,
	type = "arrow",
	onClick,
	isToggled,
}) => (
	<button
		onClick={onClick}
		className="w-full flex items-center justify-between p-4 bg-[var(--card)] border border-[var(--border)] rounded-2xl mb-3 hover:bg-[var(--muted)] transition-all active:scale-[0.98]"
	>
		<div className="flex items-center gap-4">
			<div className="w-10 h-10 rounded-full bg-[var(--muted)] flex items-center justify-center text-[var(--primary)]">
				<Icon size={20} />
			</div>
			<div className="text-left">
				<span className="block font-medium text-[var(--foreground)]">
					{title}
				</span>
			</div>
		</div>

		<div className="flex items-center gap-2">
			{value && (
				<span className="text-sm text-[var(--muted-foreground)]">{value}</span>
			)}

			{type === "arrow" && (
				<ChevronRight size={18} className="text-[var(--muted-foreground)]" />
			)}

			{type === "toggle" && (
				<div
					className={`w-12 h-6 rounded-full p-1 transition-colors duration-300 ${
						isToggled ? "bg-[var(--primary)]" : "bg-[var(--muted-foreground)]"
					}`}
				>
					<div
						className={`w-4 h-4 rounded-full bg-white shadow-sm transform transition-transform duration-300 ${
							isToggled ? "translate-x-6" : "translate-x-0"
						}`}
					></div>
				</div>
			)}
		</div>
	</button>
);

// --- New Components for Home & Bills ---

const CircularProgress = ({
	value,
	max,
	color,
	size = 60,
	strokeWidth = 6,
}) => {
	const radius = (size - strokeWidth) / 2;
	const circumference = radius * 2 * Math.PI;
	const offset = circumference - (value / max) * circumference;

	return (
		<div
			className="relative flex items-center justify-center"
			style={{ width: size, height: size }}
		>
			<svg width={size} height={size} className="transform -rotate-90">
				<circle
					cx={size / 2}
					cy={size / 2}
					r={radius}
					fill="transparent"
					className="stroke-[var(--muted)]"
					strokeWidth={strokeWidth}
				/>
				<circle
					cx={size / 2}
					cy={size / 2}
					r={radius}
					fill="transparent"
					className={color}
					strokeWidth={strokeWidth}
					strokeDasharray={circumference}
					strokeDashoffset={offset}
					strokeLinecap="round"
				/>
			</svg>
		</div>
	);
};

const CreditCardComponent = ({
	type = "VISA",
	number = "9038 4061 **** ****",
	holder = "Tashif Ahmad Khan",
	exp = "02/02",
	gradient = "from-[var(--chart-2)] to-[var(--chart-1)]",
	icon = Wifi,
	isVirtual = false,
	isCash = false,
}) => (
	<div
		className={`relative w-full h-56 bg-gradient-to-br ${gradient} rounded-[2rem] p-6 shadow-md flex flex-col justify-between overflow-hidden mb-6 ${
			isVirtual ? "border-2 border-white/30" : ""
		}`}
	>
		{!isCash && (
			<div className="absolute top-0 right-0 w-32 h-32 bg-white/20 rounded-full -mr-10 -mt-10 blur-2xl"></div>
		)}
		<div className="flex justify-between items-start z-10">
			<div className="flex flex-col">
				<div className="text-white font-bold text-lg tracking-wider opacity-90">
					{type}
				</div>
				{isVirtual && (
					<div className="text-white/70 text-xs font-medium">Virtual Card</div>
				)}
			</div>
			{/* Icon Component */}
			<div className="flex items-center gap-2">
				{isVirtual && <Cpu size={20} className="text-white opacity-80" />}
				{React.createElement(icon, {
					size: 24,
					className: "text-white opacity-80",
				})}
			</div>
		</div>
		<div className="z-10">
			{isCash ? (
				<>
					<div className="text-xs text-white/70 uppercase mb-1 font-medium">
						Total Cash
					</div>
					<div className="text-3xl font-bold text-white tracking-widest mb-4 shadow-sm">
						₹4,500.00
					</div>
				</>
			) : (
				<div className="text-2xl font-bold text-white tracking-widest mb-4 shadow-sm">
					{number}
				</div>
			)}

			<div className="flex justify-between items-end">
				<div>
					<div className="text-xs text-white/70 uppercase mb-1 font-medium">
						Card Holder
					</div>
					<div className="font-semibold text-white tracking-wide">{holder}</div>
				</div>
				<div className="text-right">
					<div className="text-xs text-white/70 uppercase mb-1 font-medium">
						Exp Date
					</div>
					<div className="font-semibold text-white tracking-wide">{exp}</div>
				</div>
			</div>
		</div>
		{(type === "VISA" || type === "Mastercard") && !isVirtual && (
			<>
				<div className="absolute bottom-6 right-6 z-10 flex items-center gap-2">
					<Nfc size={20} className="text-white/70" />
					<div className="w-12 h-8 bg-white/20 backdrop-blur-sm rounded flex overflow-hidden border border-white/10">
						<div className="w-1/2 h-full border-r border-white/20"></div>
					</div>
				</div>
			</>
		)}
		{isCash && (
			<div className="absolute bottom-4 right-6 z-10 opacity-20">
				<Banknote size={64} className="text-white" />
			</div>
		)}
	</div>
);

// --- Main App Component ---

export default function App() {
	const [currentView, setCurrentView] = useState("home"); // home, stats, wallets, addExpense, profile
	const [amount, setAmount] = useState("25.00");
	const [isDarkMode, setIsDarkMode] = useState(false);
	const [notifications, setNotifications] = useState(true);
	const [currency, setCurrency] = useState("INR");
	const [language, setLanguage] = useState("English");
	const [activeWalletTab, setActiveWalletTab] = useState("Cards"); // Cards, Virtual, Cash

	const [transactions, setTransactions] = useState([
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

	// --- Views ---

	// 1. Home View (Updated with Balance Card & Spending Analysis)
	const HomeView = () => (
		<div className="flex flex-col h-full bg-[var(--background)] overflow-y-auto hide-scrollbar transition-colors duration-300">
			{/* Header */}
			<header className="flex justify-between items-center p-6 bg-transparent">
				<div className="flex flex-col">
					<h1 className="text-3xl font-bold text-[var(--foreground)]">
						Hi, There
					</h1>
				</div>
				<div className="flex gap-3">
					<button
						onClick={toggleTheme}
						className="p-2 text-[var(--foreground)] hover:bg-[var(--muted)] rounded-full transition-colors"
					>
						{isDarkMode ? <Sun size={20} /> : <Moon size={20} />}
					</button>
					<button className="p-2 relative bg-[var(--card)] rounded-full shadow-sm text-[var(--foreground)] border border-[var(--border)]">
						<Bell size={20} />
						<span className="absolute top-1.5 right-2 w-2 h-2 bg-[var(--destructive)] rounded-full"></span>
					</button>
				</div>
			</header>

			{/* Total Balance Card */}
			<div className="px-6 mb-8">
				<div className="bg-[var(--primary)] text-[var(--primary-foreground)] p-6 rounded-[2rem] shadow-lg relative overflow-hidden">
					{/* Abstract blobs for visual interest */}
					<div className="absolute -top-10 -right-10 w-32 h-32 bg-white/20 rounded-full blur-2xl"></div>
					<div className="absolute bottom-0 left-0 w-24 h-24 bg-black/10 rounded-full blur-2xl"></div>

					<div className="relative z-10">
						<div className="text-sm font-medium opacity-90 mb-1">
							Total Balance
						</div>
						<div className="text-4xl font-bold mb-8">₹32,500.00</div>

						<div className="flex gap-4">
							<div className="flex-1 bg-black/20 rounded-2xl p-3 backdrop-blur-sm">
								<div className="flex items-center gap-1 text-xs opacity-90 mb-1">
									<div className="w-5 h-5 rounded-full bg-white/20 flex items-center justify-center">
										<ArrowDownLeft size={12} />
									</div>
									Income
								</div>
								<div className="font-semibold text-lg">₹4,200</div>
							</div>
							<div className="flex-1 bg-white/20 rounded-2xl p-3 backdrop-blur-sm">
								<div className="flex items-center gap-1 text-xs opacity-90 mb-1">
									<div className="w-5 h-5 rounded-full bg-black/10 flex items-center justify-center">
										<ArrowUpRight size={12} />
									</div>
									Expense
								</div>
								<div className="font-semibold text-lg">₹1,612</div>
							</div>
						</div>
					</div>
				</div>
			</div>

			{/* Spending Analysis Title & List */}
			<div className="px-6 pb-24">
				<div className="flex justify-between items-center mb-4">
					<h2 className="text-xl font-bold text-[var(--foreground)]">
						Spending Analysis
					</h2>
					<button className="text-sm font-medium text-[var(--muted-foreground)] bg-[var(--card)] border border-[var(--border)] px-3 py-1 rounded-lg shadow-sm">
						See All
					</button>
				</div>

				<div className="space-y-1">
					{transactions.map((t) => (
						<TransactionItem
							key={t.id}
							icon={t.icon}
							title={t.title}
							subtitle={t.subtitle}
							amount={t.amount}
							percent={t.percent}
						/>
					))}
				</div>
			</div>
		</div>
	);

	// 2. Stats View (Detailed Analysis + Bills Stuff)
	const StatsView = () => (
		<div className="flex flex-col h-full bg-[var(--background)] pb-24 overflow-y-auto hide-scrollbar transition-colors duration-300">
			<header className="flex justify-between items-start p-6 bg-[var(--background)] pb-4 transition-colors duration-300">
				<div className="flex flex-col">
					<h1 className="text-2xl font-bold text-[var(--foreground)] transition-colors duration-300">
						Analysis
					</h1>
					<p className="text-sm text-[var(--muted-foreground)]">
						Detailed Breakdown
					</p>
				</div>
				<div className="flex items-center gap-2">
					<button className="flex items-center gap-1 px-4 py-2 bg-[var(--card)] rounded-full text-sm font-medium text-[var(--foreground)] border border-[var(--border)] transition-colors duration-300 shadow-sm">
						June <ChevronDown size={14} />
					</button>
				</div>
			</header>

			{/* Chart Section */}
			<div className="px-6 bg-[var(--card)] mx-6 p-4 rounded-[2.5rem] shadow-sm border border-[var(--border)] z-10 transition-colors duration-300 mb-6">
				<ExpenseChart />
			</div>

			{/* Stats Cards */}
			<div className="px-6 mb-8">
				<div className="flex justify-between gap-3">
					<StatsCard title="Day" amount="52" />
					<StatsCard title="Week" amount="403" />
					<StatsCard title="Month" amount="1,612" />
				</div>
			</div>

			{/* Bills / Due Section (Integrated here) */}
			<div className="px-6">
				<h2 className="text-xl font-bold text-[var(--foreground)] mb-4">
					Bills & Payments
				</h2>

				{/* Insight Card */}
				<div className="bg-[var(--card)] p-4 rounded-[2rem] shadow-sm flex items-center justify-between border border-[var(--border)] mb-6">
					<div className="flex items-center gap-4">
						<div className="w-12 h-12 rounded-full bg-[var(--muted)] flex items-center justify-center text-[var(--primary)] font-bold">
							<ArrowUpRight size={20} />
						</div>
						<div>
							<p className="text-sm text-[var(--muted-foreground)]">
								You paid{" "}
								<span className="font-bold text-[var(--foreground)]">
									₹50 more
								</span>{" "}
								on
								<br />
								your cell phone bill
							</p>
						</div>
					</div>
					<button className="px-3 py-1.5 bg-[var(--muted)] text-[var(--muted-foreground)] text-xs font-bold rounded-lg">
						Check
					</button>
				</div>

				{/* Amount Paid Circle */}
				<div className="bg-[var(--card)] p-6 rounded-[2.5rem] shadow-sm border border-[var(--border)] flex items-center gap-6 mb-6">
					<CircularProgress
						value={75}
						max={100}
						size={80}
						color="stroke-[var(--chart-4)]"
						strokeWidth={8}
					/>
					<div>
						<div className="text-[var(--muted-foreground)] text-sm mb-1">
							Total Paid
						</div>
						<div className="text-2xl font-bold text-[var(--foreground)]">
							₹883
						</div>
						<div className="text-[var(--muted-foreground)] text-xs mt-1">
							of ₹2,340 bills
						</div>
					</div>
				</div>

				{/* Upcoming Dues */}
				<h3 className="text-sm font-semibold text-[var(--muted-foreground)] uppercase tracking-wider mb-2 ml-2">
					Upcoming Dues
				</h3>
				<div className="space-y-1">
					<TransactionItem
						icon={Home}
						title="Home Rent"
						subtitle="Due date: Mar 25"
						amount="339.30"
					/>
					<TransactionItem
						icon={Smartphone}
						title="Mobile Bill"
						subtitle="Due date: Mar 28"
						amount="55.00"
					/>
				</div>
			</div>
		</div>
	);

	// 3. Wallets View (Formerly Bills)
	const WalletsView = () => (
		<div className="flex flex-col h-full bg-[var(--background)] pb-24 overflow-y-auto hide-scrollbar transition-colors duration-300">
			<header className="flex justify-between items-center p-6">
				<div className="flex flex-col">
					<h1 className="text-3xl font-bold text-[var(--foreground)]">
						My Wallets
					</h1>
					<p className="text-[var(--muted-foreground)] text-sm">
						Manage your cards & cash
					</p>
				</div>
				<button className="p-2 bg-[var(--card)] border border-[var(--border)] rounded-full text-[var(--foreground)] shadow-sm">
					<MoreHorizontal size={20} />
				</button>
			</header>

			{/* Tabs */}
			<div className="px-6 mb-6">
				<div className="flex justify-between items-center bg-[var(--muted)] rounded-[2rem] p-1 text-sm font-medium">
					{["Cards", "Virtual", "Cash"].map((tab) => (
						<button
							key={tab}
							onClick={() => setActiveWalletTab(tab)}
							className={`flex-1 py-3 rounded-[2rem] transition-all ${
								activeWalletTab === tab
									? "bg-[var(--primary)] text-[var(--primary-foreground)] shadow-md"
									: "text-[var(--muted-foreground)]"
							}`}
						>
							{tab}
						</button>
					))}
				</div>
			</div>

			{/* Cards List */}
			<div className="px-6">
				{activeWalletTab === "Cards" && (
					<>
						<h3 className="text-sm font-semibold text-[var(--muted-foreground)] uppercase tracking-wider mb-4 ml-2">
							Physical Cards
						</h3>
						<CreditCardComponent
							type="VISA"
							number="9038 4061 **** ****"
							holder="Tashif Ahmad Khan"
							exp="02/28"
							gradient="from-[var(--chart-2)] to-[var(--chart-1)]"
							icon={Wifi}
						/>
						<CreditCardComponent
							type="Mastercard"
							number="5500 1234 **** ****"
							holder="Tashif Ahmad Khan"
							exp="11/26"
							gradient="from-[var(--chart-3)] to-[var(--chart-5)]"
							icon={CreditCardIcon}
						/>
					</>
				)}

				{activeWalletTab === "Virtual" && (
					<>
						<h3 className="text-sm font-semibold text-[var(--muted-foreground)] uppercase tracking-wider mb-4 ml-2">
							Virtual Cards
						</h3>
						<CreditCardComponent
							type="VISA Platinum"
							number="4111 1234 **** ****"
							holder="Tashif Ahmad Khan"
							exp="09/29"
							gradient="from-gray-800 to-gray-900"
							icon={Cpu}
							isVirtual={true}
						/>
					</>
				)}

				{activeWalletTab === "Cash" && (
					<>
						<h3 className="text-sm font-semibold text-[var(--muted-foreground)] uppercase tracking-wider mb-4 ml-2">
							Cash on Hand
						</h3>
						<CreditCardComponent
							type="Cash Wallet"
							number="Physical Cash"
							holder="Tashif Ahmad Khan"
							exp="--"
							gradient="from-green-600 to-teal-700"
							icon={Banknote}
							isCash={true}
						/>
					</>
				)}

				{/* Add New Card Button */}
				<button className="w-full py-4 border-2 border-dashed border-[var(--border)] rounded-[2rem] text-[var(--muted-foreground)] font-medium hover:bg-[var(--muted)] transition-colors flex items-center justify-center gap-2 mt-4">
					<div className="w-6 h-6 rounded-full bg-[var(--primary)] text-[var(--primary-foreground)] flex items-center justify-center text-lg leading-none pb-1">
						+
					</div>
					Add New {activeWalletTab === "Cash" ? "Entry" : "Card"}
				</button>
			</div>
		</div>
	);

	// 4. Add Expense View
	const AddExpenseView = () => (
		<div className="flex flex-col h-full bg-[var(--background)] transition-colors duration-300">
			<header className="flex justify-between items-start p-6">
				<button
					onClick={() => setCurrentView("stats")}
					className="p-2 text-[var(--foreground)]"
				>
					<X size={24} />
				</button>
				<div className="flex flex-col items-center opacity-50">
					<h1 className="text-sm font-bold text-[var(--foreground)] transition-colors duration-300">
						₹32,500.00
					</h1>
					<div className="text-xs text-[var(--muted-foreground)]">
						Total Balance
					</div>
				</div>
				<div className="flex items-center gap-1">
					<button
						onClick={toggleTheme}
						className="p-2 text-[var(--foreground)] hover:bg-[var(--muted)] rounded-full transition-colors"
					>
						{isDarkMode ? <Sun size={20} /> : <Moon size={20} />}
					</button>
					<button className="p-2 text-[var(--foreground)]">
						<Edit3 size={20} />
					</button>
				</div>
			</header>

			<div className="flex-1 flex flex-col items-center px-8 pt-4">
				<div className="flex gap-4 w-full justify-between mb-8">
					<button className="flex-1 flex items-center justify-between bg-[var(--muted)] border border-transparent px-4 py-3 rounded-2xl text-[var(--foreground)] font-medium transition-colors duration-300">
						<div className="flex items-center gap-2">
							<Wallet size={18} /> Cash
						</div>
						<ChevronDown size={16} />
					</button>
					<button className="flex-1 flex items-center justify-between bg-[var(--muted)] border border-transparent px-4 py-3 rounded-2xl text-[var(--foreground)] font-medium transition-colors duration-300">
						<div className="flex items-center gap-2">
							<Shirt size={18} /> Shopping
						</div>
						<ChevronDown size={16} />
					</button>
				</div>

				<div className="flex flex-col items-center justify-center flex-1 w-full mb-8">
					<span className="text-[var(--muted-foreground)] text-sm mb-2">
						Expenses
					</span>
					<div className="flex items-center text-6xl font-bold text-[var(--foreground)] tracking-tight transition-colors duration-300">
						<span className="text-[var(--muted-foreground)] text-4xl mr-1">
							₹
						</span>
						{amount}
						<span className="animate-pulse w-0.5 h-12 bg-[var(--foreground)] ml-1"></span>
					</div>
					<input
						type="text"
						placeholder="Add comment..."
						className="mt-6 text-center w-full outline-none bg-transparent text-[var(--muted-foreground)] placeholder-[var(--muted-foreground)] font-medium transition-colors duration-300"
					/>
				</div>
			</div>

			<div className="bg-[var(--card)] rounded-t-[3rem] p-8 pb-10 shadow-[0_-10px_40px_rgba(0,0,0,0.05)] border-t border-[var(--border)] transition-colors duration-300">
				<div className="grid grid-cols-4 gap-4 h-80">
					{[1, 2, 3].map((num) => (
						<button
							key={num}
							onClick={() => handleKeyPress(num.toString())}
							className="text-2xl font-medium text-[var(--foreground)] rounded-full hover:bg-[var(--muted)] active:scale-95 transition-all"
						>
							{num}
						</button>
					))}
					<button
						onClick={() => handleKeyPress("backspace")}
						className="flex items-center justify-center bg-[var(--destructive)] text-[var(--destructive-foreground)] rounded-full hover:opacity-90 active:scale-95 transition-all"
					>
						<Delete size={24} />
					</button>

					{[4, 5, 6].map((num) => (
						<button
							key={num}
							onClick={() => handleKeyPress(num.toString())}
							className="text-2xl font-medium text-[var(--foreground)] rounded-full hover:bg-[var(--muted)] active:scale-95 transition-all"
						>
							{num}
						</button>
					))}
					<button className="flex items-center justify-center bg-[var(--muted)] text-[var(--primary)] rounded-full hover:bg-[var(--muted)]/80 active:scale-95 transition-all">
						<Calendar size={24} />
					</button>

					{[7, 8, 9].map((num) => (
						<button
							key={num}
							onClick={() => handleKeyPress(num.toString())}
							className="text-2xl font-medium text-[var(--foreground)] rounded-full hover:bg-[var(--muted)] active:scale-95 transition-all"
						>
							{num}
						</button>
					))}

					<button
						onClick={() => handleKeyPress("check")}
						className="row-span-2 flex items-center justify-center bg-[var(--primary)] text-[var(--primary-foreground)] rounded-[2rem] shadow-xl hover:opacity-90 active:scale-95 transition-all"
					>
						<Check size={32} />
					</button>

					<button className="text-2xl font-medium text-[var(--foreground)] bg-[var(--muted)] rounded-full hover:bg-[var(--muted)]/80 active:scale-95 transition-all">
						₹
					</button>
					<button
						onClick={() => handleKeyPress("0")}
						className="text-2xl font-medium text-[var(--foreground)] rounded-full hover:bg-[var(--muted)] active:scale-95 transition-all"
					>
						0
					</button>
					<button
						onClick={() => handleKeyPress(".")}
						className="text-2xl font-medium text-[var(--foreground)] rounded-full hover:bg-[var(--muted)] active:scale-95 transition-all"
					>
						,
					</button>
				</div>
			</div>
		</div>
	);

	// 5. Profile View
	const ProfileView = () => (
		<div className="flex flex-col h-full bg-[var(--background)] pb-24 overflow-y-auto hide-scrollbar transition-colors duration-300">
			<header className="flex justify-between items-center p-6 bg-transparent">
				<button
					onClick={() => setCurrentView("home")}
					className="p-2 bg-[var(--card)] border border-[var(--border)] rounded-full text-[var(--foreground)] shadow-sm"
				>
					<ArrowLeft size={20} />
				</button>
				<h1 className="text-xl font-bold text-[var(--foreground)]">Profile</h1>
				<div className="w-10"></div> {/* Spacer for centering */}
			</header>

			{/* Avatar Section */}
			<div className="flex flex-col items-center justify-center mb-8">
				<div className="w-28 h-28 rounded-full bg-[var(--chart-2)] p-1 mb-4 shadow-lg">
					<div className="w-full h-full rounded-full bg-[var(--card)] flex items-center justify-center overflow-hidden">
						<User size={48} className="text-[var(--foreground)] opacity-50" />
					</div>
				</div>
				<h2 className="text-2xl font-bold text-[var(--foreground)]">
					Tashif Ahmad Khan
				</h2>
				<p className="text-[var(--muted-foreground)]">admin@tashif.codes</p>
			</div>

			{/* Settings List */}
			<div className="px-6 space-y-8">
				{/* General Section */}
				<div>
					<h3 className="text-sm font-semibold text-[var(--muted-foreground)] uppercase tracking-wider mb-4 ml-1">
						General
					</h3>
					<SettingItem
						icon={Languages}
						title="Language"
						value={language}
						onClick={() =>
							setLanguage(language === "English" ? "Hindi" : "English")
						}
					/>
					<SettingItem
						icon={DollarSign}
						title="Currency"
						value={currency}
						onClick={() => setCurrency(currency === "INR" ? "USD" : "INR")}
					/>
					<SettingItem
						icon={Moon}
						title="Dark Mode"
						type="toggle"
						isToggled={isDarkMode}
						onClick={toggleTheme}
					/>
				</div>

				{/* Notifications Section */}
				<div>
					<h3 className="text-sm font-semibold text-[var(--muted-foreground)] uppercase tracking-wider mb-4 ml-1">
						Notifications
					</h3>
					<SettingItem
						icon={Bell}
						title="Push Notifications"
						type="toggle"
						isToggled={notifications}
						onClick={() => setNotifications(!notifications)}
					/>
					<SettingItem icon={Shield} title="Security Alerts" type="arrow" />
				</div>

				{/* Support Section */}
				<div>
					<h3 className="text-sm font-semibold text-[var(--muted-foreground)] uppercase tracking-wider mb-4 ml-1">
						Support
					</h3>
					<SettingItem icon={CircleHelp} title="Help & Support" />
					<button className="w-full flex items-center justify-start p-4 gap-4 bg-[var(--destructive)]/10 text-[var(--destructive)] rounded-2xl mt-4 hover:bg-[var(--destructive)]/20 transition-all">
						<div className="w-10 h-10 rounded-full bg-[var(--destructive)]/20 flex items-center justify-center">
							<LogOut size={20} />
						</div>
						<span className="font-bold">Log Out</span>
					</button>
				</div>
			</div>
		</div>
	);

	const BottomNav = () => (
		<div className="absolute bottom-0 left-0 right-0 bg-[var(--card)] border-t border-[var(--border)] px-8 py-5 flex justify-between items-center rounded-t-[3rem] shadow-[0_-5px_20px_rgba(0,0,0,0.03)] z-50 transition-colors duration-300">
			<button
				onClick={() => setCurrentView("home")}
				className={`${
					currentView === "home"
						? "text-[var(--primary)]"
						: "text-[var(--muted-foreground)]"
				} hover:text-[var(--foreground)] transition-colors`}
			>
				<Home size={24} />
			</button>
			<button
				onClick={() => setCurrentView("stats")}
				className={`${
					currentView === "stats"
						? "text-[var(--primary)]"
						: "text-[var(--muted-foreground)]"
				} hover:text-[var(--foreground)] transition-colors`}
			>
				<LayoutGrid size={24} />
			</button>

			{/* Floating Action Button for Add Expense */}
			<button
				onClick={() => setCurrentView("addExpense")}
				className="bg-[var(--primary)] text-[var(--primary-foreground)] p-4 rounded-full shadow-lg -mt-10 border-4 border-[var(--background)] hover:scale-105 transition-all duration-300 flex items-center justify-center"
			>
				<Plus size={24} /> {/* Replaced BarChart2 with Plus icon */}
			</button>

			<button
				onClick={() => setCurrentView("wallets")}
				className={`${
					currentView === "wallets"
						? "text-[var(--primary)]"
						: "text-[var(--muted-foreground)]"
				} hover:text-[var(--foreground)] transition-colors`}
			>
				<Wallet size={24} />
			</button>
			<button
				onClick={() => setCurrentView("profile")}
				className={`${
					currentView === "profile"
						? "text-[var(--primary)]"
						: "text-[var(--muted-foreground)]"
				} hover:text-[var(--foreground)] transition-colors`}
			>
				<User size={24} />
			</button>
		</div>
	);

	return (
		<>
			<style>{themeStyles}</style>
			<div
				className={`${
					isDarkMode ? "dark" : ""
				} flex justify-center items-center min-h-screen bg-gray-200 dark:bg-neutral-950 font-sans transition-colors duration-500`}
			>
				<div className="relative w-full max-w-md h-[850px] bg-[var(--background)] shadow-2xl overflow-hidden rounded-[3rem] ring-8 ring-gray-900 dark:ring-neutral-900 transition-colors duration-300">
					{currentView === "addExpense" ? (
						<AddExpenseView />
					) : (
						<>
							{currentView === "home" && <HomeView />}
							{currentView === "stats" && <StatsView />}
							{currentView === "wallets" && <WalletsView />}
							{currentView === "profile" && <ProfileView />}
							<BottomNav />
						</>
					)}
				</div>
			</div>
		</>
	);
}
