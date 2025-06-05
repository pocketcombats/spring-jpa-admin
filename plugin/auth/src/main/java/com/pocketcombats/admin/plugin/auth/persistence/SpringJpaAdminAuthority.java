package com.pocketcombats.admin.plugin.auth.persistence;

import com.pocketcombats.admin.AdminField;
import com.pocketcombats.admin.AdminModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.security.core.GrantedAuthority;

@AdminModel(
        listFields = {"authority", "description"}
)
@Entity
@Table(name = "spring_admin_authority")
@SequenceGenerator(name = "spring_admin_authority_id", sequenceName = "spring_admin_authority_id_seq", allocationSize = 10, initialValue = 10)
public class SpringJpaAdminAuthority implements GrantedAuthority {

    @Id
    @GeneratedValue(generator = "spring_admin_authority_id")
    @Column(name = "id", updatable = false)
    private Integer id;

    @Column(name = "authority", unique = true, nullable = false, updatable = false)
    private String authority;

    @AdminField(representation = "#abbreviate(#this, 20)")
    @Column(name = "description")
    private String description;

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("authority", authority)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof SpringJpaAdminAuthority that)) return false;

        return new EqualsBuilder()
                .append(getAuthority(), that.getAuthority())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getAuthority())
                .toHashCode();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
