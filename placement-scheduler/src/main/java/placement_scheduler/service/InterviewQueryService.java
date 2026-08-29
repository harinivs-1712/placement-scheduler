package placement_scheduler.service;

import org.springframework.stereotype.Service;

import placement_scheduler.dto.ScheduledInterviewDTO;
import placement_scheduler.dto.UnscheduledInterviewDTO;
import placement_scheduler.entity.Interview;
import placement_scheduler.entity.UnscheduledReason;
import placement_scheduler.repository.InterviewRepository;
import placement_scheduler.repository.UnscheduledReasonRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class InterviewQueryService {

    private final InterviewRepository interviewRepository;
    private final UnscheduledReasonRepository unscheduledReasonRepository;

    public InterviewQueryService(
            InterviewRepository interviewRepository,
            UnscheduledReasonRepository unscheduledReasonRepository) {

        this.interviewRepository = interviewRepository;
        this.unscheduledReasonRepository = unscheduledReasonRepository;
    }

    public List<ScheduledInterviewDTO> getAllScheduled() {

        List<Interview> interviews =
                interviewRepository.findByStatus("scheduled");

        List<ScheduledInterviewDTO> result = new ArrayList<>();

        for (Interview i : interviews) {

            ScheduledInterviewDTO dto =
                    new ScheduledInterviewDTO();

            dto.setInterviewId(i.getInterviewId());

            dto.setDay(i.getDay());
            dto.setStartTime(i.getStartTime());
            dto.setEndTime(i.getEndTime());

            if (i.getStudent() != null) {
                dto.setStudentId(
                        i.getStudent().getStudentId());

                dto.setStudentName(
                        i.getStudent().getName());
            }

            if (i.getCompany() != null) {
                dto.setCompanyId(
                        i.getCompany().getCompanyId());

                dto.setCompanyName(
                        i.getCompany().getName());
            }

            if (i.getPanel() != null) {
                dto.setPanelId(
                        i.getPanel().getPanelId());

                dto.setPanelName(
                        i.getPanel().getLabel());
            }

            if (i.getRoom() != null) {
                dto.setRoomId(
                        i.getRoom().getRoomId());

                dto.setRoomName(
                        i.getRoom().getName());
            }

            dto.setStatus(i.getStatus());

            result.add(dto);
        }

        return result;
    }

    public List<UnscheduledInterviewDTO> getAllUnscheduled() {

        List<Interview> interviews =
                interviewRepository.findAllUnscheduled();

        List<UnscheduledInterviewDTO> result =
                new ArrayList<>();

        for (Interview i : interviews) {

            UnscheduledInterviewDTO dto =
                    new UnscheduledInterviewDTO();

            dto.setInterviewId(i.getInterviewId());

            if (i.getStudent() != null) {
                dto.setStudentId(
                        i.getStudent().getStudentId());

                dto.setStudentName(
                        i.getStudent().getName());
            }

            if (i.getCompany() != null) {
                dto.setCompanyId(
                        i.getCompany().getCompanyId());

                dto.setCompanyName(
                        i.getCompany().getName());
            }

            dto.setStatus(i.getStatus());

            List<UnscheduledReason> reasons =
                    unscheduledReasonRepository
                            .findByInterviewInterviewId(
                                    i.getInterviewId());

            if (!reasons.isEmpty()) {
                dto.setReason(
                        reasons.get(reasons.size() - 1).getReason());
            } else {
                dto.setReason("No reason recorded");
            }

            result.add(dto);
        }

        return result;
    }
}