import { Card, CardContent, Typography } from '@mui/material';
import type { ReactNode } from 'react';

interface Props {
  label: string;
  value: ReactNode;
  color?: string;
}

export default function StatCard({ label, value, color }: Props) {
  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Typography variant="body2" color="text.secondary" gutterBottom>
          {label}
        </Typography>
        <Typography variant="h6" sx={{ color, fontWeight: 700 }}>
          {value}
        </Typography>
      </CardContent>
    </Card>
  );
}
