package com.kanjilens.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.kanjilens.analysis.DictionaryLookup
import com.kanjilens.analysis.JapaneseTokenizer
import com.kanjilens.capture.ScreenCaptureManager
import com.kanjilens.capture.ScreenCaptureService
import com.kanjilens.data.models.AnalysisResult
import com.kanjilens.data.models.AppSettings
import com.kanjilens.data.models.CaptureState
import com.kanjilens.data.models.TranslationResult
import com.kanjilens.ocr.TextRecognizer
import com.kanjilens.translate.ScreenTranslator
import com.kanjilens.translate.TranslateResult
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import com.kanjilens.ui.components.CaptureButton
import com.kanjilens.ui.components.TranslationResultView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    captureManager: ScreenCaptureManager,
    textRecognizer: TextRecognizer,
    tokenizer: JapaneseTokenizer,
    dictionary: DictionaryLookup,
    translator: ScreenTranslator,
    settings: AppSettings,
    dictionaryState: CaptureState,
    translateState: CaptureState,
    onDictionaryStateChange: (CaptureState) -> Unit,
    onTranslateStateChange: (CaptureState) -> Unit,
    onSettingsClick: () -> Unit,
    onHelpClick: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val textSize by settings.textSize.collectAsState()
    val appMode by settings.appMode.collectAsState()
    val translateStyle by settings.translateStyle.collectAsState()
    val aiModel by settings.aiModel.collectAsState()
    val openaiKey by settings.openaiApiKey.collectAsState()
    val geminiKey by settings.geminiApiKey.collectAsState()
    val apiKey = when (aiModel) {
        AppSettings.MODEL_GEMINI_FLASH -> geminiKey
        AppSettings.MODEL_MLKIT_OFFLINE -> ""
        else -> openaiKey
    }

    val captureState = if (appMode == AppSettings.MODE_TRANSLATE) translateState else dictionaryState
    val onCaptureStateChange: (CaptureState) -> Unit = if (appMode == AppSettings.MODE_TRANSLATE) {
        onTranslateStateChange
    } else {
        onDictionaryStateChange
    }

    fun doDictionaryCapture() {
        scope.launch {
            onDictionaryStateChange(CaptureState.Capturing)
            val bitmap = captureManager.captureScreen()
            if (bitmap == null) {
                onDictionaryStateChange(CaptureState.Error("Failed to capture screen"))
                return@launch
            }

            onDictionaryStateChange(CaptureState.Processing)

            val recognizedText = textRecognizer.recognizeText(bitmap)
            if (recognizedText != null) {
                val tokens = tokenizer.tokenize(recognizedText)
                val words = dictionary.lookupTokens(tokens)

                onDictionaryStateChange(CaptureState.DictionarySuccess(
                    AnalysisResult(
                        originalText = recognizedText,
                        words = words,
                    )
                ))
            } else {
                onDictionaryStateChange(CaptureState.Error("No Japanese text found in screenshot"))
            }
        }
    }

    fun doTranslateCapture() {
        scope.launch {
            if (aiModel != AppSettings.MODEL_MLKIT_OFFLINE && apiKey.isBlank()) {
                onTranslateStateChange(CaptureState.Error("Add your API key in Settings"))
                return@launch
            }

            onTranslateStateChange(CaptureState.Capturing)
            val bitmap = captureManager.captureScreen()
            if (bitmap == null) {
                onTranslateStateChange(CaptureState.Error("Failed to capture screen"))
                return@launch
            }

            onTranslateStateChange(CaptureState.Processing)

            when (val result = translator.translateScreen(bitmap, apiKey, translateStyle, aiModel)) {
                is TranslateResult.Success -> {
                    onTranslateStateChange(CaptureState.TranslateSuccess(
                        TranslationResult(translation = result.text)
                    ))
                }
                is TranslateResult.Error -> {
                    onTranslateStateChange(CaptureState.Error(result.message))
                }
            }
        }
    }

    fun doCapture() {
        if (appMode == AppSettings.MODE_TRANSLATE) {
            doTranslateCapture()
        } else {
            doDictionaryCapture()
        }
    }

    val projectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            ScreenCaptureService.captureManager = captureManager

            val serviceIntent = Intent(context, ScreenCaptureService::class.java).apply {
                putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, result.data)
            }
            ContextCompat.startForegroundService(context, serviceIntent)

            onCaptureStateChange(CaptureState.Capturing)
            captureManager.awaitProjectionReady {
                doCapture()
            }
        } else {
            onCaptureStateChange(CaptureState.Error("Permission denied"))
        }
    }

    fun onCaptureClick() {
        if (captureManager.isReady) {
            doCapture()
        } else {
            val intent = captureManager.projectionManager.createScreenCaptureIntent()
            projectionLauncher.launch(intent)
        }
    }

    var modelMenuExpanded by remember { mutableStateOf(false) }
    val modelLabel = when (aiModel) {
        AppSettings.MODEL_GEMINI_FLASH -> "Gemini"
        AppSettings.MODEL_MLKIT_OFFLINE -> "Offline"
        else -> "GPT-4o"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ThorLens",
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    Box {
                        Text(
                            text = modelLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { modelMenuExpanded = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                        DropdownMenu(
                            expanded = modelMenuExpanded,
                            onDismissRequest = { modelMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Offline (ML Kit)") },
                                onClick = {
                                    settings.setAiModel(AppSettings.MODEL_MLKIT_OFFLINE)
                                    modelMenuExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Gemini Flash") },
                                onClick = {
                                    settings.setAiModel(AppSettings.MODEL_GEMINI_FLASH)
                                    modelMenuExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("GPT-4o mini") },
                                onClick = {
                                    settings.setAiModel(AppSettings.MODEL_GPT4O_MINI)
                                    modelMenuExpanded = false
                                },
                            )
                        }
                    }
                    IconButton(onClick = onHelpClick) {
                        Text(
                            text = "?",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Text(
                            text = "\u2699",
                            fontSize = 22.sp,
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Mode toggle
            ModeToggle(
                currentMode = appMode,
                onModeChange = { settings.setAppMode(it) },
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 8.dp)
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center,
            ) {
                when (val state = captureState) {
                    is CaptureState.Idle -> {
                        val hint = if (appMode == AppSettings.MODE_TRANSLATE) {
                            "Press the button to capture\nand translate the screen"
                        } else {
                            "Press the button to capture\nand look up words"
                        }
                        Text(
                            text = hint,
                            textAlign = TextAlign.Center,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    is CaptureState.Capturing -> {
                        Text(
                            text = "Capturing...",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    is CaptureState.Processing -> {
                        val modelName = when (aiModel) {
                            AppSettings.MODEL_GEMINI_FLASH -> "Gemini Flash"
                            AppSettings.MODEL_MLKIT_OFFLINE -> "Offline"
                            else -> "GPT-4o mini"
                        }
                        val label = if (appMode == AppSettings.MODE_TRANSLATE) {
                            if (aiModel == AppSettings.MODEL_MLKIT_OFFLINE) {
                                "Translating offline..."
                            } else {
                                val styleName = when (translateStyle) {
                                    AppSettings.TRANSLATE_STYLE_TRANSLATE_ONLY -> "translate"
                                    AppSettings.TRANSLATE_STYLE_TRANSLATE_AND_EXPLAIN -> "explain"
                                    else -> "auto"
                                }
                                "Translating using $modelName ($styleName)..."
                            }
                        } else {
                            "Reading text..."
                        }
                        Text(
                            text = label,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    is CaptureState.DictionarySuccess -> {
                        TranslationResultView(
                            result = state.result,
                            textSize = textSize,
                        )
                    }
                    is CaptureState.TranslateSuccess -> {
                        val translateFontSize = when (textSize) {
                            AppSettings.TEXT_SIZE_SMALL -> 14.sp
                            AppSettings.TEXT_SIZE_LARGE -> 20.sp
                            else -> 16.sp
                        }
                        if (aiModel == AppSettings.MODEL_MLKIT_OFFLINE) {
                            // Offline: show blocks with JP original + EN translation
                            Column {
                                val lines = state.result.translation.split("\n")
                                var i = 0
                                while (i < lines.size) {
                                    val line = lines[i].trim()
                                    if (line.isEmpty()) {
                                        i++
                                        continue
                                    }
                                    // JP original line
                                    Text(
                                        text = line,
                                        fontSize = translateFontSize,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = translateFontSize * 1.4,
                                    )
                                    // EN translation line (next line if exists)
                                    if (i + 1 < lines.size && lines[i + 1].trim().isNotEmpty()) {
                                        Text(
                                            text = lines[i + 1].trim(),
                                            fontSize = translateFontSize,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            lineHeight = translateFontSize * 1.4,
                                        )
                                        i += 2
                                    } else {
                                        i++
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        } else {
                            Text(
                                text = state.result.translation,
                                fontSize = translateFontSize,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = translateFontSize * 1.5,
                            )
                        }
                    }
                    is CaptureState.Error -> {
                        Text(
                            text = state.message,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            CaptureButton(
                isProcessing = captureState is CaptureState.Capturing
                    || captureState is CaptureState.Processing,
                onClick = { onCaptureClick() },
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
    }
}

@Composable
private fun ModeToggle(
    currentMode: Int,
    onModeChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        ModeOption(
            label = "Translate",
            selected = currentMode == AppSettings.MODE_TRANSLATE,
            onClick = { onModeChange(AppSettings.MODE_TRANSLATE) },
            modifier = Modifier.weight(1f),
        )
        ModeOption(
            label = "JP Dictionary",
            selected = currentMode == AppSettings.MODE_DICTIONARY,
            onClick = { onModeChange(AppSettings.MODE_DICTIONARY) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ModeOption(
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
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
        )
    }
}
