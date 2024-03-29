package com.nibokapp.nibok.data.repository.db

import android.util.Log
import com.nibokapp.nibok.data.db.Book
import com.nibokapp.nibok.data.db.Insertion
import com.nibokapp.nibok.data.db.User
import com.nibokapp.nibok.data.repository.UserRepository
import com.nibokapp.nibok.data.repository.common.BookInsertionRepositoryInterface
import com.nibokapp.nibok.data.repository.common.UserRepositoryInterface
import com.nibokapp.nibok.data.repository.db.common.LocalStorage
import com.nibokapp.nibok.data.repository.server.ServerBookInsertionRepository
import com.nibokapp.nibok.extension.*
import io.realm.Case
import io.realm.Realm
import io.realm.RealmQuery

/**
 * Local repository for book insertions.
 */
object LocalBookInsertionRepository : BookInsertionRepositoryInterface, LocalStorage<Insertion> {

    const private val TAG = "LocalBookInsertionRepo"

    private val userRepository: UserRepositoryInterface = UserRepository
    private val serverRepository: BookInsertionRepositoryInterface = ServerBookInsertionRepository

    private var feedCache: List<Insertion> = emptyList()
    private var savedCache: List<Insertion> = emptyList()
    private var publishedCache: List<Insertion> = emptyList()

    /*
     * Common functions
     */

    override fun getInsertionById(insertionId: String): Insertion? {
        val insertion = queryOneRealm {
            it.whereInsertion()
                    .idEqualTo(insertionId)
                    .findFirst()
        }
        Log.d(TAG, "For id: $insertionId found: $insertion")
        return insertion
    }

    override fun getBookByISBN(isbn: String): Book? {
        val book = queryOneRealm {
            it.where(Book::class.java)
                    .equalTo("isbn", isbn)
                    .findFirst()
        }
        Log.d(TAG, "For ISBN: $isbn found book: $book")
        return book
    }

    override fun getInsertionListFromQuery(query: String): List<Insertion> {
        val insertions = getInsertionListFromQueryWithFilter(query)
        Log.d(TAG, "Insertions corresponding to query '$query': ${insertions.size}")
        return insertions
    }

    /*
     * Feed insertions
     */

    override fun getFeedInsertionList(cached: Boolean): List<Insertion> {
        if (cached) return feedCache
        feedCache = getInsertionList(excludeByLocalUser = true)
        Log.d(TAG, "Found ${feedCache.size} feed insertions")
        return feedCache
    }

    override fun getFeedInsertionListFromQuery(query: String): List<Insertion> {
        val insertions = getInsertionListFromQueryWithFilter(query, excludeByLocalUser = true)
        Log.d(TAG, "Feed insertions corresponding to query '$query': ${insertions.size}")
        return insertions
    }

    override fun getFeedInsertionListOlderThanInsertion(insertionId: String): List<Insertion> {
        val olderInsertions = getOlderInsertionList(insertionId, excludeByLocalUser = true)
        Log.d(TAG, "Found ${olderInsertions.size} feed insertions older than: $insertionId")
        return olderInsertions
    }

    /*
     * Saved insertions
     */

    override fun getSavedInsertionList(cached: Boolean): List<Insertion> {
        if (cached) return savedCache
        savedCache = getInsertionList(includeOnlyIfSaved = true)
        Log.d(TAG, "Found ${savedCache.size} saved insertions")
        return savedCache
    }

    override fun getSavedInsertionListFromQuery(query: String): List<Insertion> {
        val insertions = getInsertionListFromQueryWithFilter(query, includeOnlyIfSaved = true)
        Log.d(TAG, "Saved insertions corresponding to query '$query': ${insertions.size}")
        return insertions
    }

    override fun getSavedInsertionListOlderThanInsertion(insertionId: String): List<Insertion> {
        val olderInsertions = getOlderInsertionList(insertionId, includeOnlyIfSaved = true)
        Log.d(TAG, "Found ${olderInsertions.size} saved insertions older than: $insertionId")
        return olderInsertions
    }

    /*
     * Published insertions
     */

    override fun getPublishedInsertionList(cached: Boolean): List<Insertion> {
        if (cached) return publishedCache
        publishedCache = getInsertionList(includeOnlyByLocalUser = true)
        Log.d(TAG, "Found ${publishedCache.size} published insertions")
        return publishedCache
    }

    override fun getPublishedInsertionListFromQuery(query: String): List<Insertion> {
        val insertions = getInsertionListFromQueryWithFilter(query, includeOnlyByLocalUser = true)
        Log.d(TAG, "Published insertions corresponding to query '$query': ${insertions.size}")
        return insertions
    }

    override fun getPublishedInsertionListOlderThanInsertion(insertionId: String): List<Insertion> {
        val olderInsertions = getOlderInsertionList(insertionId, includeOnlyByLocalUser = true)
        Log.d(TAG, "Found ${olderInsertions.size} published insertions older than: $insertionId")
        return olderInsertions
    }

    override fun deletePublishedInsertion(insertionId: String): Boolean {
        executeRealmTransaction {
            val insertion = it.whereInsertion().idEqualTo(insertionId).findAll()
            insertion.deleteAllFromRealm()
            Log.d(TAG, "Deleted insertion: $insertionId from realm")
        }
        return getInsertionById(insertionId) == null
    }

    /*
     * Insertion's save status
     */

    override fun isBookInsertionSaved(insertionId: String): Boolean {
        val isSaved = insertionId in getSavedInsertionList().map { it.id }
        Log.d(TAG, "Insertion: $insertionId is saved: $isSaved")
        return isSaved
    }

    override fun toggleInsertionSaveStatus(insertionId: String): Boolean {
        throw UnsupportedOperationException()
    }

    fun setInsertionSaveStatus(insertionId: String, isSaved: Boolean) {
        if (!localUserExists()) return
        Log.d(TAG, "Setting insertion: $insertionId save status to: $isSaved")
        executeRealmTransaction {
            changeSaveStatus(insertionId, isSaved, it)
        }
    }

    private fun changeSaveStatus(insertionId: String, isSaved: Boolean, realm: Realm, localUser: User? = null) {
        val user = localUser ?: realm.where(User::class.java).findFirst()
        if (user == null) {
            Log.d(TAG, "Local user is null, skipping change status!")
            return
        }
        if (isSaved && !isBookInsertionSaved(insertionId)) {
            Log.d(TAG, "Saving insertion: $insertionId")
            user.savedInsertionsIds.add(insertionId.toRealmString())
        } else if (!isSaved && isBookInsertionSaved(insertionId)) {
            val toRemovePos = user.savedInsertionsIds.indexOfFirst { it.value == insertionId }
            if (toRemovePos != -1) {
                val obj = user.savedInsertionsIds.removeAt(toRemovePos)
                Log.d(TAG, "Removed saved insertion: ${obj.value}")
            }
        } else {
            Log.d(TAG, "Not updating insertion save status")
        }
    }

    /*
     * Insertion publishing
     */

    override fun publishInsertion(insertion: Insertion): String? {
        throw UnsupportedOperationException()
    }

    /*
     * Local storage
     */

    override fun storeItems(items: List<Insertion>) {
        if (items.isEmpty()) return
        val ids = items.map { it.id }
        executeRealmTransaction {
            realm ->
            Log.d(TAG, "Storing insertions: $ids")
            realm.copyToRealmOrUpdate(items)

            Log.d(TAG, "Updating on storage save status for insertions: $ids")
            val user = realm.where(User::class.java).findFirst()
            ids.forEach {
                insertionId ->
                val isSaved = serverRepository.isBookInsertionSaved(insertionId)
                changeSaveStatus(insertionId, isSaved, realm, user)
            }
        }
    }

    override fun storeItem(item: Insertion) {
        val insertionId = item.id
        executeRealmTransaction {
            Log.d(TAG, "Storing insertion: $insertionId")
            it.copyToRealmOrUpdate(item)

            Log.d(TAG, "Updating on storage save status for insertion: $insertionId")
            val isSaved = serverRepository.isBookInsertionSaved(insertionId)
            changeSaveStatus(insertionId, isSaved, it)
        }
    }

    /*
     * Utilities
     */

    private fun localUserExists(): Boolean {
        return userRepository.localUserExists()
    }

    private fun getLocalUserId(): String {
        return userRepository.getLocalUserId()
    }

    /*
     * General querying with filtering
     */

    private fun getInsertionList(excludeByLocalUser: Boolean = false,
                                 includeOnlyIfSaved: Boolean = false,
                                 includeOnlyByLocalUser: Boolean = false): List<Insertion> {
        return queryManyRealm {
            it.whereInsertion()
                    .applyFiltering(excludeByLocalUser, includeOnlyIfSaved, includeOnlyByLocalUser)
                    .findAllSortedByDescendingDate()
        }
    }

    private fun getInsertionListFromQueryWithFilter(query: String,
                                                    excludeByLocalUser: Boolean = false,
                                                    includeOnlyIfSaved: Boolean = false,
                                                    includeOnlyByLocalUser: Boolean = false): List<Insertion> {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return emptyList()

        return queryManyRealm {
            it.queryInsertion(trimmedQuery)
                    .applyFiltering(excludeByLocalUser, includeOnlyIfSaved, includeOnlyByLocalUser)
                    .findAllSortedByDescendingDate()
        }
    }

    private fun getOlderInsertionList(insertionId: String,
                                      excludeByLocalUser: Boolean = false,
                                      includeOnlyIfSaved: Boolean = false,
                                      includeOnlyByLocalUser: Boolean = false): List<Insertion> {
        val currentOldestDate = getInsertionById(insertionId)?.date ?: return emptyList()
        return queryManyRealm {
            it.whereInsertion()
                    .olderThan(insertionId, currentOldestDate)
                    .applyFiltering(excludeByLocalUser, includeOnlyIfSaved, includeOnlyByLocalUser)
                    .findAllSortedByDescendingDate()
        }
    }

    /*
     * Extensions
     */

    private fun Realm.whereInsertion(): RealmQuery<Insertion> {
        return this.where(Insertion::class.java)
    }

    private fun Realm.queryInsertion(query: String): RealmQuery<Insertion> {
        return this.whereInsertion()
                .beginGroup()
                .contains("book.title", query, Case.INSENSITIVE)
                .or()
                .contains("book.authors.value", query, Case.INSENSITIVE)
                .or()
                .contains("book.publisher", query, Case.INSENSITIVE)
                .or()
                .contains("book.isbn", query, Case.INSENSITIVE)
                .endGroup()
    }

    private fun RealmQuery<Insertion>.excludeByLocalUser(userId: String): RealmQuery<Insertion> {
        return this.notEqualTo("seller.username", userId)
    }

    private fun RealmQuery<Insertion>.excludeByLocalUserIfPossible(): RealmQuery<Insertion> {
        return if (localUserExists()) {
            this.excludeByLocalUser(getLocalUserId())
        } else {
            this
        }
    }

    private fun RealmQuery<Insertion>.whereIdInList(idList: List<String>): RealmQuery<Insertion> {
        val list = if (idList.isNotEmpty()) {
            Log.d(TAG, "Searching among ids: $idList")
            idList
        } else {
            Log.d(TAG, "Fake NO_ID list")
            listOf("NO_ID")
        }
        return this.`in`("id", list.toTypedArray())
    }

    private fun RealmQuery<Insertion>.includeOnlySavedIfPossible(): RealmQuery<Insertion> {
        val localUser = userRepository.getLocalUser()
        val savedInsertionIds = localUser?.savedInsertionsIds?.toStringList() ?: emptyList()
        return this.whereIdInList(savedInsertionIds)
    }

    private fun RealmQuery<Insertion>.includeOnlyByLocalUser(userId: String): RealmQuery<Insertion> {
        return this.equalTo("seller.username", userId)
    }

    private fun RealmQuery<Insertion>.includeOnlyByLocalUserIfPossible(): RealmQuery<Insertion> {
        if (!localUserExists()) return this.whereIdInList(emptyList())
        val publishedInsertionIds = queryManyRealm {
            it.whereInsertion()
                    .includeOnlyByLocalUser(getLocalUserId())
                    .findAll()
        }.map { it.id }
        return this.whereIdInList(publishedInsertionIds)
    }

    private fun RealmQuery<Insertion>.applyFiltering(excludeByLocalUser: Boolean,
                                                     includeOnlyIfSaved: Boolean,
                                                     includeOnlyByLocalUser: Boolean): RealmQuery<Insertion> {
        return if (excludeByLocalUser) {
            this.excludeByLocalUserIfPossible()
        } else if (includeOnlyIfSaved) {
            this.includeOnlySavedIfPossible()
        } else if (includeOnlyByLocalUser) {
            this.includeOnlyByLocalUserIfPossible()
        } else {
            this
        }
    }

}
