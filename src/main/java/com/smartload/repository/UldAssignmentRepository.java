package com.smartload.repository;

import com.smartload.entity.UldAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UldAssignmentRepository extends JpaRepository<UldAssignment, Long> {
    List<UldAssignment> findByLoadPlanId(Long loadPlanId);
    void deleteByLoadPlanId(Long loadPlanId);
}
