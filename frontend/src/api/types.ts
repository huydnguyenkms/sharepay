// TypeScript mirrors of the backend DTOs (see com.sharepay.dto.*).

export type Role = 'OWNER' | 'ADMIN' | 'MEMBER' | 'VIEWER';

export type EventType =
  | 'TRAVEL' | 'PARTY' | 'DINING' | 'SPORTS' | 'WORKSHOP'
  | 'BUSINESS' | 'FAMILY' | 'COMMUNITY' | 'OTHER';

export type EventStatus = 'ACTIVE' | 'COMPLETED' | 'ARCHIVED';

export type TransactionType = 'EXPENSE' | 'SPONSOR' | 'REFUND' | 'ADJUSTMENT';

export type SplitMethod = 'EQUAL' | 'EXACT' | 'PERCENTAGE' | 'WEIGHT';

export interface User {
  id: number;
  email: string;
  displayName: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface Workspace {
  id: number;
  name: string;
  description: string | null;
  role: Role;
  memberCount: number;
  eventCount: number;
  createdAt: string;
}

export interface WorkspaceMember {
  id: number;
  userId: number;
  email: string;
  displayName: string;
  role: Role;
}

export interface Event {
  id: number;
  workspaceId: number;
  name: string;
  description: string | null;
  type: EventType;
  currency: string;
  startDate: string | null;
  endDate: string | null;
  status: EventStatus;
  memberCount: number;
  settledAt: string | null;
  createdAt: string;
}

export interface EventMember {
  id: number;
  displayName: string;
  email: string | null;
  phone: string | null;
  userId: number | null;
}

export interface KnownMember {
  displayName: string;
  email: string | null;
  phone: string | null;
  userId: number | null;
}

export interface MemberSummary {
  memberId: number;
  displayName: string;
  paid: number;
  owed: number;
  sponsored: number;
  balance: number;
}

export interface CategoryRef {
  id: number;
  name: string;
}

export interface MemberAmount {
  memberId: number;
  displayName: string;
  amount: number;
}

export interface MemberShare {
  memberId: number;
  displayName: string;
  shareAmount: number;
  inputValue: number | null;
}

export interface ReceiptRef {
  id: number;
  fileName: string;
  contentType: string;
  sizeBytes: number;
}

export interface Transaction {
  id: number;
  type: TransactionType;
  title: string;
  category: CategoryRef | null;
  amount: number;
  date: string;
  description: string | null;
  splitMethod: SplitMethod | null;
  payers: MemberAmount[];
  splits: MemberShare[];
  sponsors: MemberAmount[];
  receipts: ReceiptRef[];
  createdAt: string;
}

export interface EventTotals {
  totalExpense: number;
  totalPaid: number;
  totalSponsored: number;
  netShared: number;
  participantCount: number;
}

export interface CategoryBreakdown {
  category: string;
  amount: number;
  percentage: number;
}

export interface DashboardResponse {
  totals: EventTotals;
  expenseByCategory: CategoryBreakdown[];
  expenseByMember: MemberAmount[];
  memberBalances: MemberSummary[];
  recentTransactions: Transaction[];
}

export interface SummaryResponse {
  totals: EventTotals;
  expenseByCategory: CategoryBreakdown[];
  memberBalances: MemberSummary[];
}

export interface Transfer {
  fromMemberId: number;
  fromName: string;
  toMemberId: number;
  toName: string;
  amount: number;
}

export interface Settlement {
  transfers: Transfer[];
  transferCount: number;
  totalAmount: number;
  settled: boolean;
  settledAt: string | null;
}

// --- Request payloads ---

export interface PayerInput {
  memberId: number;
  amount: number;
}

export interface ParticipantInput {
  memberId: number;
  value: number | null;
}

export interface SponsorInput {
  memberId: number;
  amount: number;
}

export interface CreateExpenseRequest {
  title: string;
  categoryId: number | null;
  amount: number;
  date: string;
  description: string | null;
  splitMethod: SplitMethod;
  payers: PayerInput[];
  participants: ParticipantInput[];
  sponsors: SponsorInput[];
}

export interface CreateRefundRequest {
  title: string;
  categoryId: number | null;
  amount: number;
  date: string;
  description: string | null;
  receiverMemberId: number;
  beneficiaryMemberIds: number[];
}

export interface CreateAdjustmentRequest {
  title: string;
  amount: number;
  date: string;
  description: string | null;
  debitMemberId: number;
  creditMemberId: number;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  fieldErrors: Record<string, string> | null;
}
