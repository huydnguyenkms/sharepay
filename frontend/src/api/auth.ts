import { api } from './client';
import type { AuthResponse, User } from './types';

export const authApi = {
  register: (body: { email: string; password: string; displayName: string }) =>
    api.post<AuthResponse>('/auth/register', body).then((r) => r.data),

  login: (body: { email: string; password: string }) =>
    api.post<AuthResponse>('/auth/login', body).then((r) => r.data),

  me: () => api.get<User>('/auth/me').then((r) => r.data),
};
