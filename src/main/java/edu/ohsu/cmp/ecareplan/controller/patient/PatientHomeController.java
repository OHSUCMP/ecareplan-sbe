package edu.ohsu.cmp.ecareplan.controller.patient;

import edu.ohsu.cmp.ecareplan.entity.Endpoint;
import edu.ohsu.cmp.ecareplan.exception.ConfigurationException;
import edu.ohsu.cmp.ecareplan.model.Audience;
import edu.ohsu.cmp.ecareplan.model.AuditSeverity;
import edu.ohsu.cmp.ecareplan.model.fhir.FHIRCredentials;
import edu.ohsu.cmp.ecareplan.service.EndpointService;
import edu.ohsu.cmp.ecareplan.service.SessionService;
import edu.ohsu.cmp.ecareplan.workspace.UserWorkspace;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
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
@RequestMapping("/patient")
public class PatientHomeController extends BasePatientController {
    private static final Logger logger = LoggerFactory.getLogger(PatientHomeController.class);

    // this is the home page for the MyCarePlanner patient-focused app

    @Autowired
    private SessionService sessionService;

    @Autowired
    private EndpointService endpointService;

    @Value("#{new Boolean('${security.browser.cache-credentials}')}")
    private Boolean cacheCredentials;

    @Value("${system.status-message}")
    private String systemStatusMessage;

    @GetMapping("launch")
    public String launch(HttpSession session, Model model) {
        sessionService.forceExpiration(session.getId());
        setCommonViewComponents(model);
        Endpoint patientEndpoint = endpointService.getPatientLaunchEndpoint();
        model.addAttribute("clientId", patientEndpoint.getClientId());
        model.addAttribute("scope", patientEndpoint.getScope());
        model.addAttribute("redirectUri", patientEndpoint.getRedirectUri()); // /patient/complete-handshake
        model.addAttribute("iss", patientEndpoint.getIss());
        return "launch";
    }

    @GetMapping("complete-handshake")
    public String completeHandshake(HttpSession session, Model model) {
        setCommonViewComponents(model);
        model.addAttribute("cacheCredentials", cacheCredentials);
        model.addAttribute("redirectUri", "/patient");
        return "complete-handshake";
    }

    @PostMapping("prepare-session")
    public ResponseEntity<?> prepareSession(HttpSession session,
                                            @RequestParam String clientId,
                                            @RequestParam String serverUrl,
                                            @RequestParam String bearerToken,
                                            @RequestParam String patientId,
                                            @RequestParam String userId) throws ConfigurationException, IOException {

        Endpoint patientEndpoint = endpointService.getPatientLaunchEndpoint();
        if ( ! patientEndpoint.getClientId().equals(clientId) || ! patientEndpoint.getIss().equals(serverUrl) ) {
            logger.error("clientId or serverUrl do not match expected values for PATIENT context (clientId={}, serverUrl={})", clientId, serverUrl);
            auditService.doAudit(session.getId(), AuditSeverity.WARN, "invalid launch", "received invalid clientId or serverUrl for PATIENT context");
            return ResponseEntity.badRequest().body("invalid clientId or serverUrl");
        }

        FHIRCredentials credentials = new FHIRCredentials(clientId, serverUrl, bearerToken, patientId, userId);

        sessionService.prepareSession(session.getId(), credentials, Audience.PATIENT);

        return ResponseEntity.ok("patient session established");
    }

    @GetMapping(value = {"", "/"})
    public String view(HttpSession session, Model model) throws Exception {
        String sessionId = session.getId();
        if (sessionService.exists(sessionId)) {
            logger.info("session exists.  requesting data for session " + sessionId);

            UserWorkspace workspace = userWorkspaceService.get(sessionId);

            setCommonViewComponents(sessionId, model);
            model.addAttribute("sessionEstablished", true);
            model.addAttribute("pageStyles", new String[] { "patient/home.css" });
            model.addAttribute("patientModels", workspace.getAllPatientModels());

            if (StringUtils.isNotBlank(systemStatusMessage)) {
                model.addAttribute("systemStatusMessage", systemStatusMessage);
            }

            auditService.doAudit(sessionId, AuditSeverity.INFO, "visited home page");

            return "patient/home";

        } else {
            logger.debug("session does not exist.  redirecting to launch page");
            return "redirect:/patient/launch";
        }
    }
}
