interface CircularProgressProps {
	value: number;
	max: number;
	color: string;
	size?: number;
	strokeWidth?: number;
}

export const CircularProgress = ({
	value,
	max,
	color,
	size = 60,
	strokeWidth = 6,
}: CircularProgressProps) => {
	const radius = (size - strokeWidth) / 2;
	const circumference = radius * 2 * Math.PI;
	const offset = circumference - (value / max) * circumference;

	return (
		<div
			className="relative flex items-center justify-center"
			style={{ width: size, height: size }}
		>
			<svg width={size} height={size} className="transform -rotate-90">
				<circle
					cx={size / 2}
					cy={size / 2}
					r={radius}
					fill="transparent"
					className="stroke-(--muted)"
					strokeWidth={strokeWidth}
				/>
				<circle
					cx={size / 2}
					cy={size / 2}
					r={radius}
					fill="transparent"
					className={color}
					strokeWidth={strokeWidth}
					strokeDasharray={circumference}
					strokeDashoffset={offset}
					strokeLinecap="round"
				/>
			</svg>
		</div>
	);
};
