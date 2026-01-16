import {
	ArrowLeft,
	Bell,
	CircleHelp,
	Database,
	DollarSign,
	Languages,
	LogOut,
	Moon,
	Shield,
	User,
} from "lucide-react";
import { SettingItem } from "../shared/SettingItem";

interface ProfileViewProps {
	isDarkMode: boolean;
	toggleTheme: () => void;
	notifications: boolean;
	setNotifications: (value: boolean) => void;
	currency: string;
	setCurrency: (value: string) => void;
	language: string;
	setLanguage: (value: string) => void;
	setCurrentView: (view: string) => void;
}

export const ProfileView = ({
	isDarkMode,
	toggleTheme,
	notifications,
	setNotifications,
	currency,
	setCurrency,
	language,
	setLanguage,
	setCurrentView,
}: ProfileViewProps) => (
	<div className="flex flex-col h-full bg-(--background) pb-24 md:pb-6 overflow-y-auto hide-scrollbar transition-colors duration-300">
		<div className="max-w-5xl mx-auto w-full">
			<header className="flex justify-between items-center p-6 bg-transparent">
				<button
					onClick={() => setCurrentView("home")}
					className="p-2 bg-(--card) border border-(--border) rounded-full text-(--foreground) shadow-sm"
				>
					<ArrowLeft size={20} />
				</button>
				<h1 className="text-xl font-bold text-(--foreground)">Profile</h1>
				<div className="w-10"></div> {/* Spacer for centering */}
			</header>

			{/* Avatar Section */}
			<div className="flex flex-col items-center justify-center mb-8">
				<div className="w-28 h-28 rounded-full bg-(--chart-2) p-1 mb-4 shadow-lg">
					<div className="w-full h-full rounded-full bg-(--card) flex items-center justify-center overflow-hidden">
						<User size={48} className="text-(--foreground) opacity-50" />
					</div>
				</div>
				<h2 className="text-2xl font-bold text-(--foreground)">
					Tashif Ahmad Khan
				</h2>
				<p className="text-(--muted-foreground)">admin@tashif.codes</p>
			</div>

			{/* Settings List */}
			<div className="px-6 md:grid md:grid-cols-3 md:gap-8 md:items-start space-y-8 md:space-y-0">
				{/* General Section */}
				<div>
					<h3 className="text-sm font-semibold text-(--muted-foreground) uppercase tracking-wider mb-4 ml-1">
						General
					</h3>
					<SettingItem
						icon={Languages}
						title="Language"
						value={language}
						onClick={() =>
							setLanguage(language === "English" ? "Hindi" : "English")
						}
					/>
					<SettingItem
						icon={DollarSign}
						title="Currency"
						value={currency}
						onClick={() => setCurrency(currency === "INR" ? "USD" : "INR")}
					/>
					<SettingItem
						icon={Moon}
						title="Dark Mode"
						type="toggle"
						isToggled={isDarkMode}
						onClick={toggleTheme}
					/>
					<SettingItem
						icon={Database}
						title="Data Management"
						value="Import/Export"
						onClick={() => setCurrentView("data-management")}
					/>
				</div>

				{/* Notifications Section */}
				<div>
					<h3 className="text-sm font-semibold text-(--muted-foreground) uppercase tracking-wider mb-4 ml-1">
						Notifications
					</h3>
					<SettingItem
						icon={Bell}
						title="Push Notifications"
						type="toggle"
						isToggled={notifications}
						onClick={() => setNotifications(!notifications)}
					/>
					<SettingItem icon={Shield} title="Security Alerts" type="arrow" />
				</div>

				{/* Support Section */}
				<div>
					<h3 className="text-sm font-semibold text-(--muted-foreground) uppercase tracking-wider mb-4 ml-1">
						Support
					</h3>
					<SettingItem icon={CircleHelp} title="Help & Support" />
					<button className="w-full flex items-center justify-start p-4 gap-4 bg-(--destructive)/10 text-(--destructive) rounded-2xl mt-4 hover:bg-(--destructive)/20 transition-all">
						<div className="w-10 h-10 rounded-full bg-(--destructive)/20 flex items-center justify-center">
							<LogOut size={20} />
						</div>
						<span className="font-bold">Log Out</span>
					</button>
				</div>
			</div>
		</div>
	</div>
);
