package com.pocketcombats.admin.demo.admin;

import com.pocketcombats.admin.AdminField;
import com.pocketcombats.admin.AdminFieldOverride;
import com.pocketcombats.admin.AdminModel;
import com.pocketcombats.admin.demo.entity.Post;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.ConversionService;

/**
 * Demonstrates JPA Admin annotations applied indirectly.
 */
@AdminModel(
        model = Post.class,
        listFields = {"textPreview", "authorName", "postTime", "approved"},
        sortFields = {"postTime", "author.username"},
        filterFields = "approved",
        fieldOverrides = {
                @AdminFieldOverride(name = "text", field = @AdminField(template = "admin/widget/textarea"))
        }
)
public class PostAdminModel {

    private final ConversionService conversionService;

    public PostAdminModel(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @AdminField(label = "Text")
    public String getTextPreview(Post post) {
        return StringUtils.abbreviate(post.getText(), 30);
    }

    @AdminField(label = "Author")
    public String getAuthorName(Post post) {
        return post.getAuthor().getUsername();
    }
}
