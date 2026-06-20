package edu.ohsu.cmp.ecareplan.model.dataset;

import java.util.List;

public class Consolidated<T extends BaseDataSetModel> {
    private final T mostRecentData;
    private final List<T> historicalData;

    public Consolidated(T mostRecentData, List<T> historicalData) {
        this.mostRecentData = mostRecentData;
        this.historicalData = historicalData;
    }

    public T getMostRecentData() {
        return mostRecentData;
    }

    public List<T> getHistoricalData() {
        return historicalData;
    }
}
