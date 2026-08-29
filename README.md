# Placement Interview Scheduler

A backend system for managing placement interviews with **capacity-aware scheduling, disruption handling, automatic replanning, and complete audit tracking**.

## 🚀 Features

* Generates a realistic placement-season dataset.
* Schedules student interviews based on:

  * Student availability
  * Company slots
  * Company priority
  * Panel availability
  * Room availability
  * Interview duration
* Handles real-time disruptions without completely rebuilding the schedule.
* Automatically identifies the affected interviews.
* Replans only the affected interviews.
* Maintains a complete audit trail of scheduling changes.
* Tracks why an interview could not be scheduled.
* Supports student withdrawal, panel drop, and room unavailability.
* Company delay handling is currently in progress. 

## 🛠️ Tech Stack

### Backend

* Java
* Spring Boot
* Spring Data JPA / Hibernate
* PostgreSQL
* Maven
* REST APIs
* Docker

### Frontend

* React 19
* Vite
* TypeScript
* Vanilla CSS
* Lucide React

## 🗄️ Database

The system contains entities for:

* `Student`
* `Company`
* `Panel`
* `Room`
* `CompanySlot`
* `Shortlist`
* `Interview`
* `UnscheduledReason`
* `DisruptionEvent`
* `ReplanRun`
* `InterviewChange`

The database is designed so that rooms are shared across companies, while panels belong to companies. 

## 📊 Dataset Generation

The dataset generator creates realistic placement data with:

* Different student CGPAs and branches.
* Multiple company priority tiers.
* Different CGPA cutoffs.
* Branch-based company eligibility.
* Different panel capacities.
* 15-minute and 30-minute interviews.
* Different company scheduling patterns.
* CGPA-weighted shortlisting.
* Oversubscribed companies to create realistic scheduling constraints.
* Optional seed for reproducible datasets. 

## 📅 Scheduling Algorithm

The scheduler uses a **priority-ordered greedy scheduling approach**.

### Scheduling process

1. Reset existing interview scheduling state.
2. Sort interviews according to company priority and shortlist rank.
3. Check available company days.
4. Find valid time slots.
5. Check student availability.
6. Check panel availability.
7. Check room availability.
8. Assign the first conflict-free combination.
9. Record the reason when scheduling fails.

A `ScheduleIndex` is used to efficiently track student, panel, and room bookings. 

## ❌ Unscheduled Reasons

When an interview cannot be scheduled, the system records the reason:

* `NO_COMPANY_SLOT`
* `NO_VALID_TIME_SLOTS`
* `STUDENT_UNAVAILABLE`
* `PANEL_UNAVAILABLE`
* `ROOM_UNAVAILABLE`

This provides an audit trail explaining the actual bottleneck. 

## ⚡ Disruption Management

The system uses a **blast-radius based replanning approach**.

Instead of regenerating the entire schedule:

1. Detect the disruption.
2. Identify affected interviews.
3. Invalidate only those interviews.
4. Replan the affected interviews.
5. Record successful changes.
6. Record interviews that still could not be scheduled.

The replanning system also prevents interviews from being moved before the disruption's day/time. 

## 🚨 Supported Disruptions

### Panel Drop

* Panel becomes inactive.
* Affected interviews are identified.
* Interviews are replanned using remaining panels and rooms.
* Interviews are prevented from being moved into the past.

### Room Unavailability

* Room becomes unavailable.
* Affected interviews are identified.
* Interviews can be moved to other available rooms.
* Because rooms are shared, multiple companies can be affected by the same disruption.

### Student Withdrawal

* Student is marked as withdrawn.
* Remaining interviews are cancelled.
* Both scheduled and unscheduled interviews are handled.
* Freed capacity can be used to backfill other unscheduled interviews.

### Company Delay

* Invalidates interviews during the delay window.
* Replans before, after, or on later days.
* Freed panels → same company only.
* Freed rooms → any company.

## 🔄 Backfill

When a disruption frees scheduling capacity:

* Existing unscheduled interviews can use the newly available capacity.
* The same replanning engine is reused.
* This allows the system to opportunistically fill available slots instead of leaving capacity unused. 

## 🔍 Validation

The system has been tested for:

* Zero student double-bookings.
* Zero room double-bookings.
* Correct unscheduled-reason classification.
* Correct root-cause attribution.
* Correct disruption handling.
* Same-panel replanning.
* Cross-panel replanning.
* Cross-day replanning.
* Prevention of interviews returning to a disrupted resource.
* Prevention of interviews being moved before the disruption. 

## 🔗 API Endpoints

### Panel Drop

```http
POST /api/disruptions/panel-drop
```

Request:

```json
{
  "panelId": 123,
  "day": 2,
  "effectiveTime": "11:30",
  "details": "Panel unavailable"
}
```

### Room Unavailability

```http
POST /api/disruptions/room-unavailable
```

### Student Withdrawal

```http
POST /api/disruptions/student-withdrawal
```

Request:

```json
{
  "studentId": 5602,
  "day": 1,
  "effectiveTime": "11:30",
  "details": "Accepted an offer, withdrawing from all remaining interviews"
}
```

### Company Delay

```http
POST /api/disruptions/company-delay
```

Request:

```json
{
  "companyId": 249,
  "day": 2,
  "unavailableStart": "11:30",
  "unavailableEnd": "12:30",
  "details": "Company delayed"
}
```

The company-delay endpoint is currently under development. 

## ▶️ Running the Backend Locally

### 1. Clone the repository

```bash
git clone <your-repository-url>
cd placement-scheduler
```

### 2. Configure PostgreSQL

Create a PostgreSQL database and configure the application properties/environment variables.

Example:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/placement_scheduler
spring.datasource.username=postgres
spring.datasource.password=YOUR_PASSWORD

spring.jpa.hibernate.ddl-auto=update
```

### 3. Run the application

```bash
./mvnw spring-boot:run
```

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Backend runs on:

```text
http://localhost:8080
```

## 🐳 Docker

Build the backend image:

```bash
docker build -t placement-scheduler-backend .
```

Run the container:

```bash
docker run -p 8080:8080 placement-scheduler-backend
```

## 🌐 Deployment

The project is structured as:

```text
Placement Week Scheduler
│
├── frontend/
│   └── React + Vite frontend
│
└── placement-scheduler/
    └── Spring Boot backend
```

* Backend: Spring Boot + Docker
* Database: PostgreSQL
* Frontend: React + Vite
* Production environment variables are used for database and API configuration.

## 📌 Current Limitations

* Panel-drop and room-unavailability currently scope their disruption query to the specified day.
* Backfill currently considers the entire unscheduled pool rather than only the exact capacity freed by a disruption.
* Company-delay implementation is still in progress. 

## Live Links

Frontend - https://placement-scheduler-bsf3.onrender.com/

Backend - https://placement-scheduler-backend-ml5w.onrender.com

## 🎯 Project Goal

The goal is to build a **realistic placement scheduling system** that does not simply generate a timetable, but can also **adapt to real-world disruptions while preserving constraints, explaining failures, and maintaining a complete history of scheduling decisions**. 
