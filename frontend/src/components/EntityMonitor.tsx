import React, { useState, useEffect } from 'react';
import type { Student, Company, Panel, Room } from '../services/api';
import { Search, Filter, Users, Building, Shield, Home } from 'lucide-react';

interface EntityMonitorProps {
  students: Student[];
  companies: Company[];
  panels: Panel[];
  rooms: Room[];
}

type EntityTab = 'STUDENTS' | 'COMPANIES' | 'PANELS' | 'ROOMS';

export const EntityMonitor: React.FC<EntityMonitorProps> = ({
  students,
  companies,
  panels,
  rooms
}) => {
  const [activeTab, setActiveTab] = useState<EntityTab>('STUDENTS');
  const [expandedStudentId, setExpandedStudentId] = useState<number | null>(null);
  const [searchQuery, setSearchQuery] = useState<string>('');
  
  // Filtering states
  const [branchFilter, setBranchFilter] = useState<string>('ALL');
  const [tierFilter, setTierFilter] = useState<string>('ALL');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');

  // Reset filters when changing tabs to prevent state collision (e.g. ACTIVE status filtering out AVAILABLE rooms)
  useEffect(() => {
    setSearchQuery('');
    setBranchFilter('ALL');
    setTierFilter('ALL');
    setStatusFilter('ALL');
  }, [activeTab]);

  const filteredStudents = students.filter(student => {
    const matchesSearch = student.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      student.studentId.toString().includes(searchQuery);
    const matchesBranch = branchFilter === 'ALL' || student.branch === branchFilter;
    const matchesStatus = statusFilter === 'ALL' || student.status === statusFilter;
    return matchesSearch && matchesBranch && matchesStatus;
  });

  const filteredCompanies = companies.filter(company => {
    const matchesSearch = company.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      company.companyId.toString().includes(searchQuery);
    const matchesTier = tierFilter === 'ALL' || company.priorityTier.toString() === tierFilter;
    const matchesStatus = statusFilter === 'ALL' || company.status === statusFilter;
    return matchesSearch && matchesTier && matchesStatus;
  });

  const filteredPanels = panels.filter(panel => {
    const matchesSearch = panel.label.toLowerCase().includes(searchQuery.toLowerCase()) ||
      panel.company?.name?.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesStatus = statusFilter === 'ALL' || panel.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  const filteredRooms = rooms
    .filter(room => {
      const matchesSearch = room.name.toLowerCase().includes(searchQuery.toLowerCase());
      const matchesStatus = statusFilter === 'ALL' || room.status === statusFilter;
      return matchesSearch && matchesStatus;
    })
    .sort((a, b) => a.name.localeCompare(b.name, undefined, { numeric: true, sensitivity: 'base' }));

  // Extract unique branches
  const branches = Array.from(new Set(students.map(s => s.branch)));

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px', animation: 'fadeIn 0.4s' }}>
      
      {/* Header & Tabs */}
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        flexWrap: 'wrap',
        gap: '16px'
      }}>
        <div>
          <h1 style={{ fontSize: '1.75rem', fontWeight: '600', letterSpacing: '-0.02em', color: 'var(--text-primary)' }}>
            Entity Registry Explorer
          </h1>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
            Telemetry status of Students, Recruiters, Rooms, and Interview Panels.
          </p>
        </div>

        {/* Tab Selection */}
        <div style={{ display: 'flex', gap: '6px', background: 'rgba(255, 255, 255, 0.02)', border: '1px solid var(--border-color)', borderRadius: '6px', padding: '4px' }}>
          <button
            onClick={() => { setActiveTab('STUDENTS'); setSearchQuery(''); }}
            className={`tab-button ${activeTab === 'STUDENTS' ? 'active' : ''}`}
            style={{ padding: '6px 12px', display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.75rem' }}
          >
            <Users size={14} />
            Students ({students.length})
          </button>
          <button
            onClick={() => { setActiveTab('COMPANIES'); setSearchQuery(''); }}
            className={`tab-button ${activeTab === 'COMPANIES' ? 'active' : ''}`}
            style={{ padding: '6px 12px', display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.75rem' }}
          >
            <Building size={14} />
            Companies ({companies.length})
          </button>
          <button
            onClick={() => { setActiveTab('PANELS'); setSearchQuery(''); }}
            className={`tab-button ${activeTab === 'PANELS' ? 'active' : ''}`}
            style={{ padding: '6px 12px', display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.75rem' }}
          >
            <Shield size={14} />
            Panels ({panels.length})
          </button>
          <button
            onClick={() => { setActiveTab('ROOMS'); setSearchQuery(''); }}
            className={`tab-button ${activeTab === 'ROOMS' ? 'active' : ''}`}
            style={{ padding: '6px 12px', display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.75rem' }}
          >
            <Home size={14} />
            Rooms ({rooms.length})
          </button>
        </div>
      </div>

      {/* Search & Filter deck */}
      <div className="glass-panel" style={{
        padding: '16px 20px',
        display: 'flex',
        gap: '16px',
        alignItems: 'center',
        flexWrap: 'wrap'
      }}>
        {/* Search */}
        <div style={{ position: 'relative', flexGrow: 1, minWidth: '240px' }}>
          <Search size={16} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
          <input
            type="text"
            placeholder={`Search ${activeTab.toLowerCase()}...`}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{
              width: '100%',
              background: 'rgba(0,0,0,0.2)',
              border: '1px solid var(--border-color)',
              color: 'white',
              borderRadius: '6px',
              padding: '8px 12px 8px 36px',
              fontSize: '0.85rem'
            }}
          />
        </div>

        {/* Filters */}
        <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
          <Filter size={16} style={{ color: 'var(--text-muted)' }} />
          
          {/* Branch filter (only for students) */}
          {activeTab === 'STUDENTS' && (
            <select
              value={branchFilter}
              onChange={(e) => setBranchFilter(e.target.value)}
              style={{
                background: 'rgba(0,0,0,0.2)',
                border: '1px solid var(--border-color)',
                color: 'white',
                padding: '8px 12px',
                borderRadius: '6px',
                fontSize: '0.8rem'
              }}
            >
              <option value="ALL">ALL BRANCHES</option>
              {branches.map(b => (
                <option key={b} value={b}>{b}</option>
              ))}
            </select>
          )}

          {/* Tier filter (only for companies) */}
          {activeTab === 'COMPANIES' && (
            <select
              value={tierFilter}
              onChange={(e) => setTierFilter(e.target.value)}
              style={{
                background: 'rgba(0,0,0,0.2)',
                border: '1px solid var(--border-color)',
                color: 'white',
                padding: '8px 12px',
                borderRadius: '6px',
                fontSize: '0.8rem'
              }}
            >
              <option value="ALL">ALL TIERS</option>
              <option value="1">TIER 1</option>
              <option value="2">TIER 2</option>
              <option value="3">TIER 3</option>
            </select>
          )}

          {/* Common Status filter */}
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            style={{
              background: 'rgba(0,0,0,0.2)',
              border: '1px solid var(--border-color)',
              color: 'white',
              padding: '8px 12px',
              borderRadius: '6px',
              fontSize: '0.8rem'
            }}
          >
            <option value="ALL">ALL STATUS</option>
            {activeTab === 'STUDENTS' && (
              <>
                <option value="SCHEDULED">SCHEDULED</option>
                <option value="UNSCHEDULED">UNSCHEDULED</option>
                <option value="ACTIVE">ACTIVE</option>
                <option value="WITHDRAWN">WITHDRAWN</option>
              </>
            )}
            {activeTab === 'COMPANIES' && (
              <>
                <option value="ACTIVE">ACTIVE</option>
                <option value="DELAYED">DELAYED</option>
              </>
            )}
            {activeTab === 'PANELS' && (
              <>
                <option value="ACTIVE">ACTIVE</option>
                <option value="INACTIVE">INACTIVE</option>
              </>
            )}
            {activeTab === 'ROOMS' && (
              <>
                <option value="AVAILABLE">AVAILABLE</option>
                <option value="UNAVAILABLE">UNAVAILABLE</option>
              </>
            )}
          </select>
        </div>
      </div>

      {/* Main Table viewports */}
      <div className="glass-panel" style={{ padding: '20px' }}>
        
        {/* Table 1: Students */}
        {activeTab === 'STUDENTS' && (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.8rem', textAlign: 'left' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border-color)', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>
                  <th style={{ padding: '10px' }}>Student ID</th>
                  <th style={{ padding: '10px' }}>Name</th>
                  <th style={{ padding: '10px' }}>Branch</th>
                  <th style={{ padding: '10px' }}>CGPA</th>
                  <th style={{ padding: '10px', textAlign: 'right' }}>Status</th>
                </tr>
              </thead>
              <tbody>
                {filteredStudents.flatMap(student => {
                  const isExpanded = expandedStudentId === student.studentId;
                  const row = (
                    <tr 
                      key={`student-${student.studentId}`} 
                      onClick={() => setExpandedStudentId(isExpanded ? null : student.studentId)}
                      style={{ 
                        borderBottom: '1px solid rgba(255,255,255,0.01)', 
                        color: 'var(--text-secondary)',
                        cursor: 'pointer',
                        background: isExpanded ? 'rgba(255,255,255,0.02)' : 'transparent'
                      }}
                    >
                      <td style={{ padding: '12px 10px', fontFamily: 'var(--font-mono)' }}>#{student.studentId}</td>
                      <td style={{ padding: '12px 10px', fontWeight: 'bold', color: 'var(--text-primary)' }}>{student.name}</td>
                      <td style={{ padding: '12px 10px' }}>
                        <span className="badge badge-cyan" style={{ fontSize: '0.65rem' }}>{student.branch}</span>
                      </td>
                      <td style={{ padding: '12px 10px', fontFamily: 'var(--font-mono)', fontWeight: 'bold', color: 'var(--color-cyan)' }}>{student.cgpa.toFixed(2)}</td>
                      <td style={{ padding: '12px 10px', textAlign: 'right' }}>
                        <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                          {(!student.interviews || student.interviews.length === 0) ? (
                            <div style={{ display: 'flex', gap: '12px', fontSize: '0.75rem', fontFamily: 'var(--font-mono)' }}>
                              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end' }}>
                                <span style={{ fontSize: '0.55rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Selected</span>
                                <span style={{ fontWeight: '600', color: 'var(--text-secondary)' }}>0</span>
                              </div>
                              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', borderLeft: '1px solid var(--border-color)', paddingLeft: '12px' }}>
                                <span style={{ fontSize: '0.55rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Scheduled</span>
                                <span style={{ fontWeight: '600', color: 'var(--text-secondary)' }}>0</span>
                              </div>
                            </div>
                          ) : (
                            (() => {
                              const total = student.interviews.length;
                              const scheduled = student.interviews.filter((i: any) => i.status === 'SCHEDULED').length;
                              return (
                                <div style={{ display: 'flex', gap: '12px', fontSize: '0.75rem', fontFamily: 'var(--font-mono)' }}>
                                  <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end' }}>
                                    <span style={{ fontSize: '0.55rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Selected</span>
                                    <span style={{ fontWeight: '600', color: 'var(--text-primary)' }}>{total}</span>
                                  </div>
                                  <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', borderLeft: '1px solid var(--border-color)', paddingLeft: '12px' }}>
                                    <span style={{ fontSize: '0.55rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Scheduled</span>
                                    <span style={{ fontWeight: '600', color: 'var(--color-cyan)' }}>{scheduled}</span>
                                  </div>
                                </div>
                              );
                            })()
                          )}
                        </div>
                      </td>
                    </tr>
                  );

                  if (!isExpanded) return [row];

                  const detailRow = (
                    <tr key={`student-detail-${student.studentId}`} style={{ background: 'rgba(0,0,0,0.15)' }}>
                      <td colSpan={5} style={{ padding: '12px 20px', borderBottom: '1px solid var(--border-color)' }}>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                          <span style={{ fontSize: '0.7rem', fontWeight: 'bold', color: 'var(--color-cyan)', fontFamily: 'var(--font-mono)', letterSpacing: '0.05em' }}>
                            SHORTLISTED COMPANIES & INTERVIEW STATUS
                          </span>
                          
                          {(!student.interviews || student.interviews.length === 0) ? (
                            <span style={{ fontSize: '0.65rem', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>
                              NO SHORTLISTED INTERVIEWS RECORDED FOR THIS STUDENT
                            </span>
                          ) : (
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', marginTop: '4px' }}>
                              {student.interviews.map((intr: any) => (
                                <div key={intr.interviewId} style={{
                                  display: 'flex',
                                  justifyContent: 'space-between',
                                  alignItems: 'center',
                                  background: 'rgba(0,0,0,0.1)',
                                  padding: '6px 12px',
                                  borderRadius: '6px',
                                  border: '1px solid var(--border-color)'
                                }}>
                                  <div style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
                                    <span style={{ fontSize: '0.75rem', fontWeight: 'bold', color: 'var(--text-primary)' }}>
                                      {intr.companyName}
                                    </span>
                                    <span style={{ fontSize: '0.65rem', color: 'var(--text-secondary)', fontFamily: 'var(--font-mono)' }}>
                                      {intr.status === 'SCHEDULED' 
                                        ? `Day ${intr.day} @ ${intr.startTime ? intr.startTime.slice(0, 5) : '09:00'} - ${intr.endTime ? intr.endTime.slice(0, 5) : '09:15'} // Room: ${intr.roomName || 'N/A'} (Panel: ${intr.panelName || 'N/A'})`
                                        : 'Pending Schedule Invalidation'
                                      }
                                    </span>
                                  </div>
                                  <span className={`badge ${intr.status === 'SCHEDULED' ? 'badge-emerald' : 'badge-rose'}`} style={{ fontSize: '0.6rem' }}>
                                    {intr.status}
                                  </span>
                                </div>
                              ))}
                            </div>
                          )}
                        </div>
                      </td>
                    </tr>
                  );

                  return [row, detailRow];
                })}
              </tbody>
            </table>
          </div>
        )}

        {/* Table 2: Companies */}
        {activeTab === 'COMPANIES' && (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.8rem', textAlign: 'left' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border-color)', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>
                  <th style={{ padding: '10px' }}>Company ID</th>
                  <th style={{ padding: '10px' }}>Name</th>
                  <th style={{ padding: '10px' }}>Tier</th>
                  <th style={{ padding: '10px' }}>Cutoff CGPA</th>
                  <th style={{ padding: '10px' }}>Interview Duration</th>
                  <th style={{ padding: '10px' }}>Eligible Branches</th>
                  <th style={{ padding: '10px', textAlign: 'right' }}>Status</th>
                </tr>
              </thead>
              <tbody>
                {filteredCompanies.map(company => (
                  <tr key={company.companyId} style={{ borderBottom: '1px solid rgba(255,255,255,0.01)', color: 'var(--text-secondary)' }}>
                    <td style={{ padding: '12px 10px', fontFamily: 'var(--font-mono)' }}>#{company.companyId}</td>
                    <td style={{ padding: '12px 10px', fontWeight: 'bold', color: 'var(--text-primary)' }}>{company.name}</td>
                    <td style={{ padding: '12px 10px' }}>
                      <span className={`badge ${company.priorityTier === 1 ? 'badge-cyan' : company.priorityTier === 2 ? 'badge-emerald' : 'badge-rose'}`}>
                        Tier {company.priorityTier}
                      </span>
                    </td>
                    <td style={{ padding: '12px 10px', fontFamily: 'var(--font-mono)', fontWeight: 'bold', color: 'var(--color-cyan)' }}>
                      {company.cgpaCutoff.toFixed(2)}
                    </td>
                    <td style={{ padding: '12px 10px', fontFamily: 'var(--font-mono)' }}>{company.interviewDurationMin} mins</td>
                    <td style={{ padding: '12px 10px' }}>
                      <div style={{ display: 'flex', gap: '4px', flexWrap: 'wrap' }}>
                        {company.eligibleBranches?.map(b => (
                          <span key={b} style={{ fontSize: '0.6rem', padding: '1px 4px', border: '1px solid var(--border-color)', borderRadius: '2px', background: 'rgba(255,255,255,0.02)' }}>
                            {b}
                          </span>
                        )) || 'All'}
                      </div>
                    </td>
                    <td style={{ padding: '12px 10px', textAlign: 'right' }}>
                      <span className={`badge ${company.status === 'ACTIVE' ? 'badge-emerald' : 'badge-rose'}`}>
                        {company.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Table 3: Panels Grouped by Company */}
        {activeTab === 'PANELS' && (() => {
          const groups: { [key: string]: typeof filteredPanels } = {};
          filteredPanels.forEach(panel => {
            const compName = panel.company?.name || 'Unassigned / Independent Panels';
            if (!groups[compName]) groups[compName] = [];
            groups[compName].push(panel);
          });

          return (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
              {Object.keys(groups).sort().map(compName => (
                <div key={compName} className="glass-panel" style={{ padding: '16px', background: 'rgba(255,255,255,0.01)', border: '1px solid var(--border-color)' }}>
                  <h4 style={{ fontSize: '0.8rem', fontWeight: 'bold', color: 'var(--color-cyan)', marginBottom: '12px', borderBottom: '1px solid var(--border-color)', paddingBottom: '6px', fontFamily: 'var(--font-mono)' }}>
                    {compName.toUpperCase()}
                  </h4>
                  <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.8rem', textAlign: 'left' }}>
                    <thead>
                      <tr style={{ borderBottom: '1px solid rgba(255,255,255,0.02)', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>
                        <th style={{ padding: '6px 10px', width: '20%' }}>Panel ID</th>
                        <th style={{ padding: '6px 10px', width: '45%' }}>Label</th>
                        <th style={{ padding: '6px 10px', textAlign: 'right', width: '35%' }}>Operations Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {groups[compName].map(panel => (
                        <tr key={panel.panelId} style={{ borderBottom: '1px solid rgba(255,255,255,0.01)', color: 'var(--text-secondary)' }}>
                          <td style={{ padding: '8px 10px', fontFamily: 'var(--font-mono)' }}>#{panel.panelId}</td>
                          <td style={{ padding: '8px 10px', fontWeight: 'bold', color: 'var(--text-primary)' }}>{panel.label}</td>
                          <td style={{ padding: '8px 10px', textAlign: 'right' }}>
                            <span className={`badge ${panel.status === 'ACTIVE' ? 'badge-emerald' : 'badge-rose'}`} style={{ fontSize: '0.65rem' }}>
                              {panel.status === 'ACTIVE' ? 'ACTIVE' : 'DROPPED / INACTIVE'}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ))}
            </div>
          );
        })()}

        {/* Table 4: Rooms */}
        {activeTab === 'ROOMS' && (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.8rem', textAlign: 'left' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border-color)', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>
                  <th style={{ padding: '10px' }}>Room ID</th>
                  <th style={{ padding: '10px' }}>Room Name</th>
                  <th style={{ padding: '10px', textAlign: 'right' }}>Infrastructure Status</th>
                </tr>
              </thead>
              <tbody>
                {filteredRooms.map(room => (
                  <tr key={room.roomId} style={{ borderBottom: '1px solid rgba(255,255,255,0.01)', color: 'var(--text-secondary)' }}>
                    <td style={{ padding: '12px 10px', fontFamily: 'var(--font-mono)' }}>#{room.roomId}</td>
                    <td style={{ padding: '12px 10px', fontWeight: 'bold', color: 'var(--text-primary)' }}>{room.name}</td>
                    <td style={{ padding: '12px 10px', textAlign: 'right' }}>
                      <span className={`badge ${room.status === 'AVAILABLE' ? 'badge-emerald' : 'badge-rose'}`}>
                        {room.status === 'AVAILABLE' ? 'AVAILABLE' : 'BLOCKED / UNAVAILABLE'}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

      </div>
      
    </div>
  );
};
