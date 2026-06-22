package edu.ohsu.cmp.ecareplan.entity;

import edu.ohsu.cmp.ecareplan.model.dataset.DataSetName;
import jakarta.persistence.*;

@Entity
@Table(name="resource_categorization_coding")
public class ResourceCategorizationCoding implements ResourceCategorization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private DataSetName dataSetName;

    private String codeSystemUrl;
    private String code;
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

    public String getCodeSystemUrl() {
        return codeSystemUrl;
    }

    public void setCodeSystemUrl(String codeSystemUrl) {
        this.codeSystemUrl = codeSystemUrl;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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
