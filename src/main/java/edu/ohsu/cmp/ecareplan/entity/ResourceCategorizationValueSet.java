package edu.ohsu.cmp.ecareplan.entity;

import edu.ohsu.cmp.ecareplan.model.dataset.DataSetName;
import jakarta.persistence.*;
import org.apache.commons.lang3.StringUtils;

@Entity
@Table(name = "resource_categorization_valueset")
public class ResourceCategorizationValueSet implements ResourceCategorization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private DataSetName dataSetName;

    private String valuesetName;
    private String valuesetOid;
    private String category;
    private String commonName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DataSetName getDataSetName() {
        return dataSetName;
    }

    public void setDataSetName(DataSetName dataSetName) {
        this.dataSetName = dataSetName;
    }

    public String getValuesetName() {
        return valuesetName;
    }

    public void setValuesetName(String valuesetName) {
        this.valuesetName = valuesetName;
    }

    public String getValuesetOid() {
        return valuesetOid;
    }

    public void setValuesetOid(String valuesetOid) {
        this.valuesetOid = valuesetOid;
    }

    public boolean hasCategory() {
        return StringUtils.isNotBlank(category);
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }
}
