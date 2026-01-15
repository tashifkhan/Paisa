import api from '../lib/api';
import type { BackendTransaction } from './types';

export interface TransactionFilters {
    wallet_id?: string;
    category_id?: string;
    type?: string;
    start_date?: string;
    end_date?: string;
    limit?: number;
}

export interface CreateTransactionData {
    amount: number;
    currency?: string;
    type: string;
    date?: string;
    note?: string;
    wallet_id?: string;
    group_id?: string;
    category_id?: string;
    split_strategy?: string;
}

export const expenseService = {
    async getTransactions(filters?: TransactionFilters): Promise<BackendTransaction[]> {
        const params = new URLSearchParams();
        if (filters?.wallet_id) params.append('wallet_id', filters.wallet_id);
        if (filters?.category_id) params.append('category_id', filters.category_id);
        if (filters?.type) params.append('type', filters.type);
        if (filters?.start_date) params.append('start_date', filters.start_date);
        if (filters?.end_date) params.append('end_date', filters.end_date);
        if (filters?.limit) params.append('limit', filters.limit.toString());
        
        const query = params.toString() ? `?${params.toString()}` : '';
        const response = await api.get<BackendTransaction[]>(`/expenses/${query}`);
        return response.data;
    },

    async getTransaction(id: string): Promise<BackendTransaction> {
        const response = await api.get<BackendTransaction>(`/expenses/${id}`);
        return response.data;
    },

    async addTransaction(data: CreateTransactionData): Promise<{ status: string; id: string }> {
        const response = await api.post('/expenses/add', data);
        return response.data;
    },

    async updateTransaction(id: string, data: Partial<CreateTransactionData>): Promise<{ status: string }> {
        const response = await api.put(`/expenses/${id}`, data);
        return response.data;
    },

    async deleteTransaction(id: string): Promise<{ status: string }> {
        const response = await api.delete(`/expenses/${id}`);
        return response.data;
    },

    async exportTransactions(): Promise<Blob> {
        const response = await api.get('/expenses/export', {
            responseType: 'blob'
        });
        return response.data;
    },

    async uploadBill(file: File): Promise<{ url: string }> {
        const formData = new FormData();
        formData.append('file', file);
        const response = await api.post('/expenses/upload-bill', formData, {
            headers: {
                'Content-Type': 'multipart/form-data',
            },
        });
        return response.data;
    }
};
