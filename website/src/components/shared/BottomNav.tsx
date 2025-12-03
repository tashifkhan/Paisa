import { Home, LayoutGrid, Plus, Handshake, User } from "lucide-react";
import { useNavigate, useLocation } from "react-router-dom";

export const BottomNav = () => {
	const navigate = useNavigate();
	const location = useLocation();
	const pathname = location.pathname.slice(1) || "home";

	return (
		<div className="absolute bottom-0 left-0 right-0 bg-(--card) border-t border-(--border) px-8 py-5 flex justify-between items-center rounded-t-4xl shadow-[0_-5px_20px_rgba(0,0,0,0.03) z-50 transition-colors duration-300">
			<button
				onClick={() => navigate("/")}
				className={`${
					pathname === "home" || pathname === ""
						? "text-(--primary)"
						: "text-(--muted-foreground)"
				} hover:text-(--foreground) transition-colors`}
			>
				<Home size={24} />
			</button>
			<button
				onClick={() => navigate("/stats")}
				className={`${
					pathname === "stats"
						? "text-(--primary)"
						: "text-(--muted-foreground)"
				} hover:text-(--foreground) transition-colors`}
			>
				<LayoutGrid size={24} />
			</button>

			{/* Floating Action Button for Add Expense */}
			<button
				onClick={() => navigate("/add-expense")}
				className="bg-(--primary) text-(--primary-foreground) p-4 rounded-full shadow-lg -mt-10 border-4 border-(--background) hover:scale-105 transition-all duration-300 flex items-center justify-center"
			>
				<Plus size={24} />
			</button>

			<button
				onClick={() => navigate("/debts")}
				className={`${
					pathname === "debts" ||
					pathname === "create-group" ||
					pathname === "group-detail"
						? "text-(--primary)"
						: "text-(--muted-foreground)"
				} hover:text-(--foreground) transition-colors`}
			>
				<Handshake size={24} />
			</button>
			<button
				onClick={() => navigate("/profile")}
				className={`${
					pathname === "profile"
						? "text-(--primary)"
						: "text-(--muted-foreground)"
				} hover:text-(--foreground) transition-colors`}
			>
				<User size={24} />
			</button>
		</div>
	);
};
