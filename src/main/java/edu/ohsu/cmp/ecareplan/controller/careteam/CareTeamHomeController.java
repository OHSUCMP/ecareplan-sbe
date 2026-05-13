package edu.ohsu.cmp.ecareplan.controller.careteam;

import edu.ohsu.cmp.ecareplan.controller.BaseController;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/careteam")
public class CareTeamHomeController extends BaseController {

    @GetMapping(value = {"", "/"})
    public String view(HttpSession session, Model model) throws Exception {
        throw new NotImplementedException("CareTeamHomeController view method not implemented");
    }

}