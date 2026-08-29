package placement_scheduler.service;

/**
 * Result of a batch replan operation — how many of the affected interviews
 * were successfully re-placed vs. still couldn't be scheduled.
 *
 * cancelledCount is 0 for panel-drop/room-unavailability results (those
 * only relocate, never cancel). Student withdrawal populates it — that
 * disruption directly cancels the withdrawn student's own interviews,
 * separately from whatever backfill replanning happens afterward for
 * other students using the freed capacity.
 */
public class ReplanResult {

    private final int replannedCount;
    private final int failedCount;
    private final int cancelledCount;

    public ReplanResult(int replannedCount, int failedCount) {
        this(replannedCount, failedCount, 0);
    }

    public ReplanResult(int replannedCount, int failedCount, int cancelledCount) {
        this.replannedCount = replannedCount;
        this.failedCount = failedCount;
        this.cancelledCount = cancelledCount;
    }

    public int getReplannedCount() {
        return replannedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public int getCancelledCount() {
        return cancelledCount;
    }
}