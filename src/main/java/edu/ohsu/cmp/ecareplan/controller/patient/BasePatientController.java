package edu.ohsu.cmp.ecareplan.controller.patient;

import edu.ohsu.cmp.ecareplan.controller.BaseController;
import org.springframework.ui.Model;

public class BasePatientController extends BaseController {
    private static final String APPLICATION_NAME = "MyCarePlanner (SBE)";

    protected void setCommonViewComponents(Model model) {
        setCommonViewComponents(null, model);
    }

    protected void setCommonViewComponents(String sessionId, Model model) {
        model.addAttribute("applicationName", APPLICATION_NAME);
        super.setCommonViewComponents(sessionId, model);
    }
}
