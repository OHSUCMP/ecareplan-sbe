package edu.ohsu.cmp.ecareplan.controller.patient;

import edu.ohsu.cmp.ecareplan.controller.BaseController;
import edu.ohsu.cmp.ecareplan.model.ProgressStatus;
import edu.ohsu.cmp.ecareplan.model.dataset.Consolidatable;
import edu.ohsu.cmp.ecareplan.model.dataset.Consolidated;
import edu.ohsu.cmp.ecareplan.service.SessionService;
import edu.ohsu.cmp.ecareplan.workspace.UserWorkspace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;

import java.util.*;

public class BasePatientController extends BaseController {
    private static final String APPLICATION_NAME = "MyCarePlanner";

    @Autowired
    protected SessionService sessionService;

    protected void setCommonViewComponents(Model model) {
        setCommonViewComponents(null, model);
    }

    protected void setCommonViewComponents(String sessionId, Model model) {
        model.addAttribute("applicationName", APPLICATION_NAME);

        if (sessionId != null && userWorkspaceService.exists(sessionId)) {
            UserWorkspace workspace = userWorkspaceService.get(sessionId);
            boolean loading = workspace.getCurrentProgress().stream().anyMatch(p ->
                    p.getStatus().equals(ProgressStatus.WAITING_TO_START) || p.getStatus().equals(ProgressStatus.RUNNING));
            model.addAttribute("loading", loading);
        }

        super.setCommonViewComponents(sessionId, model);
    }

    protected <T extends Consolidatable> List<Consolidated<T>> consolidate(List<T> sourceList) {
        if (sourceList == null) return null;

        Map<String, List<T>> map = new LinkedHashMap<>();
        for (T t : sourceList) {
            if ( ! map.containsKey(t.getConsolidationGroupBy()) ) {
                map.put(t.getConsolidationGroupBy(), new ArrayList<>());
            }
            map.get(t.getConsolidationGroupBy()).add(t);
        }

        List<Consolidated<T>> list = new ArrayList<>();

        for (List<T> values : map.values()) {
            if (values.size() > 1) {
                List<T> sorted = values.stream().sorted((o1, o2) -> {
                    if (o1 == null && o2 == null) return 0;
                    if (o1 == null) return 1;
                    if (o2 == null) return -1;

                    if (o1.getConsolidationSortBy() == null && o2.getConsolidationSortBy() == null) return 0;
                    if (o1.getConsolidationSortBy() == null && o2.getConsolidationSortBy() != null) return 1;
                    if (o1.getConsolidationSortBy() != null && o2.getConsolidationSortBy() == null) return -1;

                    return o1.getConsolidationSortBy().compareTo(o2.getConsolidationSortBy());
                }).toList().reversed();
                list.add(new Consolidated<>(sorted));

            } else {
                list.add(new Consolidated<>(values));
            }
        }

        return list;
    }
}
