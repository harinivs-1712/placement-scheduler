import React, { useState } from 'react';
import type { ScheduledInterviewDTO } from '../services/api';
import { Calendar, Layers, Clock, Users, Building, Shield, Download } from 'lucide-react';

interface GanttTimelineProps {
  scheduledInterviews: ScheduledInterviewDTO[];
  rooms: { roomId: number; name: string }[];
  panels: { panelId: number; label: string; companyName?: string }[];
  placementDays: number;
}

type GroupType = 'ROOM' | 'PANEL' | 'COMPANY';

export const GanttTimeline: React.FC<GanttTimelineProps> = ({
  scheduledInterviews,
  rooms,
  panels,
  placementDays
}) => {
  const [selectedDay, setSelectedDay] = useState<number>(1);
  const [groupBy, setGroupBy] = useState<GroupType>('ROOM');
  const [hoveredInterview, setHoveredInterview] = useState<ScheduledInterviewDTO | null>(null);

  const handleExportExcel = () => {
    const headers = [
      'Interview ID',
      'Day',
      'Start Time',
      'End Time',
      'Room ID',
      'Room Name',
      'Panel ID',
      'Panel Label',
      'Student ID',
      'Student Name',
      'Company ID',
      'Company Name',
      'Status'
    ];

    const csvRows = scheduledInterviews.map(i => [
      i.interviewId,
      i.day,
      i.startTime,
      i.endTime,
      i.roomId,
      `"${(i.roomName || '').replace(/"/g, '""')}"`,
      i.panelId,
      `"${(i.panelName || '').replace(/"/g, '""')}"`,
      i.studentId,
      `"${(i.studentName || '').replace(/"/g, '""')}"`,
      i.companyId,
      `"${(i.companyName || '').replace(/"/g, '""')}"`,
      i.status
    ]);

    const csvContent = [headers.join(','), ...csvRows.map(row => row.join(','))].join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `placement_week_schedule_day_${selectedDay}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  // Time conversion helpers
  const START_HOUR = 9; // 9:00 AM
  const END_HOUR = 17;  // 5:00 PM
  const TOTAL_MINUTES = (END_HOUR - START_HOUR) * 60; // 480 minutes

  const timeToMinutes = (timeStr: string): number => {
    if (!timeStr) return 0;
    const parts = timeStr.split(':');
    const hours = parseInt(parts[0], 10);
    const minutes = parseInt(parts[1], 10);
    return hours * 60 + minutes;
  };

  const getPositionStyles = (startTime: string, endTime: string) => {
    const startMin = timeToMinutes(startTime);
    const endMin = timeToMinutes(endTime);
    
    const dayStartMin = START_HOUR * 60;
    
    const leftOffset = startMin - dayStartMin;
    const duration = endMin - startMin;
    
    const leftPercent = (leftOffset / TOTAL_MINUTES) * 100;
    const widthPercent = (duration / TOTAL_MINUTES) * 100;
    
    return {
      left: `${Math.max(0, Math.min(100, leftPercent))}%`,
      width: `${Math.max(1, Math.min(100, widthPercent))}%`
    };
  };

  // Filter interviews for the selected day
  const dayInterviews = scheduledInterviews.filter(i => i.day === selectedDay);

  // Grouping logic
  let rows: { id: number | string; label: string; sublabel?: string; interviews: ScheduledInterviewDTO[] }[] = [];

  if (groupBy === 'ROOM') {
    const sortedRooms = [...rooms].sort((a, b) => a.name.localeCompare(b.name, undefined, { numeric: true, sensitivity: 'base' }));
    rows = sortedRooms.map(room => ({
      id: room.roomId,
      label: room.name,
      sublabel: 'POOLED ROOM',
      interviews: dayInterviews.filter(i => i.roomId == room.roomId)
    }));
  } else if (groupBy === 'PANEL') {
    const sortedPanels = [...panels].sort((a, b) => {
      const compA = a.companyName || '';
      const compB = b.companyName || '';
      const compComp = compA.localeCompare(compB, undefined, { sensitivity: 'base' });
      if (compComp !== 0) return compComp;
      return a.label.localeCompare(b.label, undefined, { numeric: true, sensitivity: 'base' });
    });
    rows = sortedPanels
      .map(panel => ({
        id: panel.panelId,
        label: panel.companyName ? `${panel.companyName} - ${panel.label}` : panel.label,
        sublabel: 'ACTIVE PANEL',
        interviews: dayInterviews.filter(i => i.panelId == panel.panelId)
      }))
      .filter(row => row.interviews.length > 0);
  } else if (groupBy === 'COMPANY') {
    // Extract unique companies
    const uniqueCompanyIds = Array.from(new Set(dayInterviews.map(i => i.companyId)));
    rows = uniqueCompanyIds.map(companyId => {
      const firstMatch = dayInterviews.find(i => i.companyId === companyId);
      const name = firstMatch ? firstMatch.companyName : `Company ${companyId}`;
      return {
        id: companyId,
        label: name,
        sublabel: 'RECRUITER PROFILE',
        interviews: dayInterviews.filter(i => i.companyId == companyId)
      };
    });
  }

  // Generate hourly markers
  const hoursArray = Array.from({ length: END_HOUR - START_HOUR + 1 }, (_, i) => START_HOUR + i);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px', animation: 'fadeIn 0.4s' }}>
      
      {/* Filters Deck */}
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        flexWrap: 'wrap',
        gap: '16px'
      }}>
        <div>
          <h1 style={{ fontSize: '1.75rem', fontWeight: '600', letterSpacing: '-0.02em', color: 'var(--text-primary)' }}>
            Placement Timeline
          </h1>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
            Gantt chart displaying room, panel, and student placement sequences.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '16px', alignItems: 'center' }}>
          {/* Day selection */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', background: 'rgba(255, 255, 255, 0.02)', border: '1px solid var(--border-color)', borderRadius: '6px', padding: '4px' }}>
            <Calendar size={14} style={{ color: 'var(--text-secondary)', marginLeft: '8px' }} />
            {Array.from({ length: placementDays || 1 }, (_, i) => i + 1).map(day => (
              <button
                key={day}
                onClick={() => setSelectedDay(day)}
                style={{
                  background: selectedDay === day ? 'rgba(6, 182, 212, 0.15)' : 'transparent',
                  border: 'none',
                  color: selectedDay === day ? 'var(--color-cyan)' : 'var(--text-secondary)',
                  padding: '4px 10px',
                  borderRadius: '4px',
                  fontSize: '0.75rem',
                  fontWeight: 'bold',
                  cursor: 'pointer',
                  fontFamily: 'var(--font-mono)'
                }}
              >
                DAY {day}
              </button>
            ))}
          </div>

          {/* Grouping selection */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', background: 'rgba(255, 255, 255, 0.02)', border: '1px solid var(--border-color)', borderRadius: '6px', padding: '4px' }}>
            <Layers size={14} style={{ color: 'var(--text-secondary)', marginLeft: '8px' }} />
            {(['ROOM', 'PANEL', 'COMPANY'] as GroupType[]).map(type => (
              <button
                key={type}
                onClick={() => setGroupBy(type)}
                style={{
                  background: groupBy === type ? 'rgba(168, 85, 247, 0.15)' : 'transparent',
                  border: 'none',
                  color: groupBy === type ? 'var(--color-purple)' : 'var(--text-secondary)',
                  padding: '4px 10px',
                  borderRadius: '4px',
                  fontSize: '0.75rem',
                  fontWeight: 'bold',
                  cursor: 'pointer',
                  fontFamily: 'var(--font-mono)'
                }}
              >
                {type}
              </button>
            ))}
          </div>

          {/* Export to Excel Action */}
          <button
            onClick={handleExportExcel}
            className="pulse-glow-cyan"
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              background: 'rgba(6, 182, 212, 0.08)',
              border: '1px solid rgba(6, 182, 212, 0.3)',
              borderRadius: '6px',
              padding: '6px 12px',
              color: 'var(--color-cyan)',
              fontSize: '0.75rem',
              fontWeight: 'bold',
              cursor: 'pointer',
              height: '32px',
              fontFamily: 'var(--font-mono)'
            }}
          >
            <Download size={14} />
            EXPORT EXCEL
          </button>
        </div>
      </div>

      {/* Main Gantt Viewport */}
      <div className="glass-panel" style={{
        padding: '24px',
        overflowX: 'auto',
        position: 'relative',
        minWidth: '700px'
      }}>
        {/* Timeline Header (Hours) */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: '150px 1fr',
          borderBottom: '2px solid var(--border-color)',
          paddingBottom: '12px',
          marginBottom: '12px'
        }}>
          <div style={{ fontSize: '0.75rem', fontWeight: 'bold', textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>
            TRACK ID
          </div>
          <div style={{ position: 'relative', height: '20px' }}>
            {hoursArray.map((hour, idx) => {
              const leftPercent = (idx / (hoursArray.length - 1)) * 100;
              const formattedTime = hour <= 12 ? `${hour}:00 ${hour === 12 ? 'PM' : 'AM'}` : `${hour - 12}:00 PM`;
              return (
                <div key={hour} style={{
                  position: 'absolute',
                  left: `${leftPercent}%`,
                  transform: 'translateX(-50%)',
                  fontSize: '0.65rem',
                  fontFamily: 'var(--font-mono)',
                  color: 'var(--text-muted)',
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  gap: '4px'
                }}>
                  <span>{formattedTime}</span>
                  <div style={{ width: '1px', height: '6px', background: 'var(--border-color)' }} />
                </div>
              );
            })}
          </div>
        </div>

        {/* Timeline Grid Rows */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
          {rows.map(row => (
            <div key={row.id} style={{
              display: 'grid',
              gridTemplateColumns: '150px 1fr',
              alignItems: 'center',
              background: 'rgba(255, 255, 255, 0.01)',
              border: '1px solid rgba(255, 255, 255, 0.02)',
              borderRadius: '6px',
              minHeight: '44px',
              padding: '4px 0'
            }}>
              
              {/* Row Header */}
              <div style={{ paddingLeft: '8px', borderRight: '1px solid var(--border-color)', height: '100%', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
                <span style={{ fontSize: '0.8rem', fontWeight: 'bold', color: 'var(--text-primary)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {row.label}
                </span>
                <span style={{ fontSize: '0.6rem', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>
                  {row.sublabel}
                </span>
              </div>

              {/* Gantt track for this row */}
              <div style={{ position: 'relative', height: '100%', minHeight: '34px', width: '100%' }}>
                {/* Vertical grid lines */}
                {hoursArray.map((_, idx) => {
                  const leftPercent = (idx / (hoursArray.length - 1)) * 100;
                  return (
                    <div key={idx} style={{
                      position: 'absolute',
                      left: `${leftPercent}%`,
                      top: 0,
                      bottom: 0,
                      width: '1px',
                      background: 'rgba(255,255,255,0.015)',
                      pointerEvents: 'none'
                    }} />
                  );
                })}

                {/* Scheduled Interviews Blocks */}
                {row.interviews.map(interview => {
                  const pos = getPositionStyles(interview.startTime, interview.endTime);
                  
                  // Color code based on tier
                  let border = 'rgba(6, 182, 212, 0.4)';
                  let bg = 'rgba(6, 182, 212, 0.08)';
                  let glow = '0 0 8px rgba(6, 182, 212, 0.1)';
                  
                  if (interview.status === 'UNSCHEDULED') {
                    border = 'rgba(244, 63, 94, 0.4)';
                    bg = 'rgba(244, 63, 94, 0.08)';
                    glow = 'none';
                  } else if (interview.companyId % 3 === 1) {
                    border = 'rgba(168, 85, 247, 0.4)';
                    bg = 'rgba(168, 85, 247, 0.08)';
                    glow = '0 0 8px rgba(168, 85, 247, 0.1)';
                  } else if (interview.companyId % 3 === 2) {
                    border = 'rgba(16, 185, 129, 0.4)';
                    bg = 'rgba(16, 185, 129, 0.08)';
                    glow = '0 0 8px rgba(16, 185, 129, 0.1)';
                  }

                  return (
                    <div
                      key={interview.interviewId}
                      style={{
                        position: 'absolute',
                        top: '4px',
                        bottom: '4px',
                        left: pos.left,
                        width: pos.width,
                        border: `1px solid ${border}`,
                        background: bg,
                        boxShadow: glow,
                        borderRadius: '4px',
                        cursor: 'pointer',
                        padding: '2px 6px',
                        display: 'flex',
                        flexDirection: 'column',
                        justifyContent: 'center',
                        overflow: 'hidden',
                        transition: 'all 0.2s',
                        zIndex: hoveredInterview?.interviewId === interview.interviewId ? 10 : 2
                      }}
                      onMouseEnter={() => setHoveredInterview(interview)}
                      onMouseLeave={() => setHoveredInterview(null)}
                    >
                      <span style={{ fontSize: '0.65rem', fontWeight: 'bold', color: 'white', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                        {groupBy === 'ROOM'
                          ? `${interview.companyName} - ${interview.panelName}`
                          : interview.studentName
                        }
                      </span>
                      <span style={{ fontSize: '0.55rem', color: 'var(--text-secondary)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', fontFamily: 'var(--font-mono)' }}>
                        {groupBy === 'ROOM'
                          ? interview.studentName
                          : groupBy === 'COMPANY'
                            ? `${interview.panelName} (${interview.roomName})`
                            : interview.companyName
                        }
                      </span>
                    </div>
                  );
                })}
              </div>

            </div>
          ))}
        </div>
      </div>

      {/* Floating Hover Card (Details Tooltip) */}
      {hoveredInterview && (
        <div className="glass-panel active animate-fade-in" style={{
          position: 'fixed',
          bottom: '24px',
          right: '24px',
          width: '320px',
          padding: '16px',
          zIndex: 9999,
          display: 'flex',
          flexDirection: 'column',
          gap: '12px',
          background: 'rgba(9, 13, 21, 0.95)'
        }}>
          {/* Header */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span className="mono-text" style={{ fontSize: '0.75rem', fontWeight: 'bold', color: 'var(--color-cyan)' }}>
              SLOT DETAILS // #{hoveredInterview.interviewId}
            </span>
            <span className="badge badge-cyan" style={{ fontSize: '0.65rem' }}>
              Day {hoveredInterview.day}
            </span>
          </div>

          {/* Time block */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', background: 'rgba(255, 255, 255, 0.02)', padding: '8px', borderRadius: '4px', border: '1px solid var(--border-color)' }}>
            <Clock size={16} style={{ color: 'var(--color-cyan)' }} />
            <span style={{ fontSize: '0.85rem', fontWeight: 'bold', fontFamily: 'var(--font-mono)' }}>
              {hoveredInterview.startTime} - {hoveredInterview.endTime}
            </span>
          </div>

          {/* Student metadata */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Users size={14} style={{ color: 'var(--text-muted)' }} />
              <span style={{ fontSize: '0.8rem', fontWeight: 'bold' }}>{hoveredInterview.studentName}</span>
            </div>
            <div style={{ paddingLeft: '22px', display: 'flex', gap: '12px', fontSize: '0.7rem', color: 'var(--text-secondary)' }}>
              <span>{hoveredInterview.studentEmail}</span>
              <span>•</span>
              <span style={{ color: 'var(--color-purple)' }}>ID: {hoveredInterview.studentId}</span>
            </div>
          </div>

          {/* Company metadata */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Building size={14} style={{ color: 'var(--text-muted)' }} />
              <span style={{ fontSize: '0.8rem', fontWeight: 'bold' }}>{hoveredInterview.companyName}</span>
            </div>
            <div style={{ paddingLeft: '22px', fontSize: '0.7rem', color: 'var(--text-secondary)' }}>
              <span>Recruiter ID: {hoveredInterview.companyId}</span>
            </div>
          </div>

          <hr style={{ borderColor: 'var(--border-color)' }} />

          {/* Infrastructure placement */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', fontSize: '0.75rem' }}>
            <div style={{ display: 'flex', gap: '6px', alignItems: 'center' }}>
              <Shield size={14} style={{ color: 'var(--color-purple)' }} />
              <span>{hoveredInterview.panelName}</span>
            </div>
            <div style={{ display: 'flex', gap: '6px', alignItems: 'center' }}>
              <Clock size={14} style={{ color: 'var(--color-emerald)' }} />
              <span>{hoveredInterview.roomName}</span>
            </div>
          </div>
        </div>
      )}

    </div>
  );
};
