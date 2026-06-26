import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { eventsApi } from '../api/events';
import type { EventBody } from '../api/events';
import type { EventStatus } from '../api/types';

export function useEvents(workspaceId: number, status?: EventStatus) {
  return useQuery({
    queryKey: ['workspace', workspaceId, 'events', status ?? 'all'],
    queryFn: () => eventsApi.list(workspaceId, status),
    enabled: !!workspaceId,
  });
}

export function useEvent(eventId: number) {
  return useQuery({
    queryKey: ['event', eventId],
    queryFn: () => eventsApi.get(eventId),
    enabled: !!eventId,
  });
}

export function useCreateEvent(workspaceId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: EventBody) => eventsApi.create(workspaceId, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['workspace', workspaceId, 'events'] }),
  });
}

export function useUpdateEvent(eventId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: EventBody) => eventsApi.update(eventId, body),
    onSuccess: (event) => {
      qc.invalidateQueries({ queryKey: ['event', eventId] });
      qc.invalidateQueries({ queryKey: ['workspace', event.workspaceId, 'events'] });
    },
  });
}

export function useUpdateEventStatus(eventId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (status: EventStatus) => eventsApi.updateStatus(eventId, status),
    onSuccess: (event) => {
      qc.invalidateQueries({ queryKey: ['event', eventId] });
      qc.invalidateQueries({ queryKey: ['workspace', event.workspaceId, 'events'] });
    },
  });
}

export function useDuplicateEvent(eventId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: { name?: string; copyMembers: boolean }) => eventsApi.duplicate(eventId, body),
    onSuccess: (event) => qc.invalidateQueries({ queryKey: ['workspace', event.workspaceId, 'events'] }),
  });
}

export function useDeleteEvent(workspaceId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (eventId: number) => eventsApi.remove(eventId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['workspace', workspaceId, 'events'] }),
  });
}
