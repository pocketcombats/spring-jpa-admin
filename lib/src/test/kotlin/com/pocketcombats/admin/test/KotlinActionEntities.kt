package com.pocketcombats.admin.test

import com.pocketcombats.admin.AdminAction

/** The idiomatic Kotlin shape: bulk actions declared in the default companion object. */
class KotlinActionEntity {

    var archived: Boolean = false
    var published: Boolean = false

    companion object {

        @AdminAction
        fun archive(entities: List<KotlinActionEntity>) {
            entities.forEach { it.archived = true }
        }

        @JvmStatic
        @AdminAction
        fun publish(entities: List<KotlinActionEntity>) {
            entities.forEach { it.published = true }
        }
    }
}

/** A named companion compiles to a nested class and a static field named after the companion. */
class KotlinNamedCompanionEntity {

    var archived: Boolean = false

    companion object Actions {

        @AdminAction
        fun archive(entities: List<KotlinNamedCompanionEntity>) {
            entities.forEach { it.archived = true }
        }
    }
}

/** A member function compiles to an instance method and cannot back an entity-level action. */
class KotlinMemberActionEntity {

    @AdminAction
    fun archive(entities: List<KotlinMemberActionEntity>) = Unit
}
