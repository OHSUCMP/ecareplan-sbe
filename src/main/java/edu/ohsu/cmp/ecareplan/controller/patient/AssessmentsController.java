package edu.ohsu.cmp.ecareplan.controller.patient;

import edu.ohsu.cmp.ecareplan.model.AuditSeverity;
import edu.ohsu.cmp.ecareplan.model.dataset.DataSet;
import edu.ohsu.cmp.ecareplan.model.progress.IProgress;
import edu.ohsu.cmp.ecareplan.service.AssessmentService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/patient/assessments")
public class AssessmentsController extends BasePatientController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private AssessmentService assessmentService;

    @GetMapping(value = {"", "/"})
    public String view(HttpSession session, Model model) throws Exception {
        String sessionId = session.getId();
        if (sessionService.exists(sessionId)) {
            setCommonViewComponents(sessionId, model);

            model.addAttribute("pageWebjars", new String[] { "chart.js/dist/chart.umd.min.js" });
            model.addAttribute("pageScripts", new String[] { "progress.js", "chart.js" });
            model.addAttribute("pageStyles", new String[] { "progress.css", "chart.css" });

            model.addAttribute("assessmentModels", assessmentService.getAssessmentModels(sessionId));

            auditService.doAudit(sessionId, AuditSeverity.INFO, "visited /patient/assessments");

            return "patient/assessments";

        } else {
            logger.debug("session does not exist for {}.  redirecting to launch page", sessionId);
            return "redirect:/patient/launch";
        }
    }

    @PostMapping("progress")
    public ResponseEntity<List<IProgress>> getCurrentProgress(HttpSession session) {
        if (userWorkspaceService.exists(session.getId())) {
            List<IProgress> list = new ArrayList<>();
            list.addAll(userWorkspaceService.get(session.getId()).getCurrentProgress(DataSet.QUESTIONNAIRE_RESPONSES));
            list.addAll(userWorkspaceService.get(session.getId()).getCurrentProgress(DataSet.SURVEY_OBSERVATIONS));
            return new ResponseEntity<>(list, HttpStatus.OK);

        } else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }
}
