import { Home, LayoutGrid, Plus, Handshake, User } from "lucide-react";
import { useNavigate, useLocation } from "react-router-dom";

export const SideNav = () => {
	const navigate = useNavigate();
	const location = useLocation();
	const pathname = location.pathname.slice(1) || "home";

	return (
		<div className="hidden md:flex flex-col w-64 h-screen bg-(--card) border-r border-(--border) py-6 px-4 justify-between shadow-xl z-50 transition-colors duration-300">
			<div className="flex flex-col gap-2">
				<div className="mb-8 px-4 flex items-center gap-2">
					<div className="w-8 h-8 bg-(--primary) rounded-lg flex items-center justify-center">
						<span className="text-white font-bold text-xl">P</span>
					</div>
					<h1 className="text-2xl font-bold text-(--foreground)">Paisa</h1>
				</div>

				<NavButton
					icon={<Home size={24} />}
					label="Home"
					isActive={pathname === "home" || pathname === ""}
					onClick={() => navigate("/")}
				/>
				<NavButton
					icon={<LayoutGrid size={24} />}
					label="Stats"
					isActive={pathname === "stats"}
					onClick={() => navigate("/stats")}
				/>
				<NavButton
					icon={<Handshake size={24} />}
					label="Debts"
					isActive={
						pathname === "debts" ||
						pathname === "create-group" ||
						pathname === "group-detail"
					}
					onClick={() => navigate("/debts")}
				/>
				<NavButton
					icon={<User size={24} />}
					label="Profile"
					isActive={pathname === "profile"}
					onClick={() => navigate("/profile")}
				/>
			</div>

			<button
				onClick={() => navigate("/add-expense")}
				className="bg-(--primary) text-(--primary-foreground) p-4 rounded-xl shadow-lg hover:opacity-90 transition-all duration-300 flex items-center justify-center gap-2 mb-4"
			>
				<Plus size={24} />
				<span className="font-medium">Add Expense</span>
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
		className={`flex items-center gap-4 p-4 rounded-xl transition-colors w-full text-left ${
			isActive
				? "bg-(--primary)/10 text-(--primary)"
				: "text-(--muted-foreground) hover:bg-(--muted) hover:text-(--foreground)"
		}`}
	>
		{icon}
		<span className="font-medium">{label}</span>
	</button>
);
