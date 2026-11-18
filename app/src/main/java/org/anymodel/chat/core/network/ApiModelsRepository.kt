package org.anymodel.chat.core.network

import javax.inject.Inject
import javax.inject.Singleton
import org.anymodel.chat.settings.domain.AiModel
import org.anymodel.chat.settings.domain.ModelsRepository

@Singleton
class ApiModelsRepository @Inject constructor(
    private val api: AnyModelApi
) : ModelsRepository {

    override suspend fun fetchModels(): List<AiModel> =
        api.getModels().data
            .map { AiModel(id = it.id, ownedBy = it.ownedBy) }
            .sortedBy { it.id.lowercase() }
}
