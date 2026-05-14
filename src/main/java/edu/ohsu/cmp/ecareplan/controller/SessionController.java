package edu.ohsu.cmp.ecareplan.controller;

import edu.ohsu.cmp.ecareplan.exception.CaseNotHandledException;
import edu.ohsu.cmp.ecareplan.exception.ConfigurationException;
import edu.ohsu.cmp.ecareplan.model.Audience;
import edu.ohsu.cmp.ecareplan.model.AuditSeverity;
import edu.ohsu.cmp.ecareplan.model.fhir.FHIRCredentials;
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
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

@Controller
public class SessionController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SessionService sessionService;

    @Value("${smart.patient.scope}")
    private String patientScope;

    @Value("${smart.patient.iss}")
    private String patientIss;

    @Value("${smart.patient.clientId}")
    private String patientClientId;

    @Value("${smart.careTeam.scope}")
    private String careTeamScope;

    @Value("${smart.careTeam.iss}")
    private String careTeamIss;

    @Value("${smart.careTeam.clientId}")
    private String careTeamClientId;

    @Value("${smart.baseRedirectUri}")
    private String baseRedirectURI;

    @GetMapping("launch-careteam")
    public String launchCareTeam(HttpSession session, Model model) {
        sessionService.forceExpiration(session.getId());
        setCommonViewComponents(model);
        model.addAttribute("clientId", careTeamClientId);
        model.addAttribute("scope", careTeamScope);
        model.addAttribute("redirectUri", baseRedirectURI + "/careteam");
        model.addAttribute("iss", careTeamIss);
        return "launch-careteam";
    }

    @GetMapping("launch-patient")
    public String launchPatient(HttpSession session, Model model) {
        sessionService.forceExpiration(session.getId());
        setCommonViewComponents(model);
        model.addAttribute("clientId", patientClientId);
        model.addAttribute("scope", patientScope);
        model.addAttribute("redirectUri", baseRedirectURI + "/patient");
        model.addAttribute("iss", patientIss);
        return "launch-patient";
    }

    @PostMapping("prepare-session")
    public ResponseEntity<?> prepareSession(HttpSession session,
                                            @RequestParam String clientId,
                                            @RequestParam String serverUrl,
                                            @RequestParam String bearerToken,
                                            @RequestParam String patientId,
                                            @RequestParam String userId) throws ConfigurationException, IOException {

        Audience audience;
        if (StringUtils.equals(clientId, patientClientId)) {
            audience = Audience.PATIENT;
        } else if (StringUtils.equals(clientId, careTeamClientId)) {
            audience = Audience.CARE_TEAM;
        } else {
            throw new CaseNotHandledException("couldn't determine audience from clientId=" + clientId);
        }

        logger.debug("preparing " + audience + " session " + session.getId());

        FHIRCredentials credentials = new FHIRCredentials(clientId, serverUrl, bearerToken, patientId, userId);

        if (Audience.PATIENT.equals(audience)) {
            sessionService.prepareSession(session.getId(), credentials, audience);
            return ResponseEntity.ok("patient session established");

        } else if (Audience.CARE_TEAM.equals(audience)) {
            sessionService.prepareSession(session.getId(), credentials, audience);
            return ResponseEntity.ok("care team session established");

        } else {
            throw new CaseNotHandledException("no case exists for handling audience=" + audience);
        }
    }

    @PostMapping("validate-session")
    public ResponseEntity<?> validateSession(HttpSession session) {
        logger.debug("validating session " + session.getId() + " - exists? --> " + userWorkspaceService.exists(session.getId()));
        return userWorkspaceService.exists(session.getId()) ?
                ResponseEntity.ok(true) :
                ResponseEntity.ok(false);
    }

    @GetMapping("unauthorized")
    public String unauthorized(HttpSession session) {
        return "unauthorized";
    }

    @GetMapping("logout")
    public String logout(HttpSession session) {
        auditService.doAudit(session.getId(), AuditSeverity.INFO, "logged out"); // must occur before expire action
        sessionService.forceExpiration(session.getId());
        return "logout";
    }

    @GetMapping("inactivity-logout")
    public String inactivityLogout(HttpSession session) {
        auditService.doAudit(session.getId(), AuditSeverity.INFO, "logged out due to inactivity"); // must occur before expire action
        sessionService.forceExpiration(session.getId());
        return "inactivity-logout";
    }

    @PostMapping("clear-session")
    public ResponseEntity<?> clearSession(HttpSession session) {
        sessionService.forceExpiration(session.getId());
        return ResponseEntity.ok("session cleared");
    }

    @PostMapping("refresh")
    public ResponseEntity<?> refresh(HttpSession session) {
        logger.info("refreshing data for session=" + session.getId());
        UserWorkspace workspace = userWorkspaceService.get(session.getId());
        workspace.clearCaches();
        workspace.populate();
        return ResponseEntity.ok("refreshing");
    }
}
