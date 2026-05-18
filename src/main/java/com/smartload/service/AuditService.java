package com.smartload.service;

import com.smartload.entity.AuditLog;
import com.smartload.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    public void logFieldChange(
            String manifestId,
            String packageId,
            String entityType,
            String fieldName,
            Object oldValue,
            Object newValue
    ) {
        String oldStr = oldValue != null ? String.valueOf(oldValue) : null;
        String newStr = newValue != null ? String.valueOf(newValue) : null;
        if (oldStr != null && oldStr.equals(newStr)) {
            return;
        }
        if (oldStr == null && newStr == null) {
            return;
        }

        AuditLog entry = new AuditLog();
        entry.setManifestId(manifestId);
        entry.setPackageId(packageId);
        entry.setEntityType(entityType);
        entry.setFieldName(fieldName);
        entry.setOldValue(oldStr);
        entry.setNewValue(newStr);
        entry.setUserEmail(resolveUserEmail());
        auditLogRepository.save(entry);
    }

    private String resolveUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null) {
            return auth.getName();
        }
        return null;
    }
}
