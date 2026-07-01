package edu.ohsu.cmp.ecareplan.controller.patient;

import edu.ohsu.cmp.ecareplan.model.AuditSeverity;
import edu.ohsu.cmp.ecareplan.model.EndpointModel;
import edu.ohsu.cmp.ecareplan.service.EndpointService;
import edu.ohsu.cmp.ecareplan.workspace.UserWorkspace;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/patient/select-endpoint")
public class EndpointController extends BasePatientController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private EndpointService endpointService;

    @GetMapping(value = {"", "/"})
    public String view(HttpSession session, Model model) throws Exception {
        String sessionId = session.getId();
        if (sessionService.exists(sessionId)) {
            setCommonViewComponents(sessionId, model);

            model.addAttribute("pageScripts", new String[] { "fhir-client-v2.6.3.min.js" });

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

            auditService.doAudit(sessionId, AuditSeverity.INFO, "visited /patient/select-endpoint");

            return "patient/select-endpoint";

        } else {
            logger.debug("session does not exist for {}.  redirecting to launch page", sessionId);
            return "redirect:/patient/launch";
        }
    }
}
