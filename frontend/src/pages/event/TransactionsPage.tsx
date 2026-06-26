import { useMemo, useState } from 'react';
import {
  Box,
  Button,
  Card,
  Chip,
  CircularProgress,
  InputAdornment,
  Stack,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Tabs,
  TextField,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import SearchIcon from '@mui/icons-material/Search';
import { useNavigate, useOutletContext } from 'react-router-dom';
import type { EventContext } from '../../components/EventLayout';
import { useTransactions } from '../../hooks/useEventData';
import type { TransactionType } from '../../api/types';
import { TX_TYPE_COLOR } from '../../lib/meta';
import { formatMoney } from '../../lib/money';
import { formatDate } from '../../lib/format';
import EmptyState from '../../components/EmptyState';

const TYPE_TABS: { label: string; value?: TransactionType }[] = [
  { label: 'All' },
  { label: 'Expense', value: 'EXPENSE' },
  { label: 'Sponsor', value: 'SPONSOR' },
  { label: 'Refund', value: 'REFUND' },
  { label: 'Adjustment', value: 'ADJUSTMENT' },
];

export default function TransactionsPage() {
  const { event } = useOutletContext<EventContext>();
  const { data, isLoading } = useTransactions(event.id);
  const [tab, setTab] = useState(0);
  const [search, setSearch] = useState('');
  const navigate = useNavigate();

  const filtered = useMemo(() => {
    const type = TYPE_TABS[tab].value;
    return (data ?? []).filter(
      (tx) =>
        (!type || tx.type === type) &&
        (!search || tx.title.toLowerCase().includes(search.toLowerCase())),
    );
  }, [data, tab, search]);

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }} flexWrap="wrap" gap={1}>
        <Typography variant="h6">Transactions</Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => navigate(`/events/${event.id}/transactions/new`)}
        >
          Add Transaction
        </Button>
      </Stack>

      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} sx={{ mb: 2 }} alignItems={{ sm: 'center' }}>
        <Tabs value={tab} onChange={(_, v) => setTab(v)} variant="scrollable" scrollButtons="auto" sx={{ flex: 1 }}>
          {TYPE_TABS.map((t) => (
            <Tab key={t.label} label={t.label} />
          ))}
        </Tabs>
        <TextField
          size="small"
          placeholder="Search transactions…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon fontSize="small" />
              </InputAdornment>
            ),
          }}
        />
      </Stack>

      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
          <CircularProgress />
        </Box>
      ) : filtered.length === 0 ? (
        <EmptyState
          title="No transactions"
          description="Add an expense, refund, or adjustment to get started."
          action={
            <Button variant="contained" startIcon={<AddIcon />} onClick={() => navigate(`/events/${event.id}/transactions/new`)}>
              Add Transaction
            </Button>
          }
        />
      ) : (
        <Card sx={{ overflowX: 'auto' }}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Transaction</TableCell>
                <TableCell>Type</TableCell>
                <TableCell>Category</TableCell>
                <TableCell>Date</TableCell>
                <TableCell align="right">Amount</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filtered.map((tx) => (
                <TableRow
                  key={tx.id}
                  hover
                  sx={{ cursor: 'pointer' }}
                  onClick={() => navigate(`/events/${event.id}/transactions/${tx.id}`)}
                >
                  <TableCell>
                    <Typography fontWeight={600}>{tx.title}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      {tx.payers.map((p) => p.displayName).join(', ') || '—'}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Chip label={tx.type} size="small" color={TX_TYPE_COLOR[tx.type]} variant="outlined" />
                  </TableCell>
                  <TableCell>{tx.category?.name ?? '—'}</TableCell>
                  <TableCell>{formatDate(tx.date)}</TableCell>
                  <TableCell align="right">
                    <Typography
                      fontWeight={600}
                      sx={{ color: tx.type === 'REFUND' ? 'success.main' : 'inherit' }}
                    >
                      {tx.type === 'REFUND' ? '-' : ''}
                      {formatMoney(tx.amount, event.currency)}
                    </Typography>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Card>
      )}
    </Box>
  );
}
