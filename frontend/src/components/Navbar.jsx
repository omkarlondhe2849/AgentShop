import { NavLink, useNavigate } from 'react-router-dom'
import { Bot, MessageSquare, LayoutDashboard, ShoppingBag, Zap, LogIn, LogOut, UserPlus, User, Moon, Sun, TrendingUp, ShieldAlert } from 'lucide-react'
import { useState, useEffect } from 'react'

export default function Navbar() {
  const navigate = useNavigate()
  const [user, setUser] = useState(null)
  const [darkMode, setDarkMode] = useState(false)

  useEffect(() => {
    const isDark = localStorage.getItem('agentShopDarkMode') === 'true'
    setDarkMode(isDark)
    if (isDark) {
      document.documentElement.classList.add('dark')
    }
  }, [])

  const toggleDarkMode = () => {
    setDarkMode(!darkMode)
    if (!darkMode) {
      document.documentElement.classList.add('dark')
      localStorage.setItem('agentShopDarkMode', 'true')
    } else {
      document.documentElement.classList.remove('dark')
      localStorage.setItem('agentShopDarkMode', 'false')
    }
  }

  const loadUser = () => {
    const userData = localStorage.getItem('agentShopUser')
    if (userData) {
      setUser(JSON.parse(userData))
    } else {
      setUser(null)
    }
  }

  useEffect(() => {
    loadUser()
    window.addEventListener('auth-change', loadUser)
    return () => window.removeEventListener('auth-change', loadUser)
  }, [])

  const handleLogout = () => {
    localStorage.removeItem('agentShopUser')
    localStorage.removeItem('agentShopToken')
    loadUser()
    window.dispatchEvent(new Event('auth-change'))
    navigate('/')
  }

  return (
    <nav className="navbar">
      <NavLink to="/" className="brand-logo-container">
        <div className="brand-icon-wrapper">
          <Bot size={22} />
        </div>
        <div>
          <span className="brand-text">Agent<span>Shop</span></span>
          <div style={{ fontSize: '0.6rem', fontWeight: 700, color: 'var(--text-muted)', letterSpacing: '0.08em', marginTop: '-3px', textTransform: 'uppercase' }}>
            Powered by Razorpay
          </div>
        </div>
      </NavLink>

      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
        <div className="navbar-links">
          <NavLink to="/" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`} end>
            <Zap size={16} />
            <span>Home</span>
          </NavLink>
          <NavLink to="/store" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
            <ShoppingBag size={16} />
            <span>Store</span>
          </NavLink>
          <NavLink to="/chat" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
            <MessageSquare size={16} />
            <span>AI Copilot</span>
          </NavLink>
          <NavLink to="/dashboard" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
            <LayoutDashboard size={16} />
            <span>Dashboard</span>
          </NavLink>
          <NavLink to="/insights" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
            <TrendingUp size={16} />
            <span>Insights</span>
          </NavLink>
          <NavLink to="/audit-trail" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
            <ShieldAlert size={16} />
            <span>Audit Trail</span>
          </NavLink>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', borderLeft: '1px solid var(--border-color)', paddingLeft: '1rem' }}>
          <button 
            onClick={toggleDarkMode} 
            style={{ background: 'transparent', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer', padding: '6px', borderRadius: '50%', display: 'flex', transition: 'color 150ms' }}
            title="Toggle Dark Mode"
          >
            {darkMode ? <Sun size={18} /> : <Moon size={18} />}
          </button>
          {user ? (
            <>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                <div style={{ width: 28, height: 28, borderRadius: '50%', background: 'var(--gradient-primary)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'white', fontSize: '0.75rem', fontWeight: 700 }}>
                  {user.name?.charAt(0)?.toUpperCase() || 'U'}
                </div>
                {user.name}
              </div>
              <button onClick={handleLogout} style={{ display: 'flex', alignItems: 'center', gap: '4px', background: 'transparent', border: 'none', color: 'var(--accent-danger)', fontSize: '0.8rem', fontWeight: 600, cursor: 'pointer', padding: '4px 8px', borderRadius: '6px', transition: 'background 150ms' }}
                onMouseEnter={e => e.currentTarget.style.background = 'var(--accent-danger-light)'}
                onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
              >
                <LogOut size={14} /> Logout
              </button>
            </>
          ) : (
            <>
              <NavLink to="/login" style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-secondary)', textDecoration: 'none' }}>
                <LogIn size={14} /> Login
              </NavLink>
              <NavLink to="/register" className="btn btn-primary btn-sm" style={{ fontSize: '0.8rem' }}>
                <Zap size={14} /> Get Started
              </NavLink>
            </>
          )}
        </div>
      </div>
    </nav>
  )
}
