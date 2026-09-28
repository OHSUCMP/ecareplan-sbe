package edu.ohsu.cmp.ecareplan.controller.patient;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping("/mycareplanner")
public class BackwardCompatibilityController {

    @GetMapping("launch.html")
    public RedirectView launch() {
        return redirectWithOriginalQueryString("/patient/launch");
    }

    @GetMapping("index.html")
    public RedirectView smartCallback() {
        return redirectWithOriginalQueryString("/patient/smart-callback");
    }

    private RedirectView redirectWithOriginalQueryString(String targetUrl) {
        RedirectView redirectView = new RedirectView(targetUrl);
        redirectView.setContextRelative(true);
        redirectView.setPropagateQueryParams(true);
        return redirectView;
    }
}
