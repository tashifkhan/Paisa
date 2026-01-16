import { Banknote, Cpu, Nfc, Wifi, type LucideIcon } from "lucide-react";
import React from "react";

interface CreditCardComponentProps {
	type?: string;
	number?: string;
	holder?: string;
	exp?: string;
	gradient?: string;
	icon?: LucideIcon;
	isVirtual?: boolean;
	isCash?: boolean;
}

export const CreditCardComponent = ({
	type = "VISA",
	number = "9038 4061 **** ****",
	holder = "Tashif Ahmad Khan",
	exp = "02/02",
	gradient = "from-(--chart-2) to-(--chart-1)",
	icon = Wifi,
	isVirtual = false,
	isCash = false,
}: CreditCardComponentProps) => (
	<div
		className={`relative w-full h-56 bg-linear-to-br ${gradient} rounded-4xl p-6 shadow-md flex flex-col justify-between overflow-hidden mb-6 ${
			isVirtual ? "border-2 border-white/30" : ""
		}`}
	>
		{!isCash && (
			<div className="absolute top-0 right-0 w-32 h-32 bg-white/20 rounded-full -mr-10 -mt-10 blur-2xl"></div>
		)}
		<div className="flex justify-between items-start z-10">
			<div className="flex flex-col">
				<div className="text-white font-bold text-lg tracking-wider opacity-90">
					{type}
				</div>
				{isVirtual && (
					<div className="text-white/70 text-xs font-medium">Virtual Card</div>
				)}
			</div>
			{/* Icon Component */}
			<div className="flex items-center gap-2">
				{isVirtual && <Cpu size={20} className="text-white opacity-80" />}
				{React.createElement(icon, {
					size: 24,
					className: "text-white opacity-80",
				})}
			</div>
		</div>
		<div className="z-10">
			{isCash ? (
				<>
					<div className="text-xs text-white/70 uppercase mb-1 font-medium">
						Total Cash
					</div>
					<div className="text-3xl font-bold text-white tracking-widest mb-4 shadow-sm">
						{number}
					</div>
				</>
			) : (
				<div className="text-2xl font-bold text-white tracking-widest mb-4 shadow-sm">
					{number}
				</div>
			)}

			<div className="flex justify-between items-end">
				<div>
					<div className="text-xs text-white/70 uppercase mb-1 font-medium">
						Card Holder
					</div>
					<div className="font-semibold text-white tracking-wide">{holder}</div>
				</div>
				<div className="text-right">
					<div className="text-xs text-white/70 uppercase mb-1 font-medium">
						Exp Date
					</div>
					<div className="font-semibold text-white tracking-wide">{exp}</div>
				</div>
			</div>
		</div>
		{(type === "VISA" || type === "Mastercard") && !isVirtual && (
			<>
				<div className="absolute bottom-6 right-6 z-10 flex items-center gap-2">
					<Nfc size={20} className="text-white/70" />
					<div className="w-12 h-8 bg-white/20 backdrop-blur-sm rounded flex overflow-hidden border border-white/10">
						<div className="w-1/2 h-full border-r border-white/20"></div>
					</div>
				</div>
			</>
		)}
		{isCash && (
			<div className="absolute bottom-4 right-6 z-10 opacity-20">
				<Banknote size={64} className="text-white" />
			</div>
		)}
	</div>
);
