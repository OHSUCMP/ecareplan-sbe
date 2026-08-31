package edu.ohsu.cmp.ecareplan.service;

import edu.ohsu.cmp.ecareplan.entity.AuditData;
import edu.ohsu.cmp.ecareplan.entity.User;
import edu.ohsu.cmp.ecareplan.model.AuditSeverity;
import edu.ohsu.cmp.ecareplan.repository.AuditRepository;
import edu.ohsu.cmp.ecareplan.util.logging.LogRedactor;
import edu.ohsu.cmp.ecareplan.workspace.UserWorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private UserWorkspaceService userWorkspaceService;

    @Autowired
    private AuditRepository repository;

    public void doAudit(String sessionId, AuditSeverity severity, String action) {
        if (userWorkspaceService.exists(sessionId)) {
            Long userId = userWorkspaceService.get(sessionId).getUserId();
            doAudit(userId, severity, action, null);

        } else {
            logger.warn("attempted to generate audit for nonexistent session " + sessionId + ": severity=" + severity +
                    ", action=" + action);
        }
    }

    public void doAudit(String sessionId, AuditSeverity severity, String action, String details) {
        if (userWorkspaceService.exists(sessionId)) {
            Long userId = userWorkspaceService.get(sessionId).getUserId();
            doAudit(userId, severity, action, details);

        } else {
            logger.warn("attempted to generate audit for nonexistent session " + sessionId + ": severity=" + severity +
                    ", action=" + action + ", details=" + details);
        }
    }

    public void doAudit(User user, AuditSeverity severity, String action) {
        doAudit(user, severity, action, null);
    }

    public void doAudit(User user, AuditSeverity severity, String action, String details) {
        doAudit(user.getId(), severity, action, details);
    }

    private void doAudit(Long userId, AuditSeverity severity, String action, String details) {
        AuditData auditData = new AuditData(
                userId,
                severity,
                action,
                LogRedactor.redact(details)
        );

        try {
            repository.save(auditData);

        } catch (Exception e) {
            logger.error("caught " + e.getClass().getName() + " attempting to create " + auditData + " - " + e.getMessage(), e);
        }
    }
}
