package com.kanjilens.ui.screens

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    onCropClick: (Bitmap) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val textSize by settings.textSize.collectAsState()
    val appMode by settings.appMode.collectAsState()
    val translateStyle by settings.translateStyle.collectAsState()
    val aiModel by settings.aiModel.collectAsState()
    val openaiKey by settings.openaiApiKey.collectAsState()
    val geminiKey by settings.geminiApiKey.collectAsState()
    val outputLanguage by settings.outputLanguage.collectAsState()
    val cropEnabled by settings.cropEnabled.collectAsState()
    val apiKey = when (aiModel) {
        AppSettings.MODEL_GEMINI_FLASH -> geminiKey
        AppSettings.MODEL_MLKIT_OFFLINE, AppSettings.MODEL_MLKIT_OFFLINE_AUTO -> ""
        else -> openaiKey
    }

    val isAutoMode = aiModel == AppSettings.MODEL_MLKIT_OFFLINE_AUTO
    var autoJob by remember { mutableStateOf<Job?>(null) }
    var lastOcrText by remember { mutableStateOf<String?>(null) }

    fun isSignificantChange(oldText: String, newText: String): Boolean {
        if (oldText.isEmpty()) return true
        val diff = kotlin.math.abs(oldText.length - newText.length)
        if (diff > 3) return true
        // Character-level comparison: if more than 20% different, it's significant
        val maxLen = maxOf(oldText.length, newText.length)
        if (maxLen == 0) return false
        var matches = 0
        for (i in 0 until minOf(oldText.length, newText.length)) {
            if (oldText[i] == newText[i]) matches++
        }
        val similarity = matches.toFloat() / maxLen
        return similarity < 0.8f
    }

    fun stopAutoMode() {
        autoJob?.cancel()
        autoJob = null
        lastOcrText = null
        settings.setAiModel(AppSettings.MODEL_MLKIT_OFFLINE)
    }

    fun cropBitmap(bitmap: Bitmap): Bitmap {
        if (!cropEnabled) return bitmap
        val region = settings.cropRegion
        val x = (region.left * bitmap.width).toInt().coerceIn(0, bitmap.width)
        val y = (region.top * bitmap.height).toInt().coerceIn(0, bitmap.height)
        val w = ((region.right - region.left) * bitmap.width).toInt().coerceIn(1, bitmap.width - x)
        val h = ((region.bottom - region.top) * bitmap.height).toInt().coerceIn(1, bitmap.height - y)
        return Bitmap.createBitmap(bitmap, x, y, w, h)
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
            val fullBitmap = captureManager.captureScreen()
            if (fullBitmap == null) {
                onDictionaryStateChange(CaptureState.Error("Failed to capture screen"))
                return@launch
            }

            val bitmap = cropBitmap(fullBitmap)
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
            val fullBitmap = captureManager.captureScreen()
            if (fullBitmap == null) {
                onTranslateStateChange(CaptureState.Error("Failed to capture screen"))
                return@launch
            }

            val bitmap = cropBitmap(fullBitmap)
            onTranslateStateChange(CaptureState.Processing)

            when (val result = translator.translateScreen(
                bitmap, apiKey, translateStyle, aiModel, outputLanguage,
                onDownloading = { onTranslateStateChange(CaptureState.DownloadingModel) },
                ollamaUrl = settings.ollamaUrl.value,
                ollamaModel = settings.ollamaModel.value,
                ollamaVision = settings.ollamaVision.value,
                customUrl = settings.customUrl.value,
                customApiKey = settings.customApiKey.value,
                customModel = settings.customModel.value,
                customVision = settings.customVision.value,
            )) {
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

    fun doAutoTranslateCycle() {
        scope.launch {
            val fullBitmap = captureManager.captureScreen() ?: return@launch

            val bitmap = cropBitmap(fullBitmap)

            // Get OCR text first for dedup
            val blocks = textRecognizer.recognizeTextBlocks(bitmap)
            if (blocks.isNullOrEmpty()) return@launch

            val currentText = blocks.joinToString("")
                .filter { c -> c.code > 0x3000 } // Keep only CJK chars for dedup

            if (currentText.isEmpty()) return@launch

            if (!isSignificantChange(lastOcrText ?: "", currentText)) {
                return@launch // Text hasn't changed, skip
            }
            lastOcrText = currentText

            onTranslateStateChange(CaptureState.Processing)

            when (val result = translator.translateScreen(
                bitmap, "", AppSettings.TRANSLATE_STYLE_AUTO, AppSettings.MODEL_MLKIT_OFFLINE, outputLanguage,
                onDownloading = { onTranslateStateChange(CaptureState.DownloadingModel) },
                ollamaUrl = settings.ollamaUrl.value,
                ollamaModel = settings.ollamaModel.value,
                ollamaVision = settings.ollamaVision.value,
                customUrl = settings.customUrl.value,
                customApiKey = settings.customApiKey.value,
                customModel = settings.customModel.value,
                customVision = settings.customVision.value,
            )) {
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

    fun startAutoMode() {
        if (autoJob?.isActive == true) return
        lastOcrText = null
        autoJob = scope.launch {
            while (true) {
                if (captureManager.isReady) {
                    doAutoTranslateCycle()
                }
                delay(1000L)
            }
        }
    }

    var pendingAutoAfterPermission by remember { mutableStateOf(false) }
    var pendingCropAfterPermission by remember { mutableStateOf(false) }

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

            if (pendingCropAfterPermission) {
                pendingCropAfterPermission = false
                captureManager.awaitProjectionReady {
                    scope.launch {
                        val bmp = captureManager.captureScreen()
                        if (bmp != null) onCropClick(bmp)
                    }
                }
            } else if (pendingAutoAfterPermission) {
                pendingAutoAfterPermission = false
                captureManager.awaitProjectionReady {
                    startAutoMode()
                }
            } else {
                onCaptureStateChange(CaptureState.Capturing)
                captureManager.awaitProjectionReady {
                    doCapture()
                }
            }
        } else {
            pendingCropAfterPermission = false
            pendingAutoAfterPermission = false
            onCaptureStateChange(CaptureState.Error("Permission denied"))
        }
    }

    fun onCaptureClick() {
        if (captureManager.isReady) {
            doCapture()
        } else {
            pendingCropAfterPermission = false
            val intent = captureManager.projectionManager.createScreenCaptureIntent()
            projectionLauncher.launch(intent)
        }
    }

    fun onCropRegionClick() {
        if (captureManager.isReady) {
            scope.launch {
                val bmp = captureManager.captureScreen()
                if (bmp != null) onCropClick(bmp)
            }
        } else {
            pendingCropAfterPermission = true
            val intent = captureManager.projectionManager.createScreenCaptureIntent()
            projectionLauncher.launch(intent)
        }
    }

    var modelMenuExpanded by remember { mutableStateOf(false) }
    val modelLabel = when (aiModel) {
        AppSettings.MODEL_GEMINI_FLASH -> "Gemini"
        AppSettings.MODEL_MLKIT_OFFLINE -> "Offline"
        AppSettings.MODEL_MLKIT_OFFLINE_AUTO -> "Auto"
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
                    // Region chip
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (cropEnabled) "Region" else "Full",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (cropEnabled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onCropRegionClick() }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                        )
                        if (cropEnabled) {
                            Text(
                                text = "\u2715",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .clickable { settings.clearCropRegion() }
                                    .padding(start = 2.dp, end = 4.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    // Model chip
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
                                text = { Text("Offline") },
                                onClick = {
                                    stopAutoMode()
                                    settings.setAiModel(AppSettings.MODEL_MLKIT_OFFLINE)
                                    modelMenuExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Offline Auto") },
                                onClick = {
                                    settings.setAiModel(AppSettings.MODEL_MLKIT_OFFLINE_AUTO)
                                    modelMenuExpanded = false
                                    if (captureManager.isReady) {
                                        startAutoMode()
                                    } else {
                                        pendingAutoAfterPermission = true
                                        val intent = captureManager.projectionManager.createScreenCaptureIntent()
                                        projectionLauncher.launch(intent)
                                    }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Gemini Flash") },
                                onClick = {
                                    stopAutoMode()
                                    settings.setAiModel(AppSettings.MODEL_GEMINI_FLASH)
                                    modelMenuExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("GPT-4o mini") },
                                onClick = {
                                    stopAutoMode()
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
                    is CaptureState.DownloadingModel -> {
                        val langName = AppSettings.languageDisplayName(outputLanguage)
                        Text(
                            text = "Downloading $langName model...",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    is CaptureState.Processing -> {
                        val langName = AppSettings.languageDisplayName(outputLanguage)
                        val label = if (appMode == AppSettings.MODE_TRANSLATE) {
                            if (aiModel == AppSettings.MODEL_MLKIT_OFFLINE || aiModel == AppSettings.MODEL_MLKIT_OFFLINE_AUTO) {
                                "Translating to $langName..."
                            } else {
                                val modelName = when (aiModel) {
                                    AppSettings.MODEL_GEMINI_FLASH -> "Gemini Flash"
                                    else -> "GPT-4o mini"
                                }
                                val styleName = when (translateStyle) {
                                    AppSettings.TRANSLATE_STYLE_TRANSLATE_ONLY -> "translate"
                                    AppSettings.TRANSLATE_STYLE_TRANSLATE_AND_EXPLAIN -> "explain"
                                    else -> "auto"
                                }
                                "Translating to $langName using $modelName ($styleName)..."
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
                        if (aiModel == AppSettings.MODEL_MLKIT_OFFLINE || aiModel == AppSettings.MODEL_MLKIT_OFFLINE_AUTO) {
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
                    || captureState is CaptureState.DownloadingModel
                    || captureState is CaptureState.Processing,
                onClick = { onCaptureClick() },
                modifier = Modifier.padding(bottom = 16.dp),
                isAutoMode = isAutoMode,
                onStopAuto = { stopAutoMode() },
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
