package com.pocketcombats.admin.plugin.auth.persistence;

import com.pocketcombats.admin.AdminField;
import com.pocketcombats.admin.AdminModel;
import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;

@AdminModel(
        listFields = {"user", "timestamp", "address"},
        insertable = false,
        updatable = false,
        disableActions = "delete"
)
@Entity
@Table(name = "spring_admin_user_auth_log")
@SequenceGenerator(name = "spring_admin_user_auth_log_id", sequenceName = "spring_admin_user_auth_log_id_seq", allocationSize = 100)
public class SpringJpaAdminUserAuthLog implements Serializable {

    @Id
    @Column(name = "id", updatable = false)
    @GeneratedValue(generator = "spring_admin_user_auth_log_id")
    private Long id;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private Instant timestamp;

    @AdminField(representation = "username")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private SpringJpaAdminUser user;

    @Column(name = "address", nullable = false, updatable = false)
    private String address;

    @Column(name = "agent", nullable = false, updatable = false)
    private String agent;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public SpringJpaAdminUser getUser() {
        return user;
    }

    public void setUser(SpringJpaAdminUser user) {
        this.user = user;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }
}
