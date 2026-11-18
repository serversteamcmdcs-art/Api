package org.anymodel.chat.chat.domain

import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    /**
     * Поток текстовых дельт ответа ассистента.
     * Бросает исключения сети/HTTP — маппинг в текст делает ApiErrors.
     */
    fun streamReply(
        history: List<ChatMessage>,
        model: String,
        systemPrompt: String,
        temperature: Float,
        maxTokens: Int?
    ): Flow<String>
}
