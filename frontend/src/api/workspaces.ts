import { api } from './client';
import type { KnownMember, Role, Workspace, WorkspaceMember } from './types';

export const workspacesApi = {
  list: () => api.get<Workspace[]>('/workspaces').then((r) => r.data),

  get: (id: number) => api.get<Workspace>(`/workspaces/${id}`).then((r) => r.data),

  create: (body: { name: string; description?: string | null }) =>
    api.post<Workspace>('/workspaces', body).then((r) => r.data),

  update: (id: number, body: { name: string; description?: string | null }) =>
    api.put<Workspace>(`/workspaces/${id}`, body).then((r) => r.data),

  remove: (id: number) => api.delete(`/workspaces/${id}`).then(() => undefined),

  members: (id: number) =>
    api.get<WorkspaceMember[]>(`/workspaces/${id}/members`).then((r) => r.data),

  knownMembers: (id: number) =>
    api.get<KnownMember[]>(`/workspaces/${id}/known-members`).then((r) => r.data),

  addMember: (id: number, body: { email: string; role: Role }) =>
    api.post<WorkspaceMember>(`/workspaces/${id}/members`, body).then((r) => r.data),

  updateMemberRole: (id: number, memberId: number, role: Role) =>
    api.put<WorkspaceMember>(`/workspaces/${id}/members/${memberId}`, { role }).then((r) => r.data),

  removeMember: (id: number, memberId: number) =>
    api.delete(`/workspaces/${id}/members/${memberId}`).then(() => undefined),
};
