package org.anymodel.chat.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Поля без значений по умолчанию кодируются всегда (Json.encodeDefaults = false),
// null-поля (temperature, max_tokens) пропускаются благодаря explicitNulls = false.
@Serializable
data class ChatRequestDto(
    val model: String,
    val messages: List<ChatMessageDto>,
    val stream: Boolean,
    val temperature: Float? = null,
    @SerialName("max_tokens") val maxTokens: Int? = null
)

@Serializable
data class ChatMessageDto(
    val role: String,
    val content: String
)

@Serializable
data class ChatChunkDto(
    val choices: List<ChunkChoiceDto> = emptyList()
)

@Serializable
data class ChunkChoiceDto(
    val delta: DeltaDto? = null,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class DeltaDto(
    val content: String? = null
)

@Serializable
data class ApiErrorEnvelope(
    val error: ApiErrorBody? = null
)

@Serializable
data class ApiErrorBody(
    val message: String? = null
)
