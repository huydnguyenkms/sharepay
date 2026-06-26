import { api } from './client';
import type { EventMember } from './types';

export interface EventMemberBody {
  displayName: string;
  email?: string | null;
  phone?: string | null;
  userId?: number | null;
}

export const membersApi = {
  list: (eventId: number) =>
    api.get<EventMember[]>(`/events/${eventId}/members`).then((r) => r.data),

  add: (eventId: number, body: EventMemberBody) =>
    api.post<EventMember>(`/events/${eventId}/members`, body).then((r) => r.data),

  update: (eventId: number, memberId: number, body: EventMemberBody) =>
    api.put<EventMember>(`/events/${eventId}/members/${memberId}`, body).then((r) => r.data),

  remove: (eventId: number, memberId: number) =>
    api.delete(`/events/${eventId}/members/${memberId}`).then(() => undefined),
};
