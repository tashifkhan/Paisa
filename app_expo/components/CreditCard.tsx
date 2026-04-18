import { LinearGradient } from "expo-linear-gradient";
import React from "react";
import { StyleSheet, View } from "react-native";
import { Text } from "react-native-paper";
import { List } from "react-native-paper";

interface CreditCardProps {
	type?: string;
	number?: string;
	holder?: string;
	exp?: string;
	balance?: string;
	gradient?: string;
	isCash?: boolean;
	isVirtual?: boolean;
}

const CreditCardComponent = ({
	type = "VISA",
	number = "9038 4061 **** ****",
	holder = "Tashif Ahmad Khan",
	exp = "02/02",
	balance,
	gradient = "from-[var(--chart-2)] to-[var(--chart-1)]",
	isVirtual = false,
	isCash = false,
}: CreditCardProps) => {
	const getColors = (): [string, string] => {
		if (isCash) return ["#16a34a", "#0f766e"];
		if (isVirtual) return ["#1f2937", "#111827"];
		if (gradient.includes("green")) return ["#16a34a", "#0f766e"];
		if (gradient.includes("8a79ab") || gradient.includes("chart-1"))
			return ["#8a79ab", "#6a5990"];
		if (gradient.includes("chart-3") || gradient.includes("77b8a1"))
			return ["#77b8a1", "#4a9080"];
		if (gradient.includes("chart-4") || gradient.includes("f0c88d"))
			return ["#f0c88d", "#d4964a"];
		if (gradient.includes("chart-5") || gradient.includes("a0bbe3"))
			return ["#a0bbe3", "#6090c8"];
		if (gradient.includes("chart-2") || gradient.includes("e6a5b8"))
			return ["#e6a5b8", "#c87a94"];
		return ["#8a79ab", "#6a5990"];
	};

	const iconName = isCash ? "cash" : isVirtual ? "cpu-64-bit" : "contactless-payment";

	return (
		<LinearGradient
			colors={getColors()}
			start={{ x: 0, y: 0 }}
			end={{ x: 1, y: 1 }}
			style={[styles.card, isVirtual && styles.virtualBorder]}
		>
			{/* Decorative circle */}
			{!isCash && <View style={styles.decorCircle} />}

			{/* Top row: type + icon */}
			<View style={styles.topRow}>
				<View>
					<Text variant="titleMedium" style={styles.whiteText}>
						{type}
					</Text>
					{isVirtual && (
						<Text variant="labelSmall" style={styles.fadedText}>
							Virtual Card
						</Text>
					)}
				</View>
				<List.Icon icon={iconName} color="rgba(255,255,255,0.8)" style={styles.topIcon} />
			</View>

			{/* Body */}
			<View style={styles.body}>
				{isCash ? (
					<>
						<Text variant="labelSmall" style={styles.fadedLabel}>
							BALANCE
						</Text>
						<Text variant="headlineMedium" style={[styles.whiteText, styles.bold]}>
							{balance ?? "–"}
						</Text>
					</>
				) : (
					<>
						{balance && (
							<>
								<Text variant="labelSmall" style={styles.fadedLabel}>
									BALANCE
								</Text>
								<Text variant="titleLarge" style={[styles.whiteText, styles.bold]}>
									{balance}
								</Text>
							</>
						)}
						<Text variant="bodyLarge" style={[styles.whiteText, { opacity: 0.8, letterSpacing: 2 }]}>
							{number}
						</Text>
					</>
				)}

				<View style={styles.bottomRow}>
					<View>
						<Text variant="labelSmall" style={styles.fadedLabel}>
							CARD HOLDER
						</Text>
						<Text variant="bodyMedium" style={[styles.whiteText, { fontWeight: "600" }]}>
							{holder}
						</Text>
					</View>
					<View style={{ alignItems: "flex-end" }}>
						<Text variant="labelSmall" style={styles.fadedLabel}>
							EXP DATE
						</Text>
						<Text variant="bodyMedium" style={[styles.whiteText, { fontWeight: "600" }]}>
							{exp}
						</Text>
					</View>
				</View>
			</View>

			{/* NFC indicator for card types */}
			{(type === "VISA" || type === "Mastercard") && !isVirtual && (
				<View style={styles.nfcRow}>
					<List.Icon icon="nfc" color="rgba(255,255,255,0.5)" style={styles.nfcIcon} />
					<View style={styles.chip}>
						<View style={styles.chipHalf} />
					</View>
				</View>
			)}

			{/* Cash banknote watermark */}
			{isCash && (
				<View style={styles.cashWatermark}>
					<List.Icon icon="cash-multiple" color="rgba(255,255,255,0.15)" style={styles.watermarkIcon} />
				</View>
			)}
		</LinearGradient>
	);
};

const styles = StyleSheet.create({
	card: {
		width: "100%",
		height: 224,
		borderRadius: 24,
		padding: 24,
		justifyContent: "space-between",
		overflow: "hidden",
		marginBottom: 16,
	},
	virtualBorder: {
		borderWidth: 2,
		borderColor: "rgba(255,255,255,0.3)",
	},
	decorCircle: {
		position: "absolute",
		top: -40,
		right: -40,
		width: 128,
		height: 128,
		borderRadius: 64,
		backgroundColor: "rgba(255,255,255,0.15)",
	},
	topRow: {
		flexDirection: "row",
		justifyContent: "space-between",
		alignItems: "flex-start",
		zIndex: 10,
	},
	topIcon: {
		margin: 0,
	},
	body: {
		flex: 1,
		justifyContent: "flex-end",
		zIndex: 10,
		gap: 4,
	},
	bottomRow: {
		flexDirection: "row",
		justifyContent: "space-between",
		alignItems: "flex-end",
		marginTop: 8,
	},
	whiteText: {
		color: "#fff",
	},
	bold: {
		fontWeight: "800",
	},
	fadedText: {
		color: "rgba(255,255,255,0.7)",
	},
	fadedLabel: {
		color: "rgba(255,255,255,0.6)",
		fontWeight: "600",
		letterSpacing: 1,
	},
	nfcRow: {
		position: "absolute",
		bottom: 24,
		right: 24,
		flexDirection: "row",
		alignItems: "center",
		gap: 6,
		zIndex: 10,
	},
	nfcIcon: {
		margin: 0,
	},
	chip: {
		width: 48,
		height: 32,
		backgroundColor: "rgba(255,255,255,0.2)",
		borderRadius: 4,
		flexDirection: "row",
		overflow: "hidden",
		borderWidth: 1,
		borderColor: "rgba(255,255,255,0.1)",
	},
	chipHalf: {
		width: "50%",
		height: "100%",
		borderRightWidth: 1,
		borderRightColor: "rgba(255,255,255,0.2)",
	},
	cashWatermark: {
		position: "absolute",
		bottom: 16,
		right: 24,
		zIndex: 10,
		opacity: 1,
	},
	watermarkIcon: {
		margin: 0,
	},
});

export default CreditCardComponent;
