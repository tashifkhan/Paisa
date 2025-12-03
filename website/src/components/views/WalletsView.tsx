import {
	MoreHorizontal,
	Wifi,
	Cpu,
	Banknote,
	CreditCard as CreditCardIcon,
} from "lucide-react";
import { CreditCardComponent } from "../shared/CreditCardComponent";

interface WalletsViewProps {
	activeWalletTab: string;
	setActiveWalletTab: (tab: string) => void;
}

export const WalletsView = ({
	activeWalletTab,
	setActiveWalletTab,
}: WalletsViewProps) => (
	<div className="flex flex-col h-full bg-(--background) pb-24 md:pb-6 overflow-y-auto hide-scrollbar transition-colors duration-300">
		<div className="max-w-5xl mx-auto w-full">
			<header className="flex justify-between items-center p-6">
				<div className="flex flex-col">
					<h1 className="text-3xl font-bold text-(--foreground)">My Wallets</h1>
					<p className="text-(--muted-foreground) text-sm">
						Manage your cards & cash
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

			{/* Cards List */}
			<div className="px-6">
				{activeWalletTab === "Cards" && (
					<>
						<h3 className="text-sm font-semibold text-(--muted-foreground) uppercase tracking-wider mb-4 ml-2">
							Physical Cards
						</h3>
						<div className="md:grid md:grid-cols-2 md:gap-6">
							<CreditCardComponent
								type="VISA"
								number="9038 4061 **** ****"
								holder="Tashif Ahmad Khan"
								exp="02/28"
								gradient="from-(--chart-2) to-(--chart-1)"
								icon={Wifi}
							/>
							<CreditCardComponent
								type="Mastercard"
								number="5500 1234 **** ****"
								holder="Tashif Ahmad Khan"
								exp="11/26"
								gradient="from-(--chart-3) to-(--chart-5)"
								icon={CreditCardIcon}
							/>
						</div>
					</>
				)}

				{activeWalletTab === "Virtual" && (
					<>
						<h3 className="text-sm font-semibold text-(--muted-foreground) uppercase tracking-wider mb-4 ml-2">
							Virtual Cards
						</h3>
						<div className="md:grid md:grid-cols-2 md:gap-6">
							<CreditCardComponent
								type="VISA Platinum"
								number="4111 1234 **** ****"
								holder="Tashif Ahmad Khan"
								exp="09/29"
								gradient="from-gray-800 to-gray-900"
								icon={Cpu}
								isVirtual={true}
							/>
						</div>
					</>
				)}

				{activeWalletTab === "Cash" && (
					<>
						<h3 className="text-sm font-semibold text-(--muted-foreground) uppercase tracking-wider mb-4 ml-2">
							Cash on Hand
						</h3>
						<div className="md:grid md:grid-cols-2 md:gap-6">
							<CreditCardComponent
								type="Cash Wallet"
								number="Physical Cash"
								holder="Tashif Ahmad Khan"
								exp="--"
								gradient="from-green-600 to-teal-700"
								icon={Banknote}
								isCash={true}
							/>
						</div>
					</>
				)}

				{/* Add New Card Button */}
				<button className="w-full py-4 border-2 border-dashed border-(--border) rounded-4xl text-(--muted-foreground) font-medium hover:bg-(--muted) transition-colors flex items-center justify-center gap-2 mt-4">
					<div className="w-6 h-6 rounded-full bg-(--primary) text-(--primary-foreground) flex items-center justify-center text-lg leading-none pb-1">
						+
					</div>
					Add New {activeWalletTab === "Cash" ? "Entry" : "Card"}
				</button>
			</div>
		</div>
	</div>
);
