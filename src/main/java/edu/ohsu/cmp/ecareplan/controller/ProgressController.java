package edu.ohsu.cmp.ecareplan.controller;

import edu.ohsu.cmp.ecareplan.model.ProgressModel;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
public class ProgressController extends BaseController {
    @PostMapping("current-progress")
    public ResponseEntity<List<ProgressModel>> getBloodPressureObservations(HttpSession session) {
        if (userWorkspaceService.exists(session.getId())) {
            List<ProgressModel> list = userWorkspaceService.get(session.getId()).getCurrentProgress();
            return new ResponseEntity<>(list, HttpStatus.OK);

        } else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

}
