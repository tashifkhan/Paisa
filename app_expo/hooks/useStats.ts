import { useQuery } from '@tanstack/react-query';
import { statsService } from '@/services/statsService';
import { queryKeys } from './queryKeys';

export function useStatsFull(days: number = 30) {
  return useQuery({
    queryKey: ['stats', 'full', days] as const,
    queryFn: () => statsService.getFullStats(days),
  });
}

export function useStatsCategory(days: number = 30, type: 'expense' | 'income' = 'expense') {
  return useQuery({
    queryKey: ['stats', 'category', days, type] as const,
    queryFn: () => statsService.getCategoryBreakdown(days, type),
  });
}

export function useStatsComparison(days: number = 30) {
  return useQuery({
    queryKey: queryKeys.statsComparison(days),
    queryFn: () => statsService.getComparison(days),
  });
}

export function useStatsTrends(months: number = 12) {
  return useQuery({
    queryKey: ['stats', 'trends', months] as const,
    queryFn: () => statsService.getTrends(months),
  });
}
