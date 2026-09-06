package edu.ohsu.cmp.ecareplan.model.dataset;

import java.util.List;

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
}
