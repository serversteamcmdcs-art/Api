package org.anymodel.chat.settings.domain

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setDefaultModel(modelId: String?)
    suspend fun setGlobalSystemPrompt(prompt: String)
    suspend fun setTemperature(value: Float)
    suspend fun setMaxTokens(value: Int?)
}
