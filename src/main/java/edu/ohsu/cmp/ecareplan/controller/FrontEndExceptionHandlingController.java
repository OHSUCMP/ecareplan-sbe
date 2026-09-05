package edu.ohsu.cmp.ecareplan.controller;

import edu.ohsu.cmp.ecareplan.model.AuditSeverity;
import edu.ohsu.cmp.ecareplan.model.FrontEndException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/front-end-exception")
public class FrontEndExceptionHandlingController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(FrontEndExceptionHandlingController.class);

    @PostMapping(value = "audit", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> handleFrontEndException(HttpServletRequest request,
                                                          @RequestBody FrontEndException fee) {

        HttpSession session = request.getSession(false);
        if (session != null && userWorkspaceService.exists(session.getId())) {
            String userAgent = request.getHeader("User-Agent");

            logger.error("received {} rendering {} for session {} : agent={}, message={} at {}",
                    fee.getType(), fee.getPageUrl(), session.getId(), userAgent, fee.getMessage(), fee.getStackTrace());

            auditService.doAudit(session.getId(), AuditSeverity.UI_ERROR, "rendering " + fee.getPageUrl(),
                    "type=" + fee.getType() +
                    ", agent=" + userAgent +
                    ", message=" + fee.getMessage() +
                    ", stackTrace=" + fee.getStackTrace());

            return ResponseEntity.ok().build();
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
