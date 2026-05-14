package edu.ohsu.cmp.ecareplan.workspace;

import edu.ohsu.cmp.ecareplan.exception.ConfigurationException;
import edu.ohsu.cmp.ecareplan.exception.SessionMissingException;
import edu.ohsu.cmp.ecareplan.model.Audience;
import edu.ohsu.cmp.ecareplan.model.fhir.FHIRCredentialsWithClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserWorkspaceService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ApplicationContext ctx;

    private final Map<String, UserWorkspace> map;

    public UserWorkspaceService() {
        map = new ConcurrentHashMap<>();
    }

    public boolean exists(String sessionId) {
        return map.containsKey(sessionId);
    }

    public void init(String sessionId, Audience audience, FHIRCredentialsWithClient fcc) throws ConfigurationException {
        try {
            if (shutdown(sessionId)) {
                logger.warn("found pre-existing User Workspace for session=" + sessionId +
                        " during init, which we shut down.  this is weird, as this should have been cleared earlier.  ???");
            }

            UserWorkspace workspace = new UserWorkspace(ctx, sessionId, audience, fcc);
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
}
