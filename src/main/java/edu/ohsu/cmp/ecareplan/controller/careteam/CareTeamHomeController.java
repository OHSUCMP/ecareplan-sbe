package edu.ohsu.cmp.ecareplan.controller.careteam;

import edu.ohsu.cmp.ecareplan.controller.BaseController;
import edu.ohsu.cmp.ecareplan.entity.Endpoint;
import edu.ohsu.cmp.ecareplan.exception.ConfigurationException;
import edu.ohsu.cmp.ecareplan.model.Audience;
import edu.ohsu.cmp.ecareplan.model.AuditSeverity;
import edu.ohsu.cmp.ecareplan.model.fhir.FHIRCredentials;
import edu.ohsu.cmp.ecareplan.service.EndpointService;
import edu.ohsu.cmp.ecareplan.service.SessionService;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

@Controller
@RequestMapping("/care-team")
public class CareTeamHomeController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(CareTeamHomeController.class);

    @Autowired
    private SessionService sessionService;

    @Autowired
    private EndpointService endpointService;

    private final Endpoint careTeamEndpoint = endpointService.getCareTeamLaunchEndpoint();

    @Value("#{new Boolean('${security.browser.cache-credentials}')}")
    private Boolean cacheCredentials;

    @Value("${system.status-message}")
    private String systemStatusMessage;

    @GetMapping("launch")
    public String launch(HttpSession session, Model model) {
        sessionService.forceExpiration(session.getId());
        setCommonViewComponents(model);
        model.addAttribute("clientId", careTeamEndpoint.getClientId());
        model.addAttribute("scope", careTeamEndpoint.getScope());
        model.addAttribute("redirectUri", careTeamEndpoint.getRedirectUri()); // /care-team/complete-handshake
        model.addAttribute("iss", careTeamEndpoint.getIss());
        return "launch";
    }

    @GetMapping("complete-handshake")
    public String completeHandshake(HttpSession session, Model model) {
        setCommonViewComponents(model);
        model.addAttribute("cacheCredentials", cacheCredentials);
        model.addAttribute("redirectUri", "/care-team");
        return "complete-handshake";
    }

    @PostMapping("prepare-session")
    public ResponseEntity<?> prepareSession(HttpSession session,
                                            @RequestParam String clientId,
                                            @RequestParam String serverUrl,
                                            @RequestParam String bearerToken,
                                            @RequestParam String patientId,
                                            @RequestParam String userId) throws ConfigurationException, IOException {

        if ( ! careTeamEndpoint.getClientId().equals(clientId) || ! careTeamEndpoint.getIss().equals(serverUrl) ) {
            logger.error("clientId or serverUrl do not match expected values for CARE_TEAM context (clientId={}, serverUrl={})", clientId, serverUrl);
            auditService.doAudit(session.getId(), AuditSeverity.WARN, "invalid launch", "received invalid clientId or serverUrl for CARE_TEAM context");
            return ResponseEntity.badRequest().body("invalid clientId or serverUrl");
        }

        FHIRCredentials credentials = new FHIRCredentials(clientId, serverUrl, bearerToken, patientId, userId);

        sessionService.prepareSession(session.getId(), credentials, Audience.CARE_TEAM);

        return ResponseEntity.ok("care-team session established");
    }

    @GetMapping(value = {"", "/"})
    public String view(HttpSession session, Model model) throws Exception {
        String sessionId = session.getId();
        if (sessionService.exists(sessionId)) {

            throw new NotImplementedException("care-team home page not yet implemented");

        } else {
            logger.debug("session does not exist.  redirecting to launch page");
            return "redirect:/care-team/launch";
        }
    }
}