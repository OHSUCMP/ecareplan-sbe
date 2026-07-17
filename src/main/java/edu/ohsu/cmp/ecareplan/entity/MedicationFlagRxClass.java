package edu.ohsu.cmp.ecareplan.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "medication_flag_rxclass")
public class MedicationFlagRxClass {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long medicationFlagId;
    private String rxClass;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMedicationFlagId() {
        return medicationFlagId;
    }

    public void setMedicationFlagId(Long medicationFlagId) {
        this.medicationFlagId = medicationFlagId;
    }

    public String getRxClass() {
        return rxClass;
    }

    public void setRxClass(String rxClass) {
        this.rxClass = rxClass;
    }
}
