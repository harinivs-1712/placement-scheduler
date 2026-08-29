package placement_scheduler.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "shortlists",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_student_company",
            columnNames = {"student_id", "company_id"}
        )
    }
)
public class Shortlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long shortlistId;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private Integer rank;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Constructor
    public Shortlist() {
    }

    // Getters and Setters

    public Long getShortlistId() {
        return shortlistId;
    }

    public void setShortlistId(Long shortlistId) {
        this.shortlistId = shortlistId;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
