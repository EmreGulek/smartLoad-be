package com.smartload.repository;

import com.smartload.entity.Aircraft;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AircraftRepository extends JpaRepository<Aircraft, Long> {
    Optional<Aircraft> findByIcaoCode(String icaoCode);
}
