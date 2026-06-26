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
  ListItemIcon,
  Menu,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import DeleteIcon from '@mui/icons-material/Delete';
import ArchiveIcon from '@mui/icons-material/Archive';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import EventIcon from '@mui/icons-material/Event';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import type { Dayjs } from 'dayjs';
import { useNavigate, useParams } from 'react-router-dom';
import { useSnackbar } from 'notistack';
import { Tabs, Tab } from '@mui/material';
import {
  useCreateEvent,
  useDeleteEvent,
  useDuplicateEvent,
  useEvents,
  useUpdateEventStatus,
} from '../hooks/useEvents';
import { useWorkspace } from '../hooks/useWorkspaces';
import type { Event, EventStatus, EventType } from '../api/types';
import { EVENT_TYPES, EVENT_TYPE_COLOR, STATUS_COLOR } from '../lib/meta';
import { errorMessage } from '../api/client';
import { formatDateRange } from '../lib/format';
import EmptyState from '../components/EmptyState';
import ConfirmDialog from '../components/ConfirmDialog';

const STATUS_TABS: { label: string; value?: EventStatus }[] = [
  { label: 'All' },
  { label: 'Active', value: 'ACTIVE' },
  { label: 'Completed', value: 'COMPLETED' },
  { label: 'Archived', value: 'ARCHIVED' },
];

export default function EventsPage() {
  const { workspaceId } = useParams();
  const wsId = Number(workspaceId);
  const { data: workspace } = useWorkspace(wsId);
  const [tab, setTab] = useState(0);
  const { data: events, isLoading } = useEvents(wsId, STATUS_TABS[tab].value);
  const [createOpen, setCreateOpen] = useState(false);
  const navigate = useNavigate();

  return (
    <Box>
      <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 1 }}>
        <IconButton onClick={() => navigate('/workspaces')} size="small">
          <ArrowBackIcon />
        </IconButton>
        <Typography variant="h4" sx={{ flex: 1 }}>
          {workspace?.name ?? 'Events'}
        </Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>
          New Event
        </Button>
      </Stack>

      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 3, borderBottom: '1px solid #eceef3' }}>
        {STATUS_TABS.map((t) => (
          <Tab key={t.label} label={t.label} />
        ))}
      </Tabs>

      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
          <CircularProgress />
        </Box>
      ) : events && events.length > 0 ? (
        <Grid container spacing={2}>
          {events.map((ev) => (
            <Grid item xs={12} sm={6} md={4} key={ev.id}>
              <EventCard event={ev} workspaceId={wsId} onOpen={() => navigate(`/events/${ev.id}/dashboard`)} />
            </Grid>
          ))}
        </Grid>
      ) : (
        <EmptyState
          icon={<EventIcon />}
          title="No events here"
          description="Create an event to start adding expenses."
          action={
            <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>
              New Event
            </Button>
          }
        />
      )}

      <CreateEventDialog workspaceId={wsId} open={createOpen} onClose={() => setCreateOpen(false)} />
    </Box>
  );
}

function EventCard({
  event,
  workspaceId,
  onOpen,
}: {
  event: Event;
  workspaceId: number;
  onOpen: () => void;
}) {
  const [anchor, setAnchor] = useState<null | HTMLElement>(null);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const duplicate = useDuplicateEvent(event.id);
  const updateStatus = useUpdateEventStatus(event.id);
  const remove = useDeleteEvent(workspaceId);
  const { enqueueSnackbar } = useSnackbar();

  const run = (p: Promise<unknown>, ok: string) =>
    p.then(() => enqueueSnackbar(ok, { variant: 'success' })).catch((err) =>
      enqueueSnackbar(errorMessage(err), { variant: 'error' }),
    );

  return (
    <Card sx={{ height: '100%', position: 'relative' }}>
      <IconButton
        size="small"
        sx={{ position: 'absolute', top: 8, right: 8, zIndex: 1 }}
        onClick={(e) => setAnchor(e.currentTarget)}
      >
        <MoreVertIcon fontSize="small" />
      </IconButton>
      <CardActionArea onClick={onOpen}>
        <CardContent>
          <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1, pr: 4 }}>
            <Typography fontWeight={700} noWrap sx={{ flex: 1 }}>
              {event.name}
            </Typography>
          </Stack>
          <Stack direction="row" spacing={1} sx={{ mb: 1 }}>
            <Chip label={event.type} size="small" sx={{ bgcolor: EVENT_TYPE_COLOR[event.type], color: '#fff' }} />
            <Chip label={event.status} size="small" color={STATUS_COLOR[event.status]} />
          </Stack>
          <Typography variant="body2" color="text.secondary">
            {formatDateRange(event.startDate, event.endDate)}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {event.memberCount} members · {event.currency}
          </Typography>
        </CardContent>
      </CardActionArea>

      <Menu anchorEl={anchor} open={!!anchor} onClose={() => setAnchor(null)}>
        <MenuItem
          onClick={() => {
            setAnchor(null);
            run(duplicate.mutateAsync({ copyMembers: true }), 'Event duplicated');
          }}
        >
          <ListItemIcon>
            <ContentCopyIcon fontSize="small" />
          </ListItemIcon>
          Duplicate
        </MenuItem>
        {event.status !== 'COMPLETED' && (
          <MenuItem
            onClick={() => {
              setAnchor(null);
              run(updateStatus.mutateAsync('COMPLETED'), 'Marked completed');
            }}
          >
            <ListItemIcon>
              <CheckCircleIcon fontSize="small" />
            </ListItemIcon>
            Mark completed
          </MenuItem>
        )}
        {event.status !== 'ARCHIVED' && (
          <MenuItem
            onClick={() => {
              setAnchor(null);
              run(updateStatus.mutateAsync('ARCHIVED'), 'Archived');
            }}
          >
            <ListItemIcon>
              <ArchiveIcon fontSize="small" />
            </ListItemIcon>
            Archive
          </MenuItem>
        )}
        <MenuItem
          onClick={() => {
            setAnchor(null);
            setConfirmDelete(true);
          }}
        >
          <ListItemIcon>
            <DeleteIcon fontSize="small" />
          </ListItemIcon>
          Delete
        </MenuItem>
      </Menu>

      <ConfirmDialog
        open={confirmDelete}
        title="Delete event?"
        message={`This permanently deletes "${event.name}" and all its transactions.`}
        confirmLabel="Delete"
        destructive
        onClose={() => setConfirmDelete(false)}
        onConfirm={() => run(remove.mutateAsync(event.id), 'Event deleted')}
      />
    </Card>
  );
}

function CreateEventDialog({
  workspaceId,
  open,
  onClose,
}: {
  workspaceId: number;
  open: boolean;
  onClose: () => void;
}) {
  const create = useCreateEvent(workspaceId);
  const { enqueueSnackbar } = useSnackbar();
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [type, setType] = useState<EventType>('TRAVEL');
  const [currency, setCurrency] = useState('VND');
  const [start, setStart] = useState<Dayjs | null>(null);
  const [end, setEnd] = useState<Dayjs | null>(null);

  const submit = async () => {
    try {
      await create.mutateAsync({
        name,
        description,
        type,
        currency,
        startDate: start ? start.format('YYYY-MM-DD') : null,
        endDate: end ? end.format('YYYY-MM-DD') : null,
      });
      enqueueSnackbar('Event created', { variant: 'success' });
      onClose();
      setName('');
      setDescription('');
    } catch (err) {
      enqueueSnackbar(errorMessage(err), { variant: 'error' });
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>New Event</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          <TextField label="Event name" value={name} onChange={(e) => setName(e.target.value)} fullWidth autoFocus />
          <TextField
            label="Description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            fullWidth
            multiline
            minRows={2}
          />
          <Stack direction="row" spacing={2}>
            <TextField select label="Type" value={type} onChange={(e) => setType(e.target.value as EventType)} fullWidth>
              {EVENT_TYPES.map((t) => (
                <MenuItem key={t} value={t}>
                  {t}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              label="Currency"
              value={currency}
              onChange={(e) => setCurrency(e.target.value.toUpperCase())}
              sx={{ width: 120 }}
              inputProps={{ maxLength: 3 }}
            />
          </Stack>
          <Stack direction="row" spacing={2}>
            <DatePicker label="Start date" value={start} onChange={setStart} slotProps={{ textField: { fullWidth: true } }} />
            <DatePicker
              label="End date"
              value={end}
              minDate={start ?? undefined}
              onChange={setEnd}
              slotProps={{ textField: { fullWidth: true } }}
            />
          </Stack>
        </Stack>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose}>Cancel</Button>
        <Button
          variant="contained"
          onClick={submit}
          disabled={!name.trim() || currency.length !== 3 || create.isPending}
        >
          Create
        </Button>
      </DialogActions>
    </Dialog>
  );
}
