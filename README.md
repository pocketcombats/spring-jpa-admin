# Spring JPA Admin

Spring JPA Admin is a powerful library built on top of Spring Boot, JPA and Thymeleaf, designed to simplify the development of administrative interfaces. 
This library reads entity metadata to provide a user-friendly and highly customizable admin interface, enabling trusted users to manage content.

## Requirements
- Java 17+
- Spring Boot 4.0+
- Jakarta Persistence API (JPA) 3.2+

For Spring Boot 3.x applications, use version 1.0.2.

## Installation
Spring JPA Admin consists of two main artifacts:

1. **Annotations** (`com.pocketcombats.spring-jpa-admin:annotation`): Required in modules that register entities with the admin site.
2. **Library** (`com.pocketcombats.spring-jpa-admin:spring-jpa-admin`): Core functionality for the admin site.

### Maven
```xml
<dependencies>
    <!-- Annotations (can be in a separate module) -->
    <dependency>
        <groupId>com.pocketcombats.spring-jpa-admin</groupId>
        <artifactId>annotation</artifactId>
        <version>${spring-jpa-admin.version}</version>
    </dependency>

    <!-- Admin site -->
    <dependency>
        <groupId>com.pocketcombats.spring-jpa-admin</groupId>
        <artifactId>spring-jpa-admin</artifactId>
        <version>${spring-jpa-admin.version}</version>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

### Gradle
```kotlin
dependencies {
    api(platform("com.pocketcombats.spring-jpa-admin:bom:1.0.4"))

    // Annotations
    implementation("com.pocketcombats.spring-jpa-admin:annotation")

    // Admin site
    runtimeOnly("com.pocketcombats.spring-jpa-admin:spring-jpa-admin")
}
```

## Security
Spring JPA Admin ensures the security of sensitive endpoints by requiring the `"ROLE_JPA_ADMIN"` role. 
Users without this role will not be able to access the admin interface.  
The library provides fine-grained per-model permission configuration through the `@AdminModelPermissions` annotation, 
allowing you to control who can view, edit, or create entities for each model. See the [Permissions](#permissions) section for details.

For authentication, you can use the [Authentication Plugin](#authentication-plugin) or integrate with your existing authentication.

Two properties tune the security auto-configuration:
- `spring.jpa-admin.configure-security` (default: `true`) — registers a filter chain permitting the static `/webjars/**` resources used by the admin UI. Disable it if your application manages its own filter chains.
- `spring.jpa-admin.method-security` (default: `true`) — enables `@Secured` method security, which enforces the role checks on all admin endpoints. Disable it only if your application already enables `@Secured` support itself, e.g. via its own `@EnableMethodSecurity(securedEnabled = true)`.

> **Upgrade note:** earlier versions enabled `@Secured` enforcement only when `configure-security` was on,
> so setting `spring.jpa-admin.configure-security=false` used to leave the role checks inert.
> Method security is now a separate, default-on switch: it stays active regardless of `configure-security`,
> and admin users therefore always need the `ROLE_JPA_ADMIN` role (endpoints return 403 otherwise).
> This fail-closed default is deliberate — a custom filter chain that forgets to cover `/admin/**` no longer
> leaves the admin endpoints unprotected. If your application intentionally guards them some other way,
> set `spring.jpa-admin.method-security=false`.

## Quick start
To quickly grasp the capabilities of Spring JPA Admin, we recommend exploring the included `demo` application.
This application serves as a practical showcase of various features, allowing you to replicate and customize them in your own project.  
Here we'll recreate some of its parts.

### Annotating Entities
To register an entity with the admin site, simply annotate the entity class with the `@AdminModel` annotation.
Let's take a look at an example using a `DemoUser` entity:
```java
@Entity
@Table(name = "demo_user")
public class DemoUser implements Serializable {

    @Id
    @Column(name = "id", updatable = false)
    @GeneratedValue
    private Integer id;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @OneToMany(mappedBy = "author")
    private List<Post> posts = new ArrayList<>();

    // ...
}
```
Once you've added the `@AdminModel` annotation, navigate to the admin site (`/admin/DemoUser/`) and see if it works: 
![Demo User list view](media/listview-001.png)  

### Customizing List View
The first column is always a link leading to the edit form, but we'll cover that later.
To start, we want to rearrange our list view columns:
```java
@AdminModel(listFields = {"username", "enabled"})
```
Restart the admin site and open our entity again
![Demo User list view rearranged](media/listview-002.png)
Much better. Also note that the `Enabled` column now contains cross and check marks instead of text because the field is of type boolean.

#### Filtering
Spring JPA Admin allows you to define filters to select specific records in the list view.
To quickly select only enabled or disabled users, let's modify our annotation:
```java
@AdminModel(
        listFields = {"username", "enabled"},
        filterFields = "enabled"
)
```
Restart the admin site and you will see the filter option for the enabled status in the list view:
![Demo User list view with filter](media/listview-003.png)
You can have as many filters for an entity as you'd like, but remember to add appropriate database indexes for query efficiency.

#### Custom List Fields
Sometimes, the entity's fields aren't sufficient to display all the required information.
Luckily, we can create custom list fields.
For example, let's add a custom field to display the number of posts for each Demo User:
```java
    public int getPostCount() {
        return posts.size();
    }
```
<sup>*Please note that while this approach is convenient for quick implementation, it can be inefficient and should not be used in real applications.*</sup>  
Update the `@AdminModel` annotation to include this custom field in the `listFields` attribute:
```java
@AdminModel(
        listFields = {"username", "postCount", "enabled"},
        filterFields = "enabled"
)
``` 
Let's see what we got as a result:
![Demo User list view with custom field](media/listview-004.png)

#### Sorting
Now let's say we want to be able to sort our Demo Users by username. Annotate `username` field with
```java
@AdminField(sortable = true)
```
Restart the admin site, and now you can sort users!
![Demo User list view with sorting](media/listview-005.png)
Most of the fields can be annotated with `sortable = true`.
For relation attributes you can specify `sortBy`. For example, if you want to sort Posts by author username, it will look like this:
```java
@ManyToOne
@JoinColumn(name = "author_id")
@AdminField(sortBy = "username")
private DemoUser author;
```

#### Searching
Spring JPA Admin allows users to search for specific records in the list view.
To enable searching for Demo Users by their ids and usernames, modify the `@AdminModel` annotation to include the `searchFields` attribute (and remove the post count at the same time):
```java
@AdminModel(
        listFields = {"username", "enabled"},
        searchFields = {"id", "username"},
        filterFields = "enabled"
)
```
As before, restart the admin site and try searching:
![Demo User list view with search field](media/listview-006.png)

Search fields can also be dotted paths through relations, including to-many ones — for example
`searchFields = "posts.title"` matches users having at least one post with a matching title.
Matching through a collection never duplicates rows in the list view.

### Edit Restrictions
It is possible to disable creation or modification of any particular entity.
For example, you can disable the creation of new entities by setting `insertable = false` in the `@AdminModel` annotation.
Similarly, using `updatable = false` disables the modification of existing records while still allowing the creation of new ones.

### Permissions
Spring JPA Admin provides fine-grained permission control through the `@AdminModelPermissions` annotation. 
This allows you to restrict who can view, edit, or create entities for each model based on user roles.

#### Configuring Permissions
To configure permissions for a model, use the `permissions` attribute in the `@AdminModel` annotation:

```java
@AdminModel(
    permissions = @AdminModelPermissions(
        view = {},
        edit = {"ROLE_MANAGER", "ROLE_ADMIN"},
        create = {"ROLE_ADMIN"}
    )
)
public class Post {
    // ...
}
```

In this example:
- Any user with access to the admin interface can view posts
- Users with either `ROLE_MANAGER` or `ROLE_ADMIN` can edit posts
- Only users with `ROLE_ADMIN` can create new posts

#### Permission Types
The `@AdminModelPermissions` annotation supports three types of permissions:

1. **view**: Controls who can see the model in the admin interface and view its entities
2. **edit**: Controls who can modify existing entities
3. **create**: Controls who can create new entities

If you don't specify permissions for a particular action, it means that action is available to all users who have 
access to the admin interface (i.e., users with the `ROLE_JPA_ADMIN` role).  
If a user doesn't have the required permission, they will receive an "Access Denied" error.

### Bulk Actions
Spring JPA Admin allows performing bulk actions on selected records in the list view.
By default, only the "delete" action is enabled.
However, you can easily add your own custom actions.

#### Entity Custom Actions
To define a custom action for a specific entity, create static methods annotated with `@AdminAction`.
These methods operate on a list of selected entity records and can perform custom logic.
For example, let's define custom actions to enable and disable Demo Users:
```java
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
```
That's it! After restarting the admin site, the custom actions will be available for selected Demo Users in the list view:
![Demo User list view with custom actions](media/listview-007.png)
Methods for custom actions must be static (unless they are defined on a separate admin model class, more on this later) and must accept a single argument with the list of selected entity records.
In Kotlin, declare them in the entity's companion object — see [Kotlin](#kotlin).

#### Site-Wide Custom Action
If you need to create a site-wide list view custom action that applies to all entities, you can implement the `AdminModelAction` interface and register it as a Spring bean.
The default "delete" action is implemented using this approach.

Here's a complete example for the `DemoUser` entity with custom column ordering, enabled filtering, sorting, searching, custom actions, and disabled "delete" action:
```java
@Entity
@Table(name = "demo_user")
@AdminModel(
        listFields = {"username", "enabled"},
        searchFields = {"id", "username"},
        filterFields = "enabled",
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
    @AdminField(updatable = false, sortable = true)
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
    
    // ...
}
```
![Demo User list view final result](media/listview-008.png)

### Field Representation
Custom field representation is used for both the list view and the form view.
Let's focus on the list view for now. Also, we'll switch to the `Post` entity.  
Remember the `author` field where we added a custom `sortBy` attribute? By default, it looks like this:
![Post list view with default author representation](media/listview-009.png)
This default representation is simply the result of calling `.toString()`, which isn't very useful for most complex types.
The values for the `Author` filter also don't provide much help.  
Let's adjust the annotation to include a custom `representation`:
```java
@AdminField(sortBy = "username", representation = "username")
```
The result is much more helpful:
![Post list view with custom author representation](media/listview-010.png)
The `representation` is an expression in [SpEL](https://docs.spring.io/spring-framework/reference/core/expressions.html) format, with the root object set to the entity being displayed.
In most cases, you simply want to reference a field, like `username` in our case, or call a method of an entity.

Next, let's do something with the `Text` field.  
We don't want to set a custom `representation` because it would also affect the form view, so we'll provide a custom list field instead:
```java
    @AdminField(label = "Text")
    public String getTextPreview() {
        return StringUtils.abbreviate(getText(), 30);
    }
```
By setting the `label`, we can display the column title as "Text" instead of "Text Preview":
![Post list view with text preview](media/listview-011.png)

### Forms
Let's continue with our `Post` entity and move on to the Edit Form.

#### Fieldsets
The default form includes all possible entity fields, including `comments` and `reactions`, which we don't want to edit.
To override the fieldset and include only the fields we are interested in editing, we can specify the desired fields in the `fieldsets` attribute:
```java
@AdminModel(
        fieldsets = @AdminFieldset(
                fields = {
                        "approved",
                        "postTime",
                        "author",
                        "text",
                        "category",
                        "tags"
                }
        )
)
```
The result will be a form with the specified fields:
![Edit Form with overridden fieldset](media/form-001.png)  
Next, let's move the `category` and `tags` fields into a dedicated fieldset named "Meta":
```java
@AdminModel(
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
        }
)
```
![Edit Form with labeled Meta fieldset](media/form-002.png)  

#### Edit Restrictions
Note that the "Author" field isn't editable.
This is because the entity column is marked as `updatable = false`, so editing it wouldn't have any effect anyway.
You can also force a form field to be read-only by setting `insertable = false` or `updatable = false` on the `@AdminField` annotation, even if the entity column doesn't impose these restrictions.

#### Widgets and Custom Widgets
The "Text" and "Tags" fields may not be very helpful in their default form.
To customize their appearance, we can provide them with `template` settings, at the same time updating the tags' `representation`:
```java
    @Column(name = "text", nullable = false)
    @AdminField(template = "admin/widget/textarea")
    private String text;

    // ...

    @ManyToMany
    @JoinTable(
            name = "post_tags",
            joinColumns = @JoinColumn(name = "post_id", referencedColumnName = "id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "tag_id", referencedColumnName = "id", nullable = false)
    )
    @AdminField(template = "admin/widget/multiselect_checkboxes", representation = "text")
    private Set<Tag> tags;
```
![Edit Form with custom templates](media/form-003.png)  
Field templates are simple [Thymeleaf fragments](https://www.thymeleaf.org/doc/articles/layouts.html), and you can further customize the appearance of the edit form by creating your own custom field templates.

> **Note:** On Spring Boot 4 (Spring Framework 7), custom templates must not use the `#themes`
> expression object or the `thymeleafRequestContext` context variable. Thymeleaf's `thymeleaf-spring6`
> integration still references the theme support that was
> [removed in Spring Framework 7](https://github.com/spring-projects/spring-framework/wiki/Upgrading-to-Spring-Framework-7.0),
> so touching either of them fails at runtime.

#### Raw ID Fields
We have the option to display the `author` field as a raw ID input instead of a select dropdown with all existing Demo Users.
To achieve this, simply add `rawId = true` to the `@AdminField` annotation for the `author` field, without the need to change the `template` attribute.
![Edit Form with raw id](media/form-005.png)

#### Autocomplete
Preloading every option into a `<select>` works for a handful of Demo Users, but not for thousands. Once a
to-one field's target exceeds a threshold, it renders as a searchable autocomplete widget that loads options
from the server as the user types.

Properties:
- `spring.jpa-admin.max-preloaded-options` — how many options a to-one field preloads into a `<select>`.
  Beyond it the field autocompletes if it can, otherwise preloads the first N (keeping the current selection)
  and notes that more exist. `0` always autocompletes; a negative value opts out entirely (uncapped select,
  never autocomplete). Overridable per field; when unset the field inherits this global value.
- `spring.jpa-admin.autocomplete-page-size` — options per page served to the widget.
- `spring.jpa-admin.max-counted-options` — cap on the row count probed for the "N of M" note; past it the
  total is reported as "M+" rather than scanning the (large) table. Must be at least `1`.

The demo overrides the threshold on `Post.editor`, forcing autocomplete even though `DemoUser` has few rows:
```java
@AdminFieldOverride(
        name = "editor",
        field = @AdminField(representation = "username", maxPreloadedOptions = 0)
)
```

Options are served by `GET /admin/{model}/field/{field}/options`, secured like every other admin endpoint.
Autocomplete requires the target to be a registered admin model with `searchFields` (typed queries match those
fields when the user can view the model, otherwise only an exact id). Targets without `searchFields`, fields
rendered by a custom `template`, and composite identifiers can't autocomplete — they treat the threshold as a
preload cap: the first N options plus the current selection, a "Showing N of M options" note, and a `WARN`
pointing at the fix (add `searchFields`, mark the field `rawId`, or raise the threshold).

> **Note:** If you override the form template via `spring.jpa-admin.templates.form`, it must include
> `/admin/js/toone-autocomplete.js` and the `admin-autocomplete` styles from the default `admin/form.html`;
> otherwise disable autocomplete with a negative threshold.

#### Embedded Fields
JPA `@Embedded` attributes are supported out of the box. Each persistent property of the embeddable type is
rendered as a separate input, grouped together under the embedded field's label.

Declare an `@Embeddable` type:
```java
@Embeddable
public class SeoMetadata {

    @Column(name = "seo_title")
    private String metaTitle;

    @Column(name = "seo_description")
    private String metaDescription;

    // getters and setters
}
```
Reference it from your entity and include it in a fieldset like any other field:
```java
    @Embedded
    private SeoMetadata seo;
```
```java
@AdminFieldset(label = "Meta", fields = {"category", "tags", "seo"})
```
The embeddable instance is created lazily — it is only instantiated once one of its properties receives a
non-empty value, so entities without embedded data keep a `null` embeddable. Properties are rendered in their
declaration order (own properties first, then any inherited from a mapped superclass), and per-property
validation errors are reported against the nested `<field>.<property>` path. The default widget is
`admin/widget/embedded`; supply a custom `template` on the field to change its appearance.

#### Validation
Spring JPA Admin utilizes [Jakarta Validation](https://beanvalidation.org/) to validate entities before saving them.  
Let's try it out and annotate the `text` field with `@NotBlank`:
```java
    @Column(name = "text", nullable = false)
    @AdminField(template = "admin/widget/textarea")
    @NotBlank
    private String text;
```
Now, if we try to save a Post with an empty text, a validation error will be displayed:
![Edit Form validation error](media/form-004.png)
You can restrict validation constraints to the admin edit form by specifying `groups = AdminValidation.class`.
This allows you to separate the validation rules for the admin form from other parts of your application.  
For example, the following annotation will only be applied to the admin edit form and won't affect other parts of your application, such as entity persistence:
```java
    @NotBlank(groups = AdminValidation.class)
    private String text;
```

#### Relation Links
To enhance our Edit Form, let's include links to related entities in the `DemoUser` entity.
We'll provide links to Posts and Comments that are associated with the currently open user instance.
To achieve this, we can include the `links` attribute in the `@AdminModel` annotation, as shown below:
```java
        links = {
                @AdminLink(target = Post.class, mappedBy = "author", sortBy = "-postTime"),
                @AdminLink(target = Comment.class, sortBy = "-postTime")
        }
```
`Post` references `DemoUser` twice (`author` and `editor`), so the link must name the owning field
via `mappedBy = "author"`; a single reference would be resolved automatically.
So our complete `DemoUser` admin annotation looks like this:
```java
@AdminModel(
        listFields = {"username", "enabled"},
        searchFields = {"id", "username"},
        filterFields = "enabled",
        fieldsets = @AdminFieldset(fields = {"enabled", "username"}),
        links = {
                @AdminLink(target = Post.class, mappedBy = "author", sortBy = "-postTime"),
                @AdminLink(target = Comment.class, sortBy = "-postTime")
        },
        // Prohibit direct demo users creation or deletion
        insertable = false,
        disableActions = "delete"
)
```
Let's take a look at the edit form now:
![Edit Form with relation links](media/form-006.png)

By clicking on "Blog Post", users can now quickly navigate to the `Post` list view, where they will see only posts created by the "Demo Editor":
![List View by User](media/listview-012.png)  
One last enhancement we want to add is previews for the latest Posts created by the user.
To enable this, modify the `@AdminLink` annotation to include the `preview` attribute, as shown below:
```java
@AdminLink(target = Post.class, mappedBy = "author", preview = 3, sortBy = "-postTime")
```
Now, when we review the updated Edit Form, we can see previews for the latest user Posts:
![Edit Form with link previews](media/form-007.png)  
These relation links and previews provide users with quick access to related content, making the admin interface more user-friendly and efficient.  

### Externalized Configuration
There are situations where you may not want to apply admin annotations directly to your entities, or it may not be feasible to do so.
In such cases, Spring JPA Admin supports **externalized configuration**, allowing you to configure the admin settings separately.
For demonstration, let's remove the admin-related annotations from the `Post` entity and create a dedicated `PostAdminModel` class to hold the admin configuration:
```java
@AdminModel(
        entity = Post.class,
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
        }
)
public class PostAdminModel {
    
    // ...
}
```
Note the `entity = Post.class` attribute, which specifies the target entity for the admin configuration.

#### Component Model
Admin models are instantiated as Spring beans, which means they can depend on other beans and take advantage of additional functionality compared to plain entities.

#### Custom List Fields
Let's further clean up the `Post` entity by moving the `getTextPreview` method to the admin model.
We need to make a slight modification to the method to accept the target entity instance:
```java
    @AdminField(label = "Text")
    public String getTextPreview(Post post) {
        return StringUtils.abbreviate(post.getText(), 30);
    }
```

#### Custom Actions
Admin models can declare custom bulk actions.
Unlike action methods defined on entities, these methods aren't required to be static.
Let's define a custom `approve` method:
```java
    @AdminAction
    public void approve(Iterable<Post> posts) {
        for (Post post : posts) {
            post.setApproved(true);
        }
    }
```

#### Field Overrides
Next, let's clean up the `Post` entity by moving the `@AdminField` annotations to the admin model using the `fieldOverrides` attribute:
```java
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
```
The complete `PostAdminModel` retains all the functionality previously implemented by the annotations on the `Post` entity and will look like this:
```java
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
                                "editor",
                                "text"
                        }
                ),
                @AdminFieldset(
                        label = "Meta",
                        fields = {"category", "tags", "seo"}
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
                        name = "editor",
                        field = @AdminField(
                                representation = "username",
                                maxPreloadedOptions = 0
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
```
Here `label` is a localization key resolved through `MessageSource` (see [Localization](#localization)),
and `searchFields` matches posts by author username through a dotted path, as described in [Searching](#searching).

## Asynchronous bootstrap
The admin site follows Spring Boot's `spring.jpa.bootstrap` setting. With the default (synchronous)
bootstrap it reads entity metadata while the context refreshes. Under
`spring.jpa.bootstrap=async` (where the `EntityManagerFactory` is built on a background thread) it
defers reading the JPA metamodel so it no longer forces that factory to finish during startup.  
No admin-specific configuration is required.

## Kotlin
Spring JPA Admin works with Kotlin-authored entities and admin models out of the box:

- **Null safety**: the published artifacts carry [JSpecify](https://jspecify.dev/) `@NullMarked`/`@Nullable` annotations, so Kotlin (2.1+ treats them as strict) sees precise nullability for the entire API.
- **Entities**: JPA needs a no-arg constructor, so use the standard `kotlin("plugin.jpa")` (noarg) compiler plugin, the same requirement as for any Kotlin JPA entity.
- **`@AdminField` on properties**: a bare `@AdminField val title: String` annotates the backing field, while explicit use-site targets (`@field:AdminField`, `@get:AdminField`) pick a member directly.
  All of these are honored under both field and getter (property) access — when the JPA metamodel reports one member, the paired field/getter is checked too.
- **Bulk actions**: declare entity-level actions in the entity's companion object (`@JvmStatic` is not required):
  ```kotlin
  @Entity
  @AdminModel(listFields = ["username", "enabled"])
  class DemoUser {
      // ...
      companion object {
          @AdminAction
          fun disable(users: List<DemoUser>) {
              users.forEach { it.enabled = false }
          }
      }
  }
  ```
  Actions on [externalized admin models](#externalized-configuration) are plain member functions, as in Java.

## Localization
Spring JPA Admin fully supports localization. You can refer to the `spring-jpa-admin-messages.properties` file for a complete list of supported keys.
To enable localization, include `spring-jpa-admin-messages` in the list of message basenames for your Spring Boot application. You can achieve this by adding the following configuration to your `application.yaml` file:
```yaml
spring.messages:
  basename: messages,spring-jpa-admin-messages
```
With this configuration, Spring Boot will look for message properties in both the `messages.properties` file (or any other custom message file you have) and the `spring-jpa-admin-messages.properties` file.  
All core annotations in Spring JPA Admin, such as `@AdminAction`, `@AdminField`, `@AdminFieldset`, `@AdminModel`, and `@AdminPackage`, allow you to specify a `label` attribute.
This label can be either a hard-coded string or a localization key.  
Using a localization key allows you to provide translated labels for different languages, making your admin interface accessible to users from various locales.

## History
Spring JPA Admin keeps a history log of changes made through the admin site: every create, edit, delete,
and custom bulk action is recorded along with the acting username and the affected entity's id and
representation. The most recent entries appear in the "History" sidebar of the admin dashboard;
entries for models the current user isn't allowed to view are hidden.

Entries are stored in the `admin_history_log` table, mapped by the `AdminHistoryLog` entity shipped with
the library. The entity is registered automatically; if your application declares its own `@EntityScan`,
add `com.pocketcombats.admin.history` to the scanned packages. The table itself is created by whatever
manages your schema (Hibernate DDL or a migration tool).

Two properties control the feature:
- `spring.jpa-admin.history-size` (default: `10`) — how many recent entries are fetched for the dashboard. The permission filter above is applied afterwards, so fewer may actually be shown.
- `spring.jpa-admin.disable-history` (default: `false`) — set to `true` to disable recording and hide the dashboard sidebar.

## Authentication Plugin
Spring JPA Admin provides an optional authentication plugin for applications that don't otherwise require authentication.

### Installation
To use the authentication plugin, add the following dependency:

#### Maven
```xml
<dependency>
    <groupId>com.pocketcombats.spring-jpa-admin.plugin</groupId>
    <artifactId>auth</artifactId>
    <version>${spring-jpa-admin.version}</version>
    <scope>runtime</scope>
</dependency>
```

#### Gradle
```kotlin
runtimeOnly("com.pocketcombats.spring-jpa-admin.plugin:auth")
```

### Configuration
The authentication plugin can be configured using properties in your `application.yaml` file:

```yaml
spring.jpa-admin:
  auth:
    password-strength: 10  # BCrypt strength (default: 10)
    create-default-admin: true  # Whether to create a default admin user (default: false). DO NOT enable this in production!
```

## License
Spring JPA Admin is released under the Apache License 2.0. See the LICENSE file for details.
