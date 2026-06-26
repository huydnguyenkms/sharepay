import { useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  Checkbox,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  IconButton,
  InputAdornment,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import { useOutletContext } from 'react-router-dom';
import { useSnackbar } from 'notistack';
import type { EventContext } from '../../components/EventLayout';
import { useAddMembers, useMembers, useRemoveMember, useRemoveMembers, useUpdateMember } from '../../hooks/useMembers';
import { useMembersSummary } from '../../hooks/useEventData';
import { useKnownMembers } from '../../hooks/useWorkspaces';
import type { KnownMember } from '../../api/types';
import type { EventMember } from '../../api/types';
import { errorMessage } from '../../api/client';
import { formatMoney } from '../../lib/money';
import { balanceColor } from '../../lib/meta';
import ConfirmDialog from '../../components/ConfirmDialog';

export default function MembersPage() {
  const { event } = useOutletContext<EventContext>();
  const { data: members, isLoading } = useMembers(event.id);
  const { data: summary } = useMembersSummary(event.id);
  const remove = useRemoveMember(event.id);
  const removeMembers = useRemoveMembers(event.id);
  const { enqueueSnackbar } = useSnackbar();

  const [editing, setEditing] = useState<EventMember | null>(null);
  const [adding, setAdding] = useState(false);
  const [toDelete, setToDelete] = useState<EventMember | null>(null);
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [bulkConfirm, setBulkConfirm] = useState(false);

  const summaryById = new Map((summary ?? []).map((s) => [s.memberId, s]));

  const allIds = members?.map((m) => m.id) ?? [];
  const allSelected = allIds.length > 0 && allIds.every((id) => selectedIds.has(id));
  const someSelected = allIds.some((id) => selectedIds.has(id)) && !allSelected;

  const toggleAll = () => setSelectedIds(allSelected ? new Set() : new Set(allIds));
  const toggleOne = (id: number) =>
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });

  const bulkDelete = () =>
    removeMembers
      .mutateAsync([...selectedIds])
      .then((res) => {
        setSelectedIds(new Set());
        if (res.failed.length === 0) {
          enqueueSnackbar(`Removed ${res.total} member${res.total === 1 ? '' : 's'}`, { variant: 'success' });
        } else {
          enqueueSnackbar(
            `Removed ${res.total - res.failed.length}; ${res.failed.length} skipped (they have transactions)`,
            { variant: 'warning' },
          );
        }
      })
      .catch((err) => enqueueSnackbar(errorMessage(err), { variant: 'error' }));

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }} flexWrap="wrap" gap={1}>
        <Typography variant="h6">Members</Typography>
        <Stack direction="row" spacing={1}>
          {selectedIds.size > 0 && (
            <Button
              variant="outlined"
              color="error"
              startIcon={<DeleteIcon />}
              onClick={() => setBulkConfirm(true)}
              disabled={removeMembers.isPending}
            >
              Delete ({selectedIds.size})
            </Button>
          )}
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setAdding(true)}>
            Add Member
          </Button>
        </Stack>
      </Stack>

      <Card sx={{ overflowX: 'auto' }}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell padding="checkbox">
                <Checkbox
                  checked={allSelected}
                  indeterminate={someSelected}
                  onChange={toggleAll}
                  inputProps={{ 'aria-label': 'Select all members' }}
                />
              </TableCell>
              <TableCell>Member</TableCell>
              <TableCell align="right">Paid</TableCell>
              <TableCell align="right">Owed</TableCell>
              <TableCell align="right">Sponsored</TableCell>
              <TableCell align="right">Balance</TableCell>
              <TableCell align="right" />
            </TableRow>
          </TableHead>
          <TableBody>
            {members?.map((m) => {
              const s = summaryById.get(m.id);
              return (
                <TableRow key={m.id} hover selected={selectedIds.has(m.id)}>
                  <TableCell padding="checkbox">
                    <Checkbox
                      checked={selectedIds.has(m.id)}
                      onChange={() => toggleOne(m.id)}
                      inputProps={{ 'aria-label': `Select ${m.displayName}` }}
                    />
                  </TableCell>
                  <TableCell>
                    <Typography fontWeight={600}>{m.displayName}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      {m.email || '—'}
                    </Typography>
                  </TableCell>
                  <TableCell align="right">{formatMoney(s?.paid ?? 0, event.currency)}</TableCell>
                  <TableCell align="right">{formatMoney(s?.owed ?? 0, event.currency)}</TableCell>
                  <TableCell align="right">{formatMoney(s?.sponsored ?? 0, event.currency)}</TableCell>
                  <TableCell align="right" sx={{ color: balanceColor(s?.balance ?? 0), fontWeight: 700 }}>
                    {(s?.balance ?? 0) > 0 ? '+' : ''}
                    {formatMoney(s?.balance ?? 0, event.currency)}
                  </TableCell>
                  <TableCell align="right">
                    <IconButton size="small" onClick={() => setEditing(m)}>
                      <EditIcon fontSize="small" />
                    </IconButton>
                    <IconButton size="small" onClick={() => setToDelete(m)}>
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </Card>

      <Alert severity="info" sx={{ mt: 2 }}>
        Positive balance means the member should receive money. Negative balance means the member owes money.
      </Alert>

      {adding && (
        <AddMembersDialog
          eventId={event.id}
          workspaceId={event.workspaceId}
          existing={members ?? []}
          open
          onClose={() => setAdding(false)}
        />
      )}
      {editing && (
        <EditMemberDialog eventId={event.id} member={editing} open onClose={() => setEditing(null)} />
      )}

      <ConfirmDialog
        open={!!toDelete}
        title="Remove member?"
        message={`Remove ${toDelete?.displayName} from this event? Members with transactions cannot be removed.`}
        confirmLabel="Remove"
        destructive
        onClose={() => setToDelete(null)}
        onConfirm={() =>
          toDelete &&
          remove
            .mutateAsync(toDelete.id)
            .then(() => enqueueSnackbar('Member removed', { variant: 'success' }))
            .catch((err) => enqueueSnackbar(errorMessage(err), { variant: 'error' }))
        }
      />

      <ConfirmDialog
        open={bulkConfirm}
        title={`Remove ${selectedIds.size} member${selectedIds.size === 1 ? '' : 's'}?`}
        message="Selected members will be removed from this event. Anyone who already has transactions is skipped automatically."
        confirmLabel="Remove"
        destructive
        onClose={() => setBulkConfirm(false)}
        onConfirm={bulkDelete}
      />
    </Box>
  );
}

function memberKey(displayName: string, email?: string | null): string {
  return email ? `email:${email.toLowerCase()}` : `name:${displayName.toLowerCase()}`;
}

function AddMembersDialog({
  eventId,
  workspaceId,
  existing,
  open,
  onClose,
}: {
  eventId: number;
  workspaceId: number;
  existing: EventMember[];
  open: boolean;
  onClose: () => void;
}) {
  const addMembers = useAddMembers(eventId);
  const { data: known } = useKnownMembers(workspaceId);
  const { enqueueSnackbar } = useSnackbar();

  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [search, setSearch] = useState('');
  const [showNew, setShowNew] = useState(false);
  const [newName, setNewName] = useState('');
  const [newEmail, setNewEmail] = useState('');
  const [newPhone, setNewPhone] = useState('');

  // People already in this event are not offered again.
  const presentKeys = new Set(existing.map((m) => memberKey(m.displayName, m.email)));
  const options: KnownMember[] = (known ?? []).filter((k) => !presentKeys.has(memberKey(k.displayName, k.email)));
  const filtered = options.filter(
    (o) =>
      !search ||
      o.displayName.toLowerCase().includes(search.toLowerCase()) ||
      (o.email ?? '').toLowerCase().includes(search.toLowerCase()),
  );

  const toggle = (key: string) =>
    setSelected((prev) => {
      const next = new Set(prev);
      next.has(key) ? next.delete(key) : next.add(key);
      return next;
    });

  const newNameValid = newName.trim().length > 0;
  const total = selected.size + (showNew && newNameValid ? 1 : 0);

  const submit = async () => {
    const bodies = options
      .filter((o) => selected.has(memberKey(o.displayName, o.email)))
      .map((o) => ({ displayName: o.displayName, email: o.email, phone: o.phone, userId: o.userId }));
    if (showNew && newNameValid) {
      bodies.push({ displayName: newName.trim(), email: newEmail || null, phone: newPhone || null, userId: null });
    }
    try {
      await addMembers.mutateAsync(bodies);
      enqueueSnackbar(`Added ${bodies.length} member${bodies.length === 1 ? '' : 's'}`, { variant: 'success' });
      onClose();
    } catch (err) {
      enqueueSnackbar(errorMessage(err), { variant: 'error' });
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Add Members</DialogTitle>
      <DialogContent>
        {options.length > 0 && (
          <TextField
            size="small"
            fullWidth
            placeholder="Search members…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            sx={{ mt: 1 }}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon fontSize="small" />
                </InputAdornment>
              ),
            }}
          />
        )}

        <List sx={{ maxHeight: 300, overflowY: 'auto' }}>
          {filtered.map((o) => {
            const key = memberKey(o.displayName, o.email);
            return (
              <ListItemButton key={key} dense onClick={() => toggle(key)}>
                <ListItemIcon sx={{ minWidth: 36 }}>
                  <Checkbox edge="start" checked={selected.has(key)} tabIndex={-1} disableRipple />
                </ListItemIcon>
                <ListItemText primary={o.displayName} secondary={o.email ?? undefined} />
              </ListItemButton>
            );
          })}
          {options.length === 0 && (
            <Typography variant="body2" color="text.secondary" sx={{ px: 1, py: 2 }}>
              No reusable members yet — add a new person below.
            </Typography>
          )}
          {options.length > 0 && filtered.length === 0 && (
            <Typography variant="body2" color="text.secondary" sx={{ px: 1, py: 2 }}>
              No members match “{search}”.
            </Typography>
          )}
        </List>

        <Divider sx={{ my: 1 }} />

        {showNew ? (
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField label="New member name" value={newName} onChange={(e) => setNewName(e.target.value)} fullWidth autoFocus />
            <TextField label="Email" value={newEmail} onChange={(e) => setNewEmail(e.target.value)} fullWidth />
            <TextField label="Phone" value={newPhone} onChange={(e) => setNewPhone(e.target.value)} fullWidth />
          </Stack>
        ) : (
          <Button startIcon={<PersonAddIcon />} onClick={() => setShowNew(true)}>
            Add a new person
          </Button>
        )}
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" onClick={submit} disabled={total === 0 || addMembers.isPending}>
          {total > 0 ? `Add ${total} member${total === 1 ? '' : 's'}` : 'Add'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

function EditMemberDialog({
  eventId,
  member,
  open,
  onClose,
}: {
  eventId: number;
  member: EventMember;
  open: boolean;
  onClose: () => void;
}) {
  const update = useUpdateMember(eventId);
  const { enqueueSnackbar } = useSnackbar();
  const [displayName, setDisplayName] = useState(member.displayName);
  const [email, setEmail] = useState(member.email ?? '');
  const [phone, setPhone] = useState(member.phone ?? '');

  const submit = async () => {
    try {
      await update.mutateAsync({
        memberId: member.id,
        body: { displayName, email: email || null, phone: phone || null, userId: member.userId },
      });
      enqueueSnackbar('Member updated', { variant: 'success' });
      onClose();
    } catch (err) {
      enqueueSnackbar(errorMessage(err), { variant: 'error' });
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Edit Member</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          <TextField label="Display name" value={displayName} onChange={(e) => setDisplayName(e.target.value)} fullWidth autoFocus />
          <TextField label="Email" value={email} onChange={(e) => setEmail(e.target.value)} fullWidth />
          <TextField label="Phone" value={phone} onChange={(e) => setPhone(e.target.value)} fullWidth />
        </Stack>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" onClick={submit} disabled={!displayName.trim()}>
          Save
        </Button>
      </DialogActions>
    </Dialog>
  );
}
