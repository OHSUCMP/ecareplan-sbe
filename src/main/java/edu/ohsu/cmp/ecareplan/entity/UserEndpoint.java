package edu.ohsu.cmp.ecareplan.entity;

import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name = "user_endpoint")
public class UserEndpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @ManyToOne
    @JoinColumn(name = "endpointId")
    private Endpoint endpoint;

    private String encryptedRefreshTokenDataB64;
    private Date lastSyncCompleted;
    private Date created;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public String getEncryptedRefreshTokenDataB64() {
        return encryptedRefreshTokenDataB64;
    }

    public void setEncryptedRefreshTokenDataB64(String encryptedRefreshTokenDataB64) {
        this.encryptedRefreshTokenDataB64 = encryptedRefreshTokenDataB64;
    }

    public Date getLastSyncCompleted() {
        return lastSyncCompleted;
    }

    public void setLastSyncCompleted(Date lastSyncCompleted) {
        this.lastSyncCompleted = lastSyncCompleted;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}
