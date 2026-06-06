package com.smartload.repository;

import com.smartload.entity.PackagePlacement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface PackagePlacementRepository extends JpaRepository<PackagePlacement, Long> {
    List<PackagePlacement> findByUldAssignmentId(Long uldAssignmentId);

    @Modifying
    @Query("DELETE FROM PackagePlacement p WHERE p.uldAssignmentId IN " +
           "(SELECT u.id FROM UldAssignment u WHERE u.loadPlanId = :loadPlanId)")
    void deleteByLoadPlanId(Long loadPlanId);
}
