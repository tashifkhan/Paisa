export const queryKeys = {
  profile: ['profile'] as const,
  wallets: ['wallets'] as const,
  walletTotal: (currency: string) => ['wallets', 'total', currency] as const,
  transactions: (filters?: Record<string, string | number | undefined>) =>
    ['transactions', filters ?? {}] as const,
  statsComparison: (days: number) => ['stats', 'comparison', days] as const,
};
