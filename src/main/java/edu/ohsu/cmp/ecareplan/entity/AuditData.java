package edu.ohsu.cmp.ecareplan.entity;

import edu.ohsu.cmp.ecareplan.model.AuditSeverity;
import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name = "audit_data")
public class AuditData {
    private static final int MAX_EVENT_LENGTH = 100;
    private static final int MAX_DETAILS_LENGTH = 1000;

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
        this.event = truncate(event, MAX_EVENT_LENGTH);
        this.details = truncate(details, MAX_DETAILS_LENGTH);
        this.created = new Date();
    }

    private String truncate(String s, int max_length) {
        if (s == null || s.length() <= max_length) {
            return s;
        }
        return s.substring(0, max_length);
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
