package com.kanjilens.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kanjilens.data.models.AppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropScreen(
    screenshot: Bitmap,
    settings: AppSettings,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val imageBitmap = remember { screenshot.asImageBitmap() }
    val aspectRatio = screenshot.width.toFloat() / screenshot.height.toFloat()

    // Initialize from saved crop or default to full
    val savedCrop = settings.cropRegion
    val hasSavedCrop = settings.cropEnabled.value
    var startOffset by remember { mutableStateOf<Offset?>(
        if (hasSavedCrop) Offset(savedCrop.left, savedCrop.top) else null
    ) }
    var endOffset by remember { mutableStateOf<Offset?>(
        if (hasSavedCrop) Offset(savedCrop.right, savedCrop.bottom) else null
    ) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Region", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Instruction
            Text(
                text = "Drag on the image to select the area you want to translate",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            // Screenshot with crop overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspectRatio)
                        .clip(RoundedCornerShape(8.dp)),
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        canvasSize = size
                                        startOffset = Offset(
                                            offset.x / size.width,
                                            offset.y / size.height,
                                        )
                                        endOffset = startOffset
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        endOffset = Offset(
                                            (change.position.x / canvasSize.width).coerceIn(0f, 1f),
                                            (change.position.y / canvasSize.height).coerceIn(0f, 1f),
                                        )
                                    },
                                )
                            },
                    ) {
                        // Draw screenshot
                        drawImage(
                            image = imageBitmap,
                            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                        )

                        // Draw dim overlay outside selection
                        val s = startOffset
                        val e = endOffset
                        if (s != null && e != null) {
                            val left = (minOf(s.x, e.x) * size.width)
                            val top = (minOf(s.y, e.y) * size.height)
                            val right = (maxOf(s.x, e.x) * size.width)
                            val bottom = (maxOf(s.y, e.y) * size.height)

                            // Dim areas outside selection
                            val dimColor = Color.Black.copy(alpha = 0.5f)
                            // Top
                            drawRect(dimColor, Offset.Zero, Size(size.width, top))
                            // Bottom
                            drawRect(dimColor, Offset(0f, bottom), Size(size.width, size.height - bottom))
                            // Left
                            drawRect(dimColor, Offset(0f, top), Size(left, bottom - top))
                            // Right
                            drawRect(dimColor, Offset(right, top), Size(size.width - right, bottom - top))

                            // Selection border
                            drawRect(
                                color = Color.White,
                                topLeft = Offset(left, top),
                                size = Size(right - left, bottom - top),
                                style = Stroke(width = 3f),
                            )
                        }
                    }
                }
            }

            // Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Cancel
                Text(
                    text = "Cancel",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onCancel() }
                        .padding(vertical = 14.dp),
                )

                // Save
                val hasSelection = startOffset != null && endOffset != null
                Text(
                    text = "Save Region",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (hasSelection) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (hasSelection) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable {
                            if (hasSelection) {
                                val s = startOffset!!
                                val e = endOffset!!
                                settings.setCropRegion(
                                    left = minOf(s.x, e.x),
                                    top = minOf(s.y, e.y),
                                    right = maxOf(s.x, e.x),
                                    bottom = maxOf(s.y, e.y),
                                )
                                onSave()
                            }
                        }
                        .padding(vertical = 14.dp),
                )
            }
        }
    }
}
