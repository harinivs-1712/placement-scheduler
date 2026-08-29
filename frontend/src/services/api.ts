// API Service for the Placement Scheduler Command Center
const API_BASE_URL = 'http://localhost:8080/api';

// --- TypeScript Types & Interfaces ---

export interface Student {
  studentId: number;
  name: string;
  branch: string;
  cgpa: number;
  status: string;
  interviews?: any[];
}

export interface Company {
  companyId: number;
  name: string;
  cgpaCutoff: number;
  interviewDurationMin: number;
  priorityTier: number;
  status: string;
  eligibleBranches: string[];
}

export interface Room {
  roomId: number;
  name: string;
  status: string; // 'AVAILABLE' | 'UNAVAILABLE'
}

export interface Panel {
  panelId: number;
  label: string;
  status: string; // 'ACTIVE' | 'INACTIVE'
  company: {
    companyId: number;
    name: string;
  };
}

export interface Interview {
  interviewId: number;
  student: Student;
  company: Company;
  panel: Panel | null;
  room: Room | null;
  day: number | null;
  startTime: string | null; // HH:mm format
  endTime: string | null;
  status: string; // 'SCHEDULED' | 'UNSCHEDULED'
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface ScheduledInterviewDTO {
  interviewId: number;
  day: number;
  startTime: string; // LocalTime
  endTime: string; // LocalTime
  studentId: number;
  studentName: string;
  studentEmail: string;
  companyId: number;
  companyName: string;
  panelId: number;
  panelName: string;
  roomId: number;
  roomName: string;
  status: string;
}

export interface UnscheduledInterviewDTO {
  interviewId: number;
  studentId: number;
  studentName: string;
  studentEmail: string;
  companyId: number;
  companyName: string;
  status: string;
  reason: string;
}

export interface DisruptionEvent {
  eventId: number;
  eventType: string; // 'PANEL_DROP' | 'ROOM_UNAVAILABLE'
  targetType: string; // 'PANEL' | 'ROOM'
  targetId: number;
  occurredAt: string;
  details: string;
}

export interface ReplanRun {
  replanId: number;
  event: DisruptionEvent;
  startedAt: string;
  completedAt: string;
  status: string; // 'RUNNING' | 'COMPLETED'
  interviewsAffected: number;
  interviewsMoved: number;
  interviewsCancelled: number;
}

export interface UnscheduledReason {
  reasonId: number;
  interview: {
    interviewId: number;
    studentName?: string;
    companyName?: string;
  };
  reason: string;
  loggedAt: string;
}

export interface InterviewChange {
  changeId: number;
  interviewId: number;
  studentName: string;
  companyName: string;
  interview?: Interview;
  oldDay: number | null;
  oldStartTime: string | null;
  oldEndTime: string | null;
  oldPanelId: number | null;
  oldRoomId: number | null;
  newDay: number | null;
  newStartTime: string | null;
  newEndTime: string | null;
  newPanelId: number | null;
  newRoomId: number | null;
  changeType: string;
  replanId: number | null;
  changedAt: string;
}

export interface InterviewChangeResponse {
  interviewId: number;
  studentId: number;
  companyId: number;
  oldDay: number | null;
  oldStartTime: string | null;
  oldEndTime: string | null;
  oldPanelId: number | null;
  oldRoomId: number | null;
  newDay: number | null;
  newStartTime: string | null;
  newEndTime: string | null;
  newPanelId: number | null;
  newRoomId: number | null;
}

export interface FailedInterviewResponse {
  interviewId: number;
  studentId: number;
  companyId: number;
  reason: string;
}

export interface ReplanDetailsResponse {
  replanId: number;
  disruptionType: string;
  affectedCount: number;
  movedCount: number;
  failedCount: number;
  successfulChanges: InterviewChangeResponse[];
  failedInterviews: FailedInterviewResponse[];
}

// --- API Service Methods ---

async function fetchJson<T>(url: string, options?: RequestInit): Promise<T> {
  const response = await fetch(url, options);
  if (!response.ok) {
    const errorText = await response.text().catch(() => 'Unknown error');
    throw new Error(`API Error [${response.status}]: ${errorText}`);
  }
  return response.json() as Promise<T>;
}

export const api = {
  // 1. Generation & Scheduling operations (POST)
  generateDataset: async (params: { students: number; companies: number; rooms: number; days: number }): Promise<string> => {
    const response = await fetch(`${API_BASE_URL}/dataset/generate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(params),
    });
    if (!response.ok) throw new Error(await response.text());
    return response.text();
  },

  generateSchedule: async (): Promise<string> => {
    const response = await fetch(`${API_BASE_URL}/schedule/generate`, {
      method: 'POST',
    });
    if (!response.ok) throw new Error(await response.text());
    return response.text();
  },

  // 2. Disruption injection (POST)
  injectPanelDrop: async (params: { panelId: number; day: number; effectiveTime: string; details: string }): Promise<any> => {
    return fetchJson(`${API_BASE_URL}/disruptions/panel-drop`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(params),
    });
  },

  injectRoomUnavailable: async (params: { roomId: number; day: number; effectiveTime: string; details: string }): Promise<any> => {
    return fetchJson(`${API_BASE_URL}/disruptions/room-unavailable`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(params),
    });
  },

  injectStudentWithdrawal: async (params: { studentId: number; day: number; effectiveTime: string; details: string }): Promise<any> => {
    return fetchJson(`${API_BASE_URL}/disruptions/student-withdrawal`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(params),
    });
  },

  // 3. Telemetry and Schedule retrieval (GET)
  getRooms: async (): Promise<Room[]> => {
    return fetchJson<Room[]>(`${API_BASE_URL}/rooms`);
  },

  getPanels: async (): Promise<Panel[]> => {
    return fetchJson<Panel[]>(`${API_BASE_URL}/panels`);
  },

  getInterviews: async (): Promise<Interview[]> => {
    return fetchJson<Interview[]>(`${API_BASE_URL}/interviews`);
  },

  getScheduledInterviews: async (): Promise<ScheduledInterviewDTO[]> => {
    return fetchJson<ScheduledInterviewDTO[]>(`${API_BASE_URL}/interviews/scheduled`);
  },

  getUnscheduledInterviews: async (): Promise<UnscheduledInterviewDTO[]> => {
    return fetchJson<UnscheduledInterviewDTO[]>(`${API_BASE_URL}/interviews/unscheduled`);
  },

  getDisruptionEvents: async (): Promise<DisruptionEvent[]> => {
    return fetchJson<DisruptionEvent[]>(`${API_BASE_URL}/disruption-events`);
  },

  getReplanRuns: async (): Promise<ReplanRun[]> => {
    return fetchJson<ReplanRun[]>(`${API_BASE_URL}/replan-runs`);
  },

  getUnscheduledReasons: async (): Promise<UnscheduledReason[]> => {
    return fetchJson<UnscheduledReason[]>(`${API_BASE_URL}/unscheduled-reasons`);
  },

  // --- Fallback-friendly helper endpoints ---
  
  getInterviewChanges: async (): Promise<InterviewChange[]> => {
    try {
      return await fetchJson<InterviewChange[]>(`${API_BASE_URL}/interview-changes`);
    } catch (e) {
      console.warn('GET /api/interview-changes not available. Using empty list fallback.', e);
      return [];
    }
  },

  getInterviewChangesByReplanId: async (replanId: number): Promise<ReplanDetailsResponse | null> => {
    try {
      return await fetchJson<ReplanDetailsResponse>(`${API_BASE_URL}/replan-runs/${replanId}/details`);
    } catch (e) {
      console.warn(`GET /api/replan-runs/${replanId}/details not available.`, e);
      return null;
    }
  },

  getStudents: async (): Promise<any[]> => {
    try {
      return await fetchJson<any[]>(`${API_BASE_URL}/students/overview`);
    } catch (e) {
      console.warn('GET /api/students/overview not available. Extracting unique students from interviews.', e);
      return [];
    }
  },

  getCompanies: async (): Promise<Company[]> => {
    try {
      return await fetchJson<Company[]>(`${API_BASE_URL}/companies`);
    } catch (e) {
      console.warn('GET /api/companies not available. Extracting unique companies from interviews.', e);
      return [];
    }
  }
};
