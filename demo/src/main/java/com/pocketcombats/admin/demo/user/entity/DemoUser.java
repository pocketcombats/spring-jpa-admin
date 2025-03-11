package com.pocketcombats.admin.demo.user.entity;

import com.pocketcombats.admin.AdminAction;
import com.pocketcombats.admin.AdminField;
import com.pocketcombats.admin.AdminFieldset;
import com.pocketcombats.admin.AdminLink;
import com.pocketcombats.admin.AdminModel;
import com.pocketcombats.admin.demo.blog.entity.Comment;
import com.pocketcombats.admin.demo.blog.entity.Post;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "demo_user")
@AdminModel(
        listFields = {"username", "enabled"},
        searchFields = {"id", "username"},
        filterFields = "enabled",
        fieldsets = @AdminFieldset(fields = {"enabled", "username"}),
        links = {
                @AdminLink(target = Post.class, preview = 3, sortBy = "-postTime"),
                @AdminLink(target = Comment.class, sortBy = "-postTime")
        },
        // Prohibit direct demo users creation or deletion
        insertable = false,
        disableActions = "delete"
)
public class DemoUser implements Serializable {

    @Id
    @Column(name = "id", updatable = false)
    private Integer id;

    @Size(min = 3, max = 15)
    @Column(name = "username", unique = true, nullable = false)
    @AdminField(sortable = true, description = "Sample username description")
    private String username;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @OneToMany(mappedBy = "author", orphanRemoval = true)
    private List<Post> posts = new ArrayList<>();

    @Version
    @Column(name = "version")
    private Integer version;

    @AdminAction
    public static void enable(List<DemoUser> users) {
        for (DemoUser user : users) {
            user.setEnabled(true);
        }
    }

    @AdminAction
    public static void disable(List<DemoUser> users) {
        for (DemoUser user : users) {
            user.setEnabled(false);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (id == null) {
            return false;
        }

        if (this == o) {
            return true;
        }

        if (!(o instanceof DemoUser demoUser)) {
            return false;
        }

        return new EqualsBuilder()
                .append(id, demoUser.id)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(id)
                .toHashCode();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<Post> getPosts() {
        return posts;
    }

    public void setPosts(List<Post> posts) {
        this.posts = posts;
    }

    public Integer getVersion() {
        return version;
    }
}
