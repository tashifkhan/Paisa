import { useQuery } from '@tanstack/react-query';
import { expenseService } from '@/services/expenseService';
import type { TransactionFilters } from '@/services/expenseService';
import { queryKeys } from './queryKeys';

function normalizeFilters(filters?: TransactionFilters) {
  if (!filters) return {};
  return Object.fromEntries(
    Object.entries(filters).filter(([, value]) => value !== undefined)
  ) as Record<string, string | number>;
}

export function useTransactions(filters?: TransactionFilters) {
  const normalized = normalizeFilters(filters);
  return useQuery({
    queryKey: queryKeys.transactions(normalized),
    queryFn: () => expenseService.getTransactions(filters),
  });
}
