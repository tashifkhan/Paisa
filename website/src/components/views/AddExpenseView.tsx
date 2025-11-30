import { X, Sun, Moon, Edit3, Wallet, Shirt, ChevronDown, Delete, Calendar, Check } from "lucide-react";

interface AddExpenseViewProps {
	amount: string;
	isDarkMode: boolean;
	toggleTheme: () => void;
	handleKeyPress: (key: string) => void;
	setCurrentView: (view: string) => void;
}

export const AddExpenseView = ({ 
	amount, 
	isDarkMode, 
	toggleTheme, 
	handleKeyPress,
	setCurrentView 
}: AddExpenseViewProps) => (
	<div className="flex flex-col h-full bg-(--background) transition-colors duration-300">
		<header className="flex justify-between items-start p-6">
			<button
				onClick={() => setCurrentView("stats")}
				className="p-2 text-(--foreground)"
			>
				<X size={24} />
			</button>
			<div className="flex flex-col items-center opacity-50">
				<h1 className="text-sm font-bold text-(--foreground) transition-colors duration-300">
					₹32,500.00
				</h1>
				<div className="text-xs text-(--muted-foreground)">
					Total Balance
				</div>
			</div>
			<div className="flex items-center gap-1">
				<button
					onClick={toggleTheme}
					className="p-2 text-(--foreground) hover:bg-(--muted) rounded-full transition-colors"
				>
					{isDarkMode ? <Sun size={20} /> : <Moon size={20} />}
				</button>
				<button className="p-2 text-(--foreground)">
					<Edit3 size={20} />
				</button>
			</div>
		</header>

		<div className="flex-1 flex flex-col items-center px-8 pt-4">
			<div className="flex gap-4 w-full justify-between mb-8">
				<button className="flex-1 flex items-center justify-between bg-(--muted) border border-transparent px-4 py-3 rounded-2xl text-(--foreground) font-medium transition-colors duration-300">
					<div className="flex items-center gap-2">
						<Wallet size={18} /> Cash
					</div>
					<ChevronDown size={16} />
				</button>
				<button className="flex-1 flex items-center justify-between bg-(--muted) border border-transparent px-4 py-3 rounded-2xl text-(--foreground) font-medium transition-colors duration-300">
					<div className="flex items-center gap-2">
						<Shirt size={18} /> Shopping
					</div>
					<ChevronDown size={16} />
				</button>
			</div>

			<div className="flex flex-col items-center justify-center flex-1 w-full mb-8">
				<span className="text-(--muted-foreground) text-sm mb-2">
					Expenses
				</span>
				<div className="flex items-center text-6xl font-bold text-(--foreground) tracking-tight transition-colors duration-300">
					<span className="text-(--muted-foreground) text-4xl mr-1">
						₹
					</span>
					{amount}
					<span className="animate-pulse w-0.5 h-12 bg-(--foreground) ml-1"></span>
				</div>
				<input
					type="text"
					placeholder="Add comment..."
					className="mt-6 text-center w-full outline-none bg-transparent text-(--muted-foreground) placeholder-(--muted-foreground) font-medium transition-colors duration-300"
				/>
			</div>
		</div>

		<div className="bg-(--card) rounded-t-[3rem] p-8 pb-10 shadow-[0_-10px_40px_rgba(0,0,0,0.05) border-t border-(--border) transition-colors duration-300">
			<div className="grid grid-cols-4 gap-4 h-80">
				{[1, 2, 3].map((num) => (
					<button
						key={num}
						onClick={() => handleKeyPress(num.toString())}
						className="text-2xl font-medium text-(--foreground) rounded-full hover:bg-(--muted) active:scale-95 transition-all"
					>
						{num}
					</button>
				))}
				<button
					onClick={() => handleKeyPress("backspace")}
					className="flex items-center justify-center bg-(--destructive) text-(--destructive-foreground) rounded-full hover:opacity-90 active:scale-95 transition-all"
				>
					<Delete size={24} />
				</button>

				{[4, 5, 6].map((num) => (
					<button
						key={num}
						onClick={() => handleKeyPress(num.toString())}
						className="text-2xl font-medium text-(--foreground) rounded-full hover:bg-(--muted) active:scale-95 transition-all"
					>
						{num}
					</button>
				))}
				<button className="flex items-center justify-center bg-(--muted) text-(--primary) rounded-full hover:bg-(--muted)/80 active:scale-95 transition-all">
					<Calendar size={24} />
				</button>

				{[7, 8, 9].map((num) => (
					<button
						key={num}
						onClick={() => handleKeyPress(num.toString())}
						className="text-2xl font-medium text-(--foreground) rounded-full hover:bg-(--muted) active:scale-95 transition-all"
					>
						{num}
					</button>
				))}

				<button
					onClick={() => handleKeyPress("check")}
					className="row-span-2 flex items-center justify-center bg-(--primary) text-(--primary-foreground) rounded-4xl shadow-xl hover:opacity-90 active:scale-95 transition-all"
				>
					<Check size={32} />
				</button>

				<button className="text-2xl font-medium text-(--foreground) bg-(--muted) rounded-full hover:bg-(--muted)/80 active:scale-95 transition-all">
					₹
				</button>
				<button
					onClick={() => handleKeyPress("0")}
					className="text-2xl font-medium text-(--foreground) rounded-full hover:bg-(--muted) active:scale-95 transition-all"
				>
					0
				</button>
				<button
					onClick={() => handleKeyPress(".")}
					className="text-2xl font-medium text-(--foreground) rounded-full hover:bg-(--muted) active:scale-95 transition-all"
				>
					,
				</button>
			</div>
		</div>
	</div>
);
