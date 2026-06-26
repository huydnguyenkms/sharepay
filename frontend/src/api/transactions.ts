import { api } from './client';
import type {
  CreateAdjustmentRequest,
  CreateExpenseRequest,
  CreateRefundRequest,
  ReceiptRef,
  Transaction,
} from './types';

export const transactionsApi = {
  list: (eventId: number) =>
    api.get<Transaction[]>(`/events/${eventId}/transactions`).then((r) => r.data),

  get: (eventId: number, txId: number) =>
    api.get<Transaction>(`/events/${eventId}/transactions/${txId}`).then((r) => r.data),

  createExpense: (eventId: number, body: CreateExpenseRequest) =>
    api.post<Transaction>(`/events/${eventId}/transactions/expenses`, body).then((r) => r.data),

  updateExpense: (eventId: number, txId: number, body: CreateExpenseRequest) =>
    api.put<Transaction>(`/events/${eventId}/transactions/expenses/${txId}`, body).then((r) => r.data),

  createRefund: (eventId: number, body: CreateRefundRequest) =>
    api.post<Transaction>(`/events/${eventId}/transactions/refunds`, body).then((r) => r.data),

  createAdjustment: (eventId: number, body: CreateAdjustmentRequest) =>
    api.post<Transaction>(`/events/${eventId}/transactions/adjustments`, body).then((r) => r.data),

  remove: (eventId: number, txId: number) =>
    api.delete(`/events/${eventId}/transactions/${txId}`).then(() => undefined),

  uploadReceipt: (eventId: number, txId: number, file: File) => {
    const form = new FormData();
    form.append('file', file);
    return api
      .post<ReceiptRef>(`/events/${eventId}/transactions/${txId}/receipts`, form)
      .then((r) => r.data);
  },
};

/** Authenticated download of a stored receipt (token attached by the axios interceptor). */
export async function downloadReceipt(receiptId: number, fileName: string) {
  const response = await api.get(`/receipts/${receiptId}`, { responseType: 'blob' });
  const url = URL.createObjectURL(response.data as Blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
