import { useRef, useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Divider,
  Grid,
  IconButton,
  Stack,
  Typography,
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import DownloadIcon from '@mui/icons-material/Download';
import { useNavigate, useParams } from 'react-router-dom';
import { useSnackbar } from 'notistack';
import { useEvent } from '../../hooks/useEvents';
import { useDeleteTransaction, useTransaction, useUploadReceipt } from '../../hooks/useEventData';
import { downloadReceipt } from '../../api/transactions';
import { errorMessage } from '../../api/client';
import { formatMoney } from '../../lib/money';
import { formatDate } from '../../lib/format';
import { TX_TYPE_COLOR } from '../../lib/meta';
import MoneyText from '../../components/MoneyText';
import ConfirmDialog from '../../components/ConfirmDialog';

export default function TransactionDetailPage() {
  const { eventId, txId } = useParams();
  const id = Number(eventId);
  const tid = Number(txId);
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  const fileInput = useRef<HTMLInputElement>(null);

  const { data: event } = useEvent(id);
  const { data: tx, isLoading } = useTransaction(id, tid);
  const upload = useUploadReceipt(id, tid);
  const remove = useDeleteTransaction(id);
  const [confirmDelete, setConfirmDelete] = useState(false);

  if (isLoading || !tx || !event) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
        <CircularProgress />
      </Box>
    );
  }

  const currency = event.currency;

  const handleUpload = (file?: File) => {
    if (!file) return;
    upload
      .mutateAsync(file)
      .then(() => enqueueSnackbar('Receipt uploaded', { variant: 'success' }))
      .catch((err) => enqueueSnackbar(errorMessage(err), { variant: 'error' }));
  };

  return (
    <Box sx={{ maxWidth: 820, mx: 'auto' }}>
      <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 2 }}>
        <IconButton onClick={() => navigate(`/events/${id}/transactions`)} size="small">
          <ArrowBackIcon />
        </IconButton>
        <Typography variant="h5" sx={{ flex: 1 }}>
          Transaction Detail
        </Typography>
        {tx.type === 'EXPENSE' && (
          <Button startIcon={<EditIcon />} onClick={() => navigate(`/events/${id}/transactions/${tid}/edit`)}>
            Edit
          </Button>
        )}
        <Button color="error" startIcon={<DeleteIcon />} onClick={() => setConfirmDelete(true)}>
          Delete
        </Button>
      </Stack>

      <Card sx={{ mb: 2 }}>
        <CardContent>
          <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
            <Box>
              <Stack direction="row" spacing={1} alignItems="center">
                <Typography variant="h6">{tx.title}</Typography>
                <Chip label={tx.type} size="small" color={TX_TYPE_COLOR[tx.type]} variant="outlined" />
              </Stack>
              <Typography variant="body2" color="text.secondary">
                {tx.category?.name ?? 'Uncategorized'} · {formatDate(tx.date)}
              </Typography>
            </Box>
            <Typography variant="h6">{formatMoney(tx.amount, currency)}</Typography>
          </Stack>
          {tx.description && (
            <Typography variant="body2" sx={{ mt: 1 }}>
              {tx.description}
            </Typography>
          )}
        </CardContent>
      </Card>

      <Grid container spacing={2}>
        <Grid item xs={12} sm={6}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="subtitle1" gutterBottom>
                {tx.type === 'EXPENSE' || tx.type === 'SPONSOR' ? 'Paid by' : 'Credited'}
              </Typography>
              <Stack spacing={1}>
                {tx.payers.map((p) => (
                  <Stack key={p.memberId} direction="row" justifyContent="space-between">
                    <Typography variant="body2">{p.displayName}</Typography>
                    <MoneyText amount={p.amount} currency={currency} />
                  </Stack>
                ))}
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="subtitle1" gutterBottom>
                {tx.type === 'EXPENSE' || tx.type === 'SPONSOR'
                  ? `Participants & Split (${tx.splitMethod ?? 'EQUAL'})`
                  : 'Debited'}
              </Typography>
              <Stack spacing={1}>
                {tx.splits.map((s) => (
                  <Stack key={s.memberId} direction="row" justifyContent="space-between">
                    <Typography variant="body2">{s.displayName}</Typography>
                    <MoneyText amount={s.shareAmount} currency={currency} />
                  </Stack>
                ))}
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        {tx.sponsors.length > 0 && (
          <Grid item xs={12}>
            <Card>
              <CardContent>
                <Typography variant="subtitle1" gutterBottom>
                  Sponsors
                </Typography>
                <Stack spacing={1}>
                  {tx.sponsors.map((s) => (
                    <Stack key={s.memberId} direction="row" justifyContent="space-between">
                      <Typography variant="body2">{s.displayName}</Typography>
                      <MoneyText amount={s.amount} currency={currency} />
                    </Stack>
                  ))}
                </Stack>
              </CardContent>
            </Card>
          </Grid>
        )}

        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1 }}>
                <Typography variant="subtitle1">Receipts</Typography>
                <Button
                  size="small"
                  startIcon={<UploadFileIcon />}
                  onClick={() => fileInput.current?.click()}
                  disabled={upload.isPending}
                >
                  Upload
                </Button>
                <input
                  ref={fileInput}
                  type="file"
                  accept="image/*,application/pdf"
                  hidden
                  onChange={(e) => handleUpload(e.target.files?.[0])}
                />
              </Stack>
              {tx.receipts.length === 0 ? (
                <Typography variant="body2" color="text.secondary">
                  No receipts attached.
                </Typography>
              ) : (
                <Stack spacing={1}>
                  {tx.receipts.map((r) => (
                    <Stack key={r.id} direction="row" alignItems="center" spacing={1}>
                      <Typography variant="body2" sx={{ flex: 1 }} noWrap>
                        {r.fileName}
                      </Typography>
                      <IconButton size="small" onClick={() => downloadReceipt(r.id, r.fileName)}>
                        <DownloadIcon fontSize="small" />
                      </IconButton>
                    </Stack>
                  ))}
                </Stack>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Divider sx={{ my: 2 }} />
      <Typography variant="caption" color="text.secondary">
        Created {formatDate(tx.createdAt)}
      </Typography>

      <ConfirmDialog
        open={confirmDelete}
        title="Delete transaction?"
        message={`This removes "${tx.title}" and its ledger entries.`}
        confirmLabel="Delete"
        destructive
        onClose={() => setConfirmDelete(false)}
        onConfirm={() =>
          remove
            .mutateAsync(tid)
            .then(() => {
              enqueueSnackbar('Transaction deleted', { variant: 'success' });
              navigate(`/events/${id}/transactions`);
            })
            .catch((err) => enqueueSnackbar(errorMessage(err), { variant: 'error' }))
        }
      />
    </Box>
  );
}
