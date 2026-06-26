import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { workspacesApi } from '../api/workspaces';
import type { Role } from '../api/types';

const KEY = ['workspaces'];

export function useWorkspaces() {
  return useQuery({ queryKey: KEY, queryFn: workspacesApi.list });
}

export function useWorkspace(id: number) {
  return useQuery({ queryKey: ['workspace', id], queryFn: () => workspacesApi.get(id), enabled: !!id });
}

export function useCreateWorkspace() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: workspacesApi.create,
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  });
}

export function useDeleteWorkspace() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => workspacesApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  });
}

export function useKnownMembers(workspaceId: number, enabled = true) {
  return useQuery({
    queryKey: ['workspace', workspaceId, 'known-members'],
    queryFn: () => workspacesApi.knownMembers(workspaceId),
    enabled: !!workspaceId && enabled,
  });
}

export function useWorkspaceMembers(id: number) {
  return useQuery({
    queryKey: ['workspace', id, 'members'],
    queryFn: () => workspacesApi.members(id),
    enabled: !!id,
  });
}

export function useAddWorkspaceMember(id: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: { email: string; role: Role }) => workspacesApi.addMember(id, body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['workspace', id, 'members'] });
      qc.invalidateQueries({ queryKey: KEY });
    },
  });
}

export function useRemoveWorkspaceMember(id: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (memberId: number) => workspacesApi.removeMember(id, memberId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['workspace', id, 'members'] });
      qc.invalidateQueries({ queryKey: KEY });
    },
  });
}

export function useUpdateWorkspaceMemberRole(id: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ memberId, role }: { memberId: number; role: Role }) =>
      workspacesApi.updateMemberRole(id, memberId, role),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['workspace', id, 'members'] }),
  });
}
