package placement_scheduler.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long companyId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private Double cgpaCutoff;

    @Column(nullable = false)
    private Integer interviewDurationMin;

    @Column(nullable = false)
    private Integer priorityTier;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @ElementCollection
    @CollectionTable(name = "company_eligible_branches", joinColumns = @JoinColumn(name = "company_id"))
    @Column(name = "branch")
    private Set<String> eligibleBranches = new HashSet<>();

    // Constructors
    public Company() {
    }

    // Getters and Setters

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getCgpaCutoff() {
        return cgpaCutoff;
    }

    public void setCgpaCutoff(Double cgpaCutoff) {
        this.cgpaCutoff = cgpaCutoff;
    }

    public Integer getInterviewDurationMin() {
        return interviewDurationMin;
    }

    public void setInterviewDurationMin(Integer interviewDurationMin) {
        this.interviewDurationMin = interviewDurationMin;
    }

    public Integer getPriorityTier() {
        return priorityTier;
    }

    public void setPriorityTier(Integer priorityTier) {
        this.priorityTier = priorityTier;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Set<String> getEligibleBranches() {
        return eligibleBranches;
    }

    public void setEligibleBranches(Set<String> eligibleBranches) {
        this.eligibleBranches = eligibleBranches;
    }
}