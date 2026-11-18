package org.anymodel.chat.chat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.anymodel.chat.R
import org.anymodel.chat.chat.domain.ChatMessage
import org.anymodel.chat.chat.domain.Role

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onOpenSettings: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val retryLabel = stringResource(R.string.action_retry)

    // Возврат из настроек: ключ и список моделей могли измениться.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    // Ошибки (401, 429, таймаут, нет сети): Snackbar с кнопкой «Повторить».
    LaunchedEffect(state.error) {
        val message = state.error ?: return@LaunchedEffect
        val action = state.errorAction
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = retryLabel,
            duration = SnackbarDuration.Long
        )
        viewModel.consumeError()
        if (result == SnackbarResult.ActionPerformed) viewModel.retry(action)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    ModelPicker(
                        current = state.model,
                        models = state.models.map { it.id },
                        loading = state.modelsLoading,
                        onSelect = viewModel::selectModel
                    )
                },
                actions = {
                    IconButton(onClick = viewModel::clearChat, enabled = state.messages.isNotEmpty()) {
                        Icon(
                            Icons.Filled.DeleteSweep,
                            contentDescription = stringResource(R.string.chat_clear)
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.nav_settings)
                        )
                    }
                }
            )
        },
        bottomBar = {
            MessageInput(
                value = state.input,
                isGenerating = state.isGenerating,
                enabled = state.hasKey,
                onValueChange = viewModel::onInputChange,
                onSend = viewModel::send,
                onStop = viewModel::stop
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                !state.hasKey -> EmptyState(
                    title = stringResource(R.string.chat_no_key_title),
                    body = stringResource(R.string.chat_no_key_body),
                    actionLabel = stringResource(R.string.chat_open_settings),
                    onAction = onOpenSettings
                )
                state.messages.isEmpty() -> EmptyState(
                    title = stringResource(R.string.chat_empty_title),
                    body = stringResource(R.string.chat_empty_body)
                )
                else -> MessageList(
                    messages = state.messages,
                    canRegenerate = !state.isGenerating,
                    onRegenerate = viewModel::regenerate
                )
            }
        }
    }
}

@Composable
private fun ModelPicker(
    current: String?,
    models: List<String>,
    loading: Boolean,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }, enabled = models.isNotEmpty() || current != null) {
            Text(
                text = current ?: stringResource(
                    if (loading) R.string.chat_models_loading else R.string.chat_model_none
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.widthIn(max = 200.dp)
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = stringResource(R.string.chat_select_model))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            models.forEach { id ->
                DropdownMenuItem(
                    text = { Text(id) },
                    onClick = {
                        onSelect(id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (actionLabel != null && onAction != null) {
            Button(onClick = onAction, modifier = Modifier.padding(top = 16.dp)) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    canRegenerate: Boolean,
    onRegenerate: () -> Unit
) {
    val listState = rememberLazyListState()
    val last = messages.lastOrNull()

    // Держим список прокрученным к последнему сообщению, пока ответ «печатается».
    LaunchedEffect(messages.size, last?.content?.length) {
        if (messages.isNotEmpty()) listState.scrollToItem(messages.lastIndex)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(messages, key = { it.id }) { message ->
            MessageBubble(
                message = message,
                showRegenerate = canRegenerate && message.id == last?.id && message.role == Role.ASSISTANT,
                onRegenerate = onRegenerate
            )
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    showRegenerate: Boolean,
    onRegenerate: () -> Unit
) {
    val isUser = message.role == Role.USER
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            if (message.content.isEmpty() && message.isStreaming) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(14.dp)
                        .size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                // Markdown и подсветка кода появятся на этапе 3; пока — выделяемый обычный текст.
                SelectionContainer {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }
        if (showRegenerate) {
            IconButton(onClick = onRegenerate) {
                Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.chat_regenerate))
            }
        }
    }
}

@Composable
private fun MessageInput(
    value: String,
    isGenerating: Boolean,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.chat_input_hint)) },
                enabled = enabled,
                maxLines = 6,
                shape = RoundedCornerShape(24.dp)
            )
            if (isGenerating) {
                FilledIconButton(onClick = onStop) {
                    Icon(Icons.Filled.Stop, contentDescription = stringResource(R.string.chat_stop))
                }
            } else {
                FilledIconButton(onClick = onSend, enabled = enabled && value.isNotBlank()) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.chat_send))
                }
            }
        }
    }
}
