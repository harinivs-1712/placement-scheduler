import React, { useState } from 'react';
import { Database, Calendar, Play, RefreshCw, AlertTriangle, ShieldAlert, Cpu } from 'lucide-react';
import { api } from '../services/api';

interface SidebarProps {
  totalInterviewsCount: number;
  scheduledCount: number;
  unscheduledCount: number;
  placementDays: number;
  onRefreshAll: () => Promise<void>;
  onDatasetGenerated: (days: number) => void;
  activeTab: string;
  setActiveTab: (tab: string) => void;
}

export const Sidebar: React.FC<SidebarProps> = ({
  totalInterviewsCount,
  unscheduledCount,
  onRefreshAll,
  onDatasetGenerated,
  activeTab,
  setActiveTab,
}) => {
  const [students, setStudents] = useState<number>(100);
  const [companies, setCompanies] = useState<number>(10);
  const [rooms, setRooms] = useState<number>(5);
  const [days, setDays] = useState<number>(3);
  const [generating, setGenerating] = useState<boolean>(false);
  const [scheduling, setScheduling] = useState<boolean>(false);
  const [message, setMessage] = useState<{ text: string; type: 'success' | 'error' } | null>(null);

  const handleGenerateDataset = async (e: React.FormEvent) => {
    e.preventDefault();
    setGenerating(true);
    setMessage(null);
    try {
      await api.generateDataset({ students, companies, rooms, days });
      setMessage({ text: 'Dataset initialized successfully.', type: 'success' });
      onDatasetGenerated(days);
      await onRefreshAll();
    } catch (err: any) {
      console.error(err);
      setMessage({ text: err.message || 'Failed to generate dataset.', type: 'error' });
    } finally {
      setGenerating(false);
    }
  };

  const handleGenerateSchedule = async () => {
    setScheduling(true);
    setMessage(null);
    try {
      await api.generateSchedule();
      setMessage({ text: 'Schedule generated successfully.', type: 'success' });
      await onRefreshAll();
    } catch (err: any) {
      console.error(err);
      setMessage({ text: err.message || 'Failed to compile schedule.', type: 'error' });
    } finally {
      setScheduling(false);
    }
  };



  return (
    <aside className="glass-panel" style={{
      borderRadius: '0',
      borderTop: 'none',
      borderBottom: 'none',
      borderLeft: 'none',
      display: 'flex',
      flexDirection: 'column',
      height: '100vh',
      padding: '24px',
      gap: '24px',
      overflowY: 'auto'
    }}>
      {/* Title */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
        <Cpu className="text-glow-cyan" style={{ color: 'var(--color-cyan)', width: '28px', height: '28px' }} />
        <div>
          <h2 style={{ fontSize: '1.25rem', fontWeight: '700', letterSpacing: '0.05em', color: 'var(--text-primary)' }}>
            COMMAND CENTER
          </h2>
          <span style={{ fontSize: '0.65rem', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>
            v2.1 // DISRUPTION OPS
          </span>
        </div>
      </div>


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

      <hr style={{ borderColor: 'var(--border-color)' }} />

      {/* Operations Panel */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
        <h3 style={{ fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.1em', color: 'var(--text-secondary)' }}>
          SYSTEM INITIALIZATION
        </h3>
        <form onSubmit={handleGenerateDataset} style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
            <div>
              <label style={{ fontSize: '0.65rem', color: 'var(--text-secondary)' }}>Students</label>
              <input 
                type="number" 
                value={students} 
                onChange={(e) => setStudents(parseInt(e.target.value) || 0)}
                style={{
                  width: '100%',
                  background: 'rgba(0,0,0,0.2)',
                  border: '1px solid var(--border-color)',
                  color: 'white',
                  borderRadius: '4px',
                  padding: '4px 8px',
                  fontSize: '0.75rem',
                  fontFamily: 'var(--font-mono)'
                }}
              />
            </div>
            <div>
              <label style={{ fontSize: '0.65rem', color: 'var(--text-secondary)' }}>Companies</label>
              <input 
                type="number" 
                value={companies} 
                onChange={(e) => setCompanies(parseInt(e.target.value) || 0)}
                style={{
                  width: '100%',
                  background: 'rgba(0,0,0,0.2)',
                  border: '1px solid var(--border-color)',
                  color: 'white',
                  borderRadius: '4px',
                  padding: '4px 8px',
                  fontSize: '0.75rem',
                  fontFamily: 'var(--font-mono)'
                }}
              />
            </div>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
            <div>
              <label style={{ fontSize: '0.65rem', color: 'var(--text-secondary)' }}>Rooms</label>
              <input 
                type="number" 
                value={rooms} 
                onChange={(e) => setRooms(parseInt(e.target.value) || 0)}
                style={{
                  width: '100%',
                  background: 'rgba(0,0,0,0.2)',
                  border: '1px solid var(--border-color)',
                  color: 'white',
                  borderRadius: '4px',
                  padding: '4px 8px',
                  fontSize: '0.75rem',
                  fontFamily: 'var(--font-mono)'
                }}
              />
            </div>
            <div>
              <label style={{ fontSize: '0.65rem', color: 'var(--text-secondary)' }}>Placement Days</label>
              <input 
                type="number" 
                value={days} 
                onChange={(e) => setDays(parseInt(e.target.value) || 0)}
                style={{
                  width: '100%',
                  background: 'rgba(0,0,0,0.2)',
                  border: '1px solid var(--border-color)',
                  color: 'white',
                  borderRadius: '4px',
                  padding: '4px 8px',
                  fontSize: '0.75rem',
                  fontFamily: 'var(--font-mono)'
                }}
              />
            </div>
          </div>
          <button 
            type="submit" 
            disabled={generating}
            style={{
              background: 'transparent',
              border: '1px solid var(--color-purple)',
              color: 'var(--color-purple)',
              padding: '6px',
              borderRadius: '4px',
              fontSize: '0.75rem',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '6px',
              fontFamily: 'var(--font-mono)',
              textShadow: '0 0 5px var(--color-purple-glow)',
              transition: 'all 0.2s',
            }}
            className="init-btn"
          >
            <RefreshCw size={12} className={generating ? 'animate-spin' : ''} />
            {generating ? 'GENERATING...' : 'GENERATE DATASET'}
          </button>
        </form>

        <button 
          onClick={handleGenerateSchedule}
          disabled={scheduling || totalInterviewsCount === 0}
          style={{
            background: 'rgba(6, 182, 212, 0.1)',
            border: '1px solid var(--color-cyan)',
            color: 'var(--color-cyan)',
            padding: '8px',
            borderRadius: '4px',
            fontSize: '0.8rem',
            fontWeight: 'bold',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '8px',
            fontFamily: 'var(--font-mono)',
            textShadow: '0 0 5px var(--color-cyan-glow)',
            boxShadow: '0 0 8px rgba(6, 182, 212, 0.2)',
            transition: 'all 0.2s',
            marginTop: '8px'
          }}
          className="pulse-glow-cyan"
        >
          <Play size={14} />
          {scheduling ? 'SCHEDULING...' : 'RUN SOLVER ENGINE'}
        </button>
      </div>

      {message && (
        <div style={{
          padding: '8px 12px',
          borderRadius: '4px',
          fontSize: '0.7rem',
          fontFamily: 'var(--font-mono)',
          background: message.type === 'success' ? 'rgba(16, 185, 129, 0.1)' : 'rgba(244, 63, 94, 0.1)',
          border: `1px solid ${message.type === 'success' ? 'var(--color-emerald)' : 'var(--color-rose)'}`,
          color: message.type === 'success' ? 'var(--color-emerald)' : 'var(--color-rose)'
        }}>
          {message.text}
        </div>
      )}


    </aside>
  );
};
