import api from '../lib/api';
import type { BackendCategoryStats, BackendComparison, BackendFullStats, BackendStatsSummary, BackendTrend } from './types';

export const statsService = {
  async getSummary(days: number = 30): Promise<BackendStatsSummary> {
    const response = await api.get<BackendStatsSummary>(`/stats/summary?days=${days}`);
    return response.data;
  },

  async getFullStats(days: number = 30): Promise<BackendFullStats> {
    const response = await api.get<BackendFullStats>(`/stats/full?days=${days}`);
    return response.data;
  },

  async getCategoryBreakdown(days: number = 30, type: string = 'expense'): Promise<{
    period_days: number;
    type: string;
    categories: BackendCategoryStats[];
  }> {
    const response = await api.get(`/stats/category?days=${days}&type=${type}`);
    return response.data;
  },

  async getTrends(months: number = 6): Promise<{ months: number; trends: BackendTrend[] }> {
    const response = await api.get(`/stats/trends?months=${months}`);
    return response.data;
  },

  async getComparison(days: number = 30): Promise<BackendComparison> {
    const response = await api.get<BackendComparison>(`/stats/comparison?days=${days}`);
    return response.data;
  },
};
