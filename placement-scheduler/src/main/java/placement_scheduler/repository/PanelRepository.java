package placement_scheduler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import placement_scheduler.entity.Panel;

import java.util.List;

@Repository
public interface PanelRepository
        extends JpaRepository<Panel, Long> {

    List<Panel> findByCompanyCompanyId(Long companyId);
}