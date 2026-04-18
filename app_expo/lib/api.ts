import axios from 'axios';
import Constants from 'expo-constants';
import { Platform } from 'react-native';

// URL resolution order:
//   1. app.json `extra.apiUrl`
//   2. Android emulator default: http://10.0.2.2:8000
//   3. iOS simulator / web default: http://localhost:8000
//
// Notes:
// - Android emulator cannot reach host via localhost; it must use 10.0.2.2.
// - Physical device must use your machine LAN IP (e.g. http://192.168.1.100:8000).
const configUrl = Constants.expoConfig?.extra?.apiUrl as string | undefined;

function resolveApiBaseUrl() {
  const fallback = Platform.OS === 'android' ? 'http://10.0.2.2:8000' : 'http://localhost:8000';
  const raw = (configUrl ?? fallback).replace(/\/$/, '');

  // If android emulator is configured with localhost, rewrite automatically.
  if (Platform.OS === 'android') {
    return raw.replace('://localhost', '://10.0.2.2').replace('://127.0.0.1', '://10.0.2.2');
  }

  return raw;
}

export const API_BASE_URL = resolveApiBaseUrl();

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.code === 'ERR_NETWORK' || error.code === 'ECONNREFUSED') {
      console.error(`API Connection Error: Cannot reach ${API_BASE_URL}. Is the backend running?`);
    }
    return Promise.reject(error);
  }
);

export function setAuthToken(token: string | null) {
  if (token) {
    api.defaults.headers.common['Authorization'] = `Bearer ${token}`;
  } else {
    delete api.defaults.headers.common['Authorization'];
  }
}

export default api;
