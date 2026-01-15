import api from '../lib/api';
import type { BackendTransaction, BackendWallet } from './types';

export interface CreateWalletData {
    name: string;
    type?: string;
    currency?: string;
}

export const walletService = {
    async getWallets(): Promise<BackendWallet[]> {
        const response = await api.get<BackendWallet[]>('/wallets/');
        return response.data;
    },

    async getWallet(id: string): Promise<BackendWallet> {
        const response = await api.get<BackendWallet>(`/wallets/${id}`);
        return response.data;
    },

    async getTotalBalance(currency: string = 'INR'): Promise<{ currency: string; total_balance: number }> {
        const response = await api.get(`/wallets/total?currency=${currency}`);
        return response.data;
    },

    async createWallet(data: CreateWalletData): Promise<{ status: string; id: string }> {
        const response = await api.post('/wallets/', data);
        return response.data;
    },

    async updateWallet(id: string, data: Partial<CreateWalletData>): Promise<BackendWallet> {
        const response = await api.put<BackendWallet>(`/wallets/${id}`, data);
        return response.data;
    },

    async deleteWallet(id: string): Promise<{ status: string }> {
        const response = await api.delete(`/wallets/${id}`);
        return response.data;
    },

    async getWalletTransactions(id: string): Promise<BackendTransaction[]> {
        const response = await api.get<BackendTransaction[]>(`/wallets/${id}/transactions`);
        return response.data;
    },

    async adjustBalance(id: string, amount: number): Promise<{ status: string; old_balance: number; new_balance: number; adjustment: number }> {
        const response = await api.post(`/wallets/${id}/adjust-balance?amount=${amount}`);
        return response.data;
    }
};
