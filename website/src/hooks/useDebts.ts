import { useQuery } from '@tanstack/react-query';
import { debtService } from '@/services/debtService';
import { queryKeys } from './queryKeys';

export function useDebtSummary() {
  return useQuery({
    queryKey: queryKeys.debtSummary,
    queryFn: debtService.getSummary,
  });
}

export function useDebts() {
  return useQuery({
    queryKey: queryKeys.debts,
    queryFn: debtService.getDebts,
  });
}
