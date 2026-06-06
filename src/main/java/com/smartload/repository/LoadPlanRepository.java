package com.smartload.repository;

import com.smartload.entity.LoadPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoadPlanRepository extends JpaRepository<LoadPlan, Long> {
    List<LoadPlan> findByManifestIdOrderByCreatedAtDesc(String manifestId);
}
