import { type LucideIcon, ChevronRight } from "lucide-react";

interface SettingItemProps {
	icon: LucideIcon;
	title: string;
	value?: string;
	type?: "arrow" | "toggle";
	onClick?: () => void;
	isToggled?: boolean;
}

export const SettingItem = ({
	icon: Icon,
	title,
	value,
	type = "arrow",
	onClick,
	isToggled,
}: SettingItemProps) => (
	<button
		onClick={onClick}
		className="w-full flex items-center justify-between p-4 bg-(--card) border border-(--border) rounded-2xl mb-3 hover:bg-(--muted) transition-all active:scale-[0.98]"
	>
		<div className="flex items-center gap-4">
			<div className="w-10 h-10 rounded-full bg-(--muted) flex items-center justify-center text-(--primary)">
				<Icon size={20} />
			</div>
			<div className="text-left">
				<span className="block font-medium text-(--foreground)">
					{title}
				</span>
			</div>
		</div>

		<div className="flex items-center gap-2">
			{value && (
				<span className="text-sm text-(--muted-foreground)">{value}</span>
			)}

			{type === "arrow" && (
				<ChevronRight size={18} className="text-(--muted-foreground)" />
			)}

			{type === "toggle" && (
				<div
					className={`w-12 h-6 rounded-full p-1 transition-colors duration-300 ${
						isToggled ? "bg-(--primary)" : "bg-(--muted-foreground)"
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
