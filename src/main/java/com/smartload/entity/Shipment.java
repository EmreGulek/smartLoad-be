package com.smartload.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Shipment — groups packages that travel together within a manifest.
 *
 * Operational use:
 *   • `canMix=NO` rule: packages from the same shipment must stay together in a single ULD;
 *     they cannot mix with other shipments. Without a Shipment entity this rule has no
 *     foreign key to enforce (see MASTER-PLAN Faz 1 dikkat notları + ADR-0011).
 *   • Multi-leg flights (Faz 3): each shipment carries its own origin/destination pair; the
 *     `last-on-first-off` ordering uses shipment destination.
 *
 * Lifecycle:
 *   • Created during manifest save (one row per distinct shipment code encountered in the
 *     uploaded Excel; falls back to a synthetic code when absent).
 *   • Deleted via cascade when its parent manifest is removed.
 */
@Entity
@Table(name = "shipments")
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** Parent manifest. Cascade rules on Manifest side ensure shipment rows go away with it. */
    @Column(name = "manifest_id", nullable = false)
    private String manifestId;

    /** Shipment code from the Excel manifest (e.g. "TK7821-001"); synthetic if absent. */
    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "origin_code")
    private String originCode; // IATA 3-letter; nullable until origin column is in the import

    @Column(name = "destination_code")
    private String destinationCode; // IATA 3-letter; copied from the first package on save

    @Column(name = "can_mix")
    private Boolean canMix; // false → packages of this shipment must not mix with others

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Shipment() {
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getManifestId() { return manifestId; }
    public void setManifestId(String manifestId) { this.manifestId = manifestId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getOriginCode() { return originCode; }
    public void setOriginCode(String originCode) { this.originCode = originCode; }

    public String getDestinationCode() { return destinationCode; }
    public void setDestinationCode(String destinationCode) { this.destinationCode = destinationCode; }

    public Boolean getCanMix() { return canMix; }
    public void setCanMix(Boolean canMix) { this.canMix = canMix; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
