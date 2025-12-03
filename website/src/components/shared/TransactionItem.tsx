import type { LucideIcon } from "lucide-react";

interface TransactionItemProps {
	icon: LucideIcon;
	title: string;
	subtitle: string;
	amount: string;
	percent?: string;
}

export const TransactionItem = ({ icon: Icon, title, subtitle, amount, percent }: TransactionItemProps) => (
	<div className="flex items-center justify-between py-4 group cursor-pointer hover:bg-(--muted) rounded-3xl px-3 transition-all">
		<div className="flex items-center gap-4">
			<div
				className={`w-12 h-12 rounded-full flex items-center justify-center bg-(--muted) transition-colors duration-300`}
			>
				<Icon size={20} className="text-(--foreground)" />
			</div>
			<div>
				<h3 className="font-bold text-(--foreground) transition-colors duration-300">
					{title}
				</h3>
				<p className="text-sm text-(--muted-foreground)">{subtitle}</p>
			</div>
		</div>
		<div className="text-right">
			<p className="font-bold text-(--foreground) transition-colors duration-300">
				₹{amount}
			</p>
			{percent && (
				<p className="text-xs text-(--muted-foreground)">{percent}%</p>
			)}
		</div>
	</div>
);
