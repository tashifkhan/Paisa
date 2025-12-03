import { Home, Ghost } from "lucide-react";

interface NotFoundViewProps {
	onGoHome: () => void;
}

export const NotFoundView = ({ onGoHome }: NotFoundViewProps) => {
	return (
		<div className="flex flex-col h-full bg-(--background) px-6 pt-12 pb-6 items-center justify-center text-center animate-in fade-in zoom-in duration-500">
			<div className="relative mb-8">
				{/* Glowing background blob */}
				<div className="absolute inset-0 bg-(--primary)/20 rounded-full blur-xl transform scale-150 animate-pulse"></div>

				{/* Cute Ghost Icon */}
				<div className="relative bg-(--card) p-8 rounded-full shadow-xl border-2 border-(--border)">
					<img
						src="/sad-mascot-empty.png"
						alt="Sad Mascot"
						className="w-40 h-40 object-contain animate-bounce"
					/>
				</div>
			</div>

			<h1 className="text-6xl font-black text-(--foreground) mb-2 tracking-tighter">
				404
			</h1>
			<h2 className="text-2xl font-bold text-(--foreground) mb-4">
				Whoops! Ghost Town
			</h2>

			<p className="text-(--muted-foreground) max-w-xs mx-auto mb-10 text-lg leading-relaxed">
				The page you're looking for seems to have floated away into the digital
				void.
			</p>

			<button
				onClick={onGoHome}
				className="group flex items-center gap-3 px-8 py-4 bg-(--primary) text-(--primary-foreground) rounded-full font-bold text-lg shadow-lg hover:shadow-xl hover:scale-105 transition-all duration-300"
			>
				<Home
					size={20}
					className="group-hover:-translate-y-1 transition-transform"
				/>
				Take Me Home
			</button>

			{/* Decorative small elements */}
			<div className="absolute top-1/4 left-10 opacity-20 text-(--foreground) animate-spin-slow">
				<Ghost size={24} />
			</div>
			<div className="absolute bottom-1/4 right-10 opacity-20 text-(--foreground) animate-bounce delay-700">
				<Ghost size={32} />
			</div>
		</div>
	);
};
