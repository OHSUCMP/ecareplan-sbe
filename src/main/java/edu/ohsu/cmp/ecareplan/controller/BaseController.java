package edu.ohsu.cmp.ecareplan.controller;

import edu.ohsu.cmp.ecareplan.model.Audience;
import edu.ohsu.cmp.ecareplan.service.AuditService;
import edu.ohsu.cmp.ecareplan.workspace.UserWorkspaceService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.ui.Model;

public abstract class BaseController {

    @Value("${security.idle-timeout-seconds}")
    private Integer idleTimeoutSeconds;

    @Autowired
    protected UserWorkspaceService userWorkspaceService;

    @Autowired
    protected AuditService auditService;

    @Autowired
    protected Environment env;

    @Value("${system.status-message}")
    private String systemStatusMessage;

    protected void setCommonViewComponents(Model model) {
        setCommonViewComponents(null, model);
    }

    protected void setCommonViewComponents(String sessionId, Model model) {
        model.addAttribute("idleTimeoutSeconds", idleTimeoutSeconds);

        if (sessionId != null) {
            if (userWorkspaceService.exists(sessionId)) {
                model.addAttribute("sessionEstablished", true);

                Audience audience = userWorkspaceService.get(sessionId).getAudience();
                if (audience == Audience.PATIENT) {
                    model.addAttribute("patientContext", true);
                } else if (audience == Audience.CARE_TEAM) {
                    model.addAttribute("careTeamContext", true);
                }

                if (StringUtils.isNotBlank(systemStatusMessage)) {
                    model.addAttribute("systemStatusMessage", systemStatusMessage);
                }
            }
        }
    }
}
