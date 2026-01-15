import axios from 'axios';

// In production, you need to set VITE_API_URL to your deployed backend URL
// For Vercel, set this in Project Settings > Environment Variables
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8000',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000, // 10 second timeout
});

// Add response interceptor for better error handling
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // If API is not reachable (e.g., backend not deployed), 
    // provide a clearer error
    if (error.code === 'ERR_NETWORK' || error.code === 'ECONNREFUSED') {
      console.error('API Connection Error: Backend server is not reachable.');
      console.error('Make sure VITE_API_URL is set to your production backend URL.');
    }
    return Promise.reject(error);
  }
);

export default api;
