import { Typography } from '@mui/material';
import type { TypographyProps } from '@mui/material';
import { formatMoney, formatSigned } from '../lib/money';
import { balanceColor } from '../lib/meta';

interface Props extends TypographyProps {
  amount: number;
  currency?: string;
  /** Show +/- and colour by sign (for balances). */
  signed?: boolean;
}

export default function MoneyText({ amount, currency = 'VND', signed = false, sx, ...rest }: Props) {
  return (
    <Typography
      component="span"
      sx={{ fontWeight: 600, color: signed ? balanceColor(amount) : undefined, ...sx }}
      {...rest}
    >
      {signed ? formatSigned(amount, currency) : formatMoney(amount, currency)}
    </Typography>
  );
}
