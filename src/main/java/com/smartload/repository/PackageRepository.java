package com.smartload.repository;

import com.smartload.entity.Package;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Package entity. Bin-packing algorithm (Faz 3) will use these queries to load
 * the working set; manifest delete uses the bulk delete-by-manifest to clean up cascades.
 */
@Repository
public interface PackageRepository extends JpaRepository<Package, String> {

    List<Package> findByManifestId(String manifestId);

    List<Package> findByShipmentId(String shipmentId);

    List<Package> findByManifestIdAndDestinationCode(String manifestId, String destinationCode);

    /**
     * Bulk delete by manifest — cheaper and clearer than loading every row first.
     * Called from ManifestService.deleteManifest before the manifest itself is removed.
     */
    @Modifying
    @Query("DELETE FROM Package p WHERE p.manifestId = :manifestId")
    int deleteByManifestId(String manifestId);
}
