package com.pocketcombats.admin.demo.admin;

import com.pocketcombats.admin.AdminField;
import com.pocketcombats.admin.AdminFieldOverride;
import com.pocketcombats.admin.AdminModel;
import com.pocketcombats.admin.ValueFormatter;
import com.pocketcombats.admin.demo.entity.DemoUser;
import com.pocketcombats.admin.demo.entity.Post;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.ConversionService;

/**
 * Demonstrates JPA Admin annotations applied indirectly.
 */
@AdminModel(
        model = Post.class,
        listFields = {"textPreview", "author", "postTime", "approved"},
        filterFields = "approved",
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
                                sortBy = "author.username",
                                valueFormatter = PostAdminModel.UsernameValueFormatter.class
                        )
                )
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

    static class UsernameValueFormatter implements ValueFormatter {

        @Override
        public String format(Object user) {
            return ((DemoUser) user).getUsername();
        }
    }
}
