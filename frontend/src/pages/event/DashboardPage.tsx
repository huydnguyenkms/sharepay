import {
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Divider,
  Grid,
  List,
  ListItemButton,
  Stack,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import ReceiptLongIcon from '@mui/icons-material/ReceiptLong';
import { useNavigate, useOutletContext } from 'react-router-dom';
import type { EventContext } from '../../components/EventLayout';
import { useDashboard } from '../../hooks/useEventData';
import StatCard from '../../components/StatCard';
import CategoryDonut from '../../components/CategoryDonut';
import MoneyText from '../../components/MoneyText';
import { formatMoney } from '../../lib/money';
import { balanceColor, TX_TYPE_COLOR } from '../../lib/meta';
import { Chip } from '@mui/material';

export default function DashboardPage() {
  const { event } = useOutletContext<EventContext>();
  const { data, isLoading } = useDashboard(event.id);
  const navigate = useNavigate();

  if (isLoading || !data) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
        <CircularProgress />
      </Box>
    );
  }

  const { totals, expenseByCategory, memberBalances, recentTransactions } = data;
  const topBalances = [...memberBalances].sort((a, b) => Math.abs(b.balance) - Math.abs(a.balance)).slice(0, 5);

  return (
    <Box>
      <Grid container spacing={2} sx={{ mb: 1 }}>
        <Grid item xs={6} md={3}>
          <StatCard label="Total Expense" value={formatMoney(totals.totalExpense, event.currency)} />
        </Grid>
        <Grid item xs={6} md={3}>
          <StatCard label="Total Paid" value={formatMoney(totals.totalPaid, event.currency)} />
        </Grid>
        <Grid item xs={6} md={3}>
          <StatCard label="Total Sponsored" value={formatMoney(totals.totalSponsored, event.currency)} />
        </Grid>
        <Grid item xs={6} md={3}>
          <StatCard label="Net Shared" value={formatMoney(totals.netShared, event.currency)} color="#5b5bd6" />
        </Grid>
      </Grid>

      <Grid container spacing={2} sx={{ mt: 0 }}>
        <Grid item xs={12} md={7}>
          <Card>
            <CardContent>
              <Typography variant="subtitle1" gutterBottom>
                Expense by Category
              </Typography>
              <CategoryDonut data={expenseByCategory} currency={event.currency} />
            </CardContent>
          </Card>

          <Card sx={{ mt: 2 }}>
            <CardContent sx={{ pb: 0 }}>
              <Stack direction="row" justifyContent="space-between" alignItems="center">
                <Typography variant="subtitle1">Recent Transactions</Typography>
                <Button size="small" onClick={() => navigate(`/events/${event.id}/transactions`)}>
                  View all
                </Button>
              </Stack>
            </CardContent>
            <List>
              {recentTransactions.length === 0 && (
                <Typography color="text.secondary" sx={{ px: 2, pb: 2 }}>
                  No transactions yet.
                </Typography>
              )}
              {recentTransactions.map((tx) => (
                <ListItemButton key={tx.id} onClick={() => navigate(`/events/${event.id}/transactions/${tx.id}`)}>
                  <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Stack direction="row" spacing={1} alignItems="center">
                      <Typography fontWeight={600} noWrap>
                        {tx.title}
                      </Typography>
                      <Chip label={tx.type} size="small" color={TX_TYPE_COLOR[tx.type]} variant="outlined" />
                    </Stack>
                    <Typography variant="caption" color="text.secondary">
                      {tx.category?.name ?? '—'} · {tx.payers.map((p) => p.displayName).join(', ') || '—'}
                    </Typography>
                  </Box>
                  <MoneyText amount={tx.amount} currency={event.currency} />
                </ListItemButton>
              ))}
            </List>
          </Card>
        </Grid>

        <Grid item xs={12} md={5}>
          <Card>
            <CardContent>
              <Typography variant="subtitle1" gutterBottom>
                Top Balances
              </Typography>
              <Stack spacing={1.5}>
                {topBalances.map((m) => (
                  <Stack key={m.memberId} direction="row" justifyContent="space-between">
                    <Typography variant="body2">{m.displayName}</Typography>
                    <Typography variant="body2" sx={{ color: balanceColor(m.balance), fontWeight: 600 }}>
                      {m.balance > 0 ? '+' : ''}
                      {formatMoney(m.balance, event.currency)}
                    </Typography>
                  </Stack>
                ))}
              </Stack>
            </CardContent>
          </Card>

          <Card sx={{ mt: 2 }}>
            <CardContent>
              <Typography variant="subtitle1" gutterBottom>
                Quick Actions
              </Typography>
              <Stack spacing={1}>
                <Button
                  variant="contained"
                  startIcon={<AddIcon />}
                  onClick={() => navigate(`/events/${event.id}/transactions/new`)}
                >
                  Add Transaction
                </Button>
                <Button
                  variant="outlined"
                  startIcon={<ReceiptLongIcon />}
                  onClick={() => navigate(`/events/${event.id}/settlement`)}
                >
                  View Settlement
                </Button>
              </Stack>
            </CardContent>
          </Card>

          <Card sx={{ mt: 2 }}>
            <CardContent>
              <Typography variant="body2" color="text.secondary">
                Positive balance means the member should receive money. Negative means they owe.
              </Typography>
              <Divider sx={{ my: 1 }} />
              <Typography variant="caption" color="text.secondary">
                {totals.participantCount} participants
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
}
