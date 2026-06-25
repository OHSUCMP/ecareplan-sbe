package edu.ohsu.cmp.ecareplan.controller.patient;

import edu.ohsu.cmp.ecareplan.model.AuditSeverity;
import edu.ohsu.cmp.ecareplan.model.dataset.VitalsModel;
import edu.ohsu.cmp.ecareplan.workspace.UserWorkspace;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Comparator;

@Controller
@RequestMapping("/patient/vitals")
public class VitalsController extends BasePatientController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @GetMapping(value = {"", "/"})
    public String view(HttpSession session, Model model) throws Exception {
        String sessionId = session.getId();
        if (sessionService.exists(sessionId)) {
            UserWorkspace workspace = userWorkspaceService.get(sessionId);

            setCommonViewComponents(sessionId, model);

            model.addAttribute("vitalsModels", consolidate(workspace.getAllVitalsModels(), Comparator.comparing(VitalsModel::getEffectiveDate).reversed()));

            auditService.doAudit(sessionId, AuditSeverity.INFO, "visited /patient/vitals");

            return "patient/vitals";

        } else {
            logger.debug("session does not exist for {}.  redirecting to launch page", sessionId);
            return "redirect:/patient/launch";
        }
    }
}
