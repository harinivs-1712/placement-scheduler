import React from 'react';
import { Database, Calendar, AlertTriangle, ShieldAlert, Cpu } from 'lucide-react';

interface SidebarProps {
  onRefreshAll: () => Promise<void>;
  activeTab: string;
  setActiveTab: (tab: string) => void;
  unscheduledCount: number;
}

export const Sidebar: React.FC<SidebarProps> = ({
  activeTab,
  setActiveTab,
  unscheduledCount,
}) => {
  return (
    <aside className="glass-panel" style={{
      borderRadius: '0',
      borderTop: 'none',
      borderBottom: 'none',
      borderLeft: 'none',
      width: '240px',
      height: '100%',
      padding: '24px 16px',
      display: 'flex',
      flexDirection: 'column',
      gap: '20px',
      background: 'rgba(10, 15, 26, 0.9)',
      borderRight: '1px solid var(--border-color)',
      boxShadow: '4px 0 10px rgba(0,0,0,0.3)',
      flexShrink: 0
    }}>
      
      {/* Brand Logo */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
        <Cpu className="text-glow-cyan" style={{ color: 'var(--color-cyan)', width: '28px', height: '28px' }} />
        <div>
          <h2 style={{ fontSize: '1.1rem', fontWeight: '700', letterSpacing: '0.05em', color: 'var(--text-primary)' }}>
            PLACEMENT INTERVIEW SCHEDULER
          </h2>
          <span style={{ fontSize: '0.65rem', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>
            v2.1 // DISRUPTION OPS
          </span>
        </div>
      </div>

      <hr style={{ borderColor: 'var(--border-color)', margin: '0' }} />

      {/* Navigation Tabs */}
      <nav style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
        <button 
          onClick={() => setActiveTab('dashboard')} 
          className={`tab-button ${activeTab === 'dashboard' ? 'active' : ''}`}
          style={{ width: '100%', textAlign: 'left', display: 'flex', gap: '10px', alignItems: 'center' }}
        >
          <Database size={16} />
          Dashboard Monitor
        </button>
        <button 
          onClick={() => setActiveTab('timeline')} 
          className={`tab-button ${activeTab === 'timeline' ? 'active' : ''}`}
          style={{ width: '100%', textAlign: 'left', display: 'flex', gap: '10px', alignItems: 'center' }}
        >
          <Calendar size={16} />
          Gantt Schedule
        </button>
        <button 
          onClick={() => setActiveTab('disruptions')} 
          className={`tab-button ${activeTab === 'disruptions' ? 'active' : ''}`}
          style={{ width: '100%', textAlign: 'left', display: 'flex', gap: '10px', alignItems: 'center', position: 'relative' }}
        >
          <AlertTriangle size={16} />
          Disruption Console
          {unscheduledCount > 0 && (
            <span className="blink-critical" style={{
              position: 'absolute',
              right: '12px',
              padding: '2px 6px',
              borderRadius: '4px',
              fontSize: '0.6rem',
              fontWeight: 'bold',
              fontFamily: 'var(--font-mono)'
            }}>
              {unscheduledCount}
            </span>
          )}
        </button>
        <button 
          onClick={() => setActiveTab('entities')} 
          className={`tab-button ${activeTab === 'entities' ? 'active' : ''}`}
          style={{ width: '100%', textAlign: 'left', display: 'flex', gap: '10px', alignItems: 'center' }}
        >
          <ShieldAlert size={16} />
          Entity Registry
        </button>
      </nav>

    </aside>
  );
};
