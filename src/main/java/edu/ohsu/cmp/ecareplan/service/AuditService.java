package edu.ohsu.cmp.ecareplan.service;

import edu.ohsu.cmp.ecareplan.entity.AuditData;
import edu.ohsu.cmp.ecareplan.entity.User;
import edu.ohsu.cmp.ecareplan.model.AuditSeverity;
import edu.ohsu.cmp.ecareplan.repository.AuditDataRepository;
import edu.ohsu.cmp.ecareplan.util.logging.LogRedactor;
import edu.ohsu.cmp.ecareplan.workspace.UserWorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Date;
import java.util.List;

@Service
public class AuditService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private UserWorkspaceService userWorkspaceService;

    @Autowired
    private AuditDataRepository repository;

    public List<AuditData> getAuditDataForUser(User user, Collection<String> eventsToInclude, Date fromDate, Date toDate) {
        return repository.getAuditDataForUser(user.getId(), eventsToInclude, fromDate, toDate);
    }

    public void doAudit(String sessionId, AuditSeverity severity, String event) {
        if (userWorkspaceService.exists(sessionId)) {
            Long userId = userWorkspaceService.get(sessionId).getUser().getId();
            doAudit(userId, severity, event, null);

        } else {
            logger.warn("attempted to generate audit for nonexistent session " + sessionId + ": severity=" + severity +
                    ", event=" + event);
        }
    }

    public void doAudit(String sessionId, AuditSeverity severity, String event, String details) {
        if (userWorkspaceService.exists(sessionId)) {
            Long userId = userWorkspaceService.get(sessionId).getUser().getId();
            doAudit(userId, severity, event, details);

        } else {
            logger.warn("attempted to generate audit for nonexistent session " + sessionId + ": severity=" + severity +
                    ", event=" + event + ", details=" + details);
        }
    }

    public void doAudit(User user, AuditSeverity severity, String event) {
        doAudit(user, severity, event, null);
    }

    public void doAudit(User user, AuditSeverity severity, String event, String details) {
        doAudit(user.getId(), severity, event, details);
    }

    private void doAudit(Long userId, AuditSeverity severity, String event, String details) {
        AuditData auditData = new AuditData(
                userId,
                severity,
                event,
                LogRedactor.redact(details)
        );

        try {
            repository.save(auditData);

        } catch (Exception e) {
            logger.error("caught " + e.getClass().getName() + " attempting to create " + auditData + " - " + e.getMessage(), e);
        }
    }
}
