import {
	Check,
	Loader2,
	MoreHorizontal,
	Search,
	User,
	Users,
} from "lucide-react";
import React, { useState } from "react";
import { debtService } from "../../services/debtService";
import type { BackendUser } from "../../services/types";
import { userService } from "../../services/userService";

interface Debt {
	id: number;
	name: string;
	amount: number;
	type: "owed_to_me" | "owed_by_me";
	date: string;
}

interface Group {
	id: number;
	name: string;
	members: number;
	balance: number;
	type: "owe" | "owed";
	icon: any;
	color: string;
}

interface SocialViewProps {
	debts: Debt[];
	groups: Group[];
	socialTab: string;
	setSocialTab: (tab: string) => void;
	setCurrentView: (view: string) => void;
	setSelectedGroup: (group: any) => void;
	setSelectedUser: (user: any) => void;
}

export const SocialView = ({
	debts,
	groups,
	socialTab,
	setSocialTab,
	setCurrentView,
	setSelectedUser,
	setSelectedGroup, // Destructure
}: SocialViewProps) => {
	const [searchQuery, setSearchQuery] = useState("");
	const [searchResults, setSearchResults] = useState<BackendUser[]>([]);
	const [isSearching, setIsSearching] = useState(false);
	const [searchDebounce, setSearchDebounce] = useState<any>(null);

	// Add participant state
	const [participantName, setParticipantName] = useState("");
	const [participantEmail, setParticipantEmail] = useState("");
	const [isAddingParticipant, setIsAddingParticipant] = useState(false);
	const [addSuccess, setAddSuccess] = useState(false);

	const handleSearch = async (query: string) => {
		setSearchQuery(query);
		if (searchDebounce) clearTimeout(searchDebounce);

		if (!query.trim()) {
			setSearchResults([]);
			return;
		}

		setIsSearching(true);
		const timeout = setTimeout(async () => {
			try {
				const results = await userService.searchUsers(query);
				setSearchResults(results);
			} catch (error) {
				console.error("Search failed:", error);
			} finally {
				setIsSearching(false);
			}
		}, 500);
		setSearchDebounce(timeout);
	};

	const handleAddParticipant = async () => {
		if (!participantName.trim()) return;

		setIsAddingParticipant(true);
		setAddSuccess(false);
		try {
			// Create a debt record with 0 amount to establish the participant
			await debtService.addDebt({
				counterparty_name: participantName.trim(),
				amount: 0,
				type: "owed_to_me",
			});
			setAddSuccess(true);
			setParticipantName("");
			setParticipantEmail("");
			// Switch to Friends tab to show the new participant
			setTimeout(() => {
				setAddSuccess(false);
				setSocialTab("debts");
			}, 1500);
		} catch (error) {
			console.error("Failed to add participant:", error);
		} finally {
			setIsAddingParticipant(false);
		}
	};

	const netBalance = debts.reduce(
		(acc, curr) =>
			curr.type === "owed_to_me" ? acc + curr.amount : acc - curr.amount,
		0
	);

	return (
		<div className="flex flex-col h-full bg-(--background) pb-24 md:pb-6 overflow-y-auto hide-scrollbar transition-colors duration-300">
			<div className="max-w-5xl mx-auto w-full">
				<header className="flex justify-between items-center p-6">
					<div className="flex flex-col">
						<h1 className="text-3xl font-bold text-(--foreground)">Social</h1>
						<p className="text-(--muted-foreground) text-sm">
							Friends & Shared Expenses
						</p>
					</div>
					<button className="p-2 bg-(--card) border border-(--border) rounded-full text-(--foreground) shadow-sm">
						<MoreHorizontal size={20} />
					</button>
				</header>

				{/* Tabs */}
				<div className="px-6 mb-6">
					<div className="flex justify-between items-center bg-(--muted) rounded-[2rem] p-1 text-sm font-medium">
						<button
							onClick={() => setSocialTab("debts")}
							className={`flex-1 py-3 rounded-[2rem] transition-all ${
								socialTab === "debts"
									? "bg-(--primary) text-(--primary-foreground) shadow-md"
									: "text-(--muted-foreground)"
							}`}
						>
							Friends
						</button>
						<button
							onClick={() => setSocialTab("groups")}
							className={`flex-1 py-3 rounded-[2rem] transition-all ${
								socialTab === "groups"
									? "bg-(--primary) text-(--primary-foreground) shadow-md"
									: "text-(--muted-foreground)"
							}`}
						>
							Groups
						</button>
						<button
							onClick={() => setSocialTab("contacts")}
							className={`flex-1 py-3 rounded-[2rem] transition-all ${
								socialTab === "contacts"
									? "bg-(--primary) text-(--primary-foreground) shadow-md"
									: "text-(--muted-foreground)"
							}`}
						>
							Contacts
						</button>
					</div>
				</div>

				{socialTab === "debts" ? (
					<div className="md:grid md:grid-cols-12 md:gap-8 md:px-6">
						<div className="md:col-span-5 lg:col-span-4">
							{/* Net Balance Card */}
							<div className="px-6 md:px-0 mb-6 md:mb-0">
								<div
									className={`p-6 rounded-[2rem] shadow-lg relative overflow-hidden ${
										netBalance >= 0 ? "bg-(--chart-4)" : "bg-(--chart-2)"
									} text-white`}
								>
									<div className="relative z-10 text-center">
										<div className="text-sm font-medium opacity-90 mb-1">
											Net Balance
										</div>
										<div className="text-4xl font-bold mb-2">
											{netBalance >= 0 ? "+" : "-"}₹{Math.abs(netBalance)}
										</div>
										<div className="text-xs opacity-80">
											{netBalance >= 0
												? "You are overall in credit"
												: "You are overall in debt"}
										</div>
									</div>
								</div>
							</div>
						</div>

						<div className="md:col-span-7 lg:col-span-8">
							{/* Debts List */}
							<div className="px-6 md:px-0 space-y-3">
								{debts.map((debt) => (
									<div
										key={debt.id}
										onClick={() => setSelectedUser(debt)}
										className="flex items-center justify-between p-4 bg-(--card) border border-(--border) rounded-3xl hover:bg-(--muted)/50 transition-all cursor-pointer"
									>
										<div className="flex items-center gap-4">
											<div
												className={`w-12 h-12 rounded-full flex items-center justify-center ${
													debt.type === "owed_to_me"
														? "bg-green-100 text-green-600 dark:bg-green-900/30 dark:text-green-400"
														: "bg-red-100 text-red-600 dark:bg-red-900/30 dark:text-red-400"
												}`}
											>
												<User size={20} />
											</div>
											<div>
												<h3 className="font-bold text-(--foreground)">
													{debt.name}
												</h3>
												<p className="text-xs text-(--muted-foreground)">
													{debt.date}
												</p>
											</div>
										</div>
										<div className="text-right">
											<p
												className={`font-bold ${
													debt.type === "owed_to_me"
														? "text-green-500"
														: "text-red-500"
												}`}
											>
												{debt.type === "owed_to_me" ? "+" : "-"}₹{debt.amount}
											</p>
											<p className="text-xs text-(--muted-foreground)">
												{debt.type === "owed_to_me" ? "Credit" : "Debt"}
											</p>
										</div>
									</div>
								))}

								<button
									onClick={() => setSocialTab("contacts")}
									className="w-full py-4 border-2 border-dashed border-(--border) rounded-[2rem] text-(--muted-foreground) font-medium hover:bg-(--muted) transition-colors flex items-center justify-center gap-2 mt-4"
								>
									<div className="w-6 h-6 rounded-full bg-(--primary) text-(--primary-foreground) flex items-center justify-center text-lg leading-none pb-1">
										+
									</div>
									Add New Contact
								</button>
							</div>
						</div>
					</div>
				) : socialTab === "groups" ? (
					/* Groups Tab */
					<div className="px-6 md:grid md:grid-cols-2 lg:grid-cols-3 md:gap-6">
						{groups.map((group) => (
							<div
								key={group.id}
								onClick={() => setSelectedGroup(group)} // Use prop
								className="bg-(--card) border border-(--border) rounded-[2rem] p-5 hover:bg-(--muted)/50 transition-all cursor-pointer mb-4 md:mb-0"
							>
								<div className="flex justify-between items-start mb-4">
									<div className="flex items-center gap-4">
										<div
											className={`w-12 h-12 rounded-full ${group.color} flex items-center justify-center text-white font-bold text-lg shadow-md`}
										>
											{React.createElement(group.icon || Users, { size: 20 })}
										</div>
										<div>
											<h3 className="font-bold text-(--foreground) text-lg">
												{group.name}
											</h3>
											<div className="flex items-center gap-1 text-(--muted-foreground) text-xs">
												<Users size={12} /> {group.members} Members
											</div>
										</div>
									</div>
									<button className="p-2 text-(--muted-foreground) hover:text-(--foreground)">
										<MoreHorizontal size={20} />
									</button>
								</div>
								<div className="flex justify-between items-center p-3 bg-(--muted) rounded-2xl">
									<span className="text-sm text-(--muted-foreground)">
										Your share
									</span>
									<span
										className={`font-bold ${
											group.type === "owed" ? "text-green-500" : "text-red-500"
										}`}
									>
										{group.type === "owed" ? "+" : "-"}₹
										{Math.abs(group.balance)}
									</span>
								</div>
							</div>
						))}

						<button
							onClick={() => setCurrentView("create-group")}
							className="w-full py-4 bg-(--primary) text-(--primary-foreground) rounded-[2rem] font-bold text-lg shadow-lg hover:opacity-90 active:scale-95 transition-all mt-4 md:mt-0 flex items-center justify-center gap-2 md:col-span-full"
						>
							<Users size={20} /> Create New Group
						</button>
					</div>
				) : (
					/* Add Participant Tab - Anyone can be added */
					<div className="px-6 space-y-6">
						{/* Add by Name/Email */}
						<div className="bg-(--card) border border-(--border) rounded-[2rem] p-6">
							<h3 className="font-bold text-(--foreground) mb-4">
								Add New Participant
							</h3>
							<p className="text-sm text-(--muted-foreground) mb-4">
								Add anyone to split expenses with. They don't need to be on the
								app!
							</p>
							<div className="space-y-3">
								<input
									type="text"
									value={participantName}
									onChange={(e) => setParticipantName(e.target.value)}
									placeholder="Name (e.g., John Doe)"
									className="w-full bg-(--muted) text-(--foreground) px-4 py-3 rounded-2xl border border-(--border) focus:border-(--primary) outline-none transition-all placeholder:text-(--muted-foreground)/50"
								/>
								<input
									type="email"
									value={participantEmail}
									onChange={(e) => setParticipantEmail(e.target.value)}
									placeholder="Email (optional - for invite)"
									className="w-full bg-(--muted) text-(--foreground) px-4 py-3 rounded-2xl border border-(--border) focus:border-(--primary) outline-none transition-all placeholder:text-(--muted-foreground)/50"
								/>
								<button
									onClick={handleAddParticipant}
									disabled={!participantName.trim() || isAddingParticipant}
									className={`w-full py-3 rounded-2xl font-medium transition-all flex items-center justify-center gap-2 ${
										addSuccess
											? "bg-green-500 text-white"
											: !participantName.trim() || isAddingParticipant
											? "bg-(--muted) text-(--muted-foreground) cursor-not-allowed"
											: "bg-(--primary) text-(--primary-foreground) hover:opacity-90"
									}`}
								>
									{isAddingParticipant ? (
										<Loader2 className="animate-spin" size={20} />
									) : addSuccess ? (
										<>
											<Check size={20} /> Added!
										</>
									) : (
										"Add Participant"
									)}
								</button>
							</div>
						</div>

						{/* Or search existing users */}
						<div className="relative">
							<p className="text-sm text-(--muted-foreground) mb-3">
								Or find someone already on Paisa:
							</p>
							<div className="relative">
								<Search
									className="absolute left-4 top-1/2 -translate-y-1/2 text-(--muted-foreground)"
									size={20}
								/>
								<input
									type="text"
									value={searchQuery}
									onChange={(e) => handleSearch(e.target.value)}
									placeholder="Search by name or email..."
									className="w-full bg-(--card) text-(--foreground) pl-12 pr-4 py-4 rounded-[2rem] border border-(--border) focus:border-(--primary) outline-none transition-all placeholder:text-(--muted-foreground)/50"
								/>
							</div>
						</div>

						{isSearching ? (
							<div className="flex justify-center py-8">
								<Loader2 className="animate-spin text-(--muted-foreground)" />
							</div>
						) : searchResults.length > 0 ? (
							<div className="space-y-3">
								{searchResults.map((user) => (
									<div
										key={user.id}
										onClick={() => {
											setSelectedUser({
												id: user.id,
												name: user.name,
												amount: 0,
												type: "owed_to_me",
												date: "New Contact",
											});
											setCurrentView("user-detail");
										}}
										className="flex items-center justify-between p-4 bg-(--card) border border-(--border) rounded-3xl hover:bg-(--muted)/50 transition-all cursor-pointer"
									>
										<div className="flex items-center gap-4">
											<div className="w-12 h-12 rounded-full bg-(--primary)/10 text-(--primary) flex items-center justify-center font-bold text-lg">
												{(user.name || "?").charAt(0)}
											</div>
											<div>
												<h3 className="font-bold text-(--foreground)">
													{user.name || "Unknown User"}
												</h3>
												<p className="text-xs text-(--muted-foreground)">
													{user.email || "No email"}
												</p>
											</div>
										</div>
										<div className="text-xs text-(--primary) bg-(--primary)/10 px-3 py-1 rounded-full">
											On Paisa
										</div>
									</div>
								))}
							</div>
						) : searchQuery ? (
							<div className="text-center py-8">
								<p className="text-(--muted-foreground) mb-4">
									No Paisa users found
								</p>
								<p className="text-sm text-(--muted-foreground)">
									You can still add them manually above!
								</p>
							</div>
						) : null}
					</div>
				)}
			</div>
		</div>
	);
};
