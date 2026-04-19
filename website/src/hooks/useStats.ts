import { useQuery } from '@tanstack/react-query';
import { statsService } from '@/services/statsService';
import { queryKeys } from './queryKeys';

export function useStatsFull(days: number = 30) {
  return useQuery({
    queryKey: queryKeys.statsFull(days),
    queryFn: () => statsService.getFullStats(days),
  });
}

export function useStatsComparison(days: number = 30) {
  return useQuery({
    queryKey: queryKeys.statsComparison(days),
    queryFn: () => statsService.getComparison(days),
  });
}
