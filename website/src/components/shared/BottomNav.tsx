import { Handshake, Home, LayoutGrid, Plus, User } from "lucide-react";
import { useLocation, useNavigate } from "react-router-dom";

export const BottomNav = () => {
	const navigate = useNavigate();
	const location = useLocation();
	const pathname = location.pathname.slice(1) || "home";

	return (
		<div className="fixed bottom-6 left-1/2 -translate-x-1/2 bg-(--card)/80 backdrop-blur-xl border border-(--border)/50 px-8 py-4 flex justify-between items-center rounded-2xl shadow-2xl z-50 transition-all duration-300 md:hidden w-[90%] max-w-md">
			<button
				onClick={() => navigate("/")}
				className={`${
					pathname === "home" || pathname === ""
						? "text-(--primary) scale-110"
						: "text-(--muted-foreground) hover:text-(--foreground)"
				} transition-all duration-300 p-2`}
			>
				<Home
					size={24}
					strokeWidth={pathname === "home" || pathname === "" ? 2.5 : 2}
				/>
			</button>
			<button
				onClick={() => navigate("/stats")}
				className={`${
					pathname === "stats"
						? "text-(--primary) scale-110"
						: "text-(--muted-foreground) hover:text-(--foreground)"
				} transition-all duration-300 p-2`}
			>
				<LayoutGrid size={24} strokeWidth={pathname === "stats" ? 2.5 : 2} />
			</button>

			{/* Floating Action Button for Add Expense */}
			<button
				onClick={() => navigate("/add-expense")}
				className="bg-linear-to-tr from-(--primary) to-(--primary)/90 text-(--primary-foreground) p-4 rounded-2xl shadow-lg -mt-12 border-4 border-(--background) hover:scale-110 hover:-translate-y-1 active:scale-95 transition-all duration-300 flex items-center justify-center group"
			>
				<Plus
					size={26}
					className="group-hover:rotate-90 transition-transform duration-300"
					strokeWidth={3}
				/>
			</button>

			<button
				onClick={() => navigate("/debts")}
				className={`${
					pathname === "debts" ||
					pathname === "create-group" ||
					pathname === "group-detail"
						? "text-(--primary) scale-110"
						: "text-(--muted-foreground) hover:text-(--foreground)"
				} transition-all duration-300 p-2`}
			>
				<Handshake
					size={24}
					strokeWidth={
						pathname === "debts" ||
						pathname === "create-group" ||
						pathname === "group-detail"
							? 2.5
							: 2
					}
				/>
			</button>
			<button
				onClick={() => navigate("/profile")}
				className={`${
					pathname === "profile"
						? "text-(--primary) scale-110"
						: "text-(--muted-foreground) hover:text-(--foreground)"
				} transition-all duration-300 p-2`}
			>
				<User size={24} strokeWidth={pathname === "profile" ? 2.5 : 2} />
			</button>
		</div>
	);
};
