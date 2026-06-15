package edu.ohsu.cmp.ecareplan.controller.patient;

import edu.ohsu.cmp.ecareplan.controller.BaseController;
import edu.ohsu.cmp.ecareplan.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;

public class BasePatientController extends BaseController {
    private static final String APPLICATION_NAME = "MyCarePlanner (SBE)";

    @Autowired
    protected SessionService sessionService;

    protected void setCommonViewComponents(Model model) {
        setCommonViewComponents(null, model);
    }

    protected void setCommonViewComponents(String sessionId, Model model) {
        model.addAttribute("applicationName", APPLICATION_NAME);
        model.addAttribute("pageStyles", new String[] { "patient/app.css" });
        super.setCommonViewComponents(sessionId, model);
    }
}
