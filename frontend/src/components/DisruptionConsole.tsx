import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import type { Panel, Room, InterviewChange, Interview } from '../services/api';
import { ShieldAlert, AlertOctagon, Activity } from 'lucide-react';

interface DisruptionConsoleProps {
  panels: Panel[];
  rooms: Room[];
  changes: InterviewChange[];
  interviews: Interview[];
  placementDays: number;
  onRefreshAll: () => Promise<void>;
}

type DisruptionCategory = 'PANEL_DROP' | 'ROOM_UNAVAILABLE' | 'STUDENT_WITHDRAWAL' | 'COMPANY_DELAY';

export const DisruptionConsole: React.FC<DisruptionConsoleProps> = ({
  panels,
  rooms,
  changes,
  interviews,
  placementDays,
  onRefreshAll
}) => {
  const [activeCategory, setActiveCategory] = useState<DisruptionCategory>('PANEL_DROP');
  const [panelId, setPanelId] = useState<number>(0);
  const [roomId, setRoomId] = useState<number>(0);
  
  // Custom mock values for future disruptions
  const [studentId, setStudentId] = useState<string>('');
  const [companyId, setCompanyId] = useState<string>('');
  const [delayMin, setDelayMin] = useState<number>(30);
  
  const [day, setDay] = useState<number>(1);
  const [effectiveTime, setEffectiveTime] = useState<string>('12:00');
  const [details, setDetails] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(false);
  const [message, setMessage] = useState<{ text: string; type: 'success' | 'error' } | null>(null);

  // Summary Report states
  const [report, setReport] = useState<{
    category: DisruptionCategory;
    targetName: string;
    affectedCount: number;
    details: {
      studentName: string;
      companyName: string;
      isScheduled: boolean;
      newSlot?: string;
    }[];
    withdrawalDetails?: {
      studentName: string;
      cancelledInterviews: {
        studentName: string;
        companyName: string;
        reason: string;
      }[];
      movedInterviews: {
        studentName: string;
        companyName: string;
        slot: string;
      }[];
    };
  } | null>(null);





  // Filter lists to show only active resource options for injection
  const activePanels = panels.filter(p => p.status === 'ACTIVE');
  
  // Determine blocked rooms on the selected day by scanning the changes list
  const blockedRoomIdsOnSelectedDay = new Set(
    changes
      .filter(c => c.oldDay === day && c.oldRoomId !== null)
      .map(c => c.oldRoomId as number)
  );

  const activeRooms = rooms.filter(r => {
    // If it's already blocked on the selected day, filter it out from the options
    if (blockedRoomIdsOnSelectedDay.has(r.roomId)) {
      return false;
    }
    // Rooms are selectable as long as they are AVAILABLE globally (or not blocked on the selected day)
    return r.status === 'AVAILABLE' || !changes.some(c => c.oldRoomId === r.roomId && c.oldDay === day);
  });

  const getAvailableDays = (): number[] => {
    let daysWithInterviews: number[] = [];

    if (activeCategory === 'PANEL_DROP') {
      if (panelId) {
        daysWithInterviews = interviews
          .filter(i => i.status === 'SCHEDULED' && i.panel?.panelId == panelId && i.day !== null)
          .map(i => i.day as number);
      }
    } else if (activeCategory === 'ROOM_UNAVAILABLE') {
      if (roomId) {
        daysWithInterviews = interviews
          .filter(i => i.status === 'SCHEDULED' && i.room?.roomId == roomId && i.day !== null)
          .map(i => i.day as number);
      }
    } else if (activeCategory === 'STUDENT_WITHDRAWAL') {
      if (studentId) {
        daysWithInterviews = interviews
          .filter(i => i.status === 'SCHEDULED' && i.student?.name.toLowerCase().includes(studentId.toLowerCase()) && i.day !== null)
          .map(i => i.day as number);
      }
    } else if (activeCategory === 'COMPANY_DELAY') {
      if (companyId) {
        daysWithInterviews = interviews
          .filter(i => i.status === 'SCHEDULED' && i.company?.companyId == parseInt(companyId) && i.day !== null)
          .map(i => i.day as number);
      }
    }

    const uniqueDays = Array.from(new Set(daysWithInterviews)).sort((a, b) => a - b);
    
    if (uniqueDays.length === 0) {
      return Array.from({ length: placementDays || 1 }, (_, i) => i + 1);
    }
    return uniqueDays;
  };

  const availableDays = getAvailableDays();

  useEffect(() => {
    if (availableDays.length > 0 && !availableDays.includes(day)) {
      setDay(availableDays[0]);
    }
  }, [availableDays, day]);

  const handleInjectDisruption = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setMessage(null);
    setReport(null);

    const getStudentName = (sid: number) => {
      const match = interviews.find(i => i.student?.studentId === sid);
      return match ? match.student.name : `Student ${sid}`;
    };

    const getCompanyName = (cid: number) => {
      const match = interviews.find(i => i.company?.companyId === cid);
      return match ? match.company.name : `Company ID ${cid}`;
    };

    try {
      if (activeCategory === 'PANEL_DROP') {
        if (!panelId) throw new Error('Please select a panel to drop.');
        const targetPanel = panels.find(p => p.panelId === panelId);
        const name = targetPanel ? `${targetPanel.company?.name || 'Company'} - ${targetPanel.label}` : `Panel ID ${panelId}`;
        
        const res = await api.injectPanelDrop({ panelId, day, effectiveTime, details });
        
        let resDetails: any = null;
        if (res && res.replanId) {
          resDetails = await api.getInterviewChangesByReplanId(res.replanId);
        }

        const detailsList: any[] = [];
        if (resDetails) {
          if (resDetails.successfulChanges) {
            resDetails.successfulChanges.forEach((c: any) => {
              const roomObj = rooms.find(r => r.roomId === c.newRoomId);
              const panelObj = panels.find(p => p.panelId === c.newPanelId);
              const roomName = roomObj ? roomObj.name : `Room ID ${c.newRoomId}`;
              const panelName = panelObj ? panelObj.label : `Panel ID ${c.newPanelId}`;

              detailsList.push({
                studentName: getStudentName(c.studentId),
                companyName: getCompanyName(c.companyId),
                isScheduled: true,
                newSlot: `Day ${c.newDay} @ ${c.newStartTime ? c.newStartTime.slice(0, 5) : 'N/A'} - ${c.newEndTime ? c.newEndTime.slice(0, 5) : 'N/A'} (Room: ${roomName}, Panel: ${panelName})`
              });
            });
          }

          if (resDetails.failedInterviews) {
            resDetails.failedInterviews.forEach((c: any) => {
              detailsList.push({
                studentName: getStudentName(c.studentId),
                companyName: getCompanyName(c.companyId),
                isScheduled: false,
                newSlot: undefined
              });
            });
          }
        }

        setReport({
          category: 'PANEL_DROP',
          targetName: name,
          affectedCount: resDetails?.affectedCount ?? detailsList.length,
          details: detailsList
        });

      } else if (activeCategory === 'ROOM_UNAVAILABLE') {
        if (!roomId) throw new Error('Please select a room to mark unavailable.');
        const targetRoom = rooms.find(r => r.roomId === roomId);
        const name = targetRoom ? targetRoom.name : `Room ID ${roomId}`;
        
        const res = await api.injectRoomUnavailable({ roomId, day, effectiveTime, details });
        
        let resDetails: any = null;
        if (res && res.replanId) {
          resDetails = await api.getInterviewChangesByReplanId(res.replanId);
        }

        const detailsList: any[] = [];
        if (resDetails) {
          if (resDetails.successfulChanges) {
            resDetails.successfulChanges.forEach((c: any) => {
              const roomObj = rooms.find(r => r.roomId === c.newRoomId);
              const panelObj = panels.find(p => p.panelId === c.newPanelId);
              const roomName = roomObj ? roomObj.name : `Room ID ${c.newRoomId}`;
              const panelName = panelObj ? panelObj.label : `Panel ID ${c.newPanelId}`;

              detailsList.push({
                studentName: getStudentName(c.studentId),
                companyName: getCompanyName(c.companyId),
                isScheduled: true,
                newSlot: `Day ${c.newDay} @ ${c.newStartTime ? c.newStartTime.slice(0, 5) : 'N/A'} - ${c.newEndTime ? c.newEndTime.slice(0, 5) : 'N/A'} (Room: ${roomName}, Panel: ${panelName})`
              });
            });
          }

          if (resDetails.failedInterviews) {
            resDetails.failedInterviews.forEach((c: any) => {
              detailsList.push({
                studentName: getStudentName(c.studentId),
                companyName: getCompanyName(c.companyId),
                isScheduled: false,
                newSlot: undefined
              });
            });
          }
        }

        setReport({
          category: 'ROOM_UNAVAILABLE',
          targetName: name,
          affectedCount: resDetails?.affectedCount ?? detailsList.length,
          details: detailsList
        });

      } else if (activeCategory === 'STUDENT_WITHDRAWAL') {
        if (!studentId) throw new Error('Please enter Student ID.');
        
        const parsedSid = parseInt(studentId);
        const matchStudent = interviews.find(i => i.student?.studentId === parsedSid)?.student;
        const name = matchStudent ? matchStudent.name : `Student ID ${studentId}`;

        await api.injectStudentWithdrawal({
          studentId: parsedSid,
          day,
          effectiveTime,
          details
        });

        const runs = await api.getReplanRuns();
        const latestRun = runs.length > 0 ? runs[runs.length - 1] : null;
        let resDetails: any = null;
        if (latestRun && latestRun.replanId) {
          resDetails = await api.getInterviewChangesByReplanId(latestRun.replanId);
        }

        const cancelledList: any[] = [];
        const movedList: any[] = [];

        if (resDetails) {
          if (resDetails.failedInterviews) {
            resDetails.failedInterviews.forEach((c: any) => {
              if (c.studentId === parsedSid) {
                cancelledList.push({
                  studentName: getStudentName(c.studentId),
                  companyName: getCompanyName(c.companyId),
                  reason: c.reason ? c.reason.split(' (')[0] : 'Student Withdrawal'
                });
              }
            });
          }

          if (resDetails.successfulChanges) {
            resDetails.successfulChanges.forEach((c: any) => {
              const roomObj = rooms.find(r => r.roomId === c.newRoomId);
              const panelObj = panels.find(p => p.panelId === c.newPanelId);
              const roomName = roomObj ? roomObj.name : `Room ID ${c.newRoomId}`;
              const panelName = panelObj ? panelObj.label : `Panel ID ${c.newPanelId}`;

              movedList.push({
                studentName: getStudentName(c.studentId),
                companyName: getCompanyName(c.companyId),
                slot: `Day ${c.newDay} @ ${c.newStartTime ? c.newStartTime.slice(0, 5) : 'N/A'} - ${c.newEndTime ? c.newEndTime.slice(0, 5) : 'N/A'} (Room: ${roomName}, Panel: ${panelName})`
              });
            });
          }
        }

        setReport({
          category: 'STUDENT_WITHDRAWAL',
          targetName: name,
          affectedCount: resDetails?.affectedCount ?? (cancelledList.length + movedList.length),
          details: [],
          withdrawalDetails: {
            studentName: name,
            cancelledInterviews: cancelledList,
            movedInterviews: movedList
          }
        });

      } else {
        setMessage({ 
          text: `Future Disruption Extensibility: Handling for ${activeCategory} is modeled in the scheduler core.`, 
          type: 'success' 
        });
      }
      
      setDetails('');
      await onRefreshAll();
    } catch (err: any) {
      console.error(err);
      setMessage({ text: err.message || 'Failed to inject disruption.', type: 'error' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px', animation: 'fadeIn 0.4s' }}>
      
      {/* Header */}
      <div>
        <h1 style={{ fontSize: '1.75rem', fontWeight: '600', letterSpacing: '-0.02em', color: 'var(--text-primary)' }}>
          Disruption Incident Deck
        </h1>
        <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
          Inject live disruptions to trigger automatic, minimal-disturbance schedule replanning.
        </p>
      </div>

      {/* Expanded Full Width Container */}
      <div style={{
        width: '100%',
        display: 'flex',
        flexDirection: 'column',
        gap: '20px'
      }}>
        {/* Center: Disruption Injector Form */}
        <div className="glass-panel" style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div>
            <h3 style={{ fontSize: '1rem', fontWeight: '600', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <ShieldAlert style={{ color: 'var(--color-rose)' }} size={18} />
              Inject Disruption
            </h3>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
              Select disruption parameters to execute replan sequence.
            </span>
          </div>

          {/* Tab selector for categories */}
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(4, 1fr)',
            gap: '8px',
            background: 'rgba(0, 0, 0, 0.2)',
            padding: '4px',
            borderRadius: '6px',
            border: '1px solid var(--border-color)'
          }}>
            <button
              onClick={() => { setActiveCategory('PANEL_DROP'); setPanelId(0); setMessage(null); setReport(null); }}
              style={{
                background: activeCategory === 'PANEL_DROP' ? 'rgba(244, 63, 94, 0.15)' : 'transparent',
                border: 'none',
                color: activeCategory === 'PANEL_DROP' ? 'var(--color-rose)' : 'var(--text-secondary)',
                padding: '6px',
                borderRadius: '4px',
                fontSize: '0.75rem',
                fontWeight: 'bold',
                cursor: 'pointer'
              }}
            >
              Panel Drop
            </button>
            <button
              onClick={() => { setActiveCategory('ROOM_UNAVAILABLE'); setRoomId(0); setMessage(null); setReport(null); }}
              style={{
                background: activeCategory === 'ROOM_UNAVAILABLE' ? 'rgba(244, 63, 94, 0.15)' : 'transparent',
                border: 'none',
                color: activeCategory === 'ROOM_UNAVAILABLE' ? 'var(--color-rose)' : 'var(--text-secondary)',
                padding: '6px',
                borderRadius: '4px',
                fontSize: '0.75rem',
                fontWeight: 'bold',
                cursor: 'pointer'
              }}
            >
              Room Block
            </button>
            <button
              onClick={() => { setActiveCategory('STUDENT_WITHDRAWAL'); setStudentId(''); setMessage(null); setReport(null); }}
              style={{
                background: activeCategory === 'STUDENT_WITHDRAWAL' ? 'rgba(244, 63, 94, 0.15)' : 'transparent',
                border: 'none',
                color: activeCategory === 'STUDENT_WITHDRAWAL' ? 'var(--color-rose)' : 'var(--text-muted)',
                padding: '6px',
                borderRadius: '4px',
                fontSize: '0.7rem',
                fontWeight: 'bold',
                cursor: 'pointer'
              }}
            >
              Student Withdrawal
            </button>
            <button
              onClick={() => { setActiveCategory('COMPANY_DELAY'); setCompanyId(''); setMessage(null); setReport(null); }}
              style={{
                background: activeCategory === 'COMPANY_DELAY' ? 'rgba(244, 63, 94, 0.15)' : 'transparent',
                border: 'none',
                color: activeCategory === 'COMPANY_DELAY' ? 'var(--color-rose)' : 'var(--text-muted)',
                padding: '6px',
                borderRadius: '4px',
                fontSize: '0.7rem',
                fontWeight: 'bold',
                cursor: 'pointer'
              }}
            >
              Company Delay
            </button>
          </div>

          <form onSubmit={handleInjectDisruption} style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            
            {/* Conditional Input 1: Panel Selection */}
            {activeCategory === 'PANEL_DROP' && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                <label style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Select Target Panel</label>
                <select
                  value={panelId}
                  onChange={(e) => setPanelId(parseInt(e.target.value) || 0)}
                  style={{
                    background: 'rgba(0,0,0,0.3)',
                    border: '1px solid var(--border-color)',
                    color: 'white',
                    padding: '8px',
                    borderRadius: '4px',
                    fontSize: '0.8rem'
                  }}
                >
                  <option value={0}>-- CHOOSE ACTIVE PANEL --</option>
                  {activePanels.map(p => (
                    <option key={p.panelId} value={p.panelId}>
                      {p.label} (Company: {p.company?.name || `ID ${p.company?.companyId}`})
                    </option>
                  ))}
                </select>
              </div>
            )}

            {/* Conditional Input 2: Room Selection */}
            {activeCategory === 'ROOM_UNAVAILABLE' && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                <label style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Select Target Room</label>
                <select
                  value={roomId}
                  onChange={(e) => setRoomId(parseInt(e.target.value) || 0)}
                  style={{
                    background: 'rgba(0,0,0,0.3)',
                    border: '1px solid var(--border-color)',
                    color: 'white',
                    padding: '8px',
                    borderRadius: '4px',
                    fontSize: '0.8rem'
                  }}
                >
                  <option value={0}>-- CHOOSE AVAILABLE ROOM --</option>
                  {activeRooms.map(r => (
                    <option key={r.roomId} value={r.roomId}>
                      {r.name}
                    </option>
                  ))}
                </select>
              </div>
            )}

            {/* Conditional Input 3: Student Withdrawal Selection */}
            {activeCategory === 'STUDENT_WITHDRAWAL' && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                <label style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Enter Student ID</label>
                <input
                  type="text"
                  placeholder="e.g. 5604"
                  value={studentId}
                  onChange={(e) => setStudentId(e.target.value)}
                  style={{
                    background: 'rgba(0,0,0,0.3)',
                    border: '1px solid var(--border-color)',
                    color: 'white',
                    padding: '8px',
                    borderRadius: '4px',
                    fontSize: '0.8rem',
                    fontFamily: 'var(--font-mono)'
                  }}
                />
              </div>
            )}

            {/* Conditional Input 4: Company Delay Selection */}
            {activeCategory === 'COMPANY_DELAY' && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                  <label style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Enter Company ID</label>
                  <input
                    type="text"
                    placeholder="e.g. 5"
                    value={companyId}
                    onChange={(e) => setCompanyId(e.target.value)}
                    style={{
                      background: 'rgba(0,0,0,0.3)',
                      border: '1px solid var(--border-color)',
                      color: 'white',
                      padding: '8px',
                      borderRadius: '4px',
                      fontSize: '0.8rem',
                      fontFamily: 'var(--font-mono)'
                    }}
                  />
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                  <label style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Delay Duration (Minutes)</label>
                  <input
                    type="number"
                    value={delayMin}
                    onChange={(e) => setDelayMin(parseInt(e.target.value) || 0)}
                    style={{
                      background: 'rgba(0,0,0,0.3)',
                      border: '1px solid var(--border-color)',
                      color: 'white',
                      padding: '8px',
                      borderRadius: '4px',
                      fontSize: '0.8rem',
                      fontFamily: 'var(--font-mono)'
                    }}
                  />
                </div>
              </div>
            )}

            {/* Common Inputs: Day, Time, Details */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                <label style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Effective Day</label>
                <select
                  value={day}
                  onChange={(e) => setDay(parseInt(e.target.value) || 1)}
                  style={{
                    background: 'rgba(0,0,0,0.3)',
                    border: '1px solid var(--border-color)',
                    color: 'white',
                    padding: '8px',
                    borderRadius: '4px',
                    fontSize: '0.8rem'
                  }}
                >
                  {availableDays.map(d => (
                    <option key={d} value={d}>Day {d}</option>
                  ))}
                </select>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                <label style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Effective Time</label>
                <input
                  type="text"
                  placeholder="HH:mm (e.g. 14:00)"
                  value={effectiveTime}
                  onChange={(e) => setEffectiveTime(e.target.value)}
                  style={{
                    background: 'rgba(0,0,0,0.3)',
                    border: '1px solid var(--border-color)',
                    color: 'white',
                    padding: '8px',
                    borderRadius: '4px',
                    fontSize: '0.8rem',
                    fontFamily: 'var(--font-mono)'
                  }}
                />
              </div>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
              <label style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Reason / Logs Details</label>
              <textarea
                placeholder="Describe the cause (e.g., Water leakage in classroom, Interviewer stuck in traffic)"
                value={details}
                onChange={(e) => setDetails(e.target.value)}
                style={{
                  background: 'rgba(0,0,0,0.3)',
                  border: '1px solid var(--border-color)',
                  color: 'white',
                  padding: '8px',
                  borderRadius: '4px',
                  fontSize: '0.8rem',
                  minHeight: '60px',
                  fontFamily: 'var(--font-sans)',
                  resize: 'vertical'
                }}
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              style={{
                background: 'transparent',
                border: '1px solid var(--color-rose)',
                color: 'var(--color-rose)',
                padding: '6px 12px',
                borderRadius: '4px',
                fontSize: '0.75rem',
                fontWeight: 'bold',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '6px',
                fontFamily: 'var(--font-mono)',
                textShadow: '0 0 5px var(--color-rose-glow)',
                boxShadow: '0 0 10px rgba(244, 63, 94, 0.15)',
                transition: 'all 0.2s',
                marginTop: '12px',
                width: 'fit-content',
                alignSelf: 'center'
              }}
              className="pulse-glow-rose"
            >
              <AlertOctagon size={14} />
              {loading ? 'CREATING...' : 'CREATE DISRUPTION'}
            </button>
          </form>

          {message && (
            <div style={{
              padding: '10px 12px',
              borderRadius: '6px',
              fontSize: '0.75rem',
              fontFamily: 'var(--font-mono)',
              background: message.type === 'success' ? 'rgba(16, 185, 129, 0.1)' : 'rgba(244, 63, 94, 0.1)',
              border: `1px solid ${message.type === 'success' ? 'var(--color-emerald)' : 'var(--color-rose)'}`,
              color: message.type === 'success' ? 'var(--color-emerald)' : 'var(--color-rose)'
            }}>
              {message.text}
            </div>
          )}
        </div>

        {/* Dynamic Disruption Impact Summary Report */}
        {report && (
          <div className="glass-panel active animate-fade-in" style={{
            padding: '20px',
            background: 'rgba(9, 13, 21, 0.95)',
            border: '1px solid var(--color-cyan)',
            borderRadius: '8px',
            display: 'flex',
            flexDirection: 'column',
            gap: '16px',
            boxShadow: '0 0 15px rgba(6, 182, 212, 0.15)',
            animation: 'fadeIn 0.3s ease-out'
          }}>
            <div>
              <h3 style={{ fontSize: '0.9rem', fontWeight: 'bold', color: 'var(--color-cyan)', display: 'flex', alignItems: 'center', gap: '8px', borderBottom: '1px solid var(--border-color)', paddingBottom: '8px', fontFamily: 'var(--font-mono)', letterSpacing: '0.05em' }}>
                <Activity size={16} />
                DISRUPTION IMPACT SUMMARY
              </h3>
              <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginTop: '6px', fontFamily: 'var(--font-mono)' }}>
                EVENT: {report.category.replace('_', ' ')} ({report.targetName.toUpperCase()})
              </div>
            </div>

            {report.category === 'STUDENT_WITHDRAWAL' ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                
                {/* Cancelled Interviews */}
                <div>
                  <span style={{ fontSize: '0.7rem', color: 'var(--color-rose)', fontWeight: 'bold', fontFamily: 'var(--font-mono)', letterSpacing: '0.05em' }}>
                    CANCELLED INTERVIEWS:
                  </span>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', marginTop: '6px', maxHeight: '150px', overflowY: 'auto', paddingRight: '4px' }}>
                    {!report.withdrawalDetails?.cancelledInterviews || report.withdrawalDetails.cancelledInterviews.length === 0 ? (
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', fontStyle: 'italic', padding: '8px 0', textAlign: 'center' }}>
                        No interviews cancelled.
                      </div>
                    ) : (
                      report.withdrawalDetails.cancelledInterviews.map((item, idx) => (
                        <div key={idx} style={{ padding: '8px 12px', background: 'rgba(244, 63, 94, 0.04)', borderRadius: '6px', border: '1px solid rgba(244, 63, 94, 0.15)', fontSize: '0.75rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <div>
                            <div style={{ fontWeight: 'bold', color: 'var(--text-primary)' }}>{item.studentName}</div>
                            <div style={{ fontSize: '0.65rem', color: 'var(--text-secondary)', marginTop: '2px' }}>Company: {item.companyName}</div>
                          </div>
                          <span className="badge badge-rose" style={{ fontSize: '0.6rem' }}>CANCELLED</span>
                        </div>
                      ))
                    )}
                  </div>
                </div>

                {/* Moved/Backfilled Interviews */}
                <div>
                  <span style={{ fontSize: '0.7rem', color: 'var(--color-cyan)', fontWeight: 'bold', fontFamily: 'var(--font-mono)', letterSpacing: '0.05em' }}>
                    MOVED / BACKFILLED INTERVIEWS WITH SLOT:
                  </span>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', marginTop: '6px', maxHeight: '180px', overflowY: 'auto', paddingRight: '4px' }}>
                    {!report.withdrawalDetails?.movedInterviews || report.withdrawalDetails.movedInterviews.length === 0 ? (
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', fontStyle: 'italic', padding: '8px 0', textAlign: 'center' }}>
                        No other interviews moved or backfilled.
                      </div>
                    ) : (
                      report.withdrawalDetails.movedInterviews.map((item, idx) => (
                        <div key={idx} style={{ padding: '8px 12px', background: 'rgba(16, 185, 129, 0.04)', borderRadius: '6px', border: '1px solid rgba(16, 185, 129, 0.15)', fontSize: '0.75rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <div>
                            <div style={{ fontWeight: 'bold', color: 'var(--text-primary)' }}>{item.studentName}</div>
                            <div style={{ fontSize: '0.65rem', color: 'var(--text-secondary)', marginTop: '2px' }}>Company: {item.companyName}</div>
                            <div style={{ fontSize: '0.65rem', color: 'var(--color-cyan)', fontFamily: 'var(--font-mono)', marginTop: '4px' }}>{item.slot}</div>
                          </div>
                          <span className="badge badge-emerald" style={{ fontSize: '0.6rem' }}>SCHEDULED</span>
                        </div>
                      ))
                    )}
                  </div>
                </div>

              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', background: 'rgba(255,255,255,0.02)', padding: '8px 12px', borderRadius: '6px', border: '1px solid var(--border-color)' }}>
                  <span>Total Affected Interviews:</span>
                  <span style={{ fontFamily: 'var(--font-mono)', fontWeight: 'bold', color: 'var(--color-cyan)' }}>{report.affectedCount}</span>
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  <span style={{ fontSize: '0.7rem', color: 'var(--color-cyan)', fontWeight: 'bold', fontFamily: 'var(--font-mono)', letterSpacing: '0.05em' }}>
                    AFFECTED INTERVIEWS DETAIL STATUS:
                  </span>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', maxHeight: '200px', overflowY: 'auto', paddingRight: '4px' }}>
                    {report.details.length === 0 ? (
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', fontStyle: 'italic', padding: '12px 0', textAlign: 'center' }}>
                        No interviews affected after the disruption effective time.
                      </div>
                    ) : (
                      report.details.map((item, idx) => (
                        <div key={idx} style={{ padding: '8px 12px', background: item.isScheduled ? 'rgba(16, 185, 129, 0.04)' : 'rgba(244, 63, 94, 0.04)', borderRadius: '6px', border: `1px solid ${item.isScheduled ? 'rgba(16, 185, 129, 0.15)' : 'rgba(244, 63, 94, 0.15)'}`, fontSize: '0.75rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '10px' }}>
                          <div style={{ display: 'flex', flexDirection: 'column', gap: '2px', overflow: 'hidden' }}>
                            <div style={{ fontWeight: 'bold', color: 'var(--text-primary)', textOverflow: 'ellipsis', overflow: 'hidden', whiteSpace: 'nowrap' }}>{item.studentName}</div>
                            <div style={{ fontSize: '0.65rem', color: 'var(--text-secondary)', textOverflow: 'ellipsis', overflow: 'hidden', whiteSpace: 'nowrap' }}>Company: {item.companyName}</div>
                            {item.isScheduled ? (
                              <div style={{ fontSize: '0.65rem', color: 'var(--color-cyan)', fontFamily: 'var(--font-mono)', marginTop: '4px' }}>{item.newSlot}</div>
                            ) : (
                              <div style={{ fontSize: '0.65rem', color: 'var(--color-rose)', fontFamily: 'var(--font-mono)', marginTop: '4px' }}>Not Scheduled</div>
                            )}
                          </div>
                          <span className={`badge ${item.isScheduled ? 'badge-emerald' : 'badge-rose'}`} style={{ fontSize: '0.6rem', flexShrink: 0 }}>
                            {item.isScheduled ? 'SCHEDULED' : 'NOT SCHEDULED'}
                          </span>
                        </div>
                      ))
                    )}
                  </div>
                </div>
              </div>
            )}
          </div>
        )}
      </div>

    </div>
  );
};
