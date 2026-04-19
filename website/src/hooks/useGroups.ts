import { useQuery } from '@tanstack/react-query';
import { groupService } from '@/services/groupService';

export function useGroups() {
  return useQuery({
    queryKey: ['groups'],
    queryFn: groupService.getGroups,
  });
}
