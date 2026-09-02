package edu.ohsu.cmp.ecareplan.controller.patient;

import edu.ohsu.cmp.ecareplan.model.AuditSeverity;
import edu.ohsu.cmp.ecareplan.model.dataset.DataSet;
import edu.ohsu.cmp.ecareplan.model.dataset.MedicationModel;
import edu.ohsu.cmp.ecareplan.model.progress.IProgress;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Controller
@RequestMapping("/patient/medications")
public class MedicationsController extends BasePatientController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @GetMapping(value = {"", "/"})
    public String view(HttpSession session, Model model) throws Exception {
        String sessionId = session.getId();
        if (sessionService.exists(sessionId)) {
            setCommonViewComponents(sessionId, model);

            model.addAttribute("pageScripts", new String[] { "dataset.js" });
            model.addAttribute("pageStyles", new String[] { "dataset.css" });

            model.addAttribute("dataSets", DataSet.MEDICATIONS);

            auditService.doAudit(sessionId, AuditSeverity.INFO, "visited /patient/medications");

            return "patient/medications";

        } else {
            logger.debug("session does not exist for {}.  redirecting to launch page", sessionId);
            return "redirect:/patient/launch";
        }
    }

    @PostMapping("progress")
    public ResponseEntity<List<IProgress>> getProgress(HttpSession session) {
        return userWorkspaceService.exists(session.getId()) ?
                ResponseEntity.ok(userWorkspaceService.get(session.getId()).getCurrentProgress(DataSet.MEDICATIONS)) :
                ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @PostMapping("models")
    public ResponseEntity<List<MedicationModel>> getModels(HttpSession session) {
        return userWorkspaceService.exists(session.getId()) ?
                ResponseEntity.ok(userWorkspaceService.get(session.getId()).getAllDataSetModels(DataSet.MEDICATIONS)) :
                ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @GetMapping(value = "sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> getEmitter(HttpSession session) {
        return userWorkspaceService.exists(session.getId()) ?
                ResponseEntity.ok(userWorkspaceService.get(session.getId()).createNewEmitter()) :
                ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
