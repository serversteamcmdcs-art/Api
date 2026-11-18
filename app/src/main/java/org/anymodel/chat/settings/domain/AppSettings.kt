package org.anymodel.chat.settings.domain

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val defaultModel: String? = null,
    val globalSystemPrompt: String = "",
    val temperature: Float = 0.7f,
    /** null — не передавать max_tokens, модель использует своё значение по умолчанию. */
    val maxTokens: Int? = null
)
