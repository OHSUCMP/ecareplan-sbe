package edu.ohsu.cmp.ecareplan.controller.patient;

import edu.ohsu.cmp.ecareplan.entity.Endpoint;
import edu.ohsu.cmp.ecareplan.entity.UserEndpoint;
import edu.ohsu.cmp.ecareplan.exception.ConfigurationException;
import edu.ohsu.cmp.ecareplan.model.Audience;
import edu.ohsu.cmp.ecareplan.model.AuditSeverity;
import edu.ohsu.cmp.ecareplan.model.dataset.DataSet;
import edu.ohsu.cmp.ecareplan.model.dataset.PatientModel;
import edu.ohsu.cmp.ecareplan.model.fhir.FHIRCredentials;
import edu.ohsu.cmp.ecareplan.model.progress.IProgress;
import edu.ohsu.cmp.ecareplan.service.EndpointService;
import edu.ohsu.cmp.ecareplan.workspace.UserWorkspace;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Controller
@RequestMapping("/patient")
public class PatientHomeController extends BasePatientController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // this is the home page for the MyCarePlanner patient-focused app

    @Autowired
    private EndpointService endpointService;

    @Value("#{new Boolean('${security.browser.cache-credentials}')}")
    private Boolean cacheCredentials;

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

    @GetMapping("smart-callback")
    public String smartCallback(HttpSession session, Model model) {
        setCommonViewComponents(model);
        model.addAttribute("cacheCredentials", cacheCredentials);
        model.addAttribute("redirectUri", "/patient");
        return "smart-callback";
    }

    @PostMapping("complete-handshake")
    public ResponseEntity<?> completeHandshake(HttpSession session,
                                               @RequestParam String clientId,
                                               @RequestParam String serverUrl,
                                               @RequestParam String bearerToken,
                                               @RequestParam String patientId,
                                               @RequestParam String userId) throws ConfigurationException {

        if (userWorkspaceService.exists(session.getId())) {
            UserWorkspace workspace = userWorkspaceService.get(session.getId());
            Long endpointId = workspace.getCurrentlyLaunchingEndpointId();
            if (endpointId != null) {
                UserEndpoint userEndpoint = workspace.getOrCreateUserEndpoint(endpointId, patientId);
                Endpoint endpoint = userEndpoint.getEndpoint();
                if (endpoint.getClientId().equals(clientId) && endpoint.getIss().equals(serverUrl)) {
                    FHIRCredentials credentials = new FHIRCredentials(clientId, serverUrl, bearerToken, patientId, userId);
                    workspace.configureUserEndpointCredentials(userEndpoint, credentials);
                    workspace.populateEndpoint(userEndpoint.getEndpoint());
                    auditService.doAudit(session.getId(), AuditSeverity.INFO, "endpoint connected", "endpoint=" + endpoint.getName() + " (" + endpoint.getIss() + ")");
                    return ResponseEntity.ok("handshake completed");
                }
            }
        }

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
            UserWorkspace workspace = userWorkspaceService.get(sessionId);

            setCommonViewComponents(sessionId, model);

            model.addAttribute("pageScripts", new String[] { "dataset.js" });
            model.addAttribute("pageStyles", new String[] { "dataset.css" });

            model.addAttribute("dataSets", DataSet.PATIENT);
//            model.addAttribute("patientModels", workspace.getAllDataSetModels(DataSet.PATIENT));

            auditService.doAudit(sessionId, AuditSeverity.INFO, "visited /patient/home");

            return "patient/home";

        } else {
            logger.debug("session does not exist for {}.  redirecting to launch page", sessionId);
            return "redirect:/patient/launch";
        }
    }

    @PostMapping("progress")
    public ResponseEntity<List<IProgress>> getCurrentProgress(HttpSession session) {
        if (userWorkspaceService.exists(session.getId())) {
            List<IProgress> list = userWorkspaceService.get(session.getId()).getCurrentProgress();
            return new ResponseEntity<>(list, HttpStatus.OK);

        } else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("models")
    public ResponseEntity<List<PatientModel>> getPatientModels(HttpSession session) {
        String sessionId = session.getId();
        if (userWorkspaceService.exists(sessionId)) {
            UserWorkspace workspace = userWorkspaceService.get(sessionId);
            return ResponseEntity.ok(workspace.getAllDataSetModels(DataSet.PATIENT));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getEmitter(HttpSession session) {
        return userWorkspaceService.exists(session.getId()) ?
                userWorkspaceService.get(session.getId()).createNewEmitter() :
                null;
    }
}
