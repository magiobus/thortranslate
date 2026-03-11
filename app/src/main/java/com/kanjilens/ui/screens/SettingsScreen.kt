package com.kanjilens.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kanjilens.data.models.AppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onBack: () -> Unit,
) {
    val textSize by settings.textSize.collectAsState()
    val openaiApiKey by settings.openaiApiKey.collectAsState()
    val geminiApiKey by settings.geminiApiKey.collectAsState()
    val translateStyle by settings.translateStyle.collectAsState()
    val aiModel by settings.aiModel.collectAsState()
    val outputLanguage by settings.outputLanguage.collectAsState()

    var openaiKeyInput by remember { mutableStateOf(openaiApiKey) }
    var geminiKeyInput by remember { mutableStateOf(geminiApiKey) }
    var showApiKey by remember { mutableStateOf(false) }

    val apiKeyInput = if (aiModel == AppSettings.MODEL_GEMINI_FLASH) geminiKeyInput else openaiKeyInput

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Settings", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text(
                            text = "<",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Text Size
            SettingsSection(title = "Text Size") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SettingsOption(
                        label = "S",
                        selected = textSize == AppSettings.TEXT_SIZE_SMALL,
                        onClick = { settings.setTextSize(AppSettings.TEXT_SIZE_SMALL) },
                        modifier = Modifier.weight(1f),
                    )
                    SettingsOption(
                        label = "M",
                        selected = textSize == AppSettings.TEXT_SIZE_MEDIUM,
                        onClick = { settings.setTextSize(AppSettings.TEXT_SIZE_MEDIUM) },
                        modifier = Modifier.weight(1f),
                    )
                    SettingsOption(
                        label = "L",
                        selected = textSize == AppSettings.TEXT_SIZE_LARGE,
                        onClick = { settings.setTextSize(AppSettings.TEXT_SIZE_LARGE) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            // AI Model (first)
            SettingsSection(title = "AI Model") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SettingsOption(
                        label = "Offline",
                        selected = aiModel == AppSettings.MODEL_MLKIT_OFFLINE,
                        onClick = { settings.setAiModel(AppSettings.MODEL_MLKIT_OFFLINE) },
                        modifier = Modifier.weight(1f),
                    )
                    SettingsOption(
                        label = "Gemini",
                        selected = aiModel == AppSettings.MODEL_GEMINI_FLASH,
                        onClick = { settings.setAiModel(AppSettings.MODEL_GEMINI_FLASH) },
                        modifier = Modifier.weight(1f),
                    )
                    SettingsOption(
                        label = "GPT-4o",
                        selected = aiModel == AppSettings.MODEL_GPT4O_MINI,
                        onClick = { settings.setAiModel(AppSettings.MODEL_GPT4O_MINI) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (aiModel == AppSettings.MODEL_MLKIT_OFFLINE || aiModel == AppSettings.MODEL_MLKIT_OFFLINE_AUTO) {
                    Text(
                        text = "Uses ML Kit on-device translation. No API key needed. Works offline after first download (~30MB).",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            // Output Language
            var langMenuExpanded by remember { mutableStateOf(false) }
            SettingsSection(title = "Output Language") {
                Box {
                    Text(
                        text = AppSettings.languageDisplayName(outputLanguage),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { langMenuExpanded = true }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                    )
                    DropdownMenu(
                        expanded = langMenuExpanded,
                        onDismissRequest = { langMenuExpanded = false },
                    ) {
                        AppSettings.OUTPUT_LANGUAGES.forEach { (code, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    settings.setOutputLanguage(code)
                                    langMenuExpanded = false
                                },
                            )
                        }
                    }
                }
                if ((aiModel == AppSettings.MODEL_MLKIT_OFFLINE || aiModel == AppSettings.MODEL_MLKIT_OFFLINE_AUTO) && outputLanguage != AppSettings.LANG_ENGLISH) {
                    Text(
                        text = "First use will download the ${AppSettings.languageDisplayName(outputLanguage)} model (~30MB)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            if (aiModel != AppSettings.MODEL_MLKIT_OFFLINE && aiModel != AppSettings.MODEL_MLKIT_OFFLINE_AUTO) {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                // Translation Style (only for AI models, below AI Model)
                SettingsSection(title = "Translation Style") {
                    Text(
                        text = "Controls how Translate mode responds",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SettingsOption(
                            label = "Auto",
                            selected = translateStyle == AppSettings.TRANSLATE_STYLE_AUTO,
                            onClick = { settings.setTranslateStyle(AppSettings.TRANSLATE_STYLE_AUTO) },
                            modifier = Modifier.weight(1f),
                        )
                        SettingsOption(
                            label = "Translate",
                            selected = translateStyle == AppSettings.TRANSLATE_STYLE_TRANSLATE_ONLY,
                            onClick = { settings.setTranslateStyle(AppSettings.TRANSLATE_STYLE_TRANSLATE_ONLY) },
                            modifier = Modifier.weight(1f),
                        )
                        SettingsOption(
                            label = "Explain",
                            selected = translateStyle == AppSettings.TRANSLATE_STYLE_TRANSLATE_AND_EXPLAIN,
                            onClick = { settings.setTranslateStyle(AppSettings.TRANSLATE_STYLE_TRANSLATE_AND_EXPLAIN) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            if (aiModel != AppSettings.MODEL_MLKIT_OFFLINE && aiModel != AppSettings.MODEL_MLKIT_OFFLINE_AUTO) {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                // API Key
                val keyLabel = if (aiModel == AppSettings.MODEL_GEMINI_FLASH) {
                    "Google AI API Key"
                } else {
                    "OpenAI API Key"
                }
                val keyHint = if (aiModel == AppSettings.MODEL_GEMINI_FLASH) {
                    "Get your key at aistudio.google.com"
                } else {
                    "Get your key at platform.openai.com"
                }
                val keyPlaceholder = if (aiModel == AppSettings.MODEL_GEMINI_FLASH) {
                    "AIza..."
                } else {
                    "sk-..."
                }

                SettingsSection(title = keyLabel) {
                    Text(
                        text = keyHint,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            if (aiModel == AppSettings.MODEL_GEMINI_FLASH) {
                                geminiKeyInput = it
                                settings.setGeminiApiKey(it)
                            } else {
                                openaiKeyInput = it
                                settings.setOpenaiApiKey(it)
                            }
                        },
                        placeholder = { Text(keyPlaceholder) },
                        singleLine = true,
                        visualTransformation = if (showApiKey) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                    if (apiKeyInput.isNotEmpty()) {
                        Text(
                            text = if (showApiKey) "Hide key" else "Show key",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { showApiKey = !showApiKey }
                                .padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        content()
    }
}

@Composable
private fun SettingsOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
        )
    }
}
