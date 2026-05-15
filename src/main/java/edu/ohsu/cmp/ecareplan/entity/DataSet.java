package edu.ohsu.cmp.ecareplan.entity;

import edu.ohsu.cmp.ecareplan.model.DataSetName;
import jakarta.persistence.*;

@Entity
@Table(name = "data_set")
public class DataSet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private DataSetName name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DataSetName getName() {
        return name;
    }

    public void setName(DataSetName name) {
        this.name = name;
    }
}
