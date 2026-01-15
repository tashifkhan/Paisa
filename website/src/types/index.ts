import type { LucideIcon } from "lucide-react";

export interface Transaction {
	id: number | string;
	title: string;
	subtitle: string;
	amount: string;
	percent?: string;
	icon: LucideIcon;
}
