package com.smartload.dto;

/**
 * CgResultDto — Center of Gravity calculation result for a LoadPlan.
 *
 * ══ Mock B777F CG Envelope (academic approximation) ═══════════════════════════
 *
 * Real data source (AHM560) unavailable; values below are derived from publicly
 * available B777F performance data and calibrated to the Three.js scene coordinate
 * system used in SmartLoad (arm_mm: positive = toward nose, see ADR-0012).
 *
 * Scene coordinate reference:
 *   P1 (nose) arm_mm =  25 000   →  ~14 m from physical nose
 *   P9 (aft)  arm_mm = -9 900    →  ~57 m from physical nose
 *   Linear scale factor ≈ 1/1.347 (scene is compressed vs. real aircraft)
 *
 * LEMAC in scene coords: 12 700 mm  (≈ 28.8 m from nose in real coords)
 * MAC   in scene coords:  9 000 mm  (≈ 9.25 m — B777F MAC ≈ 364 in)
 * TEMAC in scene coords:  3 700 mm  (= LEMAC − MAC)
 *
 * OEW (Operating Empty Weight): 138 100 kg (typical B777F, no payload)
 * OEW CG arm (scene):           10 000 mm  → ~20% MAC  (standard empty CG)
 *
 * CG limits (%MAC) — B777F approximate values:
 *   Forward hard limit : 15 %
 *   Forward warning    : 18 %
 *   Aft warning        : 33 %
 *   Aft hard limit     : 37 %
 *
 * Note: tez/thesis document should state:
 *   "Gerçek AHM560 veya uçak performans el kitabı verileri mevcut olmadığından,
 *    akademik amaçlı yaklaşık değerler kullanılmıştır."
 *
 * ══ Formula ═══════════════════════════════════════════════════════════════════
 *
 *   CG_arm  = (OEW × OEW_arm + Σ ULD_weight × position_arm) / (OEW + Σ ULD_weight)
 *   %MAC    = (LEMAC_MM − CG_arm) / MAC_MM × 100
 *
 *   In this sign convention (higher arm = more forward):
 *     %MAC < 0          → CG forward of LEMAC (very nose-heavy)
 *     %MAC = 0          → CG at leading edge of MAC
 *     %MAC = 100        → CG at trailing edge of MAC (very tail-heavy)
 *     Typical safe range: 15–37 %
 *
 * ══════════════════════════════════════════════════════════════════════════════
 */
public class CgResultDto {

    // ── Mock B777F envelope constants ─────────────────────────────────────────
    public static final double LEMAC_MM      = 12_700.0;
    public static final double MAC_MM        =  9_000.0;
    public static final double OEW_KG        = 138_100.0;
    public static final double OEW_ARM_MM    = 10_000.0;
    public static final double FWD_LIMIT_PCT =    15.0;
    public static final double FWD_WARN_PCT  =    18.0;
    public static final double AFT_WARN_PCT  =    33.0;
    public static final double AFT_LIMIT_PCT =    37.0;

    // ── Computed fields ───────────────────────────────────────────────────────

    /** Computed CG arm in scene mm units. */
    private double cgArmMm;

    /** Computed %MAC (0 = leading edge, 100 = trailing edge). */
    private double cgMacPct;

    /** Total aircraft weight: OEW + cargo (kg). */
    private double totalWeightKg;

    /**
     * Envelope status:
     *   GREEN       — within safe limits (18–33 % MAC)
     *   YELLOW_FWD  — forward warning zone (15–18 % MAC)
     *   YELLOW_AFT  — aft warning zone    (33–37 % MAC)
     *   RED_FWD     — forward limit exceeded (< 15 % MAC)
     *   RED_AFT     — aft limit exceeded     (> 37 % MAC)
     */
    private String status;

    // ── Envelope boundaries sent to frontend for gauge rendering ─────────────
    private double lemacMm;
    private double macMm;
    private double fwdLimitPct;
    private double fwdWarnPct;
    private double aftWarnPct;
    private double aftLimitPct;
    private double oewKg;
    private double oewArmMm;

    // ── Factory ───────────────────────────────────────────────────────────────

    /**
     * Compute CG from the assignments in a load plan.
     *
     * @param assignments ULD assignment DTOs (each has armMm + totalWeightKg)
     * @return CgResultDto with status and %MAC
     */
    public static CgResultDto compute(java.util.List<UldAssignmentDto> assignments) {
        double cargoMoment = 0;
        double cargoWeight = 0;
        for (UldAssignmentDto a : assignments) {
            if (a.getPackageCount() > 0 && a.getTotalWeightKg() > 0) {
                cargoMoment += a.getTotalWeightKg() * a.getArmMm();
                cargoWeight += a.getTotalWeightKg();
            }
        }

        double totalWeight = OEW_KG + cargoWeight;
        double totalMoment = OEW_KG * OEW_ARM_MM + cargoMoment;
        double cgArm       = totalWeight > 0 ? totalMoment / totalWeight : OEW_ARM_MM;
        double cgMacPct    = (LEMAC_MM - cgArm) / MAC_MM * 100.0;

        String status;
        if      (cgMacPct < FWD_LIMIT_PCT) status = "RED_FWD";
        else if (cgMacPct < FWD_WARN_PCT)  status = "YELLOW_FWD";
        else if (cgMacPct > AFT_LIMIT_PCT) status = "RED_AFT";
        else if (cgMacPct > AFT_WARN_PCT)  status = "YELLOW_AFT";
        else                               status = "GREEN";

        CgResultDto d = new CgResultDto();
        d.cgArmMm      = Math.round(cgArm * 10.0) / 10.0;
        d.cgMacPct     = Math.round(cgMacPct * 10.0) / 10.0;
        d.totalWeightKg = Math.round(totalWeight * 10.0) / 10.0;
        d.status       = status;
        d.lemacMm      = LEMAC_MM;
        d.macMm        = MAC_MM;
        d.fwdLimitPct  = FWD_LIMIT_PCT;
        d.fwdWarnPct   = FWD_WARN_PCT;
        d.aftWarnPct   = AFT_WARN_PCT;
        d.aftLimitPct  = AFT_LIMIT_PCT;
        d.oewKg        = OEW_KG;
        d.oewArmMm     = OEW_ARM_MM;
        return d;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public double getCgArmMm()       { return cgArmMm; }
    public double getCgMacPct()      { return cgMacPct; }
    public double getTotalWeightKg() { return totalWeightKg; }
    public String getStatus()        { return status; }
    public double getLemacMm()       { return lemacMm; }
    public double getMacMm()         { return macMm; }
    public double getFwdLimitPct()   { return fwdLimitPct; }
    public double getFwdWarnPct()    { return fwdWarnPct; }
    public double getAftWarnPct()    { return aftWarnPct; }
    public double getAftLimitPct()   { return aftLimitPct; }
    public double getOewKg()         { return oewKg; }
    public double getOewArmMm()      { return oewArmMm; }
}
