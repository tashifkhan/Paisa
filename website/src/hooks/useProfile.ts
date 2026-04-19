import { useQuery } from '@tanstack/react-query';
import { userService } from '@/services/userService';
import { queryKeys } from './queryKeys';

export function useProfile() {
  return useQuery({
    queryKey: queryKeys.profile,
    queryFn: userService.getCurrentUser,
  });
}
