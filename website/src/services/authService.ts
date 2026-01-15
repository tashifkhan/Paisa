import api from '../lib/api';

interface LoginResponse {
    access_token: string;
    token_type: string;
    user_id: string;
}

export const authService = {
    async login(email: string, password: string): Promise<LoginResponse> {
        // OAuth2PasswordRequestForm expects form data
        const formData = new FormData();
        formData.append('username', email);
        formData.append('password', password);
        
        const response = await api.post<LoginResponse>('/auth/login', formData, {
            headers: {
                'Content-Type': 'multipart/form-data',
            },
        });
        
        if (response.data.access_token) {
            localStorage.setItem('token', response.data.access_token);
            // Configure axios to use this token for future requests
            api.defaults.headers.common['Authorization'] = `Bearer ${response.data.access_token}`;
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
            password
        });
        
        if (response.data.access_token) {
            localStorage.setItem('token', response.data.access_token);
            api.defaults.headers.common['Authorization'] = `Bearer ${response.data.access_token}`;
        }
        return response.data;
    },

    logout() {
        localStorage.removeItem('token');
        delete api.defaults.headers.common['Authorization'];
    },

    isAuthenticated() {
        return !!localStorage.getItem('token');
    },
    
    getToken() {
        return localStorage.getItem('token');
    }
};

// Initialize token on load
const token = localStorage.getItem('token');
if (token) {
    api.defaults.headers.common['Authorization'] = `Bearer ${token}`;
}
