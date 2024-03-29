package com.nibokapp.nibok.data.repository.server

import android.util.Log
import com.baasbox.android.BaasBox
import com.baasbox.android.BaasDocument
import com.baasbox.android.BaasUser
import com.baasbox.android.Rest
import com.baasbox.android.json.JsonObject
import com.nibokapp.nibok.data.db.Conversation
import com.nibokapp.nibok.data.db.Message
import com.nibokapp.nibok.data.repository.common.ConversationRepositoryInterface
import com.nibokapp.nibok.data.repository.server.common.ServerCollection
import com.nibokapp.nibok.data.repository.server.common.ServerConstants
import com.nibokapp.nibok.extension.toJsonArray
import com.nibokapp.nibok.server.fetch.ServerDataFetcher
import com.nibokapp.nibok.server.fetch.common.ServerDataFetcherInterface
import com.nibokapp.nibok.server.mapper.ServerDataMapper
import com.nibokapp.nibok.server.mapper.common.ServerDataMapperInterface
import com.nibokapp.nibok.server.send.ServerDataSender
import com.nibokapp.nibok.server.send.common.ServerDataSenderInterface

/**
 * Server repository for conversations.
 */
object ServerConversationRepository : ConversationRepositoryInterface {

    private const val TAG = "ServerConversationRepo"

    // Fetcher and sender used to exchange conversation data with the server
    private val fetcher: ServerDataFetcherInterface = ServerDataFetcher()
    private val sender: ServerDataSenderInterface = ServerDataSender()

    // Mapper used to map data exchanged with the server
    private val mapper: ServerDataMapperInterface = ServerDataMapper()

    // Current logged in user
    private val currentUser: BaasUser?
        get() = BaasUser.current()

    // Caches
    private var conversationCache: List<Conversation> = emptyList()

    override fun getConversationById(conversationId: String): Conversation? {
        Log.d(TAG, "Getting conversation by id: $conversationId")
        val result = fetcher.fetchConversationDocumentById(conversationId)
        return result?.let { mapper.convertDocumentToConversation(it) }
    }

    override fun getConversationPartnerName(conversationId: String): String? {
        Log.d(TAG, "Getting partner's name for conversation: $conversationId")
        val conversation = getConversationById(conversationId)
        return conversation?.partner?.username
    }

    override fun getConversationPreviewText(conversationId: String): String? {
        Log.d(TAG, "Getting preview text for conversation: $conversationId")
        val message = fetcher.fetchLatestMessageByConversation(conversationId)
        return message?.getString(ServerConstants.TEXT)
    }

    override fun getConversationListFromQuery(query: String): List<Conversation> {
        Log.d(TAG, "Getting conversation list for query: $query")
        val conversations = fetcher.fetchConversationDocumentListByQuery(query).toConversationList()
        Log.d(TAG, "Found ${conversations.size} conversations corresponding to query: $query")
        return conversations
    }

    override fun getConversationList(cached: Boolean): List<Conversation> {
        if (cached) return conversationCache
        conversationCache = fetcher.fetchRecentConversationDocumentList().toConversationList()
        Log.d(TAG, "Found ${conversationCache.size} recent conversations")
        return conversationCache
    }

    override fun getConversationListOlderThanConversation(conversationId: String): List<Conversation> {
        val conversations = fetcher.fetchConversationDocumentListOlderThanConversation(conversationId)
                .toConversationList()
        Log.d(TAG, "Found ${conversations.size} older than $conversationId")
        return conversations
    }

    override fun startConversation(partnerId: String): String? {
        val user = currentUser ?: return null

        if (user.name == partnerId) return null

        return fetchConversationIdByParticipants(user.name, partnerId)
                ?: startConversationOnServer(user.name, partnerId)
    }

    override fun getMessageListForConversation(conversationId: String): List<Message> {
        Log.d(TAG, "Getting messages for conversation: $conversationId")
        val messages = fetcher.fetchMessageDocumentListByConversation(conversationId).toMessageList()
        Log.d(TAG, "Found ${messages.size} messages for conversation: $conversationId")
        return messages
    }

    override fun getMessageListBeforeDateOfMessage(messageId: String): List<Message> {
        Log.d(TAG, "Getting messages older than: $messageId")
        val messages = fetcher.fetchMessageDocumentListBeforeDateOfMessage(messageId).toMessageList()
        Log.d(TAG, "Found ${messages.size} messages older than message: $messageId")
        return messages
    }

    override fun getMessageListAfterDateOfMessage(messageId: String): List<Message> {
        Log.d(TAG, "Getting messages newer than: $messageId")
        val messages = fetcher.fetchMessageDocumentListAfterDateOfMessage(messageId).toMessageList()
        Log.d(TAG, "Found ${messages.size} messages newer than message: $messageId")
        return messages
    }

    override fun sendMessage(message: Message): String? {
        val senderId = currentUser?.name ?: return null

        val conversationDocument = fetcher.fetchConversationDocumentById(message.conversationId)
                ?: return null
        val participants = conversationDocument.getArray(ServerConstants.PARTICIPANTS)
                ?: return null
        val recipientId = participants.filterIsInstance<String>().find { it != senderId }
                ?: return null

        Log.d(TAG, "Sending message: ${message.text} in conversation: ${message.conversationId} to: $recipientId")
        val messageDocument = getMessageDocument(message)
        val messageId = sender.sendMessageDocument(messageDocument, recipientId) ?: return null
        val messageDate = messageDocument.creationDate
        Log.d(TAG, "Message: $messageId sent, creation_date: $messageDate")
        conversationDocument.updateConversationDate(messageDate)
        return messageId
    }

    /*
     * EXTRA
     */

    private fun List<BaasDocument>.toConversationList(): List<Conversation> {
        return mapper.convertDocumentListToConversations(this)
    }

    private fun List<BaasDocument>.toMessageList(): List<Message> {
        return mapper.convertDocumentListToMessages(this)
    }

    private fun getConversationDocument(participantIds: List<String>): BaasDocument {
        val document = BaasDocument(ServerCollection.CONVERSATIONS.id)
        with(ServerConstants) {
            document.put(PARTICIPANTS, participantIds.toJsonArray())
                    .put(LAST_UPDATE_DATE, "")
        }
        return document
    }

    private fun getMessageDocument(message: Message): BaasDocument {
        val document = BaasDocument(ServerCollection.MESSAGES.id)
        with(ServerConstants) {
            document.put(CONVERSATION_ID, message.conversationId)
                    .put(SENDER_ID, message.senderId)
                    .put(TEXT, message.text)
        }
        return document
    }

    private fun fetchConversationIdByParticipants(localUserId: String, partnerId: String): String? {
        return fetcher.fetchConversationDocumentByParticipants(localUserId, partnerId)?.id
    }

    private fun startConversationOnServer(localUserId: String, partnerId: String): String? {
        val participants = listOf(localUserId, partnerId)
        val document = getConversationDocument(participants)
        return sender.sendConversationDocument(document, partnerId)
    }

    private fun BaasDocument.updateConversationDate(newDate: String): Boolean {
        Log.d(TAG, "Updating conversation date for: ${this.id} to: $newDate")
        return this.updateField(ServerConstants.LAST_UPDATE_DATE, newDate)
    }

    private fun BaasDocument.updateField(fieldName: String, fieldData: String): Boolean {
        val data = JsonObject().put("data", fieldData)
        val endpoint = "document/${this.collection}/${this.id}/.$fieldName"
        val updated = sendUpdateRequest(endpoint, data)
        return updated
    }

    private fun sendUpdateRequest(endpoint: String, data: JsonObject): Boolean {
        return BaasBox.rest().sync(
                Rest.Method.PUT,
                endpoint,
                data
        ).isSuccess
    }
}