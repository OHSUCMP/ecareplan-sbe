package edu.ohsu.cmp.ecareplan.entity;

import edu.ohsu.cmp.ecareplan.model.AuditSeverity;
import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name = "audit_data")
public class AuditData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Enumerated(EnumType.STRING)
    private AuditSeverity severity;

    private String event;

    private String details;

    private Date created;

    protected AuditData() {
    }

    public AuditData(Long userId, AuditSeverity severity, String event) {
        this(userId, severity, event, null);
    }

    public AuditData(Long userId, AuditSeverity severity, String event, String details) {
        this.userId = userId;
        this.severity = severity;
        this.event = event;
        this.details = details;
        this.created = new Date();
    }

    @Override
    public String toString() {
        return "Audit{" +
                "id=" + id +
                ", userId=" + userId +
                ", severity=" + severity +
                ", event='" + event + '\'' +
                ", details='" + details + '\'' +
                ", created=" + created +
                '}';
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long patId) {
        this.userId = patId;
    }

    public AuditSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(AuditSeverity severity) {
        this.severity = severity;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}
