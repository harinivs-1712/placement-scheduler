package placement_scheduler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import placement_scheduler.entity.Company;

@Repository
public interface CompanyRepository
        extends JpaRepository<Company, Long> {
}
