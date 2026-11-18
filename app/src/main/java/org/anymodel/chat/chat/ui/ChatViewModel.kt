package org.anymodel.chat.chat.ui

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
import org.anymodel.chat.R
import org.anymodel.chat.chat.domain.ChatMessage
import org.anymodel.chat.chat.domain.ChatRepository
import org.anymodel.chat.chat.domain.Role
import org.anymodel.chat.core.network.ApiErrors
import org.anymodel.chat.settings.domain.AiModel
import org.anymodel.chat.settings.domain.ApiKeyStore
import org.anymodel.chat.settings.domain.ModelsRepository
import org.anymodel.chat.settings.domain.SettingsRepository

/** Что делает кнопка «Повторить» в Snackbar для текущей ошибки. */
enum class ErrorAction { RETRY_GENERATION, RELOAD_MODELS, REFRESH }

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val isGenerating: Boolean = false,
    val hasKey: Boolean = false,
    val models: List<AiModel> = emptyList(),
    val modelsLoading: Boolean = false,
    val defaultModel: String? = null,
    /** Модель, выбранная в этом чате; null — используется модель по умолчанию из настроек. */
    val modelOverride: String? = null,
    /** Текст ошибки для Snackbar; сбрасывается через [ChatViewModel.consumeError]. */
    val error: String? = null,
    val errorAction: ErrorAction = ErrorAction.REFRESH
) {
    val model: String? get() = modelOverride ?: defaultModel
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatRepository: ChatRepository,
    private val settingsRepository: SettingsRepository,
    private val apiKeyStore: ApiKeyStore,
    private val modelsRepository: ModelsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState(hasKey = apiKeyStore.hasKey()))
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var generationJob: Job? = null
    private var modelsJob: Job? = null
    private var nextId = 1L

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { s ->
                _state.update { it.copy(defaultModel = s.defaultModel) }
            }
        }
        refresh()
    }

    /** Вызывается при возврате на экран (например, из настроек): ключ и список моделей могли измениться. */
    fun refresh() {
        val hasKey = apiKeyStore.hasKey()
        _state.update { it.copy(hasKey = hasKey) }
        if (hasKey && _state.value.models.isEmpty()) loadModels()
    }

    private fun loadModels() {
        modelsJob?.cancel()
        modelsJob = viewModelScope.launch {
            _state.update { it.copy(modelsLoading = true) }
            try {
                val models = modelsRepository.fetchModels()
                _state.update { it.copy(models = models, modelsLoading = false) }
            } catch (t: Throwable) {
                ApiErrors.rethrowCancellation(t)
                _state.update {
                    it.copy(
                        modelsLoading = false,
                        error = ApiErrors.message(context, t),
                        errorAction = ErrorAction.RELOAD_MODELS
                    )
                }
            }
        }
    }

    fun onInputChange(value: String) = _state.update { it.copy(input = value) }

    fun selectModel(id: String) = _state.update { it.copy(modelOverride = id) }

    fun send() {
        val current = _state.value
        val text = current.input.trim()
        if (text.isEmpty() || current.isGenerating) return
        if (!apiKeyStore.hasKey()) {
            _state.update {
                it.copy(
                    hasKey = false,
                    error = context.getString(R.string.error_no_key),
                    errorAction = ErrorAction.REFRESH
                )
            }
            return
        }
        val userMessage = ChatMessage(id = nextId++, role = Role.USER, content = text)
        _state.update { it.copy(messages = it.messages + userMessage, input = "") }
        startGeneration()
    }

    /** Остановить генерацию: уже полученный текст остаётся в чате. */
    fun stop() {
        generationJob?.cancel()
    }

    /** Перегенерировать последний ответ (также используется кнопкой «Повторить» в Snackbar). */
    fun regenerate() {
        if (_state.value.isGenerating) return
        _state.update { state ->
            val last = state.messages.lastOrNull()
            if (last?.role == Role.ASSISTANT) state.copy(messages = state.messages.dropLast(1)) else state
        }
        if (_state.value.messages.lastOrNull()?.role == Role.USER) startGeneration()
    }

    fun clearChat() {
        generationJob?.cancel()
        _state.update { it.copy(messages = emptyList(), isGenerating = false, error = null) }
    }

    fun consumeError() = _state.update { it.copy(error = null) }

    fun retry(action: ErrorAction) {
        when (action) {
            ErrorAction.RETRY_GENERATION -> regenerate()
            ErrorAction.RELOAD_MODELS -> loadModels()
            ErrorAction.REFRESH -> {
                _state.update { it.copy(models = emptyList()) }
                refresh()
            }
        }
    }

    private fun startGeneration() {
        val snapshot = _state.value
        val model = snapshot.model
        if (model == null) {
            _state.update {
                it.copy(error = context.getString(R.string.error_no_model), errorAction = ErrorAction.REFRESH)
            }
            return
        }

        val history = snapshot.messages
        val assistantId = nextId++
        _state.update {
            it.copy(
                messages = it.messages + ChatMessage(assistantId, Role.ASSISTANT, "", isStreaming = true),
                isGenerating = true,
                error = null
            )
        }

        generationJob = viewModelScope.launch {
            try {
                val settings = settingsRepository.settings.first()
                chatRepository.streamReply(
                    history = history,
                    model = model,
                    systemPrompt = settings.globalSystemPrompt,
                    temperature = settings.temperature,
                    maxTokens = settings.maxTokens
                ).collect { delta ->
                    updateMessage(assistantId) { it.copy(content = it.content + delta) }
                }
            } catch (t: Throwable) {
                ApiErrors.rethrowCancellation(t)
                _state.update {
                    it.copy(error = ApiErrors.message(context, t), errorAction = ErrorAction.RETRY_GENERATION)
                }
            } finally {
                finishGeneration(assistantId)
            }
        }
    }

    private fun updateMessage(id: Long, transform: (ChatMessage) -> ChatMessage) {
        _state.update { state ->
            state.copy(messages = state.messages.map { if (it.id == id) transform(it) else it })
        }
    }

    private fun finishGeneration(assistantId: Long) {
        _state.update { state ->
            val messages = state.messages
                .map { if (it.id == assistantId) it.copy(isStreaming = false) else it }
                // Пустой ответ (ошибка до первого токена или «Стоп» сразу) в истории не оставляем.
                .filterNot { it.id == assistantId && it.content.isEmpty() }
            state.copy(messages = messages, isGenerating = false)
        }
    }
}
