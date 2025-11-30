import { Home, LayoutGrid, Plus, Wallet, User } from "lucide-react";

interface BottomNavProps {
	currentView: string;
	setCurrentView: (view: string) => void;
}

export const BottomNav = ({ currentView, setCurrentView }: BottomNavProps) => (
	<div className="absolute bottom-0 left-0 right-0 bg-(--card) border-t border-(--border) px-8 py-5 flex justify-between items-center rounded-t-[3rem] shadow-[0_-5px_20px_rgba(0,0,0,0.03) z-50 transition-colors duration-300">
		<button
			onClick={() => setCurrentView("home")}
			className={`${
				currentView === "home"
					? "text-(--primary)"
					: "text-(--muted-foreground)"
			} hover:text-(--foreground) transition-colors`}
		>
			<Home size={24} />
		</button>
		<button
			onClick={() => setCurrentView("stats")}
			className={`${
				currentView === "stats"
					? "text-(--primary)"
					: "text-(--muted-foreground)"
			} hover:text-(--foreground) transition-colors`}
		>
			<LayoutGrid size={24} />
		</button>

		{/* Floating Action Button for Add Expense */}
		<button
			onClick={() => setCurrentView("addExpense")}
			className="bg-(--primary) text-(--primary-foreground) p-4 rounded-full shadow-lg -mt-10 border-4 border-(--background) hover:scale-105 transition-all duration-300 flex items-center justify-center"
		>
			<Plus size={24} />
		</button>

		<button
			onClick={() => setCurrentView("wallets")}
			className={`${
				currentView === "wallets"
					? "text-(--primary)"
					: "text-(--muted-foreground)"
			} hover:text-(--foreground) transition-colors`}
		>
			<Wallet size={24} />
		</button>
		<button
			onClick={() => setCurrentView("profile")}
			className={`${
				currentView === "profile"
					? "text-(--primary)"
					: "text-(--muted-foreground)"
			} hover:text-(--foreground) transition-colors`}
		>
			<User size={24} />
		</button>
	</div>
);
