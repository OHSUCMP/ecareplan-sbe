package edu.ohsu.cmp.ecareplan.controller;

import edu.ohsu.cmp.ecareplan.model.AuditSeverity;
import edu.ohsu.cmp.ecareplan.model.ProgressStatus;
import edu.ohsu.cmp.ecareplan.model.progress.IProgress;
import edu.ohsu.cmp.ecareplan.service.SessionService;
import edu.ohsu.cmp.ecareplan.workspace.UserWorkspace;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
public class SessionController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SessionService sessionService;


    @PostMapping("validate-session")
    public ResponseEntity<?> validateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            logger.debug("validating session - no HTTP session exists");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(false);
        }

        boolean sessionExists = userWorkspaceService.exists(session.getId());
        logger.debug("validating session {} - exists? --> {}", session.getId(), sessionExists);

        return sessionExists ?
                ResponseEntity.ok(true) :
                ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(false);
    }

    @GetMapping("unauthorized")
    public String unauthorized(HttpSession session) {
        return "unauthorized";
    }

    @GetMapping("logout")
    public String logout(HttpSession session) {
        if (userWorkspaceService.exists(session.getId())) {
            auditService.doAudit(session.getId(), AuditSeverity.INFO, "logged out"); // must occur before expire action
            sessionService.forceExpiration(session.getId());
        }

        session.invalidate();

        return "logout";
    }

    @GetMapping("inactivity-logout")
    public String inactivityLogout(HttpSession session) {
        if (userWorkspaceService.exists(session.getId())) {
            auditService.doAudit(session.getId(), AuditSeverity.INFO, "logged out due to inactivity"); // must occur before expire action
            sessionService.forceExpiration(session.getId());
        }

        session.invalidate();

        return "inactivity-logout";
    }

    @PostMapping("clear-session")
    public ResponseEntity<?> clearSession(HttpSession session) {
        if (userWorkspaceService.exists(session.getId())) {
            sessionService.forceExpiration(session.getId());
            return ResponseEntity.ok("session cleared");
        }
        return ResponseEntity.ok("no session");
    }

    @PostMapping("refresh")
    public ResponseEntity<?> refresh(HttpSession session) {
        if (userWorkspaceService.exists(session.getId())) {
            UserWorkspace workspace = userWorkspaceService.get(session.getId());

            List<IProgress> progress = workspace.getCurrentProgress();
            boolean isRunning = progress.stream().anyMatch(p -> p.getStatus() != ProgressStatus.COMPLETED);
            if (isRunning) {
                return ResponseEntity.status(HttpStatus.TOO_EARLY).body("cannot trigger refresh as progress is running");
            }

            logger.info("refreshing data for session=" + session.getId());
            workspace.populate();
            return ResponseEntity.ok("refreshing");
        }
        return ResponseEntity.ok("no session");
    }
}
