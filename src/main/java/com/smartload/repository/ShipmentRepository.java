package com.smartload.repository;

import com.smartload.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Shipment entity. The bin-packing algorithm (Faz 3) reads shipments to enforce
 * the `canMix=NO` rule and to determine multi-leg destination ordering (last-on-first-off).
 */
@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, String> {

    List<Shipment> findByManifestId(String manifestId);

    Optional<Shipment> findByManifestIdAndCode(String manifestId, String code);

    @Modifying
    @Query("DELETE FROM Shipment s WHERE s.manifestId = :manifestId")
    int deleteByManifestId(String manifestId);
}
