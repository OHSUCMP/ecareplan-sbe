package edu.ohsu.cmp.ecareplan.controller.patient;

import edu.ohsu.cmp.ecareplan.entity.Endpoint;
import edu.ohsu.cmp.ecareplan.model.AuditSeverity;
import edu.ohsu.cmp.ecareplan.model.EndpointModel;
import edu.ohsu.cmp.ecareplan.service.EndpointService;
import edu.ohsu.cmp.ecareplan.workspace.UserWorkspace;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/patient/health-records")
public class HealthRecordsController extends BasePatientController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private EndpointService endpointService;

    @GetMapping(value = {"", "/"})
    public String view(HttpSession session, Model model) throws Exception {
        String sessionId = session.getId();
        if (sessionService.exists(sessionId)) {
            setCommonViewComponents(sessionId, model);

            model.addAttribute("pageScripts", new String[] { "fhir-client-v2.6.3.min.js", "endpoint.js", "health-records.js" });
            model.addAttribute("pageStyles", new String[] { "endpoint.css", "patient/health-records.css" });

            UserWorkspace workspace = userWorkspaceService.get(sessionId);

            List<EndpointModel> activeEndpoints = workspace.getAllActiveEndpointModels();
            model.addAttribute("activeEndpointModels", activeEndpoints);

            Map<Long, EndpointModel> activeEndpointsMap = new LinkedHashMap<>();
            for (EndpointModel endpointModel : activeEndpoints) {
                activeEndpointsMap.put(endpointModel.getId(), endpointModel);
            }

            List<EndpointModel> notYetConnectedThirdPartyEndpoints = new ArrayList<>();
            for (EndpointModel endpointModel : endpointService.getAllThirdPartyEndpoints()) {
                if ( ! activeEndpointsMap.containsKey(endpointModel.getId()) ) {
                    notYetConnectedThirdPartyEndpoints.add(endpointModel);
                }
            }
            model.addAttribute("thirdPartyEndpointModels", notYetConnectedThirdPartyEndpoints);

            auditService.doAudit(sessionId, AuditSeverity.INFO, "visited /patient/health-records");

            return "patient/health-records";

        } else {
            logger.debug("session does not exist for {}.  redirecting to launch page", sessionId);
            return "redirect:/patient/launch";
        }
    }

    @PostMapping("report-launch")
    public ResponseEntity<String> reportLaunch(HttpSession session,
                                               @RequestParam Long endpointId) {

        if (sessionService.exists(session.getId())) {
            logger.info("reporting launch for endpointId={}", endpointId);

            try {
                UserWorkspace workspace = userWorkspaceService.get(session.getId());
                Endpoint endpoint = endpointService.getEndpoint(endpointId);
                workspace.setCurrentlyLaunchingEndpoint(endpoint);

                auditService.doAudit(session.getId(), AuditSeverity.INFO, "launch third-party endpoint",
                        "id=" + endpointId + ", name=" + endpoint.getName() + ", iss=" + endpoint.getIss());

                return ResponseEntity.ok("ok");

            } catch (Exception e) {
                logger.error("caught {} attempting to report launch for endpointId={}", e.getClass().getSimpleName(), endpointId, e);

                auditService.doAudit(session.getId(), AuditSeverity.ERROR, "launch third-party endpoint",
                        "endpointId=" + endpointId + ", error=" + e.getClass().getSimpleName() + ": " + e.getMessage());

                return ResponseEntity.badRequest().body("error - see server logs for details");
            }

        } else {
            return ResponseEntity.badRequest().body("no session");
        }
    }
}
