import { useState, useEffect } from 'react'
import { motion } from 'framer-motion'
import {
  BarChart, Bar, AreaChart, Area, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer, PieChart, Pie, Cell
} from 'recharts'
import {
  TrendingUp, ShoppingCart, DollarSign, Users, Activity,
  Shield, CheckCircle, XCircle, AlertTriangle, Clock, Eye,
  MapPin, Box, RefreshCw, Trophy, Medal, Crown
} from 'lucide-react'
import { getDashboardStats, getAuditTrail } from '../services/api'
import { toast } from 'sonner'

function getStatusDotClass(status) {
  switch (status) {
    case 'SUCCESS': return 'success'
    case 'FAILURE': return 'failure'
    case 'PENDING': return 'pending'
    case 'BLOCKED': return 'blocked'
    default: return 'info'
  }
}

function getStatusIcon(status) {
  switch (status) {
    case 'SUCCESS': return <CheckCircle size={12} />
    case 'FAILURE': return <XCircle size={12} />
    case 'PENDING': return <Clock size={12} />
    case 'BLOCKED': return <AlertTriangle size={12} />
    default: return <Activity size={12} />
  }
}

function formatTime(timestamp) {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return date.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

const CHART_COLORS = ['#528FF0', '#1ABC9C', '#2ecc71', '#f59e0b', '#e74c3c']

export default function DashboardPage() {
  const [stats, setStats] = useState(null)
  const [auditLogs, setAuditLogs] = useState([])
  const [loading, setLoading] = useState(true)
  const [timeRange, setTimeRange] = useState('all')

  useEffect(() => {
    fetchData()
    // Auto-refresh every 10 seconds
    const interval = setInterval(fetchData, 10000)
    return () => clearInterval(interval)
  }, [])

  const fetchData = async () => {
    try {
      const [statsData, auditData] = await Promise.all([
        getDashboardStats(timeRange),
        getAuditTrail(null, timeRange),
      ])
      setStats(statsData)
      setAuditLogs(auditData)
    } catch (error) {
      console.error('Error fetching dashboard data:', error)
      toast.error('Failed to load live data')
    } finally {
      setLoading(false)
    }
  }

  if (loading && !stats) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: 'calc(100vh - 65px)', color: 'var(--text-primary)' }}>
        <Activity className="spin-slow" size={32} />
      </div>
    )
  }

  const statCards = [
    {
      label: 'Total Revenue',
      value: `₹${(stats?.totalRevenue || 0).toLocaleString()}`,
      icon: <DollarSign size={18} />,
      iconBg: '#eafaf1',
      iconColor: '#2ecc71',
      trend: '+12% from last week',
      trendColor: '#2ecc71',
    },
    {
      label: 'Total Orders',
      value: stats?.totalOrders || 0,
      icon: <ShoppingCart size={18} />,
      iconBg: '#e8f0fe',
      iconColor: '#528FF0',
      trend: `${stats?.paidOrders || 0} Paid / ${(stats?.totalOrders || 0) - (stats?.paidOrders || 0)} Pending`,
      trendColor: 'var(--text-muted)',
    },
    {
      label: 'Unique Shoppers',
      value: stats?.uniqueSessions || 0,
      icon: <Users size={18} />,
      iconBg: '#f3e8ff',
      iconColor: '#9b59b6',
      trend: 'Active agent threads',
      trendColor: 'var(--text-muted)',
    },
    {
      label: 'Conversion Rate',
      value: `${stats?.conversionRate?.toFixed(1) || 0}%`,
      icon: <Activity size={18} />,
      iconBg: '#fff8e1',
      iconColor: '#f59e0b',
      trend: 'Orders to sessions',
      trendColor: 'var(--text-muted)',
    },
    {
      label: 'Money Saved by AI',
      value: `₹${(stats?.totalSaved || 0).toLocaleString()}`,
      icon: <CheckCircle size={18} />,
      iconBg: '#e0f7fa',
      iconColor: '#00bcd4',
      trend: 'Total coupon discounts',
      trendColor: 'var(--text-muted)',
    },
  ]

  return (
    <div className="dashboard-page" style={{ background: 'var(--bg-primary)', color: 'var(--text-primary)', minHeight: 'calc(100vh - 65px)' }}>
      <motion.div
        className="dashboard-header"
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        style={{ marginBottom: '2rem' }}
      >
        <h1 style={{ fontSize: '2rem', fontWeight: 800 }}>Merchant Analytics</h1>
        <p style={{ color: 'var(--text-secondary)' }}>Live metrics, intelligent aggregations, and immutable agent audit trails.</p>
      </motion.div>

      {/* Filters */}
      <div style={{ display: 'flex', gap: '10px', marginBottom: '1.5rem', flexWrap: 'wrap' }}>
        <button className={`category-pill ${timeRange === '7days' ? 'active' : ''}`} onClick={() => { setTimeRange('7days'); setTimeout(fetchData, 0) }} style={timeRange === '7days' ? { background: 'var(--rzp-blue)', color: 'white', border: 'none' } : {}}>Last 7 Days</button>
        <button className={`category-pill ${timeRange === '30days' ? 'active' : ''}`} onClick={() => { setTimeRange('30days'); setTimeout(fetchData, 0) }} style={timeRange === '30days' ? { background: 'var(--rzp-blue)', color: 'white', border: 'none' } : {}}>Last 30 Days</button>
        <button className={`category-pill ${timeRange === 'all' ? 'active' : ''}`} onClick={() => { setTimeRange('all'); setTimeout(fetchData, 0) }} style={timeRange === 'all' ? { background: 'var(--rzp-blue)', color: 'white', border: 'none' } : {}}>All Time</button>
        <div style={{ flex: 1 }}></div>
        <button className="btn btn-secondary btn-sm" onClick={fetchData}>
          <RefreshCw size={14} /> Refresh Data
        </button>
      </div>

      {/* Stats Cards */}
      <div className="stats-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1.25rem', marginBottom: '2rem' }}>
        {statCards.map((card, i) => (
          <motion.div
            key={card.label}
            className="stat-card"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.3, delay: i * 0.08 }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
              <span style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', fontWeight: 600 }}>{card.label}</span>
              <div style={{ background: card.iconBg, color: card.iconColor, padding: '6px', borderRadius: '8px', display: 'flex' }}>{card.icon}</div>
            </div>
            <div style={{ fontSize: '1.75rem', fontWeight: 800, margin: '4px 0', fontFamily: 'var(--font-heading)' }}>
              {card.value}
            </div>
            <div style={{ color: card.trendColor, fontSize: '0.8rem', display: 'flex', alignItems: 'center', gap: '4px', fontWeight: 500 }}>
              {card.trendColor === '#2ecc71' && <TrendingUp size={14} />}
              {card.trend}
            </div>
          </motion.div>
        ))}
      </div>

      {/* Recent Purchases */}
      <motion.div
        className="glass-panel"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        style={{ marginBottom: '2rem', padding: '1.5rem' }}
      >
        <h3 style={{ margin: '0 0 1rem 0', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <ShoppingCart size={18} /> Recent Purchases
        </h3>
        <div className="table-responsive" style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid var(--border-color)', color: 'var(--text-secondary)' }}>
                <th style={{ padding: '12px 8px', fontWeight: 600 }}>Order ID</th>
                <th style={{ padding: '12px 8px', fontWeight: 600 }}>Date</th>
                <th style={{ padding: '12px 8px', fontWeight: 600 }}>Customer</th>
                <th style={{ padding: '12px 8px', fontWeight: 600 }}>Items</th>
                <th style={{ padding: '12px 8px', fontWeight: 600 }}>Total</th>
              </tr>
            </thead>
            <tbody>
              {stats?.recentOrders?.length > 0 ? (
                stats.recentOrders.map((order) => (
                  <tr key={order.id} style={{ borderBottom: '1px solid var(--border-color)' }}>
                    <td style={{ padding: '12px 8px', fontWeight: 500, color: 'var(--rzp-blue)' }}>#{order.id}</td>
                    <td style={{ padding: '12px 8px', color: 'var(--text-secondary)', fontSize: '0.9rem' }}>{formatTime(order.date)}</td>
                    <td style={{ padding: '12px 8px' }}>
                      <div style={{ fontWeight: 500 }}>{order.customer}</div>
                      <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>{order.email}</div>
                    </td>
                    <td style={{ padding: '12px 8px', fontSize: '0.9rem' }}>{order.items}</td>
                    <td style={{ padding: '12px 8px', fontWeight: 'bold' }}>₹{order.total}</td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="5" style={{ padding: '20px', textAlign: 'center', color: 'var(--text-muted)' }}>
                    No recent purchases found.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </motion.div>

      {/* Main Content Grid */}
      {stats?.revenueData && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))', gap: '1.5rem', marginBottom: '2rem' }}>
          
          {/* Revenue Timeline */}
          <motion.div className="dashboard-card" initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.4 }}>
            <div className="dashboard-card-title">
              <TrendingUp size={20} style={{ color: '#528FF0' }} /> Revenue Timeline
            </div>
            <div style={{ height: '300px', width: '100%' }}>
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={stats.revenueData} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
                  <defs>
                    <linearGradient id="colorRev" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#528FF0" stopOpacity={0.15}/>
                      <stop offset="95%" stopColor="#528FF0" stopOpacity={0}/>
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--border-color)" vertical={false} />
                  <XAxis dataKey="name" stroke="var(--text-muted)" fontSize={12} tickLine={false} axisLine={false} />
                  <YAxis stroke="var(--text-muted)" fontSize={12} tickLine={false} axisLine={false} tickFormatter={(val) => `₹${val}`} width={60} />
                  <Tooltip 
                    contentStyle={{ background: 'var(--bg-card)', border: '1px solid var(--border-color)', borderRadius: '8px', color: 'var(--text-primary)', boxShadow: 'var(--shadow-md)' }} 
                    itemStyle={{ color: '#528FF0', fontWeight: 600 }}
                  />
                  <Area type="monotone" dataKey="revenue" stroke="#528FF0" strokeWidth={3} fillOpacity={1} fill="url(#colorRev)" />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </motion.div>

          {/* Category Pie */}
          <motion.div className="dashboard-card" initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.5 }}>
            <div className="dashboard-card-title">
              <Box size={20} style={{ color: '#1ABC9C' }} /> Sales By Category
            </div>
            <div style={{ height: '260px', width: '100%' }}>
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={stats.categoryData}
                    cx="50%" cy="50%" innerRadius={70} outerRadius={100} paddingAngle={5} dataKey="value"
                  >
                    {stats.categoryData?.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={CHART_COLORS[index % CHART_COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip 
                    contentStyle={{ background: 'var(--bg-card)', border: '1px solid var(--border-color)', borderRadius: '8px', color: 'var(--text-primary)', boxShadow: 'var(--shadow-md)' }} 
                    itemStyle={{ color: 'var(--text-primary)', fontWeight: 600 }}
                  />
                </PieChart>
              </ResponsiveContainer>
            </div>
            <div style={{ display: 'flex', justifyContent: 'center', gap: '16px', flexWrap: 'wrap', marginTop: '10px' }}>
              {stats.categoryData?.map((entry, i) => (
                <div key={entry.name} style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.85rem', color: 'var(--text-secondary)', fontWeight: 500 }}>
                  <div style={{ width: 10, height: 10, borderRadius: '50%', background: CHART_COLORS[i % CHART_COLORS.length] }} />
                  {entry.name}
                </div>
              ))}
            </div>
          </motion.div>
        </div>
      )}

      {/* Agent Scoreboard */}
      {stats?.topAgents && (
        <motion.div
          className="dashboard-card full-width-section"
          style={{ marginTop: '1rem' }}
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.55 }}
        >
          <div className="dashboard-card-title">
            <Trophy size={20} style={{ color: '#f59e0b' }} />
            Top Selling Agents
            <span style={{ marginLeft: 'auto', fontSize: '0.82rem', color: 'var(--text-muted)', fontWeight: 500 }}>
              Based on Total Revenue
            </span>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {stats.topAgents.map((agent, i) => (
              <div key={agent.agentId || i} style={{
                display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                padding: '12px 16px',
                background: i === 0 ? 'var(--accent-warning-light)' : 'var(--bg-tertiary)',
                borderRadius: '12px',
                border: i === 0 ? '1px solid rgba(245, 158, 11, 0.3)' : '1px solid var(--border-color)',
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                  <div style={{
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    width: '32px', height: '32px', borderRadius: '50%',
                    background: i === 0 ? '#f59e0b' : i === 1 ? '#94a3b8' : i === 2 ? '#b45309' : 'var(--border-color)',
                    color: i < 3 ? 'white' : 'var(--text-muted)', fontWeight: 700, fontSize: '0.85rem',
                  }}>
                    {i === 0 ? <Crown size={16} /> : i === 1 || i === 2 ? <Medal size={16} /> : agent.rank}
                  </div>
                  <div>
                    <div style={{ fontWeight: 700, fontSize: '0.95rem' }}>{agent.name}</div>
                    <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>Agent ID: {agent.agentId}</div>
                  </div>
                </div>
                <div style={{ fontWeight: 800, color: i === 0 ? '#d97706' : 'var(--text-primary)', fontSize: '1.05rem', fontFamily: 'var(--font-heading)' }}>
                  ₹{(agent.revenue || 0).toLocaleString()}
                </div>
              </div>
            ))}
          </div>
        </motion.div>
      )}

      {/* Audit Trail */}
      <motion.div
        className="dashboard-card full-width-section"
        style={{ marginTop: '1.5rem' }}
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.6 }}
      >
        <div className="dashboard-card-title">
          <Shield size={20} style={{ color: '#2ecc71' }} />
          Immutable Audit Trail
          <span style={{ marginLeft: 'auto', fontSize: '0.82rem', color: 'var(--text-muted)', fontWeight: 500 }}>
            {auditLogs.length} entries
          </span>
        </div>

        <div className="audit-trail" style={{ display: 'flex', flexDirection: 'column', gap: '10px', maxHeight: '500px', overflowY: 'auto', paddingRight: '8px' }}>
          {auditLogs.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
              <Eye size={32} style={{ marginBottom: '12px', opacity: 0.5, display: 'block', margin: '0 auto 12px' }} />
              <p>No audit entries yet. Start a conversation to see the trail!</p>
            </div>
          ) : (
            auditLogs.map((log, i) => (
              <motion.div
                key={log.id || i}
                className="audit-entry"
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: i * 0.02 }}
                style={{
                  borderLeft: `3px solid ${log.status === 'SUCCESS' ? '#2ecc71' : log.status === 'FAILURE' ? '#e74c3c' : '#528FF0'}`,
                }}
              >
                <div style={{ flex: 1 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
                    <div style={{ fontSize: '0.85rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '6px' }}>
                      {getStatusIcon(log.status)}
                      {log.actionType?.replace(/_/g, ' ')}
                    </div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 500 }}>{formatTime(log.timestamp)}</div>
                  </div>
                  <div style={{ fontSize: '0.88rem', color: 'var(--text-secondary)', marginBottom: log.agentReasoning ? '10px' : '0', lineHeight: 1.5 }}>{log.description}</div>
                  {log.agentReasoning && (
                    <div className="audit-reasoning">
                      🧠 {log.agentReasoning}
                    </div>
                  )}
                  {log.boundaryCheckPassed !== null && log.boundaryCheckPassed !== undefined && (
                    <div className={`audit-boundary ${log.boundaryCheckPassed ? 'passed' : 'failed'}`}>
                      {log.boundaryCheckPassed ? <CheckCircle size={14} /> : <AlertTriangle size={14} />}
                      {log.boundaryDetails || (log.boundaryCheckPassed ? 'Boundary check passed' : 'Boundary check failed')}
                    </div>
                  )}
                </div>
              </motion.div>
            ))
          )}
        </div>
      </motion.div>
    </div>
  )
}
