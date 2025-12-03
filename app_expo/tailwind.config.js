/** @type {import('tailwindcss').Config} */
module.exports = {
	// NOTE: Update this to include the paths to all of your component files.
	content: ["./app/**/*.{js,jsx,ts,tsx}", "./components/**/*.{js,jsx,ts,tsx}"],
	presets: [require("nativewind/preset")],
	theme: {
		extend: {
			colors: {
				background: "var(--background)",
				foreground: "var(--foreground)",
				card: "var(--card)",
				"card-foreground": "var(--card-foreground)",
				popover: "var(--popover)",
				"popover-foreground": "var(--popover-foreground)",
				primary: "var(--primary)",
				"primary-foreground": "var(--primary-foreground)",
				secondary: "var(--secondary)",
				"secondary-foreground": "var(--secondary-foreground)",
				muted: "var(--muted)",
				"muted-foreground": "var(--muted-foreground)",
				accent: "var(--accent)",
				"accent-foreground": "var(--accent-foreground)",
				destructive: "var(--destructive)",
				"destructive-foreground": "var(--destructive-foreground)",
				border: "var(--border)",
				input: "var(--input)",
				ring: "var(--ring)",
				"chart-1": "var(--chart-1)",
				"chart-2": "var(--chart-2)",
				"chart-3": "var(--chart-3)",
				"chart-4": "var(--chart-4)",
				"chart-5": "var(--chart-5)",
			},
		},
	},
	plugins: [],
};
