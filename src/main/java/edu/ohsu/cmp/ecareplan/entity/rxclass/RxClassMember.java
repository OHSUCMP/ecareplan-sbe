package edu.ohsu.cmp.ecareplan.entity.rxclass;

import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name = "rx_class_member")
public class RxClassMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String rxClass;
    private String rxCui;
    private String name;
    private String tty;
    private Date created;
    private Date updated;

    public RxClassMember() {
    }

    public RxClassMember(String rxClass, String rxCui, String name, String tty) {
        this.rxClass = rxClass;
        this.rxCui = rxCui;
        this.name = name;
        this.tty = tty;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRxClass() {
        return rxClass;
    }

    public void setRxClass(String rxClass) {
        this.rxClass = rxClass;
    }

    public String getRxCui() {
        return rxCui;
    }

    public void setRxCui(String rxCui) {
        this.rxCui = rxCui;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTty() {
        return tty;
    }

    public void setTty(String tty) {
        this.tty = tty;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }
}
