import {
	Card,
	CardContent,
	CardDescription,
	CardHeader,
	CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import {
	Select,
	SelectContent,
	SelectItem,
	SelectTrigger,
	SelectValue,
} from "@/components/ui/select";
import { dataService } from "@/services/dataService";
import {
	ArrowLeft,
	ChevronDown,
	ChevronUp,
	Copy,
	Download,
	FileJson,
	FileType,
	MoreHorizontal,
	Upload,
} from "lucide-react";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";

const LLM_PROMPT = `Generate a JSON array of financial transactions for import into a personal finance app. Each transaction object should have the following structure:

{
  "date": "YYYY-MM-DDTHH:MM:SS",  // ISO 8601 format, required
  "amount": 150.00,               // Number, required (positive value)
  "currency": "INR",              // String, optional (default: "INR")
  "type": "expense",              // "expense" or "income", required
  "note": "Description here",     // String, optional
  "category_id": null,            // UUID string or null, optional
  "wallet_id": null,              // UUID string or null, optional
  "group_id": null                // UUID string or null, optional
}

Example output:
[
  {
    "date": "2026-01-15T10:30:00",
    "amount": 500,
    "currency": "INR",
    "type": "expense",
    "note": "Groceries from supermarket"
  },
  {
    "date": "2026-01-14T09:00:00",
    "amount": 25000,
    "currency": "INR",
    "type": "income",
    "note": "Monthly salary"
  }
]

Please generate transactions based on the user's input. Only output the JSON array, no additional text.`;

export default function DataManagement() {
	const navigate = useNavigate();
	const [exportFormat, setExportFormat] = useState<"csv" | "json">("csv");
	const [isExporting, setIsExporting] = useState(false);
	const [isImporting, setIsImporting] = useState(false);
	const [importFile, setImportFile] = useState<File | null>(null);
	const [showDetails, setShowDetails] = useState(false);

	const handleExport = async () => {
		setIsExporting(true);
		try {
			await dataService.exportData(exportFormat);
			toast.success("Export started successfully");
		} catch (error) {
			toast.error("Failed to export data");
			console.error(error);
		} finally {
			setIsExporting(false);
		}
	};

	const handleImport = async () => {
		if (!importFile) return;

		setIsImporting(true);
		try {
			const result = await dataService.importData(importFile);
			toast.success(result.message || "Import completed successfully");
			setImportFile(null);
		} catch (error) {
			toast.error("Failed to import data");
			console.error(error);
		} finally {
			setIsImporting(false);
		}
	};

	const copyLLMPrompt = () => {
		navigator.clipboard.writeText(LLM_PROMPT);
		toast.success("LLM prompt copied to clipboard!");
	};

	return (
		<div className="flex flex-col h-full bg-(--background) pb-24 md:pb-6 overflow-y-auto hide-scrollbar transition-colors duration-300">
			<div className="max-w-5xl mx-auto w-full">
				<header className="flex justify-between items-center p-6">
					<div className="flex items-center gap-4">
						<button
							onClick={() => navigate(-1)}
							className="p-2 -ml-2 hover:bg-(--muted) rounded-full transition-colors text-(--foreground)"
						>
							<ArrowLeft size={24} />
						</button>
						<div className="flex flex-col">
							<h1 className="text-3xl font-bold text-(--foreground)">
								Data Management
							</h1>
							<p className="text-(--muted-foreground) text-sm">
								Backup & Restoration
							</p>
						</div>
					</div>
					<button className="p-2 bg-(--card) border border-(--border) rounded-full text-(--foreground) shadow-sm">
						<MoreHorizontal size={20} />
					</button>
				</header>

				<div className="px-6 grid gap-6 md:grid-cols-2">
					<Card className="rounded-4xl border-(--border) bg-(--card) shadow-sm">
						<CardHeader>
							<CardTitle className="flex items-center gap-2 text-xl text-(--foreground)">
								<div className="p-2 rounded-full bg-(--primary)/10 text-(--primary)">
									<Download className="h-5 w-5" />
								</div>
								Export Data
							</CardTitle>
							<CardDescription className="text-(--muted-foreground)">
								Download your transactions in CSV or JSON format.
							</CardDescription>
						</CardHeader>
						<CardContent className="space-y-6">
							<div className="space-y-2">
								<div className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70 text-(--foreground)">
									Format
								</div>
								<Select
									value={exportFormat}
									onValueChange={(v) => setExportFormat(v as "csv" | "json")}
								>
									<SelectTrigger className="rounded-xl h-12 bg-(--card) text-(--foreground) border-(--border)">
										<SelectValue />
									</SelectTrigger>
									<SelectContent className="bg-(--card) text-(--foreground) border-(--border)">
										<SelectItem
											value="csv"
											className="focus:bg-(--muted) focus:text-(--foreground)"
										>
											<div className="flex items-center gap-2">
												<FileType className="h-4 w-4" />
												<span>CSV (Comma Separated)</span>
											</div>
										</SelectItem>
										<SelectItem
											value="json"
											className="focus:bg-(--muted) focus:text-(--foreground)"
										>
											<div className="flex items-center gap-2">
												<FileJson className="h-4 w-4" />
												<span>JSON (JavaScript Object)</span>
											</div>
										</SelectItem>
									</SelectContent>
								</Select>
							</div>
							<button
								onClick={handleExport}
								disabled={isExporting}
								className="w-full py-4 bg-(--primary) text-(--primary-foreground) rounded-4xl font-bold text-lg shadow-lg hover:opacity-90 active:scale-95 transition-all flex items-center justify-center gap-2 disabled:opacity-70 disabled:pointer-events-none"
							>
								<Download className="h-5 w-5" />
								{isExporting ? "Exporting..." : "Export Transactions"}
							</button>
						</CardContent>
					</Card>

					<Card className="rounded-4xl border-(--border) bg-(--card) shadow-sm">
						<CardHeader>
							<CardTitle className="flex items-center gap-2 text-xl text-(--foreground)">
								<div className="p-2 rounded-full bg-(--primary)/10 text-(--primary)">
									<Upload className="h-5 w-5" />
								</div>
								Import Data
							</CardTitle>
							<CardDescription className="text-(--muted-foreground)">
								Upload a CSV or JSON file to import transactions.
							</CardDescription>
						</CardHeader>
						<CardContent className="space-y-6">
							<div className="space-y-2">
								<div className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70 text-(--foreground)">
									File
								</div>
								<Input
									id="file-upload"
									type="file"
									accept=".csv,.json"
									className="rounded-xl h-12 pt-2 bg-(--card) text-(--foreground) border-(--border) file:bg-(--primary) file:text-(--primary-foreground) file:rounded-full file:border-0 file:mr-4 file:px-4 file:text-sm file:font-semibold hover:file:bg-(--primary)/90"
									onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
										setImportFile(e.target.files?.[0] || null)
									}
								/>
							</div>
							<button
								onClick={handleImport}
								disabled={!importFile || isImporting}
								className="w-full py-4 bg-(--primary) text-(--primary-foreground) rounded-4xl font-bold text-lg shadow-lg hover:opacity-90 active:scale-95 transition-all flex items-center justify-center gap-2 disabled:opacity-70 disabled:pointer-events-none"
							>
								<Upload className="h-5 w-5" />
								{isImporting ? "Importing..." : "Import Transactions"}
							</button>
						</CardContent>
					</Card>
				</div>

				{/* Import Instructions */}
				<div className="px-6 mt-6 space-y-4">
					<Card className="rounded-4xl border-(--border) bg-(--muted)/50 shadow-none">
						<CardHeader className="pb-2">
							<button
								onClick={() => setShowDetails(!showDetails)}
								className="flex items-center justify-between w-full text-left"
							>
								<CardTitle className="text-lg text-(--foreground)">
									Import Instructions
								</CardTitle>
								{showDetails ? (
									<ChevronUp className="h-5 w-5 text-(--muted-foreground)" />
								) : (
									<ChevronDown className="h-5 w-5 text-(--muted-foreground)" />
								)}
							</button>
						</CardHeader>
						<CardContent className="space-y-4">
							{/* Basic info always visible */}
							<ul className="list-disc list-inside text-sm text-(--muted-foreground) space-y-1">
								<li>ID fields are ignored to create new records.</li>
								<li>
									Date format should be ISO (YYYY-MM-DD or YYYY-MM-DDTHH:MM:SS).
								</li>
								<li>
									Required fields:{" "}
									<code className="px-1 py-0.5 bg-(--muted) rounded text-(--foreground)">
										amount
									</code>
									,{" "}
									<code className="px-1 py-0.5 bg-(--muted) rounded text-(--foreground)">
										type
									</code>{" "}
									(expense/income).
								</li>
							</ul>

							{/* Expandable detailed section */}
							{showDetails && (
								<div className="space-y-4 pt-4 border-t border-(--border)">
									<div>
										<h4 className="text-sm font-semibold text-(--foreground) mb-2">
											JSON Structure
										</h4>
										<pre className="text-xs bg-(--background) p-4 rounded-xl overflow-x-auto text-(--foreground) border border-(--border)">
											{`[
  {
    "date": "2026-01-15T10:30:00",
    "amount": 500,
    "currency": "INR",
    "type": "expense",
    "note": "Groceries",
    "category_id": null,
    "wallet_id": null,
    "group_id": null
  }
]`}
										</pre>
									</div>

									<div>
										<h4 className="text-sm font-semibold text-(--foreground) mb-2">
											Field Reference
										</h4>
										<div className="text-xs text-(--muted-foreground) space-y-1">
											<p>
												<strong className="text-(--foreground)">date:</strong>{" "}
												ISO 8601 format (required)
											</p>
											<p>
												<strong className="text-(--foreground)">amount:</strong>{" "}
												Positive number (required)
											</p>
											<p>
												<strong className="text-(--foreground)">type:</strong>{" "}
												"expense" or "income" (required)
											</p>
											<p>
												<strong className="text-(--foreground)">
													currency:
												</strong>{" "}
												Currency code, default "INR"
											</p>
											<p>
												<strong className="text-(--foreground)">note:</strong>{" "}
												Description of transaction
											</p>
											<p>
												<strong className="text-(--foreground)">
													category_id:
												</strong>{" "}
												UUID of category (optional)
											</p>
											<p>
												<strong className="text-(--foreground)">
													wallet_id:
												</strong>{" "}
												UUID of wallet (optional)
											</p>
											<p>
												<strong className="text-(--foreground)">
													group_id:
												</strong>{" "}
												UUID of group (optional)
											</p>
										</div>
									</div>

									<div>
										<h4 className="text-sm font-semibold text-(--foreground) mb-2">
											CSV Format
										</h4>
										<p className="text-xs text-(--muted-foreground) mb-2">
											Headers: date, amount, currency, type, category, wallet,
											group, note
										</p>
									</div>
								</div>
							)}
						</CardContent>
					</Card>

					{/* LLM Prompt Copy Card */}
					<Card className="rounded-4xl border-(--border) bg-(--card) shadow-sm">
						<CardHeader className="pb-2">
							<CardTitle className="text-lg text-(--foreground) flex items-center gap-2">
								<div className="p-2 rounded-full bg-(--chart-4)/10 text-(--chart-4)">
									<Copy className="h-4 w-4" />
								</div>
								Generate with AI
							</CardTitle>
							<CardDescription className="text-(--muted-foreground)">
								Use an LLM to generate transaction data in the correct format.
							</CardDescription>
						</CardHeader>
						<CardContent>
							<button
								onClick={copyLLMPrompt}
								className="w-full py-3 bg-(--muted) text-(--foreground) rounded-2xl font-medium hover:bg-(--muted)/80 active:scale-98 transition-all flex items-center justify-center gap-2"
							>
								<Copy className="h-4 w-4" />
								Copy LLM Prompt
							</button>
							<p className="text-xs text-(--muted-foreground) mt-3 text-center">
								Paste this prompt into ChatGPT, Claude, or any LLM, then
								describe your transactions.
							</p>
						</CardContent>
					</Card>
				</div>
			</div>
		</div>
	);
}
