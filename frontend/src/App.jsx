import { Routes, Route } from 'react-router-dom'
import Navbar from './components/Navbar'
import LandingPage from './pages/LandingPage'
import ChatPage from './pages/ChatPage'
import DashboardPage from './pages/DashboardPage'
import StorefrontPage from './pages/StorefrontPage'
import { Toaster } from 'sonner'

import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import PaymentStatusPage from './pages/PaymentStatusPage'
import InsightsPage from './pages/InsightsPage'
import AuditTrailPage from './pages/AuditTrailPage'

function App() {
  return (
    <>
      <Navbar />
      <div className="page-container">
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route path="/chat" element={<ChatPage />} />
          <Route path="/store" element={<StorefrontPage />} />
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/payment-status" element={<PaymentStatusPage />} />
          <Route path="/insights" element={<InsightsPage />} />
          <Route path="/audit-trail" element={<AuditTrailPage />} />
        </Routes>
      </div>
      <Toaster
        position="top-right"
        toastOptions={{
          style: {
            background: '#111827',
            border: '1px solid rgba(255,255,255,0.08)',
            color: '#f9fafb',
            fontFamily: 'Inter, sans-serif',
          },
        }}
      />
    </>
  )
}

export default App
