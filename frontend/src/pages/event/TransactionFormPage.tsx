import { useEffect, useMemo, useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Checkbox,
  Chip,
  CircularProgress,
  Divider,
  FormControlLabel,
  Grid,
  IconButton,
  InputAdornment,
  MenuItem,
  Radio,
  RadioGroup,
  Stack,
  Tab,
  Tabs,
  TextField,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import dayjs from 'dayjs';
import { useNavigate, useParams } from 'react-router-dom';
import { useSnackbar } from 'notistack';
import { useEvent } from '../../hooks/useEvents';
import { useMembers } from '../../hooks/useMembers';
import {
  useCategories,
  useCreateAdjustment,
  useCreateExpense,
  useCreateRefund,
  useTransaction,
  useUpdateExpense,
} from '../../hooks/useEventData';
import type { SplitMethod } from '../../api/types';
import { errorMessage } from '../../api/client';
import { formatMoney } from '../../lib/money';

type FormType = 'EXPENSE' | 'REFUND' | 'ADJUSTMENT';
const SPLIT_METHODS: SplitMethod[] = ['EQUAL', 'EXACT', 'PERCENTAGE', 'WEIGHT'];

interface AmountRow {
  memberId: number | '';
  amount: string;
}

export default function TransactionFormPage() {
  const { eventId, txId } = useParams();
  const id = Number(eventId);
  const editingId = txId ? Number(txId) : undefined;
  const navigate = useNavigate();
  const { enqueueSnackbar } = useSnackbar();

  const { data: event } = useEvent(id);
  const { data: members } = useMembers(id);
  const { data: categories } = useCategories(id);
  const { data: existing } = useTransaction(id, editingId ?? 0);

  const createExpense = useCreateExpense(id);
  const updateExpense = useUpdateExpense(id, editingId ?? 0);
  const createRefund = useCreateRefund(id);
  const createAdjustment = useCreateAdjustment(id);

  const [type, setType] = useState<FormType>('EXPENSE');
  const [title, setTitle] = useState('');
  const [categoryId, setCategoryId] = useState<number | ''>('');
  const [amount, setAmount] = useState('');
  const [date, setDate] = useState(dayjs());
  const [description, setDescription] = useState('');

  // Expense
  const [payMode, setPayMode] = useState<'single' | 'multiple'>('single');
  const [singlePayer, setSinglePayer] = useState<number | ''>('');
  const [payers, setPayers] = useState<AmountRow[]>([{ memberId: '', amount: '' }]);
  const [participantIds, setParticipantIds] = useState<number[]>([]);
  const [splitMethod, setSplitMethod] = useState<SplitMethod>('EQUAL');
  const [splitValues, setSplitValues] = useState<Record<number, string>>({});
  const [sponsors, setSponsors] = useState<AmountRow[]>([]);

  // Refund
  const [receiverId, setReceiverId] = useState<number | ''>('');
  const [beneficiaryIds, setBeneficiaryIds] = useState<number[]>([]);

  // Adjustment
  const [fromId, setFromId] = useState<number | ''>('');
  const [toId, setToId] = useState<number | ''>('');

  const [submitting, setSubmitting] = useState(false);

  // Default all members as participants/beneficiaries once members load.
  useEffect(() => {
    if (members && participantIds.length === 0 && !editingId) {
      const all = members.map((m) => m.id);
      setParticipantIds(all);
      setBeneficiaryIds(all);
    }
  }, [members, editingId, participantIds.length]);

  // Prefill when editing an expense.
  useEffect(() => {
    if (!existing || !editingId) return;
    setType('EXPENSE');
    setTitle(existing.title);
    setCategoryId(existing.category?.id ?? '');
    setAmount(String(existing.amount));
    setDate(dayjs(existing.date));
    setDescription(existing.description ?? '');
    setSplitMethod(existing.splitMethod ?? 'EQUAL');
    if (existing.payers.length === 1) {
      setPayMode('single');
      setSinglePayer(existing.payers[0].memberId);
    } else {
      setPayMode('multiple');
      setPayers(existing.payers.map((p) => ({ memberId: p.memberId, amount: String(p.amount) })));
    }
    setParticipantIds(existing.splits.map((s) => s.memberId));
    setSplitValues(
      Object.fromEntries(
        existing.splits.filter((s) => s.inputValue != null).map((s) => [s.memberId, String(s.inputValue)]),
      ),
    );
    setSponsors(existing.sponsors.map((s) => ({ memberId: s.memberId, amount: String(s.amount) })));
  }, [existing, editingId]);

  const currency = event?.currency ?? 'VND';
  const amountNum = Number(amount) || 0;
  const sponsoredTotal = useMemo(
    () => sponsors.reduce((sum, s) => sum + (Number(s.amount) || 0), 0),
    [sponsors],
  );
  const net = Math.max(amountNum - sponsoredTotal, 0);

  if (!event || !members) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
        <CircularProgress />
      </Box>
    );
  }

  const memberName = (mid: number) => members.find((m) => m.id === mid)?.displayName ?? `#${mid}`;

  const toggleParticipant = (mid: number) =>
    setParticipantIds((prev) => (prev.includes(mid) ? prev.filter((x) => x !== mid) : [...prev, mid]));
  const toggleBeneficiary = (mid: number) =>
    setBeneficiaryIds((prev) => (prev.includes(mid) ? prev.filter((x) => x !== mid) : [...prev, mid]));

  const submit = async () => {
    setSubmitting(true);
    try {
      if (type === 'EXPENSE') {
        const payerList =
          payMode === 'single'
            ? [{ memberId: Number(singlePayer), amount: amountNum }]
            : payers.map((p) => ({ memberId: Number(p.memberId), amount: Number(p.amount) || 0 }));
        const body = {
          title,
          categoryId: categoryId === '' ? null : Number(categoryId),
          amount: amountNum,
          date: date.format('YYYY-MM-DD'),
          description: description || null,
          splitMethod,
          payers: payerList,
          participants: participantIds.map((mid) => ({
            memberId: mid,
            value: splitMethod === 'EQUAL' ? null : Number(splitValues[mid]) || 0,
          })),
          sponsors: sponsors
            .filter((s) => s.memberId !== '' && s.amount)
            .map((s) => ({ memberId: Number(s.memberId), amount: Number(s.amount) })),
        };
        const tx = editingId ? await updateExpense.mutateAsync(body) : await createExpense.mutateAsync(body);
        enqueueSnackbar('Expense saved', { variant: 'success' });
        navigate(`/events/${id}/transactions/${tx.id}`);
      } else if (type === 'REFUND') {
        const tx = await createRefund.mutateAsync({
          title,
          categoryId: categoryId === '' ? null : Number(categoryId),
          amount: amountNum,
          date: date.format('YYYY-MM-DD'),
          description: description || null,
          receiverMemberId: Number(receiverId),
          beneficiaryMemberIds: beneficiaryIds,
        });
        enqueueSnackbar('Refund saved', { variant: 'success' });
        navigate(`/events/${id}/transactions/${tx.id}`);
      } else {
        const tx = await createAdjustment.mutateAsync({
          title,
          amount: amountNum,
          date: date.format('YYYY-MM-DD'),
          description: description || null,
          debitMemberId: Number(fromId),
          creditMemberId: Number(toId),
        });
        enqueueSnackbar('Adjustment saved', { variant: 'success' });
        navigate(`/events/${id}/transactions/${tx.id}`);
      }
    } catch (err) {
      enqueueSnackbar(errorMessage(err), { variant: 'error' });
    } finally {
      setSubmitting(false);
    }
  };

  const payerSum = payers.reduce((s, p) => s + (Number(p.amount) || 0), 0);

  return (
    <Box sx={{ maxWidth: 760, mx: 'auto' }}>
      <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 2 }}>
        <IconButton onClick={() => navigate(-1)} size="small">
          <ArrowBackIcon />
        </IconButton>
        <Typography variant="h5">{editingId ? 'Edit Transaction' : 'Add Transaction'}</Typography>
      </Stack>

      {!editingId && (
        <Tabs value={type} onChange={(_, v) => setType(v)} sx={{ mb: 2 }}>
          <Tab value="EXPENSE" label="Expense" />
          <Tab value="REFUND" label="Refund" />
          <Tab value="ADJUSTMENT" label="Adjustment" />
        </Tabs>
      )}

      <Card>
        <CardContent>
          <Stack spacing={2}>
            <Typography variant="subtitle1">Basic Information</Typography>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={8}>
                <TextField label="Title" value={title} onChange={(e) => setTitle(e.target.value)} fullWidth />
              </Grid>
              {type !== 'ADJUSTMENT' && (
                <Grid item xs={12} sm={4}>
                  <TextField
                    select
                    label="Category"
                    value={categoryId}
                    onChange={(e) => setCategoryId(e.target.value === '' ? '' : Number(e.target.value))}
                    fullWidth
                  >
                    <MenuItem value="">None</MenuItem>
                    {categories?.map((c) => (
                      <MenuItem key={c.id} value={c.id}>
                        {c.name}
                      </MenuItem>
                    ))}
                  </TextField>
                </Grid>
              )}
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Amount"
                  type="number"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  fullWidth
                  InputProps={{ endAdornment: <InputAdornment position="end">{currency}</InputAdornment> }}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <DatePicker label="Date" value={date} onChange={(v) => v && setDate(v)} slotProps={{ textField: { fullWidth: true } }} />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  label="Description (optional)"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  fullWidth
                  multiline
                  minRows={2}
                />
              </Grid>
            </Grid>

            {type === 'EXPENSE' && (
              <>
                <Divider />
                <Typography variant="subtitle1">Paid by</Typography>
                <RadioGroup row value={payMode} onChange={(e) => setPayMode(e.target.value as 'single' | 'multiple')}>
                  <FormControlLabel value="single" control={<Radio />} label="Single payer" />
                  <FormControlLabel value="multiple" control={<Radio />} label="Multiple payers" />
                </RadioGroup>
                {payMode === 'single' ? (
                  <TextField
                    select
                    label="Payer"
                    value={singlePayer}
                    onChange={(e) => setSinglePayer(Number(e.target.value))}
                    sx={{ maxWidth: 320 }}
                  >
                    {members.map((m) => (
                      <MenuItem key={m.id} value={m.id}>
                        {m.displayName}
                      </MenuItem>
                    ))}
                  </TextField>
                ) : (
                  <Stack spacing={1}>
                    {payers.map((p, idx) => (
                      <Stack key={idx} direction="row" spacing={1}>
                        <TextField
                          select
                          label="Payer"
                          value={p.memberId}
                          onChange={(e) =>
                            setPayers((prev) =>
                              prev.map((row, i) => (i === idx ? { ...row, memberId: Number(e.target.value) } : row)),
                            )
                          }
                          sx={{ flex: 1 }}
                        >
                          {members.map((m) => (
                            <MenuItem key={m.id} value={m.id}>
                              {m.displayName}
                            </MenuItem>
                          ))}
                        </TextField>
                        <TextField
                          label="Amount"
                          type="number"
                          value={p.amount}
                          onChange={(e) =>
                            setPayers((prev) => prev.map((row, i) => (i === idx ? { ...row, amount: e.target.value } : row)))
                          }
                          sx={{ width: 160 }}
                        />
                        <IconButton onClick={() => setPayers((prev) => prev.filter((_, i) => i !== idx))}>
                          <DeleteIcon />
                        </IconButton>
                      </Stack>
                    ))}
                    <Box>
                      <Button startIcon={<AddIcon />} onClick={() => setPayers((prev) => [...prev, { memberId: '', amount: '' }])}>
                        Add payer
                      </Button>
                      <Typography
                        variant="caption"
                        sx={{ ml: 2, color: payerSum === amountNum ? 'success.main' : 'error.main' }}
                      >
                        Payers total {formatMoney(payerSum, currency)} / {formatMoney(amountNum, currency)}
                      </Typography>
                    </Box>
                  </Stack>
                )}

                <Divider />
                <Stack direction="row" justifyContent="space-between" alignItems="center" flexWrap="wrap" gap={1}>
                  <Typography variant="subtitle1">Participants &amp; Split</Typography>
                  <TextField
                    select
                    size="small"
                    label="Split method"
                    value={splitMethod}
                    onChange={(e) => setSplitMethod(e.target.value as SplitMethod)}
                    sx={{ width: 180 }}
                  >
                    {SPLIT_METHODS.map((m) => (
                      <MenuItem key={m} value={m}>
                        {m}
                      </MenuItem>
                    ))}
                  </TextField>
                </Stack>
                <Stack>
                  {members.map((m) => {
                    const selected = participantIds.includes(m.id);
                    return (
                      <Stack key={m.id} direction="row" alignItems="center" spacing={1}>
                        <FormControlLabel
                          control={<Checkbox checked={selected} onChange={() => toggleParticipant(m.id)} />}
                          label={m.displayName}
                          sx={{ flex: 1 }}
                        />
                        {selected && splitMethod !== 'EQUAL' && (
                          <TextField
                            size="small"
                            type="number"
                            label={splitMethod === 'PERCENTAGE' ? '%' : splitMethod === 'WEIGHT' ? 'weight' : 'amount'}
                            value={splitValues[m.id] ?? ''}
                            onChange={(e) => setSplitValues((prev) => ({ ...prev, [m.id]: e.target.value }))}
                            sx={{ width: 140 }}
                          />
                        )}
                      </Stack>
                    );
                  })}
                  {splitMethod === 'EQUAL' && participantIds.length > 0 && (
                    <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5 }}>
                      Each pays ~{formatMoney(net / participantIds.length, currency)} (net {formatMoney(net, currency)})
                    </Typography>
                  )}
                </Stack>

                <Divider />
                <Typography variant="subtitle1">Sponsors (optional)</Typography>
                <Stack spacing={1}>
                  {sponsors.map((s, idx) => (
                    <Stack key={idx} direction="row" spacing={1}>
                      <TextField
                        select
                        label="Sponsor"
                        value={s.memberId}
                        onChange={(e) =>
                          setSponsors((prev) => prev.map((row, i) => (i === idx ? { ...row, memberId: Number(e.target.value) } : row)))
                        }
                        sx={{ flex: 1 }}
                      >
                        {members.map((m) => (
                          <MenuItem key={m.id} value={m.id}>
                            {m.displayName}
                          </MenuItem>
                        ))}
                      </TextField>
                      <TextField
                        label="Amount"
                        type="number"
                        value={s.amount}
                        onChange={(e) =>
                          setSponsors((prev) => prev.map((row, i) => (i === idx ? { ...row, amount: e.target.value } : row)))
                        }
                        sx={{ width: 160 }}
                      />
                      <IconButton onClick={() => setSponsors((prev) => prev.filter((_, i) => i !== idx))}>
                        <DeleteIcon />
                      </IconButton>
                    </Stack>
                  ))}
                  <Box>
                    <Button startIcon={<AddIcon />} onClick={() => setSponsors((prev) => [...prev, { memberId: '', amount: '' }])}>
                      Add sponsor
                    </Button>
                    {sponsoredTotal > 0 && (
                      <Chip size="small" sx={{ ml: 2 }} label={`Sponsored ${formatMoney(sponsoredTotal, currency)}`} />
                    )}
                  </Box>
                </Stack>
              </>
            )}

            {type === 'REFUND' && (
              <>
                <Divider />
                <TextField
                  select
                  label="Refund received by"
                  value={receiverId}
                  onChange={(e) => setReceiverId(Number(e.target.value))}
                  sx={{ maxWidth: 320 }}
                >
                  {members.map((m) => (
                    <MenuItem key={m.id} value={m.id}>
                      {m.displayName}
                    </MenuItem>
                  ))}
                </TextField>
                <Typography variant="subtitle1">Credited back to</Typography>
                <Stack>
                  {members.map((m) => (
                    <FormControlLabel
                      key={m.id}
                      control={<Checkbox checked={beneficiaryIds.includes(m.id)} onChange={() => toggleBeneficiary(m.id)} />}
                      label={m.displayName}
                    />
                  ))}
                </Stack>
              </>
            )}

            {type === 'ADJUSTMENT' && (
              <>
                <Divider />
                <Typography variant="body2" color="text.secondary">
                  Move money from one member to another (e.g. a manual correction).
                </Typography>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                  <TextField select label="From (pays)" value={fromId} onChange={(e) => setFromId(Number(e.target.value))} fullWidth>
                    {members.map((m) => (
                      <MenuItem key={m.id} value={m.id}>
                        {m.displayName}
                      </MenuItem>
                    ))}
                  </TextField>
                  <TextField select label="To (receives)" value={toId} onChange={(e) => setToId(Number(e.target.value))} fullWidth>
                    {members.map((m) => (
                      <MenuItem key={m.id} value={m.id}>
                        {memberName(m.id)}
                      </MenuItem>
                    ))}
                  </TextField>
                </Stack>
              </>
            )}

            <Divider />
            <Stack direction="row" justifyContent="flex-end" spacing={1}>
              <Button onClick={() => navigate(-1)}>Cancel</Button>
              <Button variant="contained" onClick={submit} disabled={submitting || !title.trim() || amountNum <= 0}>
                {editingId ? 'Save changes' : 'Create'}
              </Button>
            </Stack>
          </Stack>
        </CardContent>
      </Card>
    </Box>
  );
}
