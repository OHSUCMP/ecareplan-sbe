package edu.ohsu.cmp.ecareplan.service;

import edu.ohsu.cmp.ecareplan.exception.ConfigurationException;
import edu.ohsu.cmp.ecareplan.model.Audience;
import edu.ohsu.cmp.ecareplan.model.AuditSeverity;
import edu.ohsu.cmp.ecareplan.model.fhir.FHIRCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SessionService extends BaseService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public void prepareSession(String sessionId, FHIRCredentials credentials, Audience audience) throws ConfigurationException {
        logger.debug("preparing session {} with credentials={}", sessionId, credentials);

        userWorkspaceService.init(sessionId, audience, credentials);
        userWorkspaceService.get(sessionId).populate();

        auditService.doAudit(sessionId, AuditSeverity.INFO, "session established", "sessionId=" + sessionId +
                ", audience=" + audience);
    }

    public boolean exists(String sessionId) {
        return userWorkspaceService.exists(sessionId);
    }

    public void forceExpiration(String sessionId) {
        logger.info("expiring credentials for session {}", sessionId);
        auditService.doAudit(sessionId, AuditSeverity.INFO, "session expired", sessionId);
        userWorkspaceService.shutdown(sessionId);
    }
}
