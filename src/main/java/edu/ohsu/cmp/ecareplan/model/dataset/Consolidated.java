package edu.ohsu.cmp.ecareplan.model.dataset;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Consolidated<T extends Consolidatable> {
    private final List<T> list;

    public Consolidated(List<T> list) {
        this.list = list;
    }

    public T getMostRecentData() {
        return list.getFirst();
    }

    public List<T> getHistoricalData() {
        return list.size() > 1 ?
                list.subList(1, list.size()) :
                null;
    }

    public Map<String, List<T>> getAllDataBySourceEndpointName() {
        Map<String, List<T>> map = new LinkedHashMap<>();

        for (T item : list) {
            if ( ! map.containsKey(item.getSourceEndpointName()) ) {
                map.put(item.getSourceEndpointName(), new ArrayList<>());
            }
            map.get(item.getSourceEndpointName()).add(item);
        }

        return map;
    }
}
