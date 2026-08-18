package edu.ohsu.cmp.ecareplan.controller.patient;

import edu.ohsu.cmp.ecareplan.model.AuditSeverity;
import edu.ohsu.cmp.ecareplan.model.dataset.DataSet;
import edu.ohsu.cmp.ecareplan.model.progress.IProgress;
import edu.ohsu.cmp.ecareplan.model.view.AssessmentModel;
import edu.ohsu.cmp.ecareplan.service.AssessmentService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
            model.addAttribute("pageScripts", new String[] { "dataset.js", "chart.js" });
            model.addAttribute("pageStyles", new String[] { "dataset.css", "chart.css" });

            model.addAttribute("dataSets", DataSet.QUESTIONNAIRE_RESPONSES + "," + DataSet.SURVEY_OBSERVATIONS);

            auditService.doAudit(sessionId, AuditSeverity.INFO, "visited /patient/assessments");

            return "patient/assessments";

        } else {
            logger.debug("session does not exist for {}.  redirecting to launch page", sessionId);
            return "redirect:/patient/launch";
        }
    }

    @PostMapping("progress")
    public ResponseEntity<List<IProgress>> getProgress(HttpSession session) {
        if (userWorkspaceService.exists(session.getId())) {
            List<IProgress> list = new ArrayList<>();
            list.addAll(userWorkspaceService.get(session.getId()).getCurrentProgress(DataSet.QUESTIONNAIRE_RESPONSES));
            list.addAll(userWorkspaceService.get(session.getId()).getCurrentProgress(DataSet.SURVEY_OBSERVATIONS));
            return ResponseEntity.ok(list);

        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("models")
    public ResponseEntity<List<AssessmentModel>> getModels(HttpSession session) {
        if (userWorkspaceService.exists(session.getId())) {
            List<AssessmentModel> list = assessmentService.getAssessmentModels(session.getId());
            return ResponseEntity.ok(list);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
//        return userWorkspaceService.exists(session.getId()) ?
//                ResponseEntity.ok(assessmentService.getAssessmentModels(session.getId())) :
//                ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getEmitter(HttpSession session) {
        return userWorkspaceService.exists(session.getId()) ?
                userWorkspaceService.get(session.getId()).createNewEmitter() :
                null;
    }
}
