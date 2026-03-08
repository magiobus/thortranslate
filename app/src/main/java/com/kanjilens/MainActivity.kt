package com.kanjilens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kanjilens.analysis.DictionaryLookup
import com.kanjilens.analysis.JapaneseTokenizer
import com.kanjilens.capture.ScreenCaptureManager
import com.kanjilens.data.models.AppSettings
import com.kanjilens.data.models.CaptureState
import com.kanjilens.ocr.TextRecognizer
import com.kanjilens.translate.ScreenTranslator
import com.kanjilens.ui.screens.HelpScreen
import com.kanjilens.ui.screens.MainScreen
import com.kanjilens.ui.screens.SettingsScreen
import com.kanjilens.ui.theme.KanjiLensTheme

class MainActivity : ComponentActivity() {

    lateinit var captureManager: ScreenCaptureManager
    lateinit var textRecognizer: TextRecognizer
    lateinit var tokenizer: JapaneseTokenizer
    lateinit var dictionary: DictionaryLookup
    lateinit var settings: AppSettings
    val translator = ScreenTranslator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        captureManager = ScreenCaptureManager(this)
        textRecognizer = TextRecognizer()
        tokenizer = JapaneseTokenizer()
        dictionary = DictionaryLookup(this)
        settings = AppSettings(this)
        enableEdgeToEdge()
        setContent {
            KanjiLensTheme {
                var currentScreen by remember { mutableStateOf("main") }
                var dictionaryState by remember { mutableStateOf<CaptureState>(CaptureState.Idle) }
                var translateState by remember { mutableStateOf<CaptureState>(CaptureState.Idle) }

                when (currentScreen) {
                    "settings" -> SettingsScreen(
                        settings = settings,
                        onBack = { currentScreen = "main" },
                    )
                    "help" -> HelpScreen(
                        onBack = { currentScreen = "main" },
                    )
                    else -> MainScreen(
                        captureManager = captureManager,
                        textRecognizer = textRecognizer,
                        tokenizer = tokenizer,
                        dictionary = dictionary,
                        translator = translator,
                        settings = settings,
                        dictionaryState = dictionaryState,
                        translateState = translateState,
                        onDictionaryStateChange = { dictionaryState = it },
                        onTranslateStateChange = { translateState = it },
                        onSettingsClick = { currentScreen = "settings" },
                        onHelpClick = { currentScreen = "help" },
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        captureManager.release()
        textRecognizer.close()
    }
}
