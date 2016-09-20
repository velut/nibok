package com.nibokapp.nibok.extension

import com.nibokapp.nibok.data.db.Insertion
import com.nibokapp.nibok.data.repository.BookInsertionRepository
import com.nibokapp.nibok.data.repository.UserRepository
import com.nibokapp.nibok.data.repository.common.UserRepositoryInterface

/**
 * Extensions for the base repository
 */

/**
 * Given a list of sources return the result from the first source that provides it.
 *
 * @param predicate the way in which to retrieve the result
 *
 * @return a result or null if no result was found
 */
inline fun <T, R: Any> Iterable<T>.firstResultOrNull(predicate: (T) -> R?) : R? {
    for (element in this) {
        val result = predicate(element)
        if (result != null) return result
    }
    return null
}

/**
 * When a local user exists exclude his insertions from this list.
 */
fun List<Insertion>.excludeUserOwnInsertions() : List<Insertion> {
    val userRepository: UserRepositoryInterface = UserRepository
    if (!userRepository.localUserExists()) {
        return this
    } else {
        val userId = userRepository.getLocalUserId()
        return this.filter { it.seller?.username != userId }
    }
}

/**
 * When a local user exists include only his insertions from this list.
 */
fun List<Insertion>.includeOnlyUserOwnInsertions() : List<Insertion> {
    val userRepository: UserRepositoryInterface = UserRepository
    if (!userRepository.localUserExists()) {
        return this
    } else {
        val userId = userRepository.getLocalUserId()
        return this.filter { it.seller?.username == userId }
    }
}

/**
 * Include only saved insertions from this list.
 */
fun List<Insertion>.includeOnlySavedInsertions() : List<Insertion> {
    val savedInsertions = BookInsertionRepository.getSavedBookInsertionList()
    return this.filter { it.id in savedInsertions.map { it.id } }
}