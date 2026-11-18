package org.anymodel.chat.chat.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import org.anymodel.chat.chat.domain.ChatMessage
import org.anymodel.chat.chat.domain.ChatRepository
import org.anymodel.chat.core.network.ChatMessageDto
import org.anymodel.chat.core.network.ChatRequestDto
import org.anymodel.chat.core.network.ChatStreamer

@Singleton
class ApiChatRepository @Inject constructor(
    private val streamer: ChatStreamer
) : ChatRepository {

    override fun streamReply(
        history: List<ChatMessage>,
        model: String,
        systemPrompt: String,
        temperature: Float,
        maxTokens: Int?
    ): Flow<String> {
        val messages = buildList {
            if (systemPrompt.isNotBlank()) add(ChatMessageDto(role = "system", content = systemPrompt.trim()))
            history
                .filter { it.content.isNotBlank() }
                .forEach { add(ChatMessageDto(role = it.role.wire, content = it.content)) }
        }
        return streamer.stream(
            ChatRequestDto(
                model = model,
                messages = messages,
                stream = true,
                temperature = temperature,
                maxTokens = maxTokens
            )
        )
    }
}
