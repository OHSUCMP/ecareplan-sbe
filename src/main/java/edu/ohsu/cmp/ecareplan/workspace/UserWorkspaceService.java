package edu.ohsu.cmp.ecareplan.workspace;

import edu.ohsu.cmp.ecareplan.exception.ConfigurationException;
import edu.ohsu.cmp.ecareplan.exception.SessionMissingException;
import edu.ohsu.cmp.ecareplan.model.Audience;
import edu.ohsu.cmp.ecareplan.model.fhir.FHIRCredentials;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserWorkspaceService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${socket.timeout:300000}")
    private Integer socketTimeout;

    @Autowired
    private ApplicationContext ctx;

    private final Map<String, UserWorkspace> map;

    public UserWorkspaceService() {
        map = new ConcurrentHashMap<>();
    }

    public boolean exists(String sessionId) {
        return map.containsKey(sessionId);
    }

    public void init(String sessionId, Audience audience, FHIRCredentials credentials) throws ConfigurationException {
        try {
            if (shutdown(sessionId)) {
                logger.warn("found pre-existing User Workspace for session={} during init, which we shut down.  this is weird, as this should have been cleared earlier.  ???", sessionId);
            }

            UserWorkspace workspace = new UserWorkspace(ctx, sessionId, audience, credentials, socketTimeout);
            map.put(sessionId, workspace);

        } catch (Exception e) {
            throw new ConfigurationException(e);
        }
    }

    public UserWorkspace get(String sessionId) throws SessionMissingException {
        if (map.containsKey(sessionId)) {
            return map.get(sessionId);

        } else {
            throw new SessionMissingException(sessionId);
        }
    }

    public boolean shutdown(String sessionId) {
        if (map.containsKey(sessionId)) {
            UserWorkspace workspace = map.remove(sessionId);
            workspace.shutdown();
            return true;
        }
        return false;
    }

    @PreDestroy
    public void shutdownAll() {
        logger.info("shutting down all user workspaces");

        for (UserWorkspace workspace : map.values()) {
            try {
                workspace.shutdown();
            } catch (Exception e) {
                logger.error("caught {} shutting down workspace for session {} - {}",
                        e.getClass().getSimpleName(), workspace.getSessionId(), e.getMessage(), e);
            }
        }

        map.clear();
    }
}
