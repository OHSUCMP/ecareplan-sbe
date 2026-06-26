package edu.ohsu.cmp.ecareplan.controller.patient;

import edu.ohsu.cmp.ecareplan.controller.BaseController;
import edu.ohsu.cmp.ecareplan.model.dataset.Consolidatable;
import edu.ohsu.cmp.ecareplan.model.dataset.Consolidated;
import edu.ohsu.cmp.ecareplan.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;

import java.util.*;

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

    protected <T extends Consolidatable<S>, S extends Comparable<S>> List<Consolidated<T>> consolidate(List<T> sourceList) {
        if (sourceList == null) return null;

        Map<String, List<T>> map = new LinkedHashMap<>();
        for (T t : sourceList) {
            if ( ! map.containsKey(t.getConsolidationKey()) ) {
                map.put(t.getConsolidationKey(), new ArrayList<>());
            }
            map.get(t.getConsolidationKey()).add(t);
        }

        List<Consolidated<T>> list = new ArrayList<>();
//        Comparator comparator = Comparator.comparing(Consolidatable::getConsolidationSortBy).reversed();

        for (List<T> values : map.values()) {
            if (values.size() > 1) {
                List<T> sorted = values.stream().sorted(new Comparator<T>() {
                    @Override
                    public int compare(T o1, T o2) {
                        if (o1 == null && o2 == null) return 0;
                        if (o1 == null) return 1;
                        if (o2 == null) return -1;

                        if (o1.getConsolidationSortBy() == null && o2.getConsolidationSortBy() == null) return 0;
                        if (o1.getConsolidationSortBy() == null && o2.getConsolidationSortBy() != null) return 1;
                        if (o1.getConsolidationSortBy() != null && o2.getConsolidationSortBy() == null) return -1;

                        return o1.getConsolidationSortBy().compareTo(o2.getConsolidationSortBy());
                    }
                }).toList().reversed();
                list.add(new Consolidated<>(sorted.getFirst(), sorted.subList(1, sorted.size())));

            } else {
                list.add(new Consolidated<>(values.getFirst(), null));
            }
        }

        return list;
    }
}
