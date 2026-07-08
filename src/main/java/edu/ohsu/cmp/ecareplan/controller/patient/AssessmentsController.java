package edu.ohsu.cmp.ecareplan.controller.patient;

import edu.ohsu.cmp.ecareplan.model.AuditSeverity;
import edu.ohsu.cmp.ecareplan.model.dataset.DataSet;
import edu.ohsu.cmp.ecareplan.model.dataset.QuestionnaireResponseModel;
import edu.ohsu.cmp.ecareplan.model.dataset.SurveyObservationModel;
import edu.ohsu.cmp.ecareplan.model.view.AssessmentModel;
import edu.ohsu.cmp.ecareplan.model.view.QuestionnaireResponseAssessmentModel;
import edu.ohsu.cmp.ecareplan.model.view.SurveyObservationsAssessmentModel;
import edu.ohsu.cmp.ecareplan.workspace.UserWorkspace;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;

@Controller
@RequestMapping("/patient/assessments")
public class AssessmentsController extends BasePatientController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @GetMapping(value = {"", "/"})
    public String view(HttpSession session, Model model) throws Exception {
        String sessionId = session.getId();
        if (sessionService.exists(sessionId)) {
            UserWorkspace workspace = userWorkspaceService.get(sessionId);

            setCommonViewComponents(sessionId, model);

            model.addAttribute("assessmentModels", buildAssessmentModels(workspace));

            auditService.doAudit(sessionId, AuditSeverity.INFO, "visited /patient/assessments");

            return "patient/assessments";

        } else {
            logger.debug("session does not exist for {}.  redirecting to launch page", sessionId);
            return "redirect:/patient/launch";
        }
    }

    private List<AssessmentModel> buildAssessmentModels(UserWorkspace workspace) {
        List<AssessmentModel> list = new ArrayList<>();

        for (QuestionnaireResponseModel qrm : workspace.getAllDataSetModels(DataSet.QUESTIONNAIRE_RESPONSES)) {
            list.add(new QuestionnaireResponseAssessmentModel(qrm));
        }

        Map<String, List<SurveyObservationModel>> map = new HashMap<>();
        for (SurveyObservationModel som : workspace.getAllDataSetModels(DataSet.SURVEY_OBSERVATIONS)) {
            String key = null; // todo : set this to the logical ID of the assessment
            if ( ! map.containsKey(key) ) {
                map.put(key, new ArrayList<>());
            }
            map.get(key).add(som);
        }
        Iterator<List<SurveyObservationModel>> iter = map.values().iterator();
        while (iter.hasNext()) {
            list.add(new SurveyObservationsAssessmentModel(iter.next()));
            iter.remove();
        }

        return list;
    }
}
