import api from '../lib/api';
import type { BackendCategory } from './types';

export interface CreateCategoryData {
    name: string;
    icon?: string;
    color?: string;
    type?: 'expense' | 'income' | 'both';
}

export const categoryService = {
    async getCategories(): Promise<BackendCategory[]> {
        const response = await api.get<BackendCategory[]>('/categories/');
        return response.data;
    },

    async getDefaultCategories(): Promise<BackendCategory[]> {
        const response = await api.get<BackendCategory[]>('/categories/defaults');
        return response.data;
    },

    async getCategory(id: string): Promise<BackendCategory> {
        const response = await api.get<BackendCategory>(`/categories/${id}`);
        return response.data;
    },

    async createCategory(data: CreateCategoryData): Promise<BackendCategory> {
        const response = await api.post<BackendCategory>('/categories/', data);
        return response.data;
    },

    async updateCategory(id: string, data: Partial<CreateCategoryData>): Promise<BackendCategory> {
        const response = await api.put<BackendCategory>(`/categories/${id}`, data);
        return response.data;
    },

    async deleteCategory(id: string): Promise<{ status: string }> {
        const response = await api.delete(`/categories/${id}`);
        return response.data;
    },

    async seedDefaults(): Promise<{ status: string; created: number; total: number }> {
        const response = await api.post('/categories/seed-defaults');
        return response.data;
    }
};
