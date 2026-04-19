import {
	Banknote,
	Cpu,
	Loader2,
	MoreHorizontal,
	Plus,
	Wifi,
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import { useWalletTotal, useWallets } from "@/hooks/useWallets";
import { CreditCardComponent } from "../shared/CreditCardComponent";

interface WalletsViewProps {
	activeWalletTab: string;
	setActiveWalletTab: (tab: string) => void;
}

// Helper to get gradient based on wallet type
const getWalletGradient = (type: string, index: number): string => {
	const gradients = [
		"from-(--chart-2) to-(--chart-1)",
		"from-(--chart-3) to-(--chart-5)",
		"from-blue-600 to-indigo-700",
		"from-purple-600 to-pink-700",
	];

	if (type === "cash") return "from-green-600 to-teal-700";
	if (type === "virtual") return "from-gray-800 to-gray-900";
	return gradients[index % gradients.length];
};

// Helper to get icon based on wallet type
const getWalletIcon = (type: string) => {
	switch (type?.toLowerCase()) {
		case "cash":
			return Banknote;
		case "virtual":
			return Cpu;
		default:
			return Wifi;
	}
};

export const WalletsView = ({
	activeWalletTab,
	setActiveWalletTab,
}: WalletsViewProps) => {
	const navigate = useNavigate();
	const { data: wallets = [], isLoading: walletsLoading } = useWallets();
	const { data: totalData, isLoading: totalLoading } = useWalletTotal("INR");
	const loading = walletsLoading || totalLoading;
	const totalBalance = totalData?.total_balance ?? 0;

	const formatCurrency = (amount: number) => {
		return new Intl.NumberFormat("en-IN", {
			style: "currency",
			currency: "INR",
			minimumFractionDigits: 0,
			maximumFractionDigits: 0,
		}).format(amount);
	};

	// Filter wallets by type
	const filteredWallets = wallets.filter((wallet) => {
		const type = wallet.type?.toLowerCase() || "personal";
		if (activeWalletTab === "Cards") {
			return type !== "cash" && type !== "virtual";
		}
		if (activeWalletTab === "Virtual") {
			return type === "virtual";
		}
		if (activeWalletTab === "Cash") {
			return type === "cash";
		}
		return true;
	});

	return (
		<div className="flex flex-col h-full bg-(--background) pb-24 md:pb-6 overflow-y-auto hide-scrollbar transition-colors duration-300">
			<div className="max-w-5xl mx-auto w-full">
				<header className="flex justify-between items-center p-6">
					<div className="flex flex-col">
						<h1 className="text-3xl font-bold text-(--foreground)">
							My Wallets
						</h1>
						<p className="text-(--muted-foreground) text-sm">
							Total: {formatCurrency(totalBalance)}
						</p>
					</div>
					<button className="p-2 bg-(--card) border border-(--border) rounded-full text-(--foreground) shadow-sm">
						<MoreHorizontal size={20} />
					</button>
				</header>

				{/* Tabs */}
				<div className="px-6 mb-6">
					<div className="flex justify-between items-center bg-(--muted) rounded-4xl p-1 text-sm font-medium">
						{["Cards", "Virtual", "Cash"].map((tab) => (
							<button
								key={tab}
								onClick={() => setActiveWalletTab(tab)}
								className={`flex-1 py-3 rounded-4xl transition-all ${
									activeWalletTab === tab
										? "bg-(--primary) text-(--primary-foreground) shadow-md"
										: "text-(--muted-foreground)"
								}`}
							>
								{tab}
							</button>
						))}
					</div>
				</div>

				{/* Wallets List */}
				<div className="px-6">
					{loading ? (
						<div className="flex items-center justify-center py-12">
							<Loader2
								className="animate-spin text-(--muted-foreground)"
								size={32}
							/>
						</div>
					) : (
						<>
							<h3 className="text-sm font-semibold text-(--muted-foreground) uppercase tracking-wider mb-4 ml-2">
								{activeWalletTab === "Cards" && "Physical Cards"}
								{activeWalletTab === "Virtual" && "Virtual Cards"}
								{activeWalletTab === "Cash" && "Cash on Hand"}
							</h3>

							{filteredWallets.length > 0 ? (
								<div className="md:grid md:grid-cols-2 md:gap-6">
									{filteredWallets.map((wallet, index) => (
										<CreditCardComponent
											key={wallet.id}
											type={wallet.name}
											number={`Balance: ${formatCurrency(wallet.balance)}`}
											holder={wallet.type || "Personal"}
											exp={wallet.currency}
											gradient={getWalletGradient(wallet.type || "", index)}
											icon={getWalletIcon(wallet.type || "")}
											isCash={wallet.type?.toLowerCase() === "cash"}
											isVirtual={wallet.type?.toLowerCase() === "virtual"}
										/>
									))}
								</div>
							) : (
								<div className="text-center py-12 text-(--muted-foreground)">
									<p>No {activeWalletTab.toLowerCase()} wallets yet</p>
								</div>
							)}
						</>
					)}

					{/* Add New Card Button */}
					<button
						onClick={() => navigate("/add-wallet")}
						className="w-full py-4 border-2 border-dashed border-(--border) rounded-4xl text-(--muted-foreground) font-medium hover:bg-(--muted) transition-colors flex items-center justify-center gap-2 mt-4"
					>
						<div className="w-6 h-6 rounded-full bg-(--primary) text-(--primary-foreground) flex items-center justify-center">
							<Plus size={16} />
						</div>
						Add New {activeWalletTab === "Cash" ? "Entry" : "Wallet"}
					</button>
				</div>
			</div>
		</div>
	);
};
