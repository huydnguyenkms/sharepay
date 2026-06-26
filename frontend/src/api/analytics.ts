import { api } from './client';
import type { CategoryRef, DashboardResponse, MemberSummary, Settlement, SummaryResponse } from './types';

export const analyticsApi = {
  dashboard: (eventId: number) =>
    api.get<DashboardResponse>(`/events/${eventId}/dashboard`).then((r) => r.data),

  summary: (eventId: number) =>
    api.get<SummaryResponse>(`/events/${eventId}/summary`).then((r) => r.data),

  membersSummary: (eventId: number) =>
    api.get<MemberSummary[]>(`/events/${eventId}/members-summary`).then((r) => r.data),

  categories: (eventId: number) =>
    api.get<CategoryRef[]>(`/events/${eventId}/categories`).then((r) => r.data),
};

export const settlementApi = {
  get: (eventId: number) =>
    api.get<Settlement>(`/events/${eventId}/settlement`).then((r) => r.data),

  settle: (eventId: number) =>
    api.post<Settlement>(`/events/${eventId}/settlement/settle`).then((r) => r.data),
};

/** Authenticated blob download (token is attached by the axios interceptor). */
export async function downloadExport(eventId: number, format: 'excel' | 'csv') {
  const response = await api.get(`/events/${eventId}/export/${format}`, { responseType: 'blob' });
  const url = URL.createObjectURL(response.data as Blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `event-${eventId}.${format === 'excel' ? 'xlsx' : 'csv'}`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
