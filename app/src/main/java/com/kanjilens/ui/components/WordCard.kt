package com.kanjilens.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kanjilens.data.models.AppSettings
import com.kanjilens.data.models.WordEntry

@Composable
fun WordCard(
    word: WordEntry,
    textSize: Int = AppSettings.TEXT_SIZE_MEDIUM,
    modifier: Modifier = Modifier,
) {
    val kanjiSize = when (textSize) {
        AppSettings.TEXT_SIZE_SMALL -> 18.sp
        AppSettings.TEXT_SIZE_LARGE -> 28.sp
        else -> 22.sp
    }
    val readingSize = when (textSize) {
        AppSettings.TEXT_SIZE_SMALL -> 11.sp
        AppSettings.TEXT_SIZE_LARGE -> 18.sp
        else -> 14.sp
    }
    val meaningSize = when (textSize) {
        AppSettings.TEXT_SIZE_SMALL -> 13.sp
        AppSettings.TEXT_SIZE_LARGE -> 20.sp
        else -> 16.sp
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Kanji + reading
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = word.surface,
                fontSize = kanjiSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (word.reading.isNotEmpty() && word.reading != word.surface) {
                Text(
                    text = word.reading,
                    fontSize = readingSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Meaning
        if (word.meaning.isNotEmpty()) {
            Text(
                text = word.meaning,
                fontSize = meaningSize,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1.5f).padding(horizontal = 8.dp),
            )
        }

        // JLPT badge
        word.jlptLevel?.let { level ->
            JlptBadge(level = level)
        }
    }
}

@Composable
fun JlptBadge(level: String) {
    val color = when (level) {
        "N5" -> Color(0xFF4CAF50)
        "N4" -> Color(0xFF8BC34A)
        "N3" -> Color(0xFFFFC107)
        "N2" -> Color(0xFFFF9800)
        "N1" -> Color(0xFFF44336)
        else -> Color.Gray
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = level,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}
