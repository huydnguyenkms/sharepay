import type { EventStatus, EventType, TransactionType } from '../api/types';

export const EVENT_TYPES: EventType[] = [
  'TRAVEL', 'PARTY', 'DINING', 'SPORTS', 'WORKSHOP', 'BUSINESS', 'FAMILY', 'COMMUNITY', 'OTHER',
];

export const EVENT_TYPE_COLOR: Record<EventType, string> = {
  TRAVEL: '#2563eb',
  PARTY: '#db2777',
  DINING: '#ea580c',
  SPORTS: '#16a34a',
  WORKSHOP: '#7c3aed',
  BUSINESS: '#0891b2',
  FAMILY: '#d97706',
  COMMUNITY: '#4f46e5',
  OTHER: '#6b7280',
};

export const STATUS_COLOR: Record<EventStatus, 'success' | 'default' | 'warning'> = {
  ACTIVE: 'success',
  COMPLETED: 'default',
  ARCHIVED: 'warning',
};

export const TX_TYPE_COLOR: Record<TransactionType, 'error' | 'success' | 'info' | 'warning'> = {
  EXPENSE: 'error',
  SPONSOR: 'info',
  REFUND: 'success',
  ADJUSTMENT: 'warning',
};

export const CHART_COLORS = [
  '#5b5bd6', '#7c3aed', '#ea580c', '#16a34a', '#0891b2', '#db2777', '#d97706', '#6b7280',
];

export function balanceColor(balance: number): 'success.main' | 'error.main' | 'text.secondary' {
  if (balance > 0) return 'success.main';
  if (balance < 0) return 'error.main';
  return 'text.secondary';
}
