import {
	Bell,
	Sun,
	Moon,
	ArrowDownLeft,
	ArrowUpRight,
	EyeOff,
	Wifi,
	Banknote,
	Plus,
} from "lucide-react";
import { TransactionItem } from "../shared/TransactionItem";
import { CreditCardComponent } from "../shared/CreditCardComponent";
import type { Transaction } from "../../types";

interface HomeViewProps {
	transactions: Transaction[];
	isDarkMode: boolean;
	toggleTheme: () => void;
}

export const HomeView = ({
	transactions,
	isDarkMode,
	toggleTheme,
}: HomeViewProps) => (
	<div className="flex flex-col h-full bg-(--background) overflow-y-auto hide-scrollbar transition-colors duration-300">
		<div className="max-w-7xl mx-auto w-full pb-24 md:pb-6">
			{/* Header */}
			<header className="flex justify-between items-center p-6 bg-transparent">
				<div className="flex flex-col">
					<h1 className="text-3xl font-bold text-(--foreground)">Hi, There</h1>
				</div>
				<div className="flex gap-3">
					<button
						onClick={toggleTheme}
						className="p-2 text-(--foreground) hover:bg-(--muted) rounded-full transition-colors"
					>
						{isDarkMode ? <Sun size={20} /> : <Moon size={20} />}
					</button>
					<button className="p-2 relative bg-(--card) rounded-full shadow-sm text-(--foreground) border border-(--border)">
						<Bell size={20} />
						<span className="absolute top-1.5 right-2 w-2 h-2 bg-(--destructive) rounded-full"></span>
					</button>
				</div>
			</header>

			<div className="md:grid md:grid-cols-12 md:gap-8 md:px-6">
				<div className="md:col-span-7 lg:col-span-8">
					{/* Total Balance Card */}
					<div className="px-6 md:px-0 mb-8">
						<div className="bg-[#FFF6F1] dark:bg-(--card) text-[#3E2E28] dark:text-(--foreground) p-8 rounded-[2.5rem] shadow-sm border border-[#F5E6DE] dark:border-(--border) relative overflow-hidden transition-colors duration-300">
							{/* Gradient Blobs */}
							<div className="absolute -top-24 -right-24 w-64 h-64 bg-[#FADAC9] dark:bg-(--primary)/10 rounded-full blur-3xl opacity-60"></div>
							<div className="absolute -bottom-24 -left-24 w-64 h-64 bg-[#FADAC9] dark:bg-(--primary)/10 rounded-full blur-3xl opacity-60"></div>

							<div className="relative z-10">
								<div className="flex justify-between items-start mb-2">
									<div className="text-lg font-medium opacity-80">
										Total balance
									</div>
									<button className="opacity-60 hover:opacity-100 transition-opacity">
										<EyeOff size={24} />
									</button>
								</div>

								<div className="text-5xl font-bold mb-8 tracking-tight text-[#2D1F16] dark:text-(--foreground)">
									₹6,64,472.00
								</div>

								<div className="mb-6 text-lg font-semibold text-[#3E2E28] dark:text-(--foreground)">
									This month
								</div>

								<div className="grid grid-cols-2 gap-8">
									{/* Income */}
									<div>
										<div className="text-sm opacity-70 mb-1">Income</div>
										<div className="flex items-center flex-wrap gap-2 mb-1">
											<span className="text-xl font-bold text-[#2D1F16] dark:text-(--foreground)">
												₹4,99,100
											</span>
											<span className="text-xs font-medium text-emerald-600 dark:text-emerald-400 flex items-center">
												↑ 565.47%
											</span>
										</div>
										<div className="text-xs opacity-60 leading-relaxed">
											Compared to ₹75,000 last month
										</div>
									</div>

									{/* Expense */}
									<div>
										<div className="text-sm opacity-70 mb-1">Expense</div>
										<div className="flex items-center flex-wrap gap-2 mb-1">
											<span className="text-xl font-bold text-[#2D1F16] dark:text-(--foreground)">
												₹92,628
											</span>
											<span className="text-xs font-medium text-rose-600 dark:text-rose-400 flex items-center">
												↑ 92.97%
											</span>
										</div>
										<div className="text-xs opacity-60 leading-relaxed">
											Compared to ₹48,000 last month
										</div>
									</div>
								</div>
							</div>
						</div>
					</div>

					{/* Cards Section */}
					<div className="pl-6 md:pl-0 pb-2 mb-6 md:mb-0">
						<div className="flex justify-between items-center mb-4 pr-6">
							<h2 className="text-xl font-bold text-(--foreground)">
								My Cards
							</h2>
							<button
								onClick={() => (window.location.href = "/wallets")}
								className="text-sm font-medium text-(--muted-foreground) bg-(--card) border border-(--border) px-3 py-1 rounded-lg shadow-sm"
							>
								View All
							</button>
						</div>
						<div className="flex overflow-x-auto hide-scrollbar pb-4 pr-6 md:grid md:grid-cols-2 lg:grid-cols-3 md:gap-4 md:overflow-visible">
							<div className="mr-4 md:mr-0 flex-shrink-0">
								<CreditCardComponent
									type="VISA"
									number="9038 4061 **** ****"
									holder="Tashif"
									exp="02/28"
									gradient="from-(--chart-2) to-(--chart-1)"
									icon={Wifi}
								/>
							</div>
							<div className="mr-4 md:mr-0 flex-shrink-0">
								<CreditCardComponent
									type="Cash"
									number="Physical"
									holder="Tashif"
									exp="--"
									gradient="from-green-600 to-teal-700"
									icon={Banknote}
									isCash={true}
								/>
							</div>
							<div className="w-16 h-56 md:w-full md:h-56 bg-(--muted) rounded-4xl flex items-center justify-center flex-shrink-0 border-2 border-dashed border-(--border) cursor-pointer hover:bg-(--muted)/80 transition-colors">
								<Plus size={24} className="text-(--muted-foreground)" />
							</div>
						</div>
					</div>
				</div>

				<div className="md:col-span-5 lg:col-span-4">
					{/* Spending Analysis Title & List */}
					<div className="px-6 md:px-0 pb-24 md:pb-0">
						<div className="flex justify-between items-center mb-4">
							<h2 className="text-xl font-bold text-(--foreground)">
								Spending Analysis
							</h2>
							<button className="text-sm font-medium text-(--muted-foreground) bg-(--card) border border-(--border) px-3 py-1 rounded-lg shadow-sm">
								See All
							</button>
						</div>

						<div className="space-y-1">
							{transactions.map((t) => (
								<TransactionItem
									key={t.id}
									icon={t.icon}
									title={t.title}
									subtitle={t.subtitle}
									amount={t.amount}
									percent={t.percent}
								/>
							))}
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
);
