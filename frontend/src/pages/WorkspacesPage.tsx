import { useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardActionArea,
  CardContent,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Grid,
  IconButton,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import GroupsIcon from '@mui/icons-material/Groups';
import PeopleIcon from '@mui/icons-material/People';
import DeleteIcon from '@mui/icons-material/Delete';
import { useNavigate } from 'react-router-dom';
import { useSnackbar } from 'notistack';
import {
  useAddWorkspaceMember,
  useCreateWorkspace,
  useRemoveWorkspaceMember,
  useUpdateWorkspaceMemberRole,
  useWorkspaceMembers,
  useWorkspaces,
} from '../hooks/useWorkspaces';
import type { Role, Workspace } from '../api/types';
import { errorMessage } from '../api/client';
import EmptyState from '../components/EmptyState';

const ROLES: Role[] = ['OWNER', 'ADMIN', 'MEMBER', 'VIEWER'];

export default function WorkspacesPage() {
  const { data, isLoading } = useWorkspaces();
  const [createOpen, setCreateOpen] = useState(false);
  const [membersFor, setMembersFor] = useState<Workspace | null>(null);
  const navigate = useNavigate();

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 3 }}>
        <Typography variant="h4">My Workspaces</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>
          New Workspace
        </Button>
      </Stack>

      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
          <CircularProgress />
        </Box>
      ) : data && data.length > 0 ? (
        <Grid container spacing={2}>
          {data.map((ws) => (
            <Grid item xs={12} sm={6} md={4} key={ws.id}>
              <Card sx={{ height: '100%' }}>
                <CardActionArea onClick={() => navigate(`/workspaces/${ws.id}/events`)}>
                  <CardContent>
                    <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 1 }}>
                      <Box
                        sx={{
                          bgcolor: 'primary.light',
                          color: '#fff',
                          width: 44,
                          height: 44,
                          borderRadius: 2,
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                        }}
                      >
                        <GroupsIcon />
                      </Box>
                      <Box sx={{ flex: 1, minWidth: 0 }}>
                        <Typography fontWeight={700} noWrap>
                          {ws.name}
                        </Typography>
                        <Chip label={ws.role} size="small" sx={{ mt: 0.5 }} />
                      </Box>
                    </Stack>
                    <Typography variant="body2" color="text.secondary" noWrap>
                      {ws.description || 'No description'}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {ws.memberCount} members · {ws.eventCount} events
                    </Typography>
                  </CardContent>
                </CardActionArea>
                <Box sx={{ px: 2, pb: 1.5 }}>
                  <Button size="small" startIcon={<PeopleIcon />} onClick={() => setMembersFor(ws)}>
                    Members
                  </Button>
                </Box>
              </Card>
            </Grid>
          ))}
        </Grid>
      ) : (
        <EmptyState
          icon={<GroupsIcon />}
          title="No workspaces yet"
          description="Create a workspace to start tracking shared expenses."
          action={
            <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>
              New Workspace
            </Button>
          }
        />
      )}

      <CreateWorkspaceDialog open={createOpen} onClose={() => setCreateOpen(false)} />
      {membersFor && (
        <ManageMembersDialog
          workspace={membersFor}
          open
          onClose={() => setMembersFor(null)}
        />
      )}
    </Box>
  );
}

function CreateWorkspaceDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const create = useCreateWorkspace();
  const { enqueueSnackbar } = useSnackbar();
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');

  const submit = async () => {
    try {
      await create.mutateAsync({ name, description });
      enqueueSnackbar('Workspace created', { variant: 'success' });
      setName('');
      setDescription('');
      onClose();
    } catch (err) {
      enqueueSnackbar(errorMessage(err), { variant: 'error' });
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>New Workspace</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          <TextField label="Workspace name" value={name} onChange={(e) => setName(e.target.value)} fullWidth autoFocus />
          <TextField
            label="Description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            fullWidth
            multiline
            minRows={2}
          />
        </Stack>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" onClick={submit} disabled={!name.trim() || create.isPending}>
          Create
        </Button>
      </DialogActions>
    </Dialog>
  );
}

function ManageMembersDialog({
  workspace,
  open,
  onClose,
}: {
  workspace: Workspace;
  open: boolean;
  onClose: () => void;
}) {
  const { data: members } = useWorkspaceMembers(workspace.id);
  const add = useAddWorkspaceMember(workspace.id);
  const remove = useRemoveWorkspaceMember(workspace.id);
  const updateRole = useUpdateWorkspaceMemberRole(workspace.id);
  const { enqueueSnackbar } = useSnackbar();
  const [email, setEmail] = useState('');
  const [role, setRole] = useState<Role>('MEMBER');

  const canManage = workspace.role === 'OWNER';

  const addMember = async () => {
    try {
      await add.mutateAsync({ email, role });
      setEmail('');
      enqueueSnackbar('Member added', { variant: 'success' });
    } catch (err) {
      enqueueSnackbar(errorMessage(err), { variant: 'error' });
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{workspace.name} · Members</DialogTitle>
      <DialogContent>
        <Stack spacing={1.5} sx={{ mt: 1 }}>
          {members?.map((m) => (
            <Stack key={m.id} direction="row" alignItems="center" spacing={1}>
              <Box sx={{ flex: 1, minWidth: 0 }}>
                <Typography variant="body2" fontWeight={600} noWrap>
                  {m.displayName}
                </Typography>
                <Typography variant="caption" color="text.secondary" noWrap>
                  {m.email}
                </Typography>
              </Box>
              <TextField
                select
                size="small"
                value={m.role}
                disabled={!canManage}
                onChange={(e) =>
                  updateRole
                    .mutateAsync({ memberId: m.id, role: e.target.value as Role })
                    .catch((err) => enqueueSnackbar(errorMessage(err), { variant: 'error' }))
                }
                sx={{ width: 130 }}
              >
                {ROLES.map((r) => (
                  <MenuItem key={r} value={r}>
                    {r}
                  </MenuItem>
                ))}
              </TextField>
              {canManage && (
                <IconButton
                  size="small"
                  onClick={() =>
                    remove
                      .mutateAsync(m.id)
                      .catch((err) => enqueueSnackbar(errorMessage(err), { variant: 'error' }))
                  }
                >
                  <DeleteIcon fontSize="small" />
                </IconButton>
              )}
            </Stack>
          ))}

          {canManage && (
            <Stack direction="row" spacing={1} sx={{ pt: 1 }}>
              <TextField
                label="Add by email"
                size="small"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                fullWidth
              />
              <TextField
                select
                size="small"
                value={role}
                onChange={(e) => setRole(e.target.value as Role)}
                sx={{ width: 130 }}
              >
                {ROLES.map((r) => (
                  <MenuItem key={r} value={r}>
                    {r}
                  </MenuItem>
                ))}
              </TextField>
              <Button variant="contained" onClick={addMember} disabled={!email.trim() || add.isPending}>
                Add
              </Button>
            </Stack>
          )}
        </Stack>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  );
}
