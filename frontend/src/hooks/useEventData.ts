import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { analyticsApi, settlementApi } from '../api/analytics';
import { transactionsApi } from '../api/transactions';
import type {
  CreateAdjustmentRequest,
  CreateExpenseRequest,
  CreateRefundRequest,
} from '../api/types';

/** Anything derived from the ledger must refresh after a transaction changes. */
export function invalidateEventData(qc: ReturnType<typeof useQueryClient>, eventId: number) {
  ['transactions', 'dashboard', 'summary', 'members-summary', 'settlement'].forEach((scope) =>
    qc.invalidateQueries({ queryKey: ['event', eventId, scope] }),
  );
}

export function useTransactions(eventId: number) {
  return useQuery({
    queryKey: ['event', eventId, 'transactions'],
    queryFn: () => transactionsApi.list(eventId),
    enabled: !!eventId,
  });
}

export function useTransaction(eventId: number, txId: number) {
  return useQuery({
    queryKey: ['event', eventId, 'transactions', txId],
    queryFn: () => transactionsApi.get(eventId, txId),
    enabled: !!eventId && !!txId,
  });
}

export function useDashboard(eventId: number) {
  return useQuery({
    queryKey: ['event', eventId, 'dashboard'],
    queryFn: () => analyticsApi.dashboard(eventId),
    enabled: !!eventId,
  });
}

export function useMembersSummary(eventId: number) {
  return useQuery({
    queryKey: ['event', eventId, 'members-summary'],
    queryFn: () => analyticsApi.membersSummary(eventId),
    enabled: !!eventId,
  });
}

export function useSummary(eventId: number) {
  return useQuery({
    queryKey: ['event', eventId, 'summary'],
    queryFn: () => analyticsApi.summary(eventId),
    enabled: !!eventId,
  });
}

export function useCategories(eventId: number) {
  return useQuery({
    queryKey: ['event', eventId, 'categories'],
    queryFn: () => analyticsApi.categories(eventId),
    enabled: !!eventId,
  });
}

export function useSettlement(eventId: number) {
  return useQuery({
    queryKey: ['event', eventId, 'settlement'],
    queryFn: () => settlementApi.get(eventId),
    enabled: !!eventId,
  });
}

export function useMarkSettled(eventId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => settlementApi.settle(eventId),
    onSuccess: () => {
      invalidateEventData(qc, eventId);
      qc.invalidateQueries({ queryKey: ['event', eventId] });
    },
  });
}

export function useCreateExpense(eventId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: CreateExpenseRequest) => transactionsApi.createExpense(eventId, body),
    onSuccess: () => invalidateEventData(qc, eventId),
  });
}

export function useUpdateExpense(eventId: number, txId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: CreateExpenseRequest) => transactionsApi.updateExpense(eventId, txId, body),
    onSuccess: () => invalidateEventData(qc, eventId),
  });
}

export function useCreateRefund(eventId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: CreateRefundRequest) => transactionsApi.createRefund(eventId, body),
    onSuccess: () => invalidateEventData(qc, eventId),
  });
}

export function useCreateAdjustment(eventId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: CreateAdjustmentRequest) => transactionsApi.createAdjustment(eventId, body),
    onSuccess: () => invalidateEventData(qc, eventId),
  });
}

export function useUploadReceipt(eventId: number, txId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (file: File) => transactionsApi.uploadReceipt(eventId, txId, file),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['event', eventId, 'transactions', txId] });
      qc.invalidateQueries({ queryKey: ['event', eventId, 'transactions'] });
    },
  });
}

export function useDeleteTransaction(eventId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (txId: number) => transactionsApi.remove(eventId, txId),
    onSuccess: () => invalidateEventData(qc, eventId),
  });
}
