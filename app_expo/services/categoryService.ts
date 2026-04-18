import api from '../lib/api';
import type { BackendCategory } from './types';

export const categoryService = {
  async getCategories(): Promise<BackendCategory[]> {
    const response = await api.get<BackendCategory[]>('/categories/');
    return response.data;
  },

  async getDefaultCategories(): Promise<BackendCategory[]> {
    const response = await api.get<BackendCategory[]>('/categories/defaults');
    return response.data;
  },
};
