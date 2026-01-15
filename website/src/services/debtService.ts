import api from '../lib/api';
import type { BackendDebt, BackendDebtSummary } from './types';

export interface CreateDebtData {
    counterparty_name: string;
    amount: number;
    type: 'owed_to_me' | 'owed_by_me';
    due_date?: string;
}

export const debtService = {
    async getDebts(): Promise<BackendDebt[]> {
        const response = await api.get<BackendDebt[]>('/debts/');
        return response.data;
    },

    async getDebt(id: string): Promise<BackendDebt> {
        const response = await api.get<BackendDebt>(`/debts/${id}`);
        return response.data;
    },

    async getSummary(): Promise<BackendDebtSummary> {
        const response = await api.get<BackendDebtSummary>('/debts/summary');
        return response.data;
    },

    async getDebtsOwedToMe(): Promise<BackendDebt[]> {
        const response = await api.get<BackendDebt[]>('/debts/owed-to-me');
        return response.data;
    },

    async getDebtsOwedByMe(): Promise<BackendDebt[]> {
        const response = await api.get<BackendDebt[]>('/debts/owed-by-me');
        return response.data;
    },

    async addDebt(data: CreateDebtData): Promise<BackendDebt> {
        const response = await api.post<BackendDebt>('/debts/', data);
        return response.data;
    },

    async updateDebt(id: string, data: Partial<CreateDebtData>): Promise<BackendDebt> {
        const response = await api.put<BackendDebt>(`/debts/${id}`, data);
        return response.data;
    },
    
    async deleteDebt(id: string): Promise<{ status: string }> {
        const response = await api.delete(`/debts/${id}`);
        return response.data;
    },

    async settleDebt(id: string): Promise<{ status: string; amount: number; counterparty: string }> {
        const response = await api.post(`/debts/${id}/settle`);
        return response.data;
    }
};
