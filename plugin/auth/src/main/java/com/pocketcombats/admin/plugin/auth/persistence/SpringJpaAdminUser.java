package com.pocketcombats.admin.plugin.auth.persistence;

import com.pocketcombats.admin.AdminField;
import com.pocketcombats.admin.AdminFieldset;
import com.pocketcombats.admin.AdminLink;
import com.pocketcombats.admin.AdminModel;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;

@AdminModel(
        searchFields = "username",
        listFields = {"username", "enabled"},
        filterFields = {"enabled", "authorities"},
        fieldsets = {
                @AdminFieldset(
                        fields = {"username", "password", "enabled", "authorities"}
                )
        },
        links = {
                @AdminLink(
                        target = SpringJpaAdminUserAuthLog.class,
                        label = "Auth log",
                        preview = 5,
                        representation = "address",
                        sortBy = "-timestamp"
                ),
        }
)
@Entity
@Table(name = "spring_admin_user")
@SequenceGenerator(name = "spring_admin_user_id", sequenceName = "spring_admin_user_id_seq", allocationSize = 10)
public class SpringJpaAdminUser implements UserDetails {

    @Id
    @Column(name = "id", updatable = false)
    @GeneratedValue(generator = "spring_admin_user_id")
    private Integer id;

    @AdminField(sortable = true)
    @Column(name = "username", nullable = false)
    @NotBlank
    private String username;

    @Column(name = "username_lc", nullable = false)
    private String lowerUsername;

    @AdminField(insertable = false, template = "admin/widget/password")
    @Column(name = "password", nullable = false)
    private String password = "";

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @AdminField(
            template = "admin/widget/multiselect_checkboxes",
            representation = "authority"
    )
    @ManyToMany(cascade = CascadeType.DETACH)
    @JoinTable(
            name = "spring_admin_user_authorities",
            joinColumns = @JoinColumn(name = "admin_user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "authority_id", referencedColumnName = "id"))
    private Set<SpringJpaAdminAuthority> authorities;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
        this.lowerUsername = username.toLowerCase();
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        if (password != null) {
            this.password = password;
        }
    }

    @Override
    @Transient
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    @Transient
    public boolean isAccountNonLocked() {
        return isEnabled();
    }

    @Override
    @Transient
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(Set<SpringJpaAdminAuthority> authorities) {
        this.authorities = authorities;
    }
}
