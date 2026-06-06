package com.smartload.repository;

import com.smartload.entity.UldType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UldTypeRepository extends JpaRepository<UldType, Long> {
    List<UldType> findByAircraftId(Long aircraftId);
}
