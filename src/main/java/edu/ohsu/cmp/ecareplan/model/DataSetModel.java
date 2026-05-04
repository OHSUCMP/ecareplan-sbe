package edu.ohsu.cmp.ecareplan.model;

import edu.ohsu.cmp.ecareplan.entity.DataSet;

public class DataSetModel {
    private Long id;
    private String name;

    public DataSetModel(DataSet dataSet) {
        this.id = dataSet.getId();
        this.name = dataSet.getName();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
