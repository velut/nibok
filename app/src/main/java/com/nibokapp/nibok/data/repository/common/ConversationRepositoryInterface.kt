package com.nibokapp.nibok.data.repository.common

import com.nibokapp.nibok.data.db.Conversation
import com.nibokapp.nibok.data.db.Message
import java.util.*

/**
 * Interface for conversation repositories.
 */
interface ConversationRepositoryInterface {

    /**
     * Get the conversation with the given id.
     *
     * @param conversationId the id of the conversation
     *
     * @return the conversation with the given id or null if no such conversation was found
     */
    fun getConversationById(conversationId: String): Conversation?

    /**
     * Get the partner's name for the conversation with the given id.
     *
     * @param conversationId the id of the conversation
     *
     * @return the partner's name for the conversation with the given id
     * or null if no such conversation was found
     */
    fun getConversationPartnerName(conversationId: String): String?

    /**
     * Get the list of conversations matching the given query.
     * The query can match on the following  attributes:
     *  Partner name, messages content
     *
     *  @param query the string describing the query
     *
     *  @return the list of conversations matching the query
     */
    fun getConversationListFromQuery(query: String): List<Conversation>

    /**
     * Get the current list of conversations to display.
     *
     * @param cached set to true to return cached data. Default is false
     *
     * @return the list of currently available conversations
     */
    fun getConversationList(cached: Boolean = false): List<Conversation>

    /**
     * Get the list of conversations with a date greater or equal to the given date.
     *
     * @param date the date used in comparisons
     *
     *  @return the list of conversations with a date greater or equal to the given date
     */
    fun getConversationListAfterDate(date: Date): List<Conversation>

    /**
     * Get the list of conversations with a date smaller or equal to the given date.
     *
     * @param date the date used in comparisons
     *
     *  @return the list of conversations with a date smaller or equal to the given date
     */
    fun getConversationListBeforeDate(date: Date): List<Conversation>

    /**
     * Start a conversation with the user with the given id.
     *
     * @param partnerId the id of the user with which the conversation is held
     *
     * @return the started conversation's id or null if the conversation could not be started
     */
    fun startConversation(partnerId: String): String?

    /**
     * Get the current list of messages in the conversation with the given id.
     *
     * @param conversationId the id of the conversation
     *
     * @return the list of currently available messages in the conversation
     */
    fun getMessageListForConversation(conversationId: String): List<Message>

    /**
     * Get the list of messages that are newer than the given message.
     *
     * @param messageId the id of the message
     *
     * @return the list messages newer than the given message
     */
    fun getMessageListAfterDateOfMessage(messageId: String): List<Message>

    /**
     * Get the list of messages in the conversation with the given id that are dated before the
     * given date.
     *
     * @param conversationId the id of the conversation
     * @param date the date used in comparisons
     * @return the list messages in the conversation dated before the given date
     */
    fun getMessageListBeforeDateForConversation(conversationId: String, date: Date): List<Message>

    /**
     * Send the given message.
     *
     * @param message the message to send
     *
     * @return true if the message was sent successfully, false otherwise
     */
    fun sendMessage(message: Message): Boolean

}
