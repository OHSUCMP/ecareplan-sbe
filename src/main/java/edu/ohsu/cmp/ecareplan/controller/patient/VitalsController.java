package edu.ohsu.cmp.ecareplan.controller.patient;

import edu.ohsu.cmp.ecareplan.model.AuditSeverity;
import edu.ohsu.cmp.ecareplan.model.dataset.Consolidated;
import edu.ohsu.cmp.ecareplan.model.dataset.DataSet;
import edu.ohsu.cmp.ecareplan.model.progress.IProgress;
import edu.ohsu.cmp.ecareplan.model.view.IVitalsModel;
import edu.ohsu.cmp.ecareplan.service.view.VitalsService;
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

import java.util.List;

@Controller
@RequestMapping("/patient/vitals")
public class VitalsController extends BasePatientController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private VitalsService vitalsService;

    @GetMapping(value = {"", "/"})
    public String view(HttpSession session, Model model) throws Exception {
        String sessionId = session.getId();
        if (sessionService.exists(sessionId)) {
            setCommonViewComponents(sessionId, model);

            model.addAttribute("pageScripts", new String[] { "dataset.js" });
            model.addAttribute("pageStyles", new String[] { "dataset.css" });

            model.addAttribute("dataSets", DataSet.VITALS);

            auditService.doAudit(sessionId, AuditSeverity.INFO, "visited /patient/vitals");

            return "patient/vitals";

        } else {
            logger.debug("session does not exist for {}.  redirecting to launch page", sessionId);
            return "redirect:/patient/launch";
        }
    }

    @PostMapping("progress")
    public ResponseEntity<List<IProgress>> getProgress(HttpSession session) {
        if (userWorkspaceService.exists(session.getId())) {
            List<IProgress> list = userWorkspaceService.get(session.getId()).getCurrentProgress(DataSet.VITALS);
            return new ResponseEntity<>(list, HttpStatus.OK);

        } else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("models")
    public ResponseEntity<List<Consolidated<IVitalsModel>>> getModels(HttpSession session) {
        return userWorkspaceService.exists(session.getId()) ?
                ResponseEntity.ok(consolidate(vitalsService.getVitalsModels(session.getId()))) :
                ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @GetMapping(value = "sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getEmitter(HttpSession session) {
        return userWorkspaceService.exists(session.getId()) ?
                userWorkspaceService.get(session.getId()).createNewEmitter() :
                null;
    }
}
