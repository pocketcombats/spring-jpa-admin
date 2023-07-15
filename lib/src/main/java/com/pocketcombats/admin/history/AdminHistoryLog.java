package com.pocketcombats.admin.history;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PreRemove;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(
        name = "admin_history_log",
        indexes = @Index(name = "admin_history_log_time", columnList = "action_time")
)
public class AdminHistoryLog {

    @Id
    @Column(name = "id", updatable = false)
    @GeneratedValue
    private Long id;

    @Column(name = "action_time", nullable = false, updatable = false)
    private Instant time;

    @Column(name = "action", nullable = false, updatable = false)
    private String action;

    @Column(name = "model", nullable = false, updatable = false)
    private String model;

    @Column(name = "entity_id", nullable = false, updatable = false)
    private String entityId;

    @Column(name = "entity_repr", nullable = false, updatable = false)
    private String entityRepresentation;

    @Column(name = "username", nullable = false, updatable = false)
    private String username;

    @PreRemove
    public void preventRemoval() {
        throw new UnsupportedOperationException();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getTime() {
        return time;
    }

    public void setTime(Instant time) {
        this.time = time;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getEntityRepresentation() {
        return entityRepresentation;
    }

    public void setEntityRepresentation(String entityRepresentation) {
        this.entityRepresentation = entityRepresentation;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
