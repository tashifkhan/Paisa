import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { walletService } from '@/services/walletService';
import type { CreateWalletData } from '@/services/walletService';
import { queryKeys } from './queryKeys';

export function useWallets() {
  return useQuery({
    queryKey: queryKeys.wallets,
    queryFn: walletService.getWallets,
  });
}

export function useWalletTotal(currency: string = 'INR') {
  return useQuery({
    queryKey: queryKeys.walletTotal(currency),
    queryFn: () => walletService.getTotalBalance(currency),
  });
}

export function useCreateWallet() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateWalletData) => walletService.createWallet(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.wallets });
      queryClient.invalidateQueries({ queryKey: ['wallets', 'total'] });
    },
  });
}

export function useDeleteWallet() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => walletService.deleteWallet(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.wallets });
      queryClient.invalidateQueries({ queryKey: ['wallets', 'total'] });
    },
  });
}
