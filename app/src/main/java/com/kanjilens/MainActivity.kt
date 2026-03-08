package com.kanjilens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kanjilens.capture.ScreenCaptureManager
import com.kanjilens.ocr.TextRecognizer
import com.kanjilens.ui.screens.MainScreen
import com.kanjilens.ui.theme.KanjiLensTheme

class MainActivity : ComponentActivity() {

    lateinit var captureManager: ScreenCaptureManager
    lateinit var textRecognizer: TextRecognizer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        captureManager = ScreenCaptureManager(this)
        textRecognizer = TextRecognizer()
        enableEdgeToEdge()
        setContent {
            KanjiLensTheme {
                MainScreen(
                    captureManager = captureManager,
                    textRecognizer = textRecognizer,
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        captureManager.release()
        textRecognizer.close()
    }
}
