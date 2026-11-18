package org.anymodel.chat.settings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.anymodel.chat.settings.domain.AppSettings
import org.anymodel.chat.settings.domain.SettingsRepository
import org.anymodel.chat.settings.domain.ThemeMode

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val DYNAMIC = booleanPreferencesKey("dynamic_color")
        val DEFAULT_MODEL = stringPreferencesKey("default_model")
        val SYSTEM_PROMPT = stringPreferencesKey("global_system_prompt")
        val TEMPERATURE = floatPreferencesKey("temperature")
        val MAX_TOKENS = intPreferencesKey("max_tokens")
    }

    override val settings: Flow<AppSettings> = context.settingsDataStore.data.map { p ->
        val defaults = AppSettings()
        AppSettings(
            themeMode = p[Keys.THEME]
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: defaults.themeMode,
            dynamicColor = p[Keys.DYNAMIC] ?: defaults.dynamicColor,
            defaultModel = p[Keys.DEFAULT_MODEL],
            globalSystemPrompt = p[Keys.SYSTEM_PROMPT] ?: defaults.globalSystemPrompt,
            temperature = p[Keys.TEMPERATURE] ?: defaults.temperature,
            maxTokens = p[Keys.MAX_TOKENS]
        )
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME] = mode.name }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.DYNAMIC] = enabled }
    }

    override suspend fun setDefaultModel(modelId: String?) {
        context.settingsDataStore.edit {
            if (modelId == null) it.remove(Keys.DEFAULT_MODEL) else it[Keys.DEFAULT_MODEL] = modelId
        }
    }

    override suspend fun setGlobalSystemPrompt(prompt: String) {
        context.settingsDataStore.edit { it[Keys.SYSTEM_PROMPT] = prompt }
    }

    override suspend fun setTemperature(value: Float) {
        context.settingsDataStore.edit { it[Keys.TEMPERATURE] = value.coerceIn(0f, 2f) }
    }

    override suspend fun setMaxTokens(value: Int?) {
        context.settingsDataStore.edit {
            if (value == null) it.remove(Keys.MAX_TOKENS) else it[Keys.MAX_TOKENS] = value
        }
    }
}
