package org.anymodel.chat.chat.domain

enum class Role(val wire: String) {
    USER("user"),
    ASSISTANT("assistant")
}

data class ChatMessage(
    val id: Long,
    val role: Role,
    val content: String,
    val isStreaming: Boolean = false
)
