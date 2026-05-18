package com.smartload.service;

import com.smartload.dto.PackageUpdateRequest;
import com.smartload.entity.Package;
import com.smartload.repository.PackageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class PackageService {

    @Autowired
    private PackageRepository packageRepository;

    @Autowired
    private AuditService auditService;

    @Transactional
    public Optional<Package> updatePackage(String manifestId, String packageId, PackageUpdateRequest update) {
        Optional<Package> optional = packageRepository.findById(packageId);
        if (optional.isEmpty() || !manifestId.equals(optional.get().getManifestId())) {
            return Optional.empty();
        }
        Package pkg = optional.get();
        applyWithAudit(manifestId, packageId, "pieces", pkg.getPieces(), update.getPieces());
        if (update.getPieces() != null) {
            pkg.setPieces(update.getPieces());
        }
        applyWithAudit(manifestId, packageId, "lengthMm", pkg.getLengthMm(), update.getLengthMm());
        if (update.getLengthMm() != null) {
            pkg.setLengthMm(update.getLengthMm());
        }
        applyWithAudit(manifestId, packageId, "widthMm", pkg.getWidthMm(), update.getWidthMm());
        if (update.getWidthMm() != null) {
            pkg.setWidthMm(update.getWidthMm());
        }
        applyWithAudit(manifestId, packageId, "heightMm", pkg.getHeightMm(), update.getHeightMm());
        if (update.getHeightMm() != null) {
            pkg.setHeightMm(update.getHeightMm());
        }
        applyWithAudit(manifestId, packageId, "weightPerPieceKg", pkg.getWeightPerPieceKg(), update.getWeightPerPieceKg());
        if (update.getWeightPerPieceKg() != null) {
            pkg.setWeightPerPieceKg(update.getWeightPerPieceKg());
        }
        applyWithAudit(manifestId, packageId, "grossWeightKg", pkg.getGrossWeightKg(), update.getGrossWeightKg());
        if (update.getGrossWeightKg() != null) {
            pkg.setGrossWeightKg(update.getGrossWeightKg());
        }
        applyWithAudit(manifestId, packageId, "stackable", pkg.getStackable(), update.getStackable());
        if (update.getStackable() != null) {
            pkg.setStackable(update.getStackable());
        }
        applyWithAudit(manifestId, packageId, "rotatable", pkg.getRotatable(), update.getRotatable());
        if (update.getRotatable() != null) {
            pkg.setRotatable(update.getRotatable());
        }
        applyWithAudit(manifestId, packageId, "toploadable", pkg.getToploadable(), update.getToploadable());
        if (update.getToploadable() != null) {
            pkg.setToploadable(update.getToploadable());
        }
        applyWithAudit(manifestId, packageId, "canMix", pkg.getCanMix(), update.getCanMix());
        if (update.getCanMix() != null) {
            pkg.setCanMix(update.getCanMix());
        }
        applyWithAudit(manifestId, packageId, "uldPreference", pkg.getUldPreference(), update.getUldPreference());
        if (update.getUldPreference() != null) {
            pkg.setUldPreference(update.getUldPreference());
        }
        applyWithAudit(manifestId, packageId, "destinationCode", pkg.getDestinationCode(), update.getDestinationCode());
        if (update.getDestinationCode() != null) {
            pkg.setDestinationCode(update.getDestinationCode());
        }
        applyWithAudit(manifestId, packageId, "specialHandling", pkg.getSpecialHandling(), update.getSpecialHandling());
        if (update.getSpecialHandling() != null) {
            pkg.setSpecialHandling(update.getSpecialHandling());
        }
        applyWithAudit(manifestId, packageId, "dgClass", pkg.getDgClass(), update.getDgClass());
        if (update.getDgClass() != null) {
            pkg.setDgClass(update.getDgClass());
        }
        return Optional.of(packageRepository.save(pkg));
    }

    private void applyWithAudit(
            String manifestId,
            String packageId,
            String field,
            Object oldVal,
            Object newVal
    ) {
        if (newVal == null) {
            return;
        }
        auditService.logFieldChange(manifestId, packageId, "package", field, oldVal, newVal);
    }

    public java.util.List<Package> listByManifest(String manifestId) {
        return packageRepository.findByManifestId(manifestId);
    }
}
