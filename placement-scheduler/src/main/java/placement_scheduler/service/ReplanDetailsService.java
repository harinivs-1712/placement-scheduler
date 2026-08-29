package placement_scheduler.service;

import org.springframework.stereotype.Service;

import placement_scheduler.entity.InterviewChange;
import placement_scheduler.entity.ReplanRun;
import placement_scheduler.entity.UnscheduledReason;
import placement_scheduler.repository.InterviewChangeRepository;
import placement_scheduler.repository.ReplanRunRepository;
import placement_scheduler.repository.UnscheduledReasonRepository;
import placement_scheduler.controller.ReplanDetailsResponse;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReplanDetailsService {

    private final ReplanRunRepository replanRunRepository;
    private final InterviewChangeRepository interviewChangeRepository;
    private final UnscheduledReasonRepository unscheduledReasonRepository;

    public ReplanDetailsService(
            ReplanRunRepository replanRunRepository,
            InterviewChangeRepository interviewChangeRepository,
            UnscheduledReasonRepository unscheduledReasonRepository) {

        this.replanRunRepository = replanRunRepository;
        this.interviewChangeRepository = interviewChangeRepository;
        this.unscheduledReasonRepository = unscheduledReasonRepository;
    }

    public ReplanDetailsResponse getReplanDetails(Long replanId) {

        ReplanRun replanRun = replanRunRepository
                .findById(replanId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No replan found with id " + replanId));

        List<InterviewChange> changes =
                interviewChangeRepository.findByReplanId(replanId);

        List<UnscheduledReason> failed =
                unscheduledReasonRepository.findByReplanId(replanId);

        ReplanDetailsResponse response =
                new ReplanDetailsResponse();

        response.setReplanId(
                replanRun.getReplanId());

        response.setDisruptionType(
                replanRun.getEvent().getEventType());

        response.setAffectedCount(
                replanRun.getInterviewsAffected());

        response.setMovedCount(
                replanRun.getInterviewsMoved());

        response.setFailedCount(
                replanRun.getInterviewsCancelled());

        List<ReplanDetailsResponse.InterviewChangeResponse>
                successfulChanges = changes.stream()
                .map(this::buildChangeResponse)
                .collect(Collectors.toList());

        List<ReplanDetailsResponse.FailedInterviewResponse>
                failedInterviews = failed.stream()
                .map(this::buildFailedResponse)
                .collect(Collectors.toList());

        response.setSuccessfulChanges(
                successfulChanges);

        response.setFailedInterviews(
                failedInterviews);

        return response;
    }

    private ReplanDetailsResponse.InterviewChangeResponse
            buildChangeResponse(InterviewChange change) {

        ReplanDetailsResponse.InterviewChangeResponse response =
                new ReplanDetailsResponse.InterviewChangeResponse();

        response.setInterviewId(
                change.getInterview().getInterviewId());

        response.setStudentId(
                change.getInterview().getStudent().getStudentId());

        response.setCompanyId(
                change.getInterview().getCompany().getCompanyId());

        response.setOldDay(
                change.getOldDay());

        response.setOldStartTime(
                change.getOldStartTime());

        response.setOldEndTime(
                change.getOldEndTime());

        response.setOldPanelId(
                change.getOldPanelId());

        response.setOldRoomId(
                change.getOldRoomId());

        response.setNewDay(
                change.getNewDay());

        response.setNewStartTime(
                change.getNewStartTime());

        response.setNewEndTime(
                change.getNewEndTime());

        response.setNewPanelId(
                change.getNewPanelId());

        response.setNewRoomId(
                change.getNewRoomId());

        return response;
    }

    private ReplanDetailsResponse.FailedInterviewResponse
            buildFailedResponse(UnscheduledReason reason) {

        ReplanDetailsResponse.FailedInterviewResponse response =
                new ReplanDetailsResponse.FailedInterviewResponse();

        response.setInterviewId(
                reason.getInterview().getInterviewId());

        response.setStudentId(
                reason.getInterview().getStudent().getStudentId());

        response.setCompanyId(
                reason.getInterview().getCompany().getCompanyId());

        response.setReason(
                reason.getReason());

        return response;
    }
}
