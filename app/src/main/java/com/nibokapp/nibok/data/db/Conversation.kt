package com.nibokapp.nibok.data.db

import com.nibokapp.nibok.data.db.common.WellFormedItem
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*

/**
 * Model class for a conversation between the local user and an external user (partner).
 * Used by the local database.
 *
 * @param id the id of the conversation
 * @param userId the id of the local user
 * @param partner the external user participating in the conversation
 * @param latestMessage the latest message exchanged in the conversation
 * @param lastUpdateDate the date in which the conversation was last updated
 */
open class Conversation(

        @PrimaryKey open var id: String = "",

        open var userId: String = "",

        open var partner: ExternalUser? = null,

        open var latestMessage: Message? = null,

        open var lastUpdateDate: Date? = null

) : RealmObject(), WellFormedItem {

    override fun isWellFormed(): Boolean = with(this) {
        partner != null && lastUpdateDate != null && userId != ""
    }
}
