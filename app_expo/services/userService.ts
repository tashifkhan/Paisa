import api from '../lib/api';
import type { BackendUser } from './types';

export interface UpdateUserData {
  name?: string;
  currency?: string;
  language?: string;
}

export const userService = {
  async getCurrentUser(): Promise<BackendUser> {
    const response = await api.get<BackendUser>('/users/me');
    return response.data;
  },

  async updateCurrentUser(data: UpdateUserData): Promise<BackendUser> {
    const response = await api.put<BackendUser>('/users/me', data);
    return response.data;
  },

  async searchUsers(query: string): Promise<BackendUser[]> {
    const response = await api.get<BackendUser[]>(`/users/search/?q=${encodeURIComponent(query)}`);
    return response.data;
  },
};
