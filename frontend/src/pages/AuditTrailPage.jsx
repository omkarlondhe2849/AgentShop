import { useState, useEffect } from 'react';
import { ShieldAlert, Activity, Search, Filter, Calendar } from 'lucide-react';
import { toast } from 'sonner';

export default function AuditTrailPage() {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filterAction, setFilterAction] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    fetchLogs();
  }, []);

  const fetchLogs = async () => {
    try {
      const response = await fetch('/api/dashboard/audit-trail');
      if (response.ok) {
        const data = await response.json();
        setLogs(data);
      } else {
        toast.error('Failed to load audit logs');
      }
    } catch (error) {
      console.error('Error fetching audit logs:', error);
      toast.error('Network error while loading logs');
    } finally {
      setLoading(false);
    }
  };

  const getActionColor = (action) => {
    if (action.includes('SEARCH')) return 'var(--rzp-blue)';
    if (action.includes('CART')) return '#e67e22';
    if (action.includes('CHECKOUT') || action.includes('PAYMENT')) return 'var(--success-color)';
    if (action.includes('ERROR') || action.includes('FAILED')) return 'var(--danger-color)';
    return 'var(--text-secondary)';
  };

  const filteredLogs = logs.filter(log => {
    if (filterAction !== 'ALL' && log.actionType !== filterAction) return false;
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      return (
        log.sessionId?.toLowerCase().includes(q) ||
        log.actionType?.toLowerCase().includes(q) ||
        log.details?.toLowerCase().includes(q)
      );
    }
    return true;
  });

  const uniqueActions = ['ALL', ...new Set(logs.map(l => l.actionType))];

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: 'calc(100vh - 65px)', color: 'var(--text-primary)' }}>
        <div style={{ textAlign: 'center' }}>
          <Activity className="spin-slow" size={40} />
          <p style={{ marginTop: '1rem', color: 'var(--text-secondary)' }}>Loading Audit Trail...</p>
        </div>
      </div>
    );
  }

  return (
    <div style={{ padding: '2rem', maxWidth: '1200px', margin: '0 auto', color: 'var(--text-primary)' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '2rem' }}>
        <ShieldAlert size={28} color="var(--danger-color)" />
        <h1 style={{ fontSize: '2rem', fontWeight: 800 }}>System Audit Trail</h1>
      </div>

      <div style={{ display: 'flex', gap: '16px', marginBottom: '24px', flexWrap: 'wrap' }}>
        <div style={{ flex: 1, minWidth: '250px', position: 'relative' }}>
          <Search size={18} style={{ position: 'absolute', left: '14px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
          <input
            type="text"
            placeholder="Search logs by session, action, or details..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{
              width: '100%', padding: '12px 14px 12px 42px', borderRadius: '12px',
              border: '1px solid var(--border-color)', background: 'var(--bg-secondary)',
              color: 'var(--text-primary)', outline: 'none'
            }}
          />
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <Filter size={18} color="var(--text-secondary)" />
          <select 
            value={filterAction} 
            onChange={(e) => setFilterAction(e.target.value)}
            style={{
              padding: '12px 16px', borderRadius: '12px', border: '1px solid var(--border-color)',
              background: 'var(--bg-secondary)', color: 'var(--text-primary)', outline: 'none',
              cursor: 'pointer'
            }}
          >
            {uniqueActions.map(action => (
              <option key={action} value={action}>{action.replace(/_/g, ' ')}</option>
            ))}
          </select>
        </div>
      </div>

      <div style={{ background: 'var(--bg-secondary)', borderRadius: '16px', border: '1px solid var(--border-color)', overflow: 'hidden' }}>
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
            <thead>
              <tr style={{ background: 'var(--bg-primary)', borderBottom: '1px solid var(--border-color)' }}>
                <th style={{ padding: '16px', color: 'var(--text-secondary)', fontWeight: 600 }}>Timestamp</th>
                <th style={{ padding: '16px', color: 'var(--text-secondary)', fontWeight: 600 }}>Action</th>
                <th style={{ padding: '16px', color: 'var(--text-secondary)', fontWeight: 600 }}>Session ID</th>
                <th style={{ padding: '16px', color: 'var(--text-secondary)', fontWeight: 600 }}>Details</th>
              </tr>
            </thead>
            <tbody>
              {filteredLogs.map((log) => (
                <tr key={log.id} style={{ borderBottom: '1px solid var(--border-color)', transition: 'background 0.2s' }}>
                  <td style={{ padding: '16px', whiteSpace: 'nowrap', color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <Calendar size={14} />
                      {new Date(log.timestamp).toLocaleString()}
                    </div>
                  </td>
                  <td style={{ padding: '16px' }}>
                    <span style={{ 
                      background: `${getActionColor(log.actionType)}20`,
                      color: getActionColor(log.actionType),
                      padding: '4px 10px', borderRadius: '20px', fontSize: '0.8rem', fontWeight: 700
                    }}>
                      {log.actionType}
                    </span>
                  </td>
                  <td style={{ padding: '16px', fontSize: '0.9rem', color: 'var(--text-secondary)', fontFamily: 'monospace' }}>
                    {log.sessionId.substring(0, 16)}...
                  </td>
                  <td style={{ padding: '16px', fontSize: '0.9rem', color: 'var(--text-primary)' }}>
                    {log.details ? (log.details.length > 100 ? log.details.substring(0, 100) + '...' : log.details) : 'No details provided'}
                  </td>
                </tr>
              ))}
              {filteredLogs.length === 0 && (
                <tr>
                  <td colSpan="4" style={{ padding: '32px', textAlign: 'center', color: 'var(--text-secondary)' }}>
                    No audit logs found matching your filters.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
