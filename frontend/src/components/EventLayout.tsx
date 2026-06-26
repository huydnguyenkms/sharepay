import { Box, Chip, CircularProgress, IconButton, Stack, Tab, Tabs, Typography } from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { Outlet, useLocation, useNavigate, useParams } from 'react-router-dom';
import { useEvent } from '../hooks/useEvents';
import type { Event } from '../api/types';
import { EVENT_TYPE_COLOR, STATUS_COLOR } from '../lib/meta';
import { formatDateRange } from '../lib/format';

const TABS = [
  { label: 'Dashboard', path: 'dashboard' },
  { label: 'Transactions', path: 'transactions' },
  { label: 'Members', path: 'members' },
  { label: 'Summary', path: 'summary' },
  { label: 'Settlement', path: 'settlement' },
];

export interface EventContext {
  event: Event;
}

export default function EventLayout() {
  const { eventId } = useParams();
  const id = Number(eventId);
  const { data: event, isLoading } = useEvent(id);
  const navigate = useNavigate();
  const location = useLocation();

  if (isLoading || !event) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
        <CircularProgress />
      </Box>
    );
  }

  const current = TABS.find((t) => location.pathname.includes(`/${t.path}`))?.path ?? 'dashboard';

  return (
    <Box>
      <Stack direction="row" alignItems="flex-start" spacing={1} sx={{ mb: 2 }}>
        <IconButton onClick={() => navigate(`/workspaces/${event.workspaceId}/events`)} size="small">
          <ArrowBackIcon />
        </IconButton>
        <Box sx={{ flex: 1 }}>
          <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
            <Typography variant="h5">{event.name}</Typography>
            <Chip
              label={event.type}
              size="small"
              sx={{ bgcolor: EVENT_TYPE_COLOR[event.type], color: '#fff' }}
            />
            <Chip label={event.status} size="small" color={STATUS_COLOR[event.status]} />
          </Stack>
          <Typography variant="body2" color="text.secondary">
            {formatDateRange(event.startDate, event.endDate)} · {event.memberCount} members · {event.currency}
          </Typography>
        </Box>
      </Stack>

      <Tabs
        value={current}
        onChange={(_, value) => navigate(`/events/${id}/${value}`)}
        variant="scrollable"
        scrollButtons="auto"
        sx={{ borderBottom: '1px solid #eceef3', mb: 3 }}
      >
        {TABS.map((t) => (
          <Tab key={t.path} value={t.path} label={t.label} />
        ))}
      </Tabs>

      <Outlet context={{ event } satisfies EventContext} />
    </Box>
  );
}
