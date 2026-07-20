package com.pocketcombats.admin.test

import com.pocketcombats.admin.AdminField
import jakarta.persistence.Entity
import jakarta.persistence.Id

/**
 * Property (getter) access: `@get:Id` puts the mapping annotation on the getter, so the JPA
 * metamodel reports getters as attribute members, while a bare `@AdminField` on a property
 * still annotates the backing field.
 */
@Entity(name = "KotlinPropertyAccessPost")
class KotlinPropertyAccessPost {

    @get:Id
    var id: Int? = null

    /** Bare use-site: lands on the backing field, invisible through the getter member. */
    @AdminField(label = "Field-targeted")
    var title: String = ""

    /** Explicit getter use-site: matches the member the metamodel reports. */
    @get:AdminField(label = "Getter-targeted")
    var subtitle: String = ""

    /** An `is`-prefixed property derives attribute "active" while its backing field stays "isActive". */
    @AdminField(label = "Boolean field-targeted")
    var isActive: Boolean = false
}

/**
 * Field access: the metamodel reports backing fields, so a getter-targeted `@AdminField`
 * is the annotation that needs the paired-member fallback.
 */
@Entity(name = "KotlinFieldAccessPost")
class KotlinFieldAccessPost {

    @Id
    var id: Int? = null

    @get:AdminField(label = "Getter-targeted")
    var name: String = ""

    @AdminField(label = "Field-targeted")
    var code: String = ""
}
