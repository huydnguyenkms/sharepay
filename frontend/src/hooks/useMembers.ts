import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { membersApi } from '../api/members';
import type { EventMemberBody } from '../api/members';
import { errorMessage } from '../api/client';

export function useMembers(eventId: number) {
  return useQuery({
    queryKey: ['event', eventId, 'members'],
    queryFn: () => membersApi.list(eventId),
    enabled: !!eventId,
  });
}

function invalidateMembers(qc: ReturnType<typeof useQueryClient>, eventId: number) {
  qc.invalidateQueries({ queryKey: ['event', eventId, 'members'] });
  qc.invalidateQueries({ queryKey: ['event', eventId, 'dashboard'] });
  qc.invalidateQueries({ queryKey: ['event', eventId] });
}

export function useAddMember(eventId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: EventMemberBody) => membersApi.add(eventId, body),
    onSuccess: () => invalidateMembers(qc, eventId),
  });
}

/** Adds several members in one action, refreshing the event once at the end. */
export function useAddMembers(eventId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (bodies: EventMemberBody[]) => {
      for (const body of bodies) {
        await membersApi.add(eventId, body);
      }
    },
    onSuccess: () => invalidateMembers(qc, eventId),
  });
}

export function useUpdateMember(eventId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ memberId, body }: { memberId: number; body: EventMemberBody }) =>
      membersApi.update(eventId, memberId, body),
    onSuccess: () => invalidateMembers(qc, eventId),
  });
}

export function useRemoveMember(eventId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (memberId: number) => membersApi.remove(eventId, memberId),
    onSuccess: () => invalidateMembers(qc, eventId),
  });
}

export interface BulkRemoveResult {
  total: number;
  failed: { id: number; message: string }[];
}

/** Removes several members at once. Members that can't be removed (they have transactions)
 *  are skipped and reported, so one failure doesn't abort the rest. */
export function useRemoveMembers(eventId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (memberIds: number[]): Promise<BulkRemoveResult> => {
      const failed: { id: number; message: string }[] = [];
      for (const id of memberIds) {
        try {
          await membersApi.remove(eventId, id);
        } catch (err) {
          failed.push({ id, message: errorMessage(err) });
        }
      }
      return { total: memberIds.length, failed };
    },
    onSuccess: () => invalidateMembers(qc, eventId),
  });
}
