package com.smartload.entity;

import jakarta.persistence.*;

/**
 * PackagePlacement — exact 3D position of one package inside its ULD container.
 *
 * Coordinates (x, y, z) are in ULD-local millimetres:
 *   x = lateral (0 = left wall of ULD)
 *   y = vertical (0 = ULD floor)
 *   z = longitudinal (0 = ULD front face, depth runs toward back)
 *
 * Applied dimensions reflect the chosen rotation (rotationIndex determines
 * how the original L×W×H maps to the placed appL × appW × appH).
 *
 * Rotation index encoding (see BinPackingService.ROTATIONS):
 *   0 = (W, H, L) — natural orientation (width lateral, height vertical, length depth)
 *   1 = (L, H, W) — 90° floor rotation
 *   2 = (W, L, H) — tipped on side
 *   3 = (L, W, H) — tipped on side + floor rotate
 *   4 = (H, W, L) — nose-down
 *   5 = (H, L, W) — nose-down + floor rotate
 */
@Entity
@Table(name = "package_placements")
public class PackagePlacement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uld_assignment_id", nullable = false)
    private Long uldAssignmentId;

    /** FK to packages.id (UUID string). */
    @Column(name = "package_id", nullable = false)
    private String packageId;

    @Column(name = "x_mm", nullable = false)  private Integer xMm;
    @Column(name = "y_mm", nullable = false)  private Integer yMm;
    @Column(name = "z_mm", nullable = false)  private Integer zMm;

    /** Applied lateral dimension after rotation (ULD x-axis). */
    @Column(name = "applied_width_mm",  nullable = false) private Integer appliedWidthMm;
    /** Applied vertical dimension after rotation (ULD y-axis). */
    @Column(name = "applied_height_mm", nullable = false) private Integer appliedHeightMm;
    /** Applied depth dimension after rotation (ULD z-axis). */
    @Column(name = "applied_depth_mm",  nullable = false) private Integer appliedDepthMm;

    /** 0–5 rotation index; see class-level javadoc. */
    @Column(name = "rotation_index", nullable = false)
    private Integer rotationIndex;

    // ── Constructors ──────────────────────────────────────────────────────────

    public PackagePlacement() {}

    public PackagePlacement(Long uldAssignmentId, String packageId,
                             int xMm, int yMm, int zMm,
                             int appliedWidthMm, int appliedHeightMm, int appliedDepthMm,
                             int rotationIndex) {
        this.uldAssignmentId = uldAssignmentId;
        this.packageId       = packageId;
        this.xMm             = xMm;
        this.yMm             = yMm;
        this.zMm             = zMm;
        this.appliedWidthMm  = appliedWidthMm;
        this.appliedHeightMm = appliedHeightMm;
        this.appliedDepthMm  = appliedDepthMm;
        this.rotationIndex   = rotationIndex;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public Long    getId()                          { return id; }
    public Long    getUldAssignmentId()             { return uldAssignmentId; }
    public void    setUldAssignmentId(Long v)       { this.uldAssignmentId = v; }
    public String  getPackageId()                   { return packageId; }
    public void    setPackageId(String v)           { this.packageId = v; }
    public Integer getXMm()                         { return xMm; }
    public void    setXMm(Integer v)                { this.xMm = v; }
    public Integer getYMm()                         { return yMm; }
    public void    setYMm(Integer v)                { this.yMm = v; }
    public Integer getZMm()                         { return zMm; }
    public void    setZMm(Integer v)                { this.zMm = v; }
    public Integer getAppliedWidthMm()              { return appliedWidthMm; }
    public void    setAppliedWidthMm(Integer v)     { this.appliedWidthMm = v; }
    public Integer getAppliedHeightMm()             { return appliedHeightMm; }
    public void    setAppliedHeightMm(Integer v)    { this.appliedHeightMm = v; }
    public Integer getAppliedDepthMm()              { return appliedDepthMm; }
    public void    setAppliedDepthMm(Integer v)     { this.appliedDepthMm = v; }
    public Integer getRotationIndex()               { return rotationIndex; }
    public void    setRotationIndex(Integer v)      { this.rotationIndex = v; }
}
