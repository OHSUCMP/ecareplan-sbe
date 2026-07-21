package edu.ohsu.cmp.ecareplan.entity;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "medication_flag")
public class MedicationFlag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String label;
    private String backgroundColor;
    private String textColor;

    @OneToMany(mappedBy = "medicationFlagId", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<MedicationFlagRxClass> rxClassList;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public String getTextColor() {
        return textColor;
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }

    public List<MedicationFlagRxClass> getRxClassList() {
        return rxClassList;
    }

    public void setRxClassList(List<MedicationFlagRxClass> rxClassList) {
        this.rxClassList = rxClassList;
    }
}
