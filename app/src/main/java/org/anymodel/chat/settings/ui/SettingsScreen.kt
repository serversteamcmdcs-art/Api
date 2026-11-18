package org.anymodel.chat.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.anymodel.chat.BuildConfig
import org.anymodel.chat.R
import org.anymodel.chat.settings.domain.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val retryLabel = stringResource(R.string.action_retry)

    // Ошибки сети: понятный Snackbar с кнопкой «Повторить».
    LaunchedEffect(state.error) {
        val message = state.error ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = retryLabel,
            duration = SnackbarDuration.Long
        )
        viewModel.consumeError()
        if (result == SnackbarResult.ActionPerformed) viewModel.loadModels()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ApiSection(state, viewModel)
            HorizontalDivider()
            ModelSection(state, viewModel)
            HorizontalDivider()
            GenerationSection(state, viewModel)
            HorizontalDivider()
            AppearanceSection(state, viewModel)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun ApiSection(state: SettingsUiState, vm: SettingsViewModel) {
    var visible by remember { mutableStateOf(false) }

    SectionTitle(stringResource(R.string.settings_section_api))
    Text(
        text = stringResource(R.string.settings_base_url, BuildConfig.API_BASE_URL),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    OutlinedTextField(
        value = state.apiKeyInput,
        onValueChange = vm::onApiKeyChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.settings_api_key)) },
        supportingText = {
            Text(
                if (state.keyJustSaved || state.hasKey) stringResource(R.string.settings_api_key_saved)
                else stringResource(R.string.settings_api_key_hint)
            )
        },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = stringResource(
                        if (visible) R.string.settings_api_key_hide else R.string.settings_api_key_show
                    )
                )
            }
        }
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = vm::saveApiKey, enabled = state.apiKeyInput.isNotBlank()) {
            Text(stringResource(R.string.action_save))
        }
        if (state.hasKey) {
            IconButton(onClick = vm::clearApiKey) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.settings_api_key_clear)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelSection(state: SettingsUiState, vm: SettingsViewModel) {
    SectionTitle(stringResource(R.string.settings_section_model))

    var expanded by remember { mutableStateOf(false) }
    val selected = state.settings.defaultModel.orEmpty()

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (state.models.isNotEmpty()) expanded = it },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                label = { Text(stringResource(R.string.settings_default_model)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                state.models.forEach { model ->
                    DropdownMenuItem(
                        text = { Text(model.id) },
                        onClick = {
                            vm.selectModel(model.id)
                            expanded = false
                        }
                    )
                }
            }
        }
        if (state.modelsLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(8.dp))
        } else {
            IconButton(onClick = vm::loadModels, enabled = state.hasKey) {
                Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.action_reload))
            }
        }
    }
    if (state.models.isEmpty()) {
        Text(
            text = stringResource(
                if (state.modelsLoading) R.string.settings_models_loading else R.string.settings_models_empty
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GenerationSection(state: SettingsUiState, vm: SettingsViewModel) {
    SectionTitle(stringResource(R.string.settings_section_generation))

    OutlinedTextField(
        value = state.systemPromptInput,
        onValueChange = vm::onSystemPromptChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.settings_system_prompt)) },
        minLines = 3,
        maxLines = 8
    )

    Text(
        text = stringResource(R.string.settings_temperature, "%.1f".format(state.settings.temperature)),
        style = MaterialTheme.typography.bodyMedium
    )
    Slider(
        value = state.settings.temperature,
        onValueChange = vm::onTemperatureChange,
        valueRange = 0f..2f,
        steps = 19
    )

    OutlinedTextField(
        value = state.maxTokensInput,
        onValueChange = vm::onMaxTokensChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.settings_max_tokens)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceSection(state: SettingsUiState, vm: SettingsViewModel) {
    SectionTitle(stringResource(R.string.settings_section_appearance))

    val options = listOf(
        ThemeMode.SYSTEM to R.string.settings_theme_system,
        ThemeMode.LIGHT to R.string.settings_theme_light,
        ThemeMode.DARK to R.string.settings_theme_dark
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (mode, label) ->
            SegmentedButton(
                selected = state.settings.themeMode == mode,
                onClick = { vm.onThemeModeChange(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
            ) {
                Text(stringResource(label))
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.settings_dynamic_color),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
        Switch(checked = state.settings.dynamicColor, onCheckedChange = vm::onDynamicColorChange)
    }
}
