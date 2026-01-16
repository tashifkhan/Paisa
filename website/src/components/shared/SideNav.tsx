import { Handshake, Home, LayoutGrid, Plus, User } from "lucide-react";
import { useLocation, useNavigate } from "react-router-dom";

export const SideNav = ({ isDarkMode }: { isDarkMode: boolean }) => {
	const navigate = useNavigate();
	const location = useLocation();
	const pathname = location.pathname.slice(1) || "home";

	return (
		<div className="hidden md:flex flex-col w-64 h-[calc(100vh-2rem)] fixed left-4 top-4 bg-(--card)/80 backdrop-blur-xl border border-(--border)/50 rounded-2xl py-6 px-4 justify-between shadow-2xl z-50 transition-all duration-300">
			<div className="flex flex-col gap-6">
				<div className="mb-4 px-4 flex flex-col items-center gap-3">
					<img
						src={isDarkMode ? "/logo-dark.png" : "/logo-light.png"}
						alt="Paisa Logo"
						className="h-24 w-auto drop-shadow-lg transition-transform hover:scale-105"
					/>
				</div>

				<div className="flex flex-col gap-2">
					<NavButton
						icon={<Home size={22} />}
						label="Home"
						isActive={pathname === "home" || pathname === ""}
						onClick={() => navigate("/")}
					/>
					<NavButton
						icon={<LayoutGrid size={22} />}
						label="Stats"
						isActive={pathname === "stats"}
						onClick={() => navigate("/stats")}
					/>
					<NavButton
						icon={<Handshake size={22} />}
						label="Debts"
						isActive={
							pathname === "debts" ||
							pathname === "create-group" ||
							pathname === "group-detail"
						}
						onClick={() => navigate("/debts")}
					/>
					<NavButton
						icon={<User size={22} />}
						label="Profile"
						isActive={pathname === "profile"}
						onClick={() => navigate("/profile")}
					/>
				</div>
			</div>

			<button
				onClick={() => navigate("/add-expense")}
				className="bg-linear-to-r from-(--primary) to-(--primary)/80 text-(--primary-foreground) p-4 rounded-xl shadow-lg hover:shadow-xl hover:scale-[1.02] active:scale-95 transition-all duration-300 flex items-center justify-center gap-2 mb-2 group"
			>
				<Plus
					size={24}
					className="group-hover:rotate-90 transition-transform duration-300"
				/>
				<span className="font-semibold">Add Expense</span>
			</button>
		</div>
	);
};

const NavButton = ({
	icon,
	label,
	isActive,
	onClick,
}: {
	icon: React.ReactNode;
	label: string;
	isActive: boolean;
	onClick: () => void;
}) => (
	<button
		onClick={onClick}
		className={`flex items-center gap-4 p-3.5 rounded-xl transition-all duration-200 w-full text-left group relative overflow-hidden ${
			isActive
				? "bg-(--primary)/10 text-(--primary) font-semibold shadow-sm"
				: "text-(--muted-foreground) hover:bg-(--muted)/50 hover:text-(--foreground)"
		}`}
	>
		<div
			className={`transition-transform duration-200 ${isActive ? "scale-110" : "group-hover:scale-110"}`}
		>
			{icon}
		</div>
		<span className="z-10">{label}</span>
		{isActive && (
			<div className="absolute left-0 w-1 h-8 bg-(--primary) rounded-r-full" />
		)}
	</button>
);
