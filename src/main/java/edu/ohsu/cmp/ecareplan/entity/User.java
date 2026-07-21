package edu.ohsu.cmp.ecareplan.entity;

import edu.ohsu.cmp.ecareplan.util.CryptoUtil;
import jakarta.persistence.*;

import java.util.Base64;
import java.util.Date;

@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String patIdHash;
    private String saltB64;
    private Date created;

    protected User() {
    }

    public User(String patIdHash) {
        this.patIdHash = patIdHash;
        this.saltB64 = Base64.getEncoder().encodeToString(CryptoUtil.randomBytes(16));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPatIdHash() {
        return patIdHash;
    }

    public void setPatIdHash(String patIdHash) {
        this.patIdHash = patIdHash;
    }

    public String getSaltB64() {
        return saltB64;
    }

    public void setSaltB64(String saltB64) {
        this.saltB64 = saltB64;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}
