import { useQuery } from '@tanstack/react-query';
import { categoryService } from '@/services/categoryService';
import { queryKeys } from './queryKeys';

export function useCategories() {
  return useQuery({
    queryKey: queryKeys.categories,
    queryFn: categoryService.getCategories,
  });
}
