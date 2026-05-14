package edu.ohsu.cmp.ecareplan.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import edu.ohsu.cmp.ecareplan.exception.ConfigurationException;
import edu.ohsu.cmp.ecareplan.model.Audience;
import edu.ohsu.cmp.ecareplan.model.AuditSeverity;
import edu.ohsu.cmp.ecareplan.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.ecareplan.model.fhir.FHIRCredentials;
import edu.ohsu.cmp.ecareplan.util.FhirUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SessionService extends BaseService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${socket.timeout:300000}")
    private Integer socketTimeout;


    public void prepareSession(String sessionId, FHIRCredentials credentials, Audience audience) throws ConfigurationException {
        logger.debug("preparing session " + sessionId + " with credentials=" + credentials);
        IGenericClient client = FhirUtil.buildClient(
                credentials.getServerURL(),
                credentials.getBearerToken(),
                socketTimeout
        );
        FHIRCredentialsWithClient fcc = new FHIRCredentialsWithClient(credentials, client);

        userWorkspaceService.init(sessionId, audience, fcc);
        userWorkspaceService.get(sessionId).populate();

        auditService.doAudit(sessionId, AuditSeverity.INFO, "session established", "sessionId=" + sessionId +
                ", audience=" + audience);
    }

    public boolean exists(String sessionId) {
        return userWorkspaceService.exists(sessionId);
    }

    public void forceExpiration(String sessionId) {
        logger.info("expiring credentials for session " + sessionId);
        auditService.doAudit(sessionId, AuditSeverity.INFO, "session expired", sessionId);
        userWorkspaceService.shutdown(sessionId);
    }
}
