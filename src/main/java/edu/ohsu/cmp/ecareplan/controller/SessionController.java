package edu.ohsu.cmp.ecareplan.controller;

import edu.ohsu.cmp.ecareplan.model.AuditSeverity;
import edu.ohsu.cmp.ecareplan.service.SessionService;
import edu.ohsu.cmp.ecareplan.workspace.UserWorkspace;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class SessionController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SessionService sessionService;


    @PostMapping("validate-session")
    public ResponseEntity<?> validateSession(HttpSession session) {
        logger.debug("validating session {} - exists? --> {}, ", session.getId(), userWorkspaceService.exists(session.getId()));
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
