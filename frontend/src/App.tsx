import { Navigate, Route, Routes } from 'react-router-dom';
import ProtectedRoute from './auth/ProtectedRoute';
import AppShell from './components/AppShell';
import EventLayout from './components/EventLayout';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import WorkspacesPage from './pages/WorkspacesPage';
import EventsPage from './pages/EventsPage';
import DashboardPage from './pages/event/DashboardPage';
import MembersPage from './pages/event/MembersPage';
import TransactionsPage from './pages/event/TransactionsPage';
import TransactionFormPage from './pages/event/TransactionFormPage';
import TransactionDetailPage from './pages/event/TransactionDetailPage';
import SummaryPage from './pages/event/SummaryPage';
import SettlementPage from './pages/event/SettlementPage';

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppShell />}>
          <Route path="/" element={<Navigate to="/workspaces" replace />} />
          <Route path="/workspaces" element={<WorkspacesPage />} />
          <Route path="/workspaces/:workspaceId/events" element={<EventsPage />} />

          <Route path="/events/:eventId" element={<EventLayout />}>
            <Route index element={<Navigate to="dashboard" replace />} />
            <Route path="dashboard" element={<DashboardPage />} />
            <Route path="members" element={<MembersPage />} />
            <Route path="transactions" element={<TransactionsPage />} />
            <Route path="summary" element={<SummaryPage />} />
            <Route path="settlement" element={<SettlementPage />} />
          </Route>

          {/* Transaction add/edit/detail render outside the tab layout for a focused view. */}
          <Route path="/events/:eventId/transactions/new" element={<TransactionFormPage />} />
          <Route path="/events/:eventId/transactions/:txId/edit" element={<TransactionFormPage />} />
          <Route path="/events/:eventId/transactions/:txId" element={<TransactionDetailPage />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
