package com.kanjilens.data.models

data class WordEntry(
    val surface: String,       // The word as it appears (e.g., 家族)
    val reading: String,       // Hiragana reading (e.g., かぞく)
    val meaning: String,       // English meaning (e.g., "family")
    val jlptLevel: String?,    // JLPT level (e.g., "N4") or null
)

data class AnalysisResult(
    val originalText: String,
    val words: List<WordEntry>,
    val fullTranslation: String? = null, // Only when online
)

sealed class CaptureState {
    data object Idle : CaptureState()
    data object Capturing : CaptureState()
    data object Processing : CaptureState()
    data class Success(val result: AnalysisResult) : CaptureState()
    data class Error(val message: String) : CaptureState()
}
