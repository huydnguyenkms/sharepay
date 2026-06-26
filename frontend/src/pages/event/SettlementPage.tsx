import {
  Alert,
  Avatar,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Divider,
  Stack,
  Typography,
} from '@mui/material';
import EastIcon from '@mui/icons-material/East';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import TableChartIcon from '@mui/icons-material/TableChart';
import DescriptionIcon from '@mui/icons-material/Description';
import { useOutletContext } from 'react-router-dom';
import { useSnackbar } from 'notistack';
import type { EventContext } from '../../components/EventLayout';
import { useSettlement, useMarkSettled } from '../../hooks/useEventData';
import { downloadExport } from '../../api/analytics';
import { errorMessage } from '../../api/client';
import { formatMoney } from '../../lib/money';
import { initials } from '../../lib/format';
import EmptyState from '../../components/EmptyState';

export default function SettlementPage() {
  const { event } = useOutletContext<EventContext>();
  const { data, isLoading } = useSettlement(event.id);
  const markSettled = useMarkSettled(event.id);
  const { enqueueSnackbar } = useSnackbar();

  if (isLoading || !data) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
        <CircularProgress />
      </Box>
    );
  }

  const c = event.currency;

  const settle = () =>
    markSettled
      .mutateAsync()
      .then(() => enqueueSnackbar('Event marked as settled', { variant: 'success' }))
      .catch((err) => enqueueSnackbar(errorMessage(err), { variant: 'error' }));

  const exportFile = (format: 'excel' | 'csv') =>
    downloadExport(event.id, format).catch((err) => enqueueSnackbar(errorMessage(err), { variant: 'error' }));

  return (
    <Box sx={{ maxWidth: 720, mx: 'auto' }}>
      <Stack direction="row" justifyContent="space-between" alignItems="center" flexWrap="wrap" gap={1} sx={{ mb: 2 }}>
        <Box>
          <Typography variant="h6">Settlement</Typography>
          <Typography variant="body2" color="text.secondary">
            {data.transferCount} transfers settle all balances · total {formatMoney(data.totalAmount, c)}
          </Typography>
        </Box>
        <Stack direction="row" spacing={1}>
          <Button variant="outlined" startIcon={<TableChartIcon />} onClick={() => exportFile('excel')}>
            Excel
          </Button>
          <Button variant="outlined" startIcon={<DescriptionIcon />} onClick={() => exportFile('csv')}>
            CSV
          </Button>
        </Stack>
      </Stack>

      {data.settled && (
        <Alert severity="success" icon={<CheckCircleIcon />} sx={{ mb: 2 }}>
          This event has been marked as settled.
        </Alert>
      )}

      {data.transfers.length === 0 ? (
        <EmptyState title="Nothing to settle" description="All balances are already zero." />
      ) : (
        <Card>
          <CardContent>
            <Stack spacing={1.5} divider={<Divider flexItem />}>
              {data.transfers.map((t, i) => (
                <Stack key={i} direction="row" alignItems="center" spacing={2}>
                  <Stack direction="row" alignItems="center" spacing={1} sx={{ flex: 1 }}>
                    <Avatar sx={{ width: 32, height: 32, fontSize: 14 }}>{initials(t.fromName)}</Avatar>
                    <Typography variant="body2">{t.fromName}</Typography>
                    <Chip icon={<EastIcon />} label="pays" size="small" variant="outlined" />
                    <Avatar sx={{ width: 32, height: 32, fontSize: 14 }}>{initials(t.toName)}</Avatar>
                    <Typography variant="body2">{t.toName}</Typography>
                  </Stack>
                  <Typography fontWeight={700}>{formatMoney(t.amount, c)}</Typography>
                </Stack>
              ))}
            </Stack>
          </CardContent>
        </Card>
      )}

      <Alert severity="info" sx={{ mt: 2 }}>
        After these payments, everyone&apos;s balance will be zero.
      </Alert>

      {!data.settled && data.transfers.length > 0 && (
        <Button
          variant="contained"
          size="large"
          fullWidth
          sx={{ mt: 2 }}
          startIcon={<CheckCircleIcon />}
          onClick={settle}
          disabled={markSettled.isPending}
        >
          Mark as Settled
        </Button>
      )}
    </Box>
  );
}
