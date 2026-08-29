package placement_scheduler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import placement_scheduler.entity.CompanySlot;

import java.util.List;

@Repository
public interface CompanySlotRepository
        extends JpaRepository<CompanySlot, Long> {

    List<CompanySlot> findByCompanyCompanyId(Long companyId);
}
