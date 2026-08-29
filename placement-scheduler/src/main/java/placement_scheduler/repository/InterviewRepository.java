package placement_scheduler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import placement_scheduler.entity.Interview;

import java.time.LocalTime;
import java.util.List;

@Repository
public interface InterviewRepository
    extends JpaRepository<Interview, Long> {

  List<Interview> findByStatus(String status);

  List<Interview> findByPanelPanelIdAndStatus(
      Long panelId,
      String status);

  @Query("""
          SELECT i
          FROM Interview i
          WHERE i.panel.panelId = :panelId
            AND i.day = :day
            AND i.status = :status
            AND i.startTime < :effectiveTime
            AND i.endTime > :effectiveTime
      """)
  List<Interview> findInterviewsOverlappingPanelDrop(
      @Param("panelId") Long panelId,
      @Param("day") Integer day,
      @Param("status") String status,
      @Param("effectiveTime") LocalTime effectiveTime);

  @Query("""
          SELECT i
          FROM Interview i
          WHERE i.panel.panelId = :panelId
            AND i.day = :day
            AND i.status = :status
            AND i.startTime >= :effectiveTime
      """)
  List<Interview> findInterviewsStartingAfterPanelDrop(
      @Param("panelId") Long panelId,
      @Param("day") Integer day,
      @Param("status") String status,
      @Param("effectiveTime") LocalTime effectiveTime);

  // ------------------------------------------------------------
  // Room-based equivalents of the two panel-drop queries above,
  // used by DisruptionService.handleRoomUnavailable(). Same overlap
  // logic — an interview already in progress when the room goes
  // down, or one starting at/after the effective time — just keyed
  // on room instead of panel. NOTE: unlike the panel queries, these
  // can return interviews belonging to several different companies
  // at once, since a room isn't owned by any one company.
  // ------------------------------------------------------------

  @Query("""
          SELECT i
          FROM Interview i
          WHERE i.room.roomId = :roomId
            AND i.day = :day
            AND i.status = :status
            AND i.startTime < :effectiveTime
            AND i.endTime > :effectiveTime
      """)
  List<Interview> findInterviewsOverlappingRoomDrop(
      @Param("roomId") Long roomId,
      @Param("day") Integer day,
      @Param("status") String status,
      @Param("effectiveTime") LocalTime effectiveTime);

  @Query("""
          SELECT i
          FROM Interview i
          WHERE i.room.roomId = :roomId
            AND i.day = :day
            AND i.status = :status
            AND i.startTime >= :effectiveTime
      """)
  List<Interview> findInterviewsStartingAfterRoomDrop(
      @Param("roomId") Long roomId,
      @Param("day") Integer day,
      @Param("status") String status,
      @Param("effectiveTime") LocalTime effectiveTime);

  // ------------------------------------------------------------
  // Used by DisruptionService.handleStudentWithdrawal().
  //
  // findInterviewsOverlappingStudentWithdrawal: an interview already
  // IN PROGRESS at the moment of withdrawal — this can only ever be
  // on the exact disruption day/time, so it does NOT need the
  // day-or-later widening below.
  //
  // findInterviewsAfterStudentWithdrawal: EVERY interview from this
  // point forward, across ALL remaining days — not just the same
  // day. Withdrawal is permanent (the student is out for good), so
  // this must cover day > effectiveDay as well as later times on the
  // effective day itself. This REPLACES an earlier day-exact version
  // (findInterviewsStartingAfterStudentWithdrawal) that only checked
  // i.day = :day and silently missed later days entirely — removed
  // here so there's only one correct method to call.
  // ------------------------------------------------------------

  @Query("""
          SELECT i
          FROM Interview i
          WHERE i.student.studentId = :studentId
            AND i.day = :day
            AND i.status = :status
            AND i.startTime < :effectiveTime
            AND i.endTime > :effectiveTime
      """)
  List<Interview> findInterviewsOverlappingStudentWithdrawal(
      @Param("studentId") Long studentId,
      @Param("day") Integer day,
      @Param("status") String status,
      @Param("effectiveTime") LocalTime effectiveTime);

  @Query("""
          SELECT i
          FROM Interview i
          WHERE i.student.studentId = :studentId
            AND i.status = :status
            AND (
                  i.day > :day
                  OR (i.day = :day AND i.startTime >= :effectiveTime)
                )
      """)
  List<Interview> findInterviewsAfterStudentWithdrawal(
      @Param("studentId") Long studentId,
      @Param("day") Integer day,
      @Param("status") String status,
      @Param("effectiveTime") LocalTime effectiveTime);

  // Withdrawal also cancels every interview this student still had
  // sitting UNSCHEDULED — those never held a day/time, so there's no
  // "before/after cutoff" question for them at all: they're all
  // cancelled unconditionally, regardless of day.
  List<Interview> findByStudentStudentIdAndStatus(Long studentId, String status);

  // ------------------------------------------------------------
  // General-purpose finders (not disruption-specific) — repaired
  // here since the pasted file had a broken/incomplete @Query block
  // for findScheduledInterviewsByDay (missing its @Query("""  and
  // SELECT i opening). NOTE: findScheduledInterviewsByDay and
  // findScheduledByDay below are functionally identical duplicates —
  // kept both since you may already have call sites depending on
  // either name, but worth deleting whichever one is unused once you
  // check.
  // ------------------------------------------------------------

  @Query("""
          SELECT i
          FROM Interview i
          WHERE i.day = :day
            AND i.status = 'SCHEDULED'
          ORDER BY i.startTime
      """)
  List<Interview> findScheduledInterviewsByDay(
      @Param("day") Integer day);

  @Query("""
          SELECT i
          FROM Interview i
          WHERE i.status = 'SCHEDULED'
            AND i.day = :day
          ORDER BY i.startTime
      """)
  List<Interview> findScheduledByDay(
      @Param("day") Integer day);

  @Query("""
          SELECT i
          FROM Interview i
          WHERE i.status = 'UNSCHEDULED'
          ORDER BY i.interviewId
      """)
  List<Interview> findAllUnscheduled();

  List<Interview> findByStudentStudentId(Long studentId);
}