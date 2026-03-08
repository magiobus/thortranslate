package com.kanjilens.ui.screens

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.kanjilens.capture.ScreenCaptureManager
import com.kanjilens.capture.ScreenCaptureService
import com.kanjilens.data.models.AnalysisResult
import com.kanjilens.data.models.CaptureState
import com.kanjilens.ui.components.CaptureButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(captureManager: ScreenCaptureManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var captureState by remember { mutableStateOf<CaptureState>(CaptureState.Idle) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    fun doCapture() {
        scope.launch {
            captureState = CaptureState.Capturing
            // Small delay to let the VirtualDisplay initialize
            delay(200)
            val bitmap = captureManager.captureScreen()
            if (bitmap != null) {
                capturedBitmap = bitmap
                captureState = CaptureState.Success(
                    AnalysisResult(
                        originalText = "Screenshot captured (${bitmap.width}x${bitmap.height})",
                        words = emptyList(),
                    )
                )
            } else {
                captureState = CaptureState.Error("Failed to capture screen")
            }
        }
    }

    // MediaProjection permission launcher
    val projectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            // Register capture manager with service
            ScreenCaptureService.captureManager = captureManager

            // Start foreground service with projection data
            // Service will call startForeground() THEN getMediaProjection()
            val serviceIntent = Intent(context, ScreenCaptureService::class.java).apply {
                putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, result.data)
            }
            ContextCompat.startForegroundService(context, serviceIntent)

            // Wait for projection to be ready via callback, then capture
            captureState = CaptureState.Capturing
            captureManager.awaitProjectionReady {
                doCapture()
            }
        } else {
            captureState = CaptureState.Error("Permission denied")
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
                            text = "Capturing...",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    is CaptureState.Success -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = state.result.originalText,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                            )

                            // Show captured screenshot preview
                            capturedBitmap?.let { bitmap ->
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Captured screenshot",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 400.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.FillWidth,
                                )
                            }
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

            // Capture button at bottom
            CaptureButton(
                isProcessing = captureState is CaptureState.Capturing
                    || captureState is CaptureState.Processing,
                onClick = { onCaptureClick() },
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
    }
}
