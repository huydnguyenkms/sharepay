import { Box, Stack, Typography } from '@mui/material';
import { PieChart } from '@mui/x-charts/PieChart';
import type { CategoryBreakdown } from '../api/types';
import { CHART_COLORS } from '../lib/meta';
import { formatMoney } from '../lib/money';

interface Props {
  data: CategoryBreakdown[];
  currency: string;
}

export default function CategoryDonut({ data, currency }: Props) {
  if (data.length === 0) {
    return (
      <Box sx={{ py: 4, textAlign: 'center' }}>
        <Typography color="text.secondary">No expenses yet</Typography>
      </Box>
    );
  }

  const series = data.map((d, i) => ({
    id: i,
    value: d.amount,
    label: d.category,
    color: CHART_COLORS[i % CHART_COLORS.length],
  }));

  return (
    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems="center">
      <PieChart
        series={[{ data: series, innerRadius: 50, outerRadius: 90, paddingAngle: 2, cornerRadius: 4 }]}
        width={220}
        height={200}
        slotProps={{ legend: { hidden: true } }}
      />
      <Stack spacing={1} flex={1} sx={{ width: '100%' }}>
        {data.map((d, i) => (
          <Stack key={d.category} direction="row" alignItems="center" spacing={1}>
            <Box sx={{ width: 12, height: 12, borderRadius: '3px', bgcolor: CHART_COLORS[i % CHART_COLORS.length] }} />
            <Typography variant="body2" sx={{ flex: 1 }}>
              {d.category}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {formatMoney(d.amount, currency)} ({d.percentage}%)
            </Typography>
          </Stack>
        ))}
      </Stack>
    </Stack>
  );
}
