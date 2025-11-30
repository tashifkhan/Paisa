export const ExpenseChart = () => {
	const data = [
		{ label: "1", height: "60%", color: "bg-(--chart-1)", text: "12%" },
		{ label: "5", height: "20%", color: "bg-(--chart-2)", text: "3%" },
		{ label: "10", height: "30%", color: "bg-(--chart-3)", text: "5%" },
		{ label: "15", height: "90%", color: "bg-(--chart-4)", text: "32%" },
		{ label: "20", height: "70%", color: "bg-(--chart-5)", text: "21%" },
		{ label: "25", height: "40%", color: "bg-(--chart-1)", text: "7%" },
		{ label: "31", height: "50%", color: "bg-(--chart-2)", text: "13%" },
		{ label: "1", height: "35%", color: "bg-(--chart-3)", text: "5%" },
	];

	return (
		<div className="w-full h-64 flex items-end justify-between px-2 mt-6 mb-2">
			{data.map((item, index) => (
				<div
					key={index}
					className="flex flex-col items-center justify-end h-full w-8 gap-2"
				>
					<span className="text-xs font-semibold text-(--muted-foreground) mb-1">
						{item.text}
					</span>
					<div
						className={`w-4 rounded-full ${item.color} transition-colors duration-300`}
						style={{
							height: item.height,
							transition: "height 0.5s ease-in-out",
						}}
					></div>
					<span className="text-xs text-(--muted-foreground) mt-2">
						{item.label}
					</span>
				</div>
			))}
		</div>
	);
};
