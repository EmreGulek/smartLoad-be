package com.smartload.repository;

import com.smartload.entity.Manifest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repository for Manifest entity.
 */
@Repository
public interface ManifestRepository extends JpaRepository<Manifest, String> {
    List<Manifest> findByStatus(String status);
    List<Manifest> findByFileNameContainingIgnoreCase(String fileName);
}
