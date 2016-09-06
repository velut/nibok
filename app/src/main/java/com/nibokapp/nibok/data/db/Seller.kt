package com.nibokapp.nibok.data.db

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

/**
 * Model class for the users that published insertions to sell books.
 * Used by the local database.
 *
 * @param id the id of the seller
 * @param name the display name of the seller
 */
open class Seller(

        @PrimaryKey open var id: Long = 0,

        open var name: String = ""

) : RealmObject() {}
