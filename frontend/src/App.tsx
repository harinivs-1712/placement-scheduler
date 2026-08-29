import { useState, useEffect, useCallback } from 'react';
import { Sidebar } from './components/Sidebar';
import { Dashboard } from './components/Dashboard';
import { GanttTimeline } from './components/GanttTimeline';
import { DisruptionConsole } from './components/DisruptionConsole';
import { EntityMonitor } from './components/EntityMonitor';
import { api } from './services/api';
import type { 
  Interview, 
  ScheduledInterviewDTO, 
  UnscheduledInterviewDTO, 
  Room, 
  Panel, 
  ReplanRun, 
  UnscheduledReason, 
  InterviewChange,
  Student,
  Company
} from './services/api';
import './App.css';

function App() {
  const [activeTab, setActiveTab] = useState<string>('dashboard');
  
  // Data State
  const [interviews, setInterviews] = useState<Interview[]>([]);
  const [scheduledInterviews, setScheduledInterviews] = useState<ScheduledInterviewDTO[]>([]);
  const [unscheduledInterviews, setUnscheduledInterviews] = useState<UnscheduledInterviewDTO[]>([]);
  const [rooms, setRooms] = useState<Room[]>([]);
  const [panels, setPanels] = useState<Panel[]>([]);
  const [replanRuns, setReplanRuns] = useState<ReplanRun[]>([]);
  const [unscheduledReasons, setUnscheduledReasons] = useState<UnscheduledReason[]>([]);
  const [interviewChanges, setInterviewChanges] = useState<InterviewChange[]>([]);
  const [students, setStudents] = useState<Student[]>([]);
  const [companies, setCompanies] = useState<Company[]>([]);
  const [placementDays, setPlacementDays] = useState<number>(3);

  // centralized fetch orchestrator
  const refreshAllData = useCallback(async () => {
    try {
      // Run queries concurrently
      const results = await Promise.allSettled([
        api.getInterviews(),
        api.getScheduledInterviews(),
        api.getUnscheduledInterviews(),
        api.getRooms(),
        api.getPanels(),
        api.getReplanRuns(),
        api.getUnscheduledReasons(),
        api.getInterviewChanges(),
        api.getStudents(),
        api.getCompanies()
      ]);

      // Process results
      if (results[0].status === 'fulfilled') setInterviews(results[0].value);
      if (results[3].status === 'fulfilled') setRooms(results[3].value);
      if (results[4].status === 'fulfilled') setPanels(results[4].value);
      if (results[5].status === 'fulfilled') setReplanRuns(results[5].value);
      if (results[6].status === 'fulfilled') setUnscheduledReasons(results[6].value);
      if (results[7].status === 'fulfilled') setInterviewChanges(results[7].value);

      const baseInterviews = results[0].status === 'fulfilled' ? results[0].value : [];
      const reasonsList = results[6].status === 'fulfilled' ? results[6].value : [];

      let scheduledList: ScheduledInterviewDTO[] = [];
      let unscheduledList: UnscheduledInterviewDTO[] = [];

      if (results[1].status === 'fulfilled' && results[1].value.length > 0) {
        scheduledList = results[1].value;
      } else if (baseInterviews.length > 0) {
        scheduledList = baseInterviews
          .filter(i => i.status?.toUpperCase() === 'SCHEDULED' && i.day !== null)
          .map(i => ({
            interviewId: i.interviewId,
            day: i.day!,
            startTime: i.startTime ? i.startTime.slice(0, 5) : '09:00',
            endTime: i.endTime ? i.endTime.slice(0, 5) : '09:15',
            studentId: i.student?.studentId || 0,
            studentName: i.student?.name || 'N/A',
            studentEmail: i.student?.name ? `${i.student.name.toLowerCase().replace(/ /g, '')}@university.edu` : 'N/A',
            companyId: i.company?.companyId || 0,
            companyName: i.company?.name || 'N/A',
            panelId: i.panel?.panelId || 0,
            panelName: i.panel?.label || 'N/A',
            roomId: i.room?.roomId || 0,
            roomName: i.room?.name || 'N/A',
            status: i.status
          }));
      }

      if (results[2].status === 'fulfilled' && results[2].value.length > 0) {
        unscheduledList = results[2].value;
      } else if (baseInterviews.length > 0) {
        unscheduledList = baseInterviews
          .filter(i => i.status?.toUpperCase() === 'UNSCHEDULED')
          .map(i => {
            const match = reasonsList.find(ur => ur.interview?.interviewId === i.interviewId);
            return {
              interviewId: i.interviewId,
              studentId: i.student?.studentId || 0,
              studentName: i.student?.name || 'N/A',
              studentEmail: i.student?.name ? `${i.student.name.toLowerCase().replace(/ /g, '')}@university.edu` : 'N/A',
              companyId: i.company?.companyId || 0,
              companyName: i.company?.name || 'N/A',
              status: i.status,
              reason: match?.reason || 'No reason recorded'
            };
          });
      }

      setScheduledInterviews(scheduledList);
      setUnscheduledInterviews(unscheduledList);
      
      // Standalone Student and Company endpoints processing with fallback extraction
      let fetchedStudents: Student[] = [];
      let fetchedCompanies: Company[] = [];

      const studentBranchMap = new Map<number, string>();
      const studentDbStatusMap = new Map<number, string>();
      baseInterviews.forEach(i => {
        if (i.student && i.student.studentId) {
          if (i.student.branch) studentBranchMap.set(i.student.studentId, i.student.branch);
          if (i.student.status) studentDbStatusMap.set(i.student.studentId, i.student.status);
        }
      });

      const branchesList = ['CSE', 'ISE', 'ECE', 'EEE', 'ME', 'CIVIL'];

      if (results[8].status === 'fulfilled' && results[8].value.length > 0) {
        fetchedStudents = results[8].value.map((s: any) => {
          const studentIntrs = s.interviews || [];
          
          let dbStatus = studentDbStatusMap.get(s.studentId) || s.status || 'ACTIVE';
          if (dbStatus === 'SCHEDULED' || dbStatus === 'UNSCHEDULED') {
            dbStatus = 'ACTIVE';
          }

          return {
            studentId: s.studentId,
            name: s.name,
            branch: studentBranchMap.get(s.studentId) || branchesList[s.studentId % 6],
            cgpa: s.cgpa || 0,
            status: dbStatus,
            interviews: studentIntrs
          };
        });
      }
      if (results[9].status === 'fulfilled' && results[9].value.length > 0) {
        fetchedCompanies = results[9].value;
      }

      // If backend did not serve direct lists, extract from interviews list
      if (fetchedStudents.length === 0 && baseInterviews.length > 0) {
        const studentMap = new Map<number, Student>();
        baseInterviews.forEach(i => {
          if (i.student && i.student.studentId) {
            studentMap.set(i.student.studentId, i.student);
          }
        });
        fetchedStudents = Array.from(studentMap.values());
      }

      if (fetchedCompanies.length === 0 && baseInterviews.length > 0) {
        const companyMap = new Map<number, Company>();
        baseInterviews.forEach(i => {
          if (i.company && i.company.companyId) {
            companyMap.set(i.company.companyId, i.company);
          }
        });
        fetchedCompanies = Array.from(companyMap.values());
      }

      setStudents(fetchedStudents);
      setCompanies(fetchedCompanies);

      // Dynamically determine total placement days configured
      if (baseInterviews.length > 0) {
        const days = baseInterviews.map(i => i.day).filter((d): d is number => d !== null);
        if (days.length > 0) {
          setPlacementDays(Math.max(...days));
        }
      }

    } catch (error) {
      console.error('API sync error:', error);
    }
  }, []);

  // Initial load and periodic polling
  useEffect(() => {
    refreshAllData();
    const interval = setInterval(() => {
      refreshAllData();
    }, 5000); // Poll every 5s to sync live shifts and logs

    return () => clearInterval(interval);
  }, [refreshAllData]);

  const handleDatasetGenerated = (days: number) => {
    setPlacementDays(days);
    setActiveTab('dashboard');
  };

  return (
    <div className="command-grid" style={{ minHeight: '100vh', background: 'var(--bg-primary)' }}>
      {/* Sidebar Control Deck */}
      <Sidebar
        onRefreshAll={refreshAllData}
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        unscheduledCount={unscheduledInterviews.length}
      />

      {/* Main Operations Panel */}
      <main style={{
        padding: '32px',
        height: '100vh',
        overflowY: 'auto',
        display: 'flex',
        flexDirection: 'column',
        gap: '24px'
      }}>
        {activeTab === 'dashboard' && (
          <Dashboard
            replanRuns={replanRuns}
            unscheduledReasons={unscheduledReasons}
            interviews={interviews}
            scheduledCount={scheduledInterviews.length}
            unscheduledCount={unscheduledInterviews.length}
            onRefreshAll={refreshAllData}
            onDatasetGenerated={handleDatasetGenerated}
          />
        )}

        {activeTab === 'timeline' && (
          <GanttTimeline
            scheduledInterviews={scheduledInterviews}
            rooms={rooms}
            panels={panels.map(p => ({
              panelId: p.panelId,
              label: p.label,
              companyName: p.company?.name || `Company ID ${p.company?.companyId}`
            }))}
            placementDays={placementDays}
          />
        )}

        {activeTab === 'disruptions' && (
          <DisruptionConsole
            panels={panels}
            rooms={rooms}
            changes={interviewChanges}
            interviews={interviews}
            placementDays={placementDays}
            onRefreshAll={refreshAllData}
          />
        )}

        {activeTab === 'entities' && (
          <EntityMonitor
            students={students}
            companies={companies}
            panels={panels}
            rooms={rooms}
          />
        )}
      </main>
    </div>
  );
}

export default App;
