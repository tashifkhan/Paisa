import api, { setAuthToken } from '../lib/api';
import { storage } from '../lib/storage';

interface LoginResponse {
  access_token: string;
  token_type: string;
  user_id: string;
}

export const authService = {
  async login(email: string, password: string): Promise<LoginResponse> {
    const formData = new FormData();
    formData.append('username', email);
    formData.append('password', password);

    const response = await api.post<LoginResponse>('/auth/login', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });

    if (response.data.access_token) {
      await storage.setToken(response.data.access_token);
      setAuthToken(response.data.access_token);
    }
    return response.data;
  },

  async requestOtp(email: string) {
    return api.post('/auth/request-otp', { email });
  },

  async verifyOtp(email: string, code: string, name?: string, password?: string) {
    const response = await api.post<LoginResponse>('/auth/verify-otp', {
      email,
      code,
      name,
      password,
    });

    if (response.data.access_token) {
      await storage.setToken(response.data.access_token);
      setAuthToken(response.data.access_token);
    }
    return response.data;
  },

  async logout() {
    await storage.removeToken();
    setAuthToken(null);
  },

  async initializeFromStorage() {
    const token = await storage.getToken();
    if (token) {
      setAuthToken(token);
      return token;
    }
    return null;
  },
};
