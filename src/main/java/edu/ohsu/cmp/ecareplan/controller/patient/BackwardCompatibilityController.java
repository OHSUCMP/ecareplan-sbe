package edu.ohsu.cmp.ecareplan.controller.patient;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/mycareplanner")
public class BackwardCompatibilityController {

    @GetMapping("launch.html")
    public String launch() {
        return "redirect:/patient/launch";
    }

    @GetMapping("index.html")
    public String smartCallback() {
        return "redirect:/patient/smart-callback";
    }
}
