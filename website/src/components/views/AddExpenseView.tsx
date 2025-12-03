import { X, Sun, Moon, Edit3, Wallet, Shirt, Delete, Calendar, Check } from "lucide-react";
import { useState, useRef, type MouseEvent } from "react";
import {
	Select,
	SelectContent,
	SelectItem,
	SelectTrigger,
	SelectValue,
} from "../ui/select";

interface AddExpenseViewProps {
	amount: string;
	isDarkMode: boolean;
	toggleTheme: () => void;
	handleKeyPress: (key: string) => void;
	setCurrentView: (view: string) => void;
}

interface Ripple {
	x: number;
	y: number;
	size: number;
	id: number;
}

const RippleButton = ({ 
	children, 
	onClick, 
	className = "",
	...props 
}: { 
	children: React.ReactNode; 
	onClick?: () => void; 
	className?: string;
	[key: string]: any;
}) => {
	const [ripples, setRipples] = useState<Ripple[]>([]);
	const buttonRef = useRef<HTMLButtonElement>(null);

	const createRipple = (event: MouseEvent<HTMLButtonElement>) => {
		const button = buttonRef.current;
		if (!button) return;

		const rect = button.getBoundingClientRect();
		const size = Math.max(rect.width, rect.height);
		const x = event.clientX - rect.left - size / 2;
		const y = event.clientY - rect.top - size / 2;
		
		const newRipple = {
			x,
			y,
			size,
			id: Date.now()
		};

		setRipples((prev) => [...prev, newRipple]);

		setTimeout(() => {
			setRipples((prev) => prev.filter((r) => r.id !== newRipple.id));
		}, 600);
	};

	const handleClick = (event: MouseEvent<HTMLButtonElement>) => {
		createRipple(event);
		onClick?.();
	};

	return (
		<button
			ref={buttonRef}
			onClick={handleClick}
			className={`relative overflow-hidden ${className}`}
			{...props}
		>
			{children}
			{ripples.map((ripple) => (
				<span
					key={ripple.id}
					style={{
						position: "absolute",
						left: ripple.x,
						top: ripple.y,
						width: ripple.size,
						height: ripple.size,
						borderRadius: "50%",
						background: "rgba(128, 128, 128, 0.4)",
						transform: "scale(0)",
						animation: "ripple 600ms ease-out",
						pointerEvents: "none",
					}}
				/>
			))}
			<style>
				{`
					@keyframes ripple {
						0% {
							transform: scale(0);
							opacity: 1;
						}
						100% {
							transform: scale(2.5);
							opacity: 0;
						}
					}
				`}
			</style>
		</button>
	);
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
			<RippleButton
				onClick={() => setCurrentView("stats")}
				className="p-2 text-(--foreground)"
			>
				<X size={24} />
			</RippleButton>
			<div className="flex flex-col items-center opacity-50">
				<h1 className="text-sm font-bold text-(--foreground) transition-colors duration-300">
					₹32,500.00
				</h1>
				<div className="text-xs text-(--muted-foreground)">
					Total Balance
				</div>
			</div>
			<div className="flex items-center gap-1">
				<RippleButton
					onClick={toggleTheme}
					className="p-2 text-(--foreground) hover:bg-(--muted) rounded-full transition-colors"
				>
					{isDarkMode ? <Sun size={20} /> : <Moon size={20} />}
				</RippleButton>
				<RippleButton className="p-2 text-(--foreground)">
					<Edit3 size={20} />
				</RippleButton>
			</div>
		</header>

		<div className="flex-1 flex flex-col items-center px-8 pt-4">
			<div className="flex gap-4 w-full justify-between mb-8">
				<Select defaultValue="cash">
					<SelectTrigger className="flex-1">
						<div className="flex items-center gap-2">
							<Wallet size={18} />
							<SelectValue />
						</div>
					</SelectTrigger>
					<SelectContent>
						<SelectItem value="cash">
							<div className="flex items-center gap-2">
								<Wallet size={18} /> Cash
							</div>
						</SelectItem>
						<SelectItem value="card">Card</SelectItem>
						<SelectItem value="bank">Bank</SelectItem>
						<SelectItem value="upi">UPI</SelectItem>
					</SelectContent>
				</Select>
				<Select defaultValue="shopping">
					<SelectTrigger className="flex-1">
						<div className="flex items-center gap-2">
							<Shirt size={18} />
							<SelectValue />
						</div>
					</SelectTrigger>
					<SelectContent>
						<SelectItem value="shopping">
							<div className="flex items-center gap-2">
								<Shirt size={18} /> Shopping
							</div>
						</SelectItem>
						<SelectItem value="food">Food</SelectItem>
						<SelectItem value="transport">Transport</SelectItem>
						<SelectItem value="entertainment">Entertainment</SelectItem>
						<SelectItem value="bills">Bills</SelectItem>
						<SelectItem value="other">Other</SelectItem>
					</SelectContent>
				</Select>
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
					<RippleButton
						key={num}
						onClick={() => handleKeyPress(num.toString())}
						className="text-2xl font-medium text-(--foreground) rounded-full hover:bg-(--muted) active:scale-95 transition-all"
					>
						{num}
					</RippleButton>
				))}
				<RippleButton
					onClick={() => handleKeyPress("backspace")}
					className="flex items-center justify-center bg-(--destructive) text-(--destructive-foreground) rounded-full hover:opacity-90 active:scale-95 transition-all"
				>
					<Delete size={24} />
				</RippleButton>

				{[4, 5, 6].map((num) => (
					<RippleButton
						key={num}
						onClick={() => handleKeyPress(num.toString())}
						className="text-2xl font-medium text-(--foreground) rounded-full hover:bg-(--muted) active:scale-95 transition-all"
					>
						{num}
					</RippleButton>
				))}
				<RippleButton className="flex items-center justify-center bg-(--muted) text-(--primary) rounded-full hover:bg-(--muted)/80 active:scale-95 transition-all">
					<Calendar size={24} />
				</RippleButton>

				{[7, 8, 9].map((num) => (
					<RippleButton
						key={num}
						onClick={() => handleKeyPress(num.toString())}
						className="text-2xl font-medium text-(--foreground) rounded-full hover:bg-(--muted) active:scale-95 transition-all"
					>
						{num}
					</RippleButton>
				))}

				<RippleButton
					onClick={() => handleKeyPress("check")}
					className="row-span-2 flex items-center justify-center bg-(--primary) text-(--primary-foreground) rounded-4xl shadow-xl hover:opacity-90 active:scale-95 transition-all"
				>
					<Check size={32} />
				</RippleButton>

				<RippleButton className="text-2xl font-medium text-(--foreground) bg-(--muted) rounded-full hover:bg-(--muted)/80 active:scale-95 transition-all">
					₹
				</RippleButton>
				<RippleButton
					onClick={() => handleKeyPress("0")}
					className="text-2xl font-medium text-(--foreground) rounded-full hover:bg-(--muted) active:scale-95 transition-all"
				>
					0
				</RippleButton>
				<RippleButton
					onClick={() => handleKeyPress(".")}
					className="text-2xl font-medium text-(--foreground) rounded-full hover:bg-(--muted) active:scale-95 transition-all"
				>
					,
				</RippleButton>
			</div>
		</div>
	</div>
);
