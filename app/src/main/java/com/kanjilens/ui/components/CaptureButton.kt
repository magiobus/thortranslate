package com.kanjilens.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CaptureButton(
    isProcessing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isAutoMode: Boolean = false,
    onStopAuto: () -> Unit = {},
) {
    if (isAutoMode) {
        Button(
            onClick = onStopAuto,
            modifier = modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
            ),
            contentPadding = PaddingValues(horizontal = 24.dp),
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onError,
                    strokeWidth = 2.dp,
                    modifier = Modifier.height(24.dp).width(24.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Auto-translating...", fontSize = 18.sp, color = MaterialTheme.colorScheme.onError)
            } else {
                Text(
                    text = "Stop Auto",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onError,
                )
            }
        }
    } else {
        Button(
            onClick = onClick,
            enabled = !isProcessing,
            modifier = modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
            contentPadding = PaddingValues(horizontal = 24.dp),
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.height(24.dp).width(24.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Analyzing...", fontSize = 18.sp)
            } else {
                Text(
                    text = "Translate",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
