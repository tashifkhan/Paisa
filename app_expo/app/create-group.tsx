import { useRouter } from "expo-router";
import { ArrowLeft, Camera, Check, Search } from "lucide-react-native";
import React, { useState } from "react";
import {
	ScrollView,
	Text,
	TextInput,
	TouchableOpacity,
	View,
} from "react-native";

export default function CreateGroupScreen() {
	const router = useRouter();
	const [groupName, setGroupName] = useState("");

	const friends = [
		{ id: 1, name: "Rahul Sharma", selected: false },
		{ id: 2, name: "Anita Roy", selected: false },
		{ id: 3, name: "John Doe", selected: false },
		{ id: 4, name: "Priya Singh", selected: false },
	];

	return (
		<View className="flex-1 bg-[var(--background)]">
			<View className="flex-row justify-between items-center p-6 pt-12">
				<TouchableOpacity
					onPress={() => router.back()}
					className="p-2 bg-[var(--card)] border border-[var(--border)] rounded-full shadow-sm"
				>
					<ArrowLeft size={20} color="var(--foreground)" />
				</TouchableOpacity>
				<Text className="text-xl font-bold text-[var(--foreground)]">
					Create Group
				</Text>
				<TouchableOpacity
					onPress={() => router.back()}
					className={`p-2 rounded-full ${
						groupName ? "bg-[var(--primary)]" : "bg-[var(--muted)]"
					}`}
					disabled={!groupName}
				>
					<Check
						size={20}
						color={groupName ? "white" : "var(--muted-foreground)"}
					/>
				</TouchableOpacity>
			</View>

			<ScrollView className="flex-1 px-6">
				<View className="items-center mb-8">
					<TouchableOpacity className="w-24 h-24 rounded-full bg-[var(--muted)] items-center justify-center mb-4 border-2 border-dashed border-[var(--muted-foreground)]">
						<Camera size={32} color="var(--muted-foreground)" />
					</TouchableOpacity>
					<Text className="text-[var(--primary)] font-medium">
						Add Group Icon
					</Text>
				</View>

				<View className="mb-8">
					<Text className="text-sm font-semibold text-[var(--muted-foreground)] uppercase tracking-wider mb-2 ml-1">
						Group Name
					</Text>
					<TextInput
						className="w-full p-4 bg-[var(--card)] border border-[var(--border)] rounded-2xl text-[var(--foreground)] text-lg"
						placeholder="e.g. Goa Trip"
						placeholderTextColor="var(--muted-foreground)"
						value={groupName}
						onChangeText={setGroupName}
					/>
				</View>

				<View>
					<Text className="text-sm font-semibold text-[var(--muted-foreground)] uppercase tracking-wider mb-4 ml-1">
						Add Members
					</Text>

					<View className="flex-row items-center bg-[var(--card)] border border-[var(--border)] rounded-2xl p-3 mb-4">
						<Search
							size={20}
							color="var(--muted-foreground)"
							className="mr-2"
						/>
						<TextInput
							className="flex-1 text-[var(--foreground)]"
							placeholder="Search friends"
							placeholderTextColor="var(--muted-foreground)"
						/>
					</View>

					<View className="gap-2">
						{friends.map((friend) => (
							<TouchableOpacity
								key={friend.id}
								className="flex-row items-center justify-between p-4 bg-[var(--card)] border border-[var(--border)] rounded-2xl"
							>
								<View className="flex-row items-center gap-3">
									<View className="w-10 h-10 rounded-full bg-[var(--primary)]/10 items-center justify-center">
										<Text className="font-bold text-[var(--primary)]">
											{friend.name.charAt(0)}
										</Text>
									</View>
									<Text className="font-bold text-[var(--foreground)]">
										{friend.name}
									</Text>
								</View>
								<View className="w-6 h-6 rounded-full border-2 border-[var(--muted-foreground)]" />
							</TouchableOpacity>
						))}
					</View>
				</View>
			</ScrollView>
		</View>
	);
}
