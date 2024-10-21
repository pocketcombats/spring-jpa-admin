package com.pocketcombats.admin.demo.blog.admin;

import com.pocketcombats.admin.AdminAction;
import com.pocketcombats.admin.AdminField;
import com.pocketcombats.admin.AdminFieldOverride;
import com.pocketcombats.admin.AdminFieldset;
import com.pocketcombats.admin.AdminModel;
import com.pocketcombats.admin.demo.blog.entity.Post;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.ConversionService;

/**
 * Demonstrates JPA Admin annotations applied indirectly.
 */
@AdminModel(
        entity = Post.class,
        label = "demo.entity.post.label",
        searchFields = {"author.username", "text"},
        listFields = {"textPreview", "author", "postTime", "approved"},
        filterFields = {"approved", "author", "tags"},
        fieldsets = {
                @AdminFieldset(
                        fields = {
                                "approved",
                                "postTime",
                                "author",
                                "text"
                        }
                ),
                @AdminFieldset(
                        label = "Meta",
                        fields = {"category", "tags"}
                )
        },
        fieldOverrides = {
                @AdminFieldOverride(
                        name = "postTime",
                        field = @AdminField(sortable = true)
                ),
                @AdminFieldOverride(
                        name = "text",
                        field = @AdminField(template = "admin/widget/textarea")
                ),
                @AdminFieldOverride(
                        name = "author",
                        field = @AdminField(
                                sortBy = "username",
                                representation = "username"
                        )
                ),
                @AdminFieldOverride(
                        name = "tags",
                        field = @AdminField(
                                template = "admin/widget/multiselect_checkboxes",
                                representation = "text"
                        )
                )
        }
)
public class PostAdminModel {

    /**
     * Admin Models are instantiated as Spring beans,
     * allowing you to declare dependencies on other beans and leverage various Spring-related capabilities.
     */
    private final ConversionService conversionService;

    public PostAdminModel(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @AdminField(label = "Text")
    public String getTextPreview(Post post) {
        return StringUtils.abbreviate(post.getText(), 30);
    }

    @AdminAction
    public void approve(Iterable<Post> posts) {
        for (Post post : posts) {
            post.setApproved(true);
        }
    }
}
