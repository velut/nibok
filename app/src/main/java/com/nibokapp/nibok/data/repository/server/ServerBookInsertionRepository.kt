package com.nibokapp.nibok.data.repository.server

import android.util.Log
import com.nibokapp.nibok.data.db.Book
import com.nibokapp.nibok.data.db.Insertion
import com.nibokapp.nibok.data.repository.UserRepository
import com.nibokapp.nibok.data.repository.common.BookInsertionRepositoryInterface
import com.nibokapp.nibok.data.repository.common.UserRepositoryInterface
import com.nibokapp.nibok.extension.*
import io.realm.Case
import java.util.*

/**
 * Server repository for book insertions.
 */
object ServerBookInsertionRepository: BookInsertionRepositoryInterface {

    const private val TAG = "BookInsertionRepository"

    private val userRepository : UserRepositoryInterface = UserRepository

    private var feedCache : List<Insertion> = emptyList()
    private var savedCache : List<Insertion> = emptyList()
    private var publishedCache : List<Insertion> = emptyList()

    /*
     * COMMON FUNCTIONS
     */

    override fun getBookInsertionById(insertionId: String) : Insertion? = queryOneWithRealm {
        it.where(Insertion::class.java)
                .equalTo("id", insertionId)
                .findFirst()
    }

    override fun getBookByISBN(isbn: String): Book? = queryOneWithRealm {
        it.where(Book::class.java)
                .equalTo("isbn", isbn)
                .findFirst()
    }

    override fun getBookInsertionListFromQuery(query: String) : List<Insertion> {

        val trimmedQuery = query.trim()

        if (trimmedQuery.isEmpty()) return emptyList()

        val results = queryRealm {
            it.where(Insertion::class.java)
                    .contains("book.title", trimmedQuery, Case.INSENSITIVE)
                    .or()
                    .contains("book.authors.value", trimmedQuery, Case.INSENSITIVE)
                    .or()
                    .contains("book.publisher", trimmedQuery, Case.INSENSITIVE)
                    .or()
                    .contains("book.isbn", trimmedQuery, Case.INSENSITIVE)
                    .findAll()
        }
        Log.d(TAG, "Book insertions corresponding to query '$query' = ${results.size}")
        return results
    }

    override fun getBookInsertionListAfterDate(date: Date) : List<Insertion> {
        val results = queryRealm {
            it.where(Insertion::class.java)
                    .greaterThanOrEqualTo("date", date)
                    .findAll()}
        Log.d(TAG, "Found ${results.size} insertions after $date")
        return results
    }

    override fun getBookInsertionListBeforeDate(date: Date) : List<Insertion> {
        val results = queryRealm {
            it.where(Insertion::class.java)
                    .lessThanOrEqualTo("date", date)
                    .findAll()}
        Log.d(TAG, "Found ${results.size} insertions before $date")
        return results
    }

    /*
     * FEED BOOK INSERTIONS
     */

    override fun getFeedBookInsertionList(cached: Boolean): List<Insertion> {
        if (cached) return feedCache
        feedCache = queryRealm { it.where(Insertion::class.java).findAll()}
                .excludeUserOwnInsertions()
        Log.d(TAG, "Found ${feedCache.size} feed insertions")
        return feedCache
    }

    override fun getFeedBookInsertionListFromQuery(query: String) : List<Insertion>  =
            getBookInsertionListFromQuery(query).excludeUserOwnInsertions()

    override fun getFeedBookInsertionListAfterDate(date: Date) : List<Insertion> =
            getBookInsertionListAfterDate(date).excludeUserOwnInsertions()

    override fun getFeedBookInsertionListBeforeDate(date: Date) : List<Insertion> =
            getBookInsertionListBeforeDate(date).excludeUserOwnInsertions()

    /*
     * SAVED BOOK INSERTIONS
     */

    override fun getSavedBookInsertionList(cached: Boolean) : List<Insertion> {
        if (cached) return savedCache
        savedCache = userRepository.getLocalUser()?.savedInsertions?.toNormalList() ?: emptyList()
        return savedCache
    }

    override fun getSavedBookInsertionListFromQuery(query: String) : List<Insertion> =
            getBookInsertionListFromQuery(query).includeOnlySavedInsertions()

    override fun getSavedBookInsertionLisAfterDate(date: Date) : List<Insertion> =
            getBookInsertionListAfterDate(date).includeOnlySavedInsertions()

    override fun getSavedBookInsertionListBeforeDate(date: Date) : List<Insertion> =
            getBookInsertionListBeforeDate(date).includeOnlySavedInsertions()

    /*
     * PUBLISHED BOOK INSERTIONS
     */

    override fun getPublishedBookInsertionList(cached: Boolean) : List<Insertion> {
        if (cached) return publishedCache
        publishedCache =
                userRepository.getLocalUser()?.publishedInsertions?.toNormalList() ?: emptyList()
        return publishedCache
    }

    override fun getPublishedBookInsertionListFromQuery(query: String) : List<Insertion> =
            getBookInsertionListFromQuery(query).includeOnlyUserOwnInsertions()

    override fun getPublishedBookInsertionListAfterDate(date: Date) : List<Insertion> =
            getBookInsertionListAfterDate(date).includeOnlyUserOwnInsertions()

    override fun getPublishedBookInsertionListBeforeDate(date: Date) : List<Insertion> =
            getBookInsertionListBeforeDate(date).includeOnlyUserOwnInsertions()

    /*
     * BOOK INSERTION SAVE STATUS
     */

    override fun isBookInsertionSaved(insertionId: String) : Boolean =
            insertionId in getSavedBookInsertionList().map { it.id }

    override fun toggleBookInsertionSaveStatus(insertionId: String) : Boolean {

        if (!userRepository.localUserExists())
            throw IllegalStateException("Local user does not exist. Cannot save insertion")

        var saved = false

        withRealm {
            val insertion = it.getBookInsertionById(insertionId)
            val user = it.getLocalUser()
            val savedInsertions = user!!.savedInsertions

            saved = insertion in savedInsertions

            it.executeTransaction {
                if (!saved) {
                    savedInsertions.add(0, insertion)
                } else {
                    savedInsertions.remove(insertion)
                }
            }
            saved = insertion in savedInsertions
            Log.d(TAG, "After toggle: Save status: $saved, saved size: ${savedInsertions.size}")
        }
        return saved
    }

    /*
     * BOOK INSERTION PUBLISHING
     */

    override fun publishBookInsertion(insertion: Insertion) : Boolean =
            // TODO
            throw UnsupportedOperationException()


    private fun List<Insertion>.excludeUserOwnInsertions() : List<Insertion> {
        if (!userRepository.localUserExists()) {
            return this
        } else {
            val userId = userRepository.getLocalUserId()
            return this.filter { it.seller?.username != userId }
        }
    }

    private fun List<Insertion>.includeOnlyUserOwnInsertions() : List<Insertion> {
        if (!userRepository.localUserExists()) {
            return this
        } else {
            val userId = userRepository.getLocalUserId()
            return this.filter { it.seller?.username == userId }
        }
    }

    private fun List<Insertion>.includeOnlySavedInsertions() : List<Insertion> {
        val savedInsertions = getSavedBookInsertionList()
        return this.filter { it.id in savedInsertions.map { it.id } }
    }
}
