package edu.ohsu.cmp.ecareplan.controller.patient;

import edu.ohsu.cmp.ecareplan.model.AuditSeverity;
import edu.ohsu.cmp.ecareplan.model.dataset.GoalModel;
import edu.ohsu.cmp.ecareplan.workspace.UserWorkspace;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/patient/goals")
public class GoalsController extends BasePatientController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @GetMapping(value = {"", "/"})
    public String view(HttpSession session, Model model) throws Exception {
        String sessionId = session.getId();
        if (sessionService.exists(sessionId)) {
            UserWorkspace workspace = userWorkspaceService.get(sessionId);

            setCommonViewComponents(sessionId, model);

            List<GoalModel> goalModels = workspace.getAllGoalModels();
            model.addAttribute("personalHealthGoalModels", filterPersonalHealthGoals(goalModels));
            model.addAttribute("hospitalizationGoalModels", filterHospitalizationGoals(goalModels));

            auditService.doAudit(sessionId, AuditSeverity.INFO, "visited /patient/goals");

            return "patient/goals";

        } else {
            logger.debug("session does not exist for {}.  redirecting to launch page", sessionId);
            return "redirect:/patient/launch";
        }
    }

    private List<GoalModel> filterPersonalHealthGoals(List<GoalModel> goalModels) {
        return null;
    }

    private List<GoalModel> filterHospitalizationGoals(List<GoalModel> goalModels) {
        return null;
    }
}
