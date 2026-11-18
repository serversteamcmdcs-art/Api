package org.anymodel.chat.settings.domain

data class AiModel(val id: String, val ownedBy: String? = null)

interface ModelsRepository {
    /** GET /models. Бросает исключение сети/HTTP — маппинг в текст делает ApiErrors. */
    suspend fun fetchModels(): List<AiModel>
}
