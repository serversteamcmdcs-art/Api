package org.anymodel.chat.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET

@Serializable
data class ModelsResponse(val data: List<ModelDto> = emptyList())

@Serializable
data class ModelDto(
    val id: String,
    @SerialName("owned_by") val ownedBy: String? = null
)

/** OpenAI-совместимый API. /chat/completions добавится на этапе 2. */
interface AnyModelApi {
    @GET("models")
    suspend fun getModels(): ModelsResponse
}
