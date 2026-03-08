package com.kanjilens.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kanjilens.data.models.AnalysisResult
import com.kanjilens.data.models.CaptureState
import com.kanjilens.data.models.WordEntry
import com.kanjilens.ui.components.CaptureButton
import com.kanjilens.ui.components.TranslationResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    // TODO: Replace with ViewModel in Phase 2+
    var captureState by remember { mutableStateOf<CaptureState>(CaptureState.Idle) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "KanjiLens",
                        fontWeight = FontWeight.Bold,
                    )
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
            // Results area (scrollable)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center,
            ) {
                when (val state = captureState) {
                    is CaptureState.Idle -> {
                        Text(
                            text = "Press the button to capture\nand translate Japanese text",
                            textAlign = TextAlign.Center,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    is CaptureState.Capturing,
                    is CaptureState.Processing -> {
                        Text(
                            text = "Analyzing...",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    is CaptureState.Success -> {
                        TranslationResult(result = state.result)
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

            // Capture button at bottom
            CaptureButton(
                isProcessing = captureState is CaptureState.Capturing
                    || captureState is CaptureState.Processing,
                onClick = {
                    // TODO: Phase 2 - trigger actual screen capture
                    // For now, show mock data to validate UI
                    captureState = CaptureState.Success(
                        AnalysisResult(
                            originalText = "今回くらい、ピエールも家族とすごしてくれたらいいのに…",
                            words = listOf(
                                WordEntry("今回", "こんかい", "this time", "N3"),
                                WordEntry("くらい", "くらい", "about, approximately", "N4"),
                                WordEntry("ピエール", "", "Pierre (name)", null),
                                WordEntry("家族", "かぞく", "family", "N4"),
                                WordEntry("すごす", "すごす", "to spend (time)", "N3"),
                                WordEntry("くれる", "くれる", "to do for (me/us)", "N4"),
                                WordEntry("いい", "いい", "good", "N5"),
                                WordEntry("のに", "のに", "although, even though", "N3"),
                            ),
                        )
                    )
                },
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
    }
}
