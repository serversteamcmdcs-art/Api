package org.anymodel.chat.settings.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.anymodel.chat.core.network.ApiErrors
import org.anymodel.chat.settings.domain.AiModel
import org.anymodel.chat.settings.domain.ApiKeyStore
import org.anymodel.chat.settings.domain.AppSettings
import org.anymodel.chat.settings.domain.ModelsRepository
import org.anymodel.chat.settings.domain.SettingsRepository
import org.anymodel.chat.settings.domain.ThemeMode

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    /** Поле ввода ключа. Сохранённый ключ в UI не возвращается — только факт его наличия. */
    val apiKeyInput: String = "",
    val hasKey: Boolean = false,
    val keyJustSaved: Boolean = false,
    val models: List<AiModel> = emptyList(),
    val modelsLoading: Boolean = false,
    val systemPromptInput: String = "",
    val maxTokensInput: String = "",
    /** Текст ошибки для Snackbar; после показа сбрасывается через [SettingsViewModel.consumeError]. */
    val error: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val apiKeyStore: ApiKeyStore,
    private val modelsRepository: ModelsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState(hasKey = apiKeyStore.hasKey()))
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            // Начальные значения полей берём один раз, чтобы курсор не прыгал при записи в DataStore.
            val initial = settingsRepository.settings.first()
            _state.update {
                it.copy(
                    settings = initial,
                    systemPromptInput = initial.globalSystemPrompt,
                    maxTokensInput = initial.maxTokens?.toString().orEmpty()
                )
            }
            settingsRepository.settings.collect { s -> _state.update { it.copy(settings = s) } }
        }
        if (apiKeyStore.hasKey()) loadModels()
    }

    fun onApiKeyChange(value: String) =
        _state.update { it.copy(apiKeyInput = value, keyJustSaved = false) }

    fun saveApiKey() {
        val key = _state.value.apiKeyInput.trim()
        if (key.isEmpty()) return
        apiKeyStore.set(key)
        _state.update { it.copy(apiKeyInput = "", hasKey = true, keyJustSaved = true) }
        loadModels()
    }

    fun clearApiKey() {
        apiKeyStore.clear()
        loadJob?.cancel()
        _state.update {
            it.copy(hasKey = false, keyJustSaved = false, models = emptyList(), modelsLoading = false)
        }
    }

    fun loadModels() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(modelsLoading = true) }
            try {
                val models = modelsRepository.fetchModels()
                _state.update { it.copy(models = models, modelsLoading = false) }
                // Если модель по умолчанию ещё не выбрана — предлагаем первую из списка.
                if (settingsRepository.settings.first().defaultModel == null) {
                    models.firstOrNull()?.let { settingsRepository.setDefaultModel(it.id) }
                }
            } catch (t: Throwable) {
                ApiErrors.rethrowCancellation(t)
                _state.update {
                    it.copy(modelsLoading = false, error = ApiErrors.message(context, t))
                }
            }
        }
    }

    fun selectModel(id: String) {
        viewModelScope.launch { settingsRepository.setDefaultModel(id) }
    }

    fun onSystemPromptChange(value: String) {
        _state.update { it.copy(systemPromptInput = value) }
        viewModelScope.launch { settingsRepository.setGlobalSystemPrompt(value) }
    }

    fun onTemperatureChange(value: Float) {
        // Шаг 0.1, чтобы в UI не было длинных дробей.
        val rounded = (value * 10).toInt() / 10f
        viewModelScope.launch { settingsRepository.setTemperature(rounded) }
    }

    fun onMaxTokensChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(7)
        _state.update { it.copy(maxTokensInput = digits) }
        viewModelScope.launch { settingsRepository.setMaxTokens(digits.toIntOrNull()?.takeIf { it > 0 }) }
    }

    fun onThemeModeChange(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun onDynamicColorChange(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }
    }

    fun consumeError() = _state.update { it.copy(error = null) }
}
