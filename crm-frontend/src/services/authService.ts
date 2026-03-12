/* ============================================================
   Auth Service – login, register, profile
   ============================================================ */
import api from './api';
import type { ApiResponse, AuthResponse, LoginRequest, RegisterRequest, UserProfile } from '../types';

export const authService = {
  login: (data: LoginRequest) =>
    api.post<ApiResponse<AuthResponse>>('/api/v1/auth/login', data).then((r) => r.data),

  register: (data: RegisterRequest) =>
    api.post<ApiResponse<AuthResponse>>('/api/v1/auth/register', data).then((r) => r.data),

  getProfile: () =>
    api.get<ApiResponse<UserProfile>>('/api/v1/auth/me').then((r) => r.data),
};
