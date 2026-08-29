package placement_scheduler.service;

import org.springframework.stereotype.Service;

import placement_scheduler.dto.StudentInterviewDTO;
import placement_scheduler.dto.StudentOverviewDTO;
import placement_scheduler.entity.Interview;
import placement_scheduler.entity.Student;
import placement_scheduler.repository.InterviewRepository;
import placement_scheduler.repository.StudentRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class StudentOverviewService {

    private final StudentRepository studentRepository;
    private final InterviewRepository interviewRepository;

    public StudentOverviewService(
            StudentRepository studentRepository,
            InterviewRepository interviewRepository) {

        this.studentRepository = studentRepository;
        this.interviewRepository = interviewRepository;
    }

    public List<StudentOverviewDTO> getStudentOverview() {

        List<Student> students = studentRepository.findAll();

        List<StudentOverviewDTO> result = new ArrayList<>();

        for (Student student : students) {

            StudentOverviewDTO dto = new StudentOverviewDTO();

            dto.setStudentId(student.getStudentId());
            dto.setName(student.getName());
            dto.setCgpa(student.getCgpa());

            List<Interview> interviews =
                    interviewRepository.findByStudentStudentId(
                            student.getStudentId());

            List<StudentInterviewDTO> interviewDTOs =
                    new ArrayList<>();

            boolean hasScheduledInterview = false;

            for (Interview interview : interviews) {

                StudentInterviewDTO interviewDTO =
                        new StudentInterviewDTO();

                interviewDTO.setInterviewId(
                        interview.getInterviewId());

                interviewDTO.setStatus(
                        interview.getStatus());

                if ("SCHEDULED".equals(interview.getStatus())) {
                    hasScheduledInterview = true;
                }

                interviewDTO.setDay(interview.getDay());
                interviewDTO.setStartTime(interview.getStartTime());
                interviewDTO.setEndTime(interview.getEndTime());

                if (interview.getCompany() != null) {

                    interviewDTO.setCompanyId(
                            interview.getCompany().getCompanyId());

                    interviewDTO.setCompanyName(
                            interview.getCompany().getName());
                }

                if (interview.getPanel() != null) {

                    interviewDTO.setPanelId(
                            interview.getPanel().getPanelId());

                    interviewDTO.setPanelName(
                            interview.getPanel().getLabel());
                }

                if (interview.getRoom() != null) {

                    interviewDTO.setRoomId(
                            interview.getRoom().getRoomId());

                    interviewDTO.setRoomName(
                            interview.getRoom().getName());
                }

                interviewDTOs.add(interviewDTO);
            }

            dto.setInterviews(interviewDTOs);

            dto.setStatus(
                    hasScheduledInterview
                            ? "SCHEDULED"
                            : "UNSCHEDULED"
            );

            result.add(dto);
        }

        return result;
    }
}
