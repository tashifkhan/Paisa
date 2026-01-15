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

    async getUser(id: string): Promise<BackendUser> {
        const response = await api.get<BackendUser>(`/users/${id}`);
        return response.data;
    },

    async updatePushToken(token: string): Promise<{ status: string }> {
        const response = await api.put('/users/push-token', { token });
        return response.data;
    }
};
