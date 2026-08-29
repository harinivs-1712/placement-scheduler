import React, { useState } from 'react';
import type { ReplanRun, UnscheduledReason, Interview } from '../services/api';
import { TrendingUp, AlertOctagon, RefreshCw, CheckCircle2 } from 'lucide-react';
import { api } from '../services/api';

interface DashboardProps {
  replanRuns: ReplanRun[];
  unscheduledReasons: UnscheduledReason[];
  interviews: Interview[];
  scheduledCount: number;
  unscheduledCount: number;
  onRefreshAll: () => Promise<void>;
  onDatasetGenerated: (days: number) => void;
}

export const Dashboard: React.FC<DashboardProps> = ({
  replanRuns,
  unscheduledReasons,
  interviews,
  scheduledCount,
  unscheduledCount,
  onRefreshAll,
  onDatasetGenerated
}) => {
  const totalCount = interviews.length;
  const successRate = totalCount > 0 ? Math.round((scheduledCount / totalCount) * 100) : 0;

  const [students, setStudents] = useState<number | ''>('');
  const [companies, setCompanies] = useState<number | ''>('');
  const [rooms, setRooms] = useState<number | ''>('');
  const [days, setDays] = useState<number | ''>('');
  const [generating, setGenerating] = useState<boolean>(false);
  const [message, setMessage] = useState<{ text: string; type: 'success' | 'error' | 'info' } | null>(null);

  const sleep = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

  const handleGenerateDataset = async (e: React.FormEvent) => {
    e.preventDefault();
    setGenerating(true);
    setMessage({ text: 'Initializing dataset parameter generation...', type: 'info' });
    try {
      const finalStudents = students === '' ? 100 : students;
      const finalCompanies = companies === '' ? 10 : companies;
      const finalRooms = rooms === '' ? 5 : rooms;
      const finalDays = days === '' ? 3 : days;

      // Step 1: Generate Dataset
      await api.generateDataset({
        students: finalStudents,
        companies: finalCompanies,
        rooms: finalRooms,
        days: finalDays
      });
      setMessage({ text: 'Dataset generated successfully.', type: 'success' });
      
      // Wait for user to read the success
      await sleep(1500);

      // Step 2: Show Scheduling
      setMessage({ text: 'Running scheduling engine to allocate slots and resolve constraints...', type: 'info' });
      await sleep(1500);

      // Step 3: Trigger Schedule Generator
      await api.generateSchedule();
      setMessage({ text: 'Schedule generated successfully. Synchronizing dashboard...', type: 'success' });
      await sleep(1000);

      onDatasetGenerated(finalDays);
      await onRefreshAll();
      setMessage({ text: 'Dataset and optimal schedule generated successfully! All interview slots allocated.', type: 'success' });
    } catch (err: any) {
      console.error(err);
      setMessage({ text: err.message || 'Failed to generate dataset and schedule.', type: 'error' });
    } finally {
      setGenerating(false);
    }
  };



  // Compute bottleneck counts from unscheduled reasons
  const bottleneckCounts: Record<string, number> = {
    STUDENT_UNAVAILABLE: 0,
    PANEL_UNAVAILABLE: 0,
    ROOM_UNAVAILABLE: 0,
    NO_VALID_TIME_SLOTS: 0,
    NO_COMPANY_SLOT: 0,
    OTHER: 0
  };

  unscheduledReasons.forEach(ur => {
    const reasonText = ur.reason.toUpperCase();
    if (reasonText.includes('STUDENT_UNAVAILABLE')) {
      bottleneckCounts.STUDENT_UNAVAILABLE++;
    } else if (reasonText.includes('PANEL_UNAVAILABLE')) {
      bottleneckCounts.PANEL_UNAVAILABLE++;
    } else if (reasonText.includes('ROOM_UNAVAILABLE')) {
      bottleneckCounts.ROOM_UNAVAILABLE++;
    } else if (reasonText.includes('NO_VALID_TIME_SLOTS')) {
      bottleneckCounts.NO_VALID_TIME_SLOTS++;
    } else if (reasonText.includes('NO_COMPANY_SLOT') || reasonText.includes('COMPANY_NOT_FOUND')) {
      bottleneckCounts.NO_COMPANY_SLOT++;
    } else {
      bottleneckCounts.OTHER++;
    }
  });

  const maxReasonCount = Math.max(...Object.values(bottleneckCounts), 1);

  // Format date utility
  const formatTime = (dateStr: string) => {
    if (!dateStr) return 'N/A';
    try {
      const d = new Date(dateStr);
      if (isNaN(d.getTime())) return dateStr;
      return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } catch {
      return dateStr;
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '32px', animation: 'fadeIn 0.4s' }}>
      
      {/* System Initialization Section (Deck card on Dashboard Home) */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
        <div>
          <h1 style={{ fontSize: '1.75rem', fontWeight: '600', letterSpacing: '-0.02em', color: 'var(--text-primary)' }}>
            System Initialization
          </h1>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
            Configure parameters and generate the mock dataset for scheduler simulation.
          </p>
        </div>

        <div className="glass-panel" style={{ padding: '32px', display: 'flex', flexDirection: 'column', gap: '24px' }}>
          <form onSubmit={handleGenerateDataset} style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
            <div style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(4, 1fr)',
              gap: '20px'
            }}>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                <label style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Students</label>
                <input 
                  type="number" 
                  value={students} 
                  placeholder="e.g. 100"
                  onChange={(e) => setStudents(e.target.value === '' ? '' : parseInt(e.target.value) || 0)}
                  style={{
                    background: 'rgba(0,0,0,0.3)',
                    border: '1px solid var(--border-color)',
                    color: 'white',
                    borderRadius: '4px',
                    padding: '10px 14px',
                    fontSize: '0.9rem',
                    fontFamily: 'var(--font-mono)'
                  }}
                />
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                <label style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Companies</label>
                <input 
                  type="number" 
                  value={companies} 
                  placeholder="e.g. 10"
                  onChange={(e) => setCompanies(e.target.value === '' ? '' : parseInt(e.target.value) || 0)}
                  style={{
                    background: 'rgba(0,0,0,0.3)',
                    border: '1px solid var(--border-color)',
                    color: 'white',
                    borderRadius: '4px',
                    padding: '10px 14px',
                    fontSize: '0.9rem',
                    fontFamily: 'var(--font-mono)'
                  }}
                />
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                <label style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Rooms</label>
                <input 
                  type="number" 
                  value={rooms} 
                  placeholder="e.g. 5"
                  onChange={(e) => setRooms(e.target.value === '' ? '' : parseInt(e.target.value) || 0)}
                  style={{
                    background: 'rgba(0,0,0,0.3)',
                    border: '1px solid var(--border-color)',
                    color: 'white',
                    borderRadius: '4px',
                    padding: '10px 14px',
                    fontSize: '0.9rem',
                    fontFamily: 'var(--font-mono)'
                  }}
                />
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                <label style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Placement Days</label>
                <input 
                  type="number" 
                  value={days} 
                  placeholder="e.g. 3"
                  onChange={(e) => setDays(e.target.value === '' ? '' : parseInt(e.target.value) || 0)}
                  style={{
                    background: 'rgba(0,0,0,0.3)',
                    border: '1px solid var(--border-color)',
                    color: 'white',
                    borderRadius: '4px',
                    padding: '10px 14px',
                    fontSize: '0.9rem',
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
                padding: '10px 24px',
                borderRadius: '4px',
                fontSize: '0.875rem',
                fontWeight: 'bold',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '6px',
                fontFamily: 'var(--font-mono)',
                textShadow: '0 0 5px var(--color-purple-glow)',
                boxShadow: '0 0 8px rgba(168, 85, 247, 0.15)',
                transition: 'all 0.2s',
                height: '42px',
                width: 'fit-content'
              }}
              className="init-btn"
            >
              <RefreshCw size={14} className={generating ? 'animate-spin' : ''} />
              {generating ? 'GENERATING...' : 'GENERATE DATASET'}
            </button>
          </form>
        </div>

        {message && (
          <div style={{
            padding: '8px 12px',
            borderRadius: '4px',
            fontSize: '0.75rem',
            fontFamily: 'var(--font-mono)',
            background: message.type === 'success' ? 'rgba(16, 185, 129, 0.1)' : message.type === 'info' ? 'rgba(168, 85, 247, 0.1)' : 'rgba(244, 63, 94, 0.1)',
            border: `1px solid ${message.type === 'success' ? 'var(--color-emerald)' : message.type === 'info' ? 'var(--color-purple)' : 'var(--color-rose)'}`,
            color: message.type === 'success' ? 'var(--color-emerald)' : message.type === 'info' ? 'var(--color-purple)' : 'var(--color-rose)'
          }}>
            {message.text}
          </div>
        )}
      </div>

      {/* Header telemetry status bar */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h1 style={{ fontSize: '1.75rem', fontWeight: '600', letterSpacing: '-0.02em', color: 'var(--text-primary)' }}>
            Operations Overview
          </h1>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
            Real-time telemetry and scheduler execution metrics.
          </p>
        </div>
        <div style={{
          display: 'flex',
          gap: '12px',
          fontSize: '0.75rem',
          fontFamily: 'var(--font-mono)',
          color: 'var(--text-muted)'
        }}>
          <span>SYS_TIME: {new Date().toLocaleTimeString()}</span>
          <span>//</span>
          <span>STATUS: OPERATIONAL</span>
        </div>
      </div>

      {/* KPI Cards Grid */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
        gap: '16px'
      }}>
        
        {/* KPI 1 */}
        <div className="glass-panel" style={{ padding: '20px', display: 'flex', gap: '16px', alignItems: 'center' }}>
          <div style={{
            background: 'rgba(6, 182, 212, 0.1)',
            padding: '12px',
            borderRadius: '8px',
            border: '1px solid rgba(6, 182, 212, 0.2)',
            color: 'var(--color-cyan)',
            boxShadow: '0 0 10px rgba(6, 182, 212, 0.1)'
          }}>
            <TrendingUp size={24} />
          </div>
          <div>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', textTransform: 'uppercase' }}>Success Rate</span>
            <h3 style={{ fontSize: '1.5rem', fontWeight: '700', marginTop: '4px' }} className="mono-text text-glow-cyan">
              {successRate}%
            </h3>
          </div>
        </div>

        {/* KPI 2 */}
        <div className="glass-panel" style={{ padding: '20px', display: 'flex', gap: '16px', alignItems: 'center' }}>
          <div style={{
            background: 'rgba(16, 185, 129, 0.1)',
            padding: '12px',
            borderRadius: '8px',
            border: '1px solid rgba(16, 185, 129, 0.2)',
            color: 'var(--color-emerald)',
            boxShadow: '0 0 10px rgba(16, 185, 129, 0.1)'
          }}>
            <CheckCircle2 size={24} />
          </div>
          <div>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', textTransform: 'uppercase' }}>Scheduled</span>
            <h3 style={{ fontSize: '1.5rem', fontWeight: '700', marginTop: '4px' }} className="mono-text text-glow-emerald">
              {scheduledCount} <span style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>/ {totalCount}</span>
            </h3>
          </div>
        </div>

        {/* KPI 3 */}
        <div className="glass-panel" style={{ padding: '20px', display: 'flex', gap: '16px', alignItems: 'center' }}>
          <div style={{
            background: 'rgba(244, 63, 94, 0.1)',
            padding: '12px',
            borderRadius: '8px',
            border: '1px solid rgba(244, 63, 94, 0.2)',
            color: 'var(--color-rose)',
            boxShadow: '0 0 10px rgba(244, 63, 94, 0.1)'
          }}>
            <AlertOctagon size={24} />
          </div>
          <div>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', textTransform: 'uppercase' }}>Unscheduled</span>
            <h3 style={{ fontSize: '1.5rem', fontWeight: '700', marginTop: '4px' }} className="mono-text text-glow-rose">
              {unscheduledCount}
            </h3>
          </div>
        </div>

        {/* KPI 4 */}
        <div className="glass-panel" style={{ padding: '20px', display: 'flex', gap: '16px', alignItems: 'center' }}>
          <div style={{
            background: 'rgba(168, 85, 247, 0.1)',
            padding: '12px',
            borderRadius: '8px',
            border: '1px solid rgba(168, 85, 247, 0.2)',
            color: 'var(--color-purple)',
            boxShadow: '0 0 10px rgba(168, 85, 247, 0.1)'
          }}>
            <RefreshCw size={24} />
          </div>
          <div>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', textTransform: 'uppercase' }}>Replan Runs</span>
            <h3 style={{ fontSize: '1.5rem', fontWeight: '700', marginTop: '4px' }} className="mono-text text-glow-purple">
              {replanRuns.length}
            </h3>
          </div>
        </div>
      </div>

      {/* Main Grid: Bottlenecks & Logs */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: '1fr 1fr',
        gap: '20px'
      }}>
        {/* Left Side: Bottleneck and Failure Reasons */}
        <div className="glass-panel" style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div>
            <h3 style={{ fontSize: '1rem', fontWeight: '600', color: 'var(--text-primary)' }}>
              Bottleneck Profiling
            </h3>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
              Elimination trace analyzing schedule failure constraints.
            </span>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', marginTop: '8px' }}>
            {Object.entries(bottleneckCounts).map(([key, count]) => {
              const percentage = Math.round((count / maxReasonCount) * 100);
              let color = 'var(--color-cyan)';
              if (key === 'STUDENT_UNAVAILABLE') color = 'var(--color-amber)';
              if (key === 'PANEL_UNAVAILABLE') color = 'var(--color-purple)';
              if (key === 'ROOM_UNAVAILABLE') color = 'var(--color-rose)';

              return (
                <div key={key} style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem' }}>
                    <span style={{ fontFamily: 'var(--font-mono)', color: 'var(--text-secondary)' }}>
                      {key.replace(/_/g, ' ')}
                    </span>
                    <span style={{ fontFamily: 'var(--font-mono)', fontWeight: 'bold' }}>{count}</span>
                  </div>
                  <div style={{ height: '8px', background: 'rgba(255, 255, 255, 0.02)', border: '1px solid var(--border-color)', borderRadius: '4px', overflow: 'hidden' }}>
                    <div style={{
                      width: `${percentage}%`,
                      height: '100%',
                      background: color,
                      boxShadow: `0 0 8px ${color}`
                    }} />
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Right Side: Replan Runs Audit Log */}
        <div className="glass-panel" style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div>
            <h3 style={{ fontSize: '1rem', fontWeight: '600', color: 'var(--text-primary)' }}>
              Replan Run History
            </h3>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
              Operational log of automatic disruption replanning events.
            </span>
          </div>

          <div style={{
            flexGrow: 1,
            overflowY: 'auto',
            maxHeight: '340px',
            display: 'flex',
            flexDirection: 'column',
            gap: '12px',
            paddingRight: '4px'
          }}>
            {replanRuns.length === 0 ? (
              <div style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                height: '100%',
                color: 'var(--text-muted)',
                gap: '8px',
                padding: '40px 0'
              }}>
                <RefreshCw size={24} style={{ opacity: 0.3 }} />
                <span style={{ fontSize: '0.8rem', fontFamily: 'var(--font-mono)' }}>NO REPLAN OPERATIONS LOGGED</span>
              </div>
            ) : (
              [...replanRuns].reverse().map((run) => (
                <div key={run.replanId} style={{
                  padding: '12px',
                  borderRadius: '8px',
                  background: 'rgba(255, 255, 255, 0.01)',
                  border: '1px solid var(--border-color)',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '8px'
                }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span className="mono-text" style={{ fontSize: '0.75rem', fontWeight: 'bold', color: 'var(--color-cyan)' }}>
                      RUN #{run.replanId} // {run.event?.eventType || 'DISRUPTION'}
                    </span>
                    <span className="badge badge-cyan" style={{ fontSize: '0.65rem' }}>
                      {run.status}
                    </span>
                  </div>
                  
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '8px', fontSize: '0.7rem' }}>
                    <div style={{ display: 'flex', flexDirection: 'column' }}>
                      <span style={{ color: 'var(--text-muted)' }}>Affected:</span>
                      <span className="mono-text" style={{ fontWeight: '600' }}>{run.interviewsAffected}</span>
                    </div>
                    <div style={{ display: 'flex', flexDirection: 'column' }}>
                      <span style={{ color: 'var(--text-muted)' }}>Moved:</span>
                      <span className="mono-text" style={{ fontWeight: '600', color: 'var(--color-emerald)' }}>{run.interviewsMoved}</span>
                    </div>
                    <div style={{ display: 'flex', flexDirection: 'column' }}>
                      <span style={{ color: 'var(--text-muted)' }}>Cancelled:</span>
                      <span className="mono-text" style={{ fontWeight: '600', color: 'var(--color-rose)' }}>{run.interviewsCancelled}</span>
                    </div>
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.65rem', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>
                    <span>Details: {run.event?.details || 'N/A'}</span>
                    <span>T_COMPLETED: {formatTime(run.completedAt)}</span>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>


      
    </div>
  );
};
