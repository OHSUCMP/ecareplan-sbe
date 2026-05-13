package edu.ohsu.cmp.ecareplan.controller.patient;

import edu.ohsu.cmp.ecareplan.controller.BaseController;
import edu.ohsu.cmp.ecareplan.model.AuditSeverity;
import edu.ohsu.cmp.ecareplan.service.SessionService;
import edu.ohsu.cmp.ecareplan.workspace.UserWorkspace;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/patient")
public class PatientHomeController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(PatientHomeController.class);

    // this is the home page for the MyCarePlanner patient-focused app

    @Autowired
    private SessionService sessionService;

    @Value("#{new Boolean('${security.browser.cache-credentials}')}")
    private Boolean cacheCredentials;

    @Value("${system.status-message}")
    private String systemStatusMessage;


    @GetMapping(value = {"", "/"})
    public String view(HttpSession session, Model model) throws Exception {
        String sessionId = session.getId();
        if (sessionService.exists(sessionId)) {
            logger.info("session exists.  requesting data for session " + sessionId);

            UserWorkspace workspace = userWorkspaceService.get(sessionId);

            setCommonViewComponents(sessionId, model);
            model.addAttribute("sessionEstablished", true);
            model.addAttribute("pageStyles", new String[] { "home.css" });
            model.addAttribute("patient", workspace.getPatient());

            if (StringUtils.isNotBlank(systemStatusMessage)) {
                model.addAttribute("systemStatusMessage", systemStatusMessage);
            }

            auditService.doAudit(sessionId, AuditSeverity.INFO, "visited home page");

            return "home";

        } else {
            logger.debug("no session exists.  completing SMART-on-FHIR handshake for session " + sessionId);
            setCommonViewComponents(model);
            model.addAttribute("cacheCredentials", cacheCredentials);
            return "fhir-complete-handshake";
        }
    }
}
