import { useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  CircularProgress,
  Grid,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Tab,
  Tabs,
  Typography,
} from '@mui/material';
import { useOutletContext } from 'react-router-dom';
import type { EventContext } from '../../components/EventLayout';
import { useSummary } from '../../hooks/useEventData';
import StatCard from '../../components/StatCard';
import CategoryDonut from '../../components/CategoryDonut';
import { formatMoney } from '../../lib/money';
import { balanceColor } from '../../lib/meta';

export default function SummaryPage() {
  const { event } = useOutletContext<EventContext>();
  const { data, isLoading } = useSummary(event.id);
  const [tab, setTab] = useState(0);

  if (isLoading || !data) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
        <CircularProgress />
      </Box>
    );
  }

  const { totals, expenseByCategory, memberBalances } = data;
  const c = event.currency;

  return (
    <Box>
      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 2 }}>
        <Tab label="Overview" />
        <Tab label="By Category" />
        <Tab label="By Member" />
      </Tabs>

      {(tab === 0 || tab === 1) && (
        <Grid container spacing={2} sx={{ mb: 2 }}>
          <Grid item xs={6} md={3}>
            <StatCard label="Total Expense" value={formatMoney(totals.totalExpense, c)} />
          </Grid>
          <Grid item xs={6} md={3}>
            <StatCard label="Total Paid" value={formatMoney(totals.totalPaid, c)} />
          </Grid>
          <Grid item xs={6} md={3}>
            <StatCard label="Total Sponsored" value={formatMoney(totals.totalSponsored, c)} />
          </Grid>
          <Grid item xs={6} md={3}>
            <StatCard label="Net Shared" value={formatMoney(totals.netShared, c)} color="#5b5bd6" />
          </Grid>
        </Grid>
      )}

      {(tab === 0 || tab === 1) && (
        <Card sx={{ mb: 2 }}>
          <CardContent>
            <Typography variant="subtitle1" gutterBottom>
              Expense by Category
            </Typography>
            <CategoryDonut data={expenseByCategory} currency={c} />
          </CardContent>
        </Card>
      )}

      {(tab === 0 || tab === 2) && (
        <Card sx={{ overflowX: 'auto' }}>
          <CardContent>
            <Typography variant="subtitle1">By Member</Typography>
          </CardContent>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Member</TableCell>
                <TableCell align="right">Paid</TableCell>
                <TableCell align="right">Share</TableCell>
                <TableCell align="right">Sponsored</TableCell>
                <TableCell align="right">Balance</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {memberBalances.map((m) => (
                <TableRow key={m.memberId}>
                  <TableCell>{m.displayName}</TableCell>
                  <TableCell align="right">{formatMoney(m.paid, c)}</TableCell>
                  <TableCell align="right">{formatMoney(m.owed, c)}</TableCell>
                  <TableCell align="right">{formatMoney(m.sponsored, c)}</TableCell>
                  <TableCell align="right" sx={{ color: balanceColor(m.balance), fontWeight: 700 }}>
                    {m.balance > 0 ? '+' : ''}
                    {formatMoney(m.balance, c)}
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
