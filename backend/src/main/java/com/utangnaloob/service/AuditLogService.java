package com.utangnaloob.service;

import com.utangnaloob.dto.AuditLogDto;
import com.utangnaloob.entity.AuditLog;
import com.utangnaloob.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    public void log(String action, String performedBy, String targetUser, String details, String ipAddress) {
        AuditLog auditLog = new AuditLog(action, performedBy, targetUser, details, ipAddress);
        auditLogRepository.save(auditLog);
    }

    public List<AuditLogDto> getRecentLogs() {
        return auditLogRepository.findTop50ByOrderByTimestampDesc().stream()
                .map(log -> new AuditLogDto(
                        log.getId(),
                        log.getAction(),
                        log.getPerformedBy(),
                        log.getTargetUser(),
                        log.getDetails(),
                        log.getIpAddress(),
                        log.getTimestamp()))
                .collect(Collectors.toList());
    }

    public List<AuditLogDto> getLogsByAction(String action) {
        return auditLogRepository.findByActionOrderByTimestampDesc(action).stream()
                .map(log -> new AuditLogDto(
                        log.getId(),
                        log.getAction(),
                        log.getPerformedBy(),
                        log.getTargetUser(),
                        log.getDetails(),
                        log.getIpAddress(),
                        log.getTimestamp()))
                .collect(Collectors.toList());
    }
}
