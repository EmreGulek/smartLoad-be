package com.smartload.repository;

import com.smartload.entity.AircraftPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AircraftPositionRepository extends JpaRepository<AircraftPosition, Long> {
    List<AircraftPosition> findByAircraftIdAndIsActiveTrueOrderByDisplayOrderAsc(Long aircraftId);
}
