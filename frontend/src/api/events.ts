import { api } from './client';
import type { Event, EventStatus, EventType } from './types';

export interface EventBody {
  name: string;
  description?: string | null;
  type: EventType;
  currency: string;
  startDate?: string | null;
  endDate?: string | null;
}

export const eventsApi = {
  list: (workspaceId: number, status?: EventStatus) =>
    api
      .get<Event[]>(`/workspaces/${workspaceId}/events`, { params: status ? { status } : {} })
      .then((r) => r.data),

  get: (eventId: number) => api.get<Event>(`/events/${eventId}`).then((r) => r.data),

  create: (workspaceId: number, body: EventBody) =>
    api.post<Event>(`/workspaces/${workspaceId}/events`, body).then((r) => r.data),

  update: (eventId: number, body: EventBody) =>
    api.put<Event>(`/events/${eventId}`, body).then((r) => r.data),

  updateStatus: (eventId: number, status: EventStatus) =>
    api.patch<Event>(`/events/${eventId}/status`, { status }).then((r) => r.data),

  duplicate: (eventId: number, body: { name?: string; copyMembers: boolean }) =>
    api.post<Event>(`/events/${eventId}/duplicate`, body).then((r) => r.data),

  remove: (eventId: number) => api.delete(`/events/${eventId}`).then(() => undefined),
};
