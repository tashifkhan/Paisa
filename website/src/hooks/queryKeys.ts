export const queryKeys = {
  profile: ['profile'] as const,
  wallets: ['wallets'] as const,
  walletTotal: (currency: string) => ['wallets', 'total', currency] as const,
  transactions: (filters?: Record<string, string | number | undefined>) =>
    ['transactions', filters ?? {}] as const,
  statsFull: (days: number) => ['stats', 'full', days] as const,
  statsComparison: (days: number) => ['stats', 'comparison', days] as const,
  categories: ['categories'] as const,
  debtSummary: ['debts', 'summary'] as const,
  debts: ['debts'] as const,
};
