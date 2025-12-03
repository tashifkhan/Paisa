interface StatsCardProps {
	title: string;
	amount: string;
}

export const StatsCard = ({ title, amount }: StatsCardProps) => (
	<div className="bg-(--card) p-5 rounded-4xl shadow-sm border border-(--border) flex flex-col items-center justify-center min-w-[30%] duration-300 hover:scale-105 transition-transform">
		<span className="text-xs text-(--muted-foreground) mb-1">{title}</span>
		<span className="text-lg font-bold text-(--card-foreground) transition-colors duration-300">
			₹{amount}
		</span>
	</div>
);
