package com.kanjilens.ocr

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class TextRecognizer {

    companion object {
        private const val TAG = "KanjiLens"
    }

    private val recognizer = TextRecognition.getClient(
        JapaneseTextRecognizerOptions.Builder().build()
    )

    suspend fun recognizeText(bitmap: Bitmap): String? = suspendCancellableCoroutine { continuation ->
        val image = InputImage.fromBitmap(bitmap, 0)

        Log.d(TAG, "OCR: Starting text recognition on ${bitmap.width}x${bitmap.height} image")

        recognizer.process(image)
            .addOnSuccessListener { result ->
                val text = result.text.trim()
                Log.d(TAG, "OCR: Recognized ${result.textBlocks.size} blocks, text length=${text.length}")
                if (text.isNotEmpty()) {
                    Log.d(TAG, "OCR: Text = $text")
                    continuation.resume(text)
                } else {
                    Log.d(TAG, "OCR: No text found")
                    continuation.resume(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "OCR: Recognition failed", e)
                continuation.resume(null)
            }
    }

    fun close() {
        recognizer.close()
    }
}
