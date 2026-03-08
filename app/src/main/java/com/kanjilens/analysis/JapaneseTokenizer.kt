package com.kanjilens.analysis

import android.util.Log
import com.atilika.kuromoji.ipadic.Token
import com.atilika.kuromoji.ipadic.Tokenizer

class JapaneseTokenizer {

    companion object {
        private const val TAG = "KanjiLens"
    }

    private val tokenizer: Tokenizer by lazy {
        Log.d(TAG, "Tokenizer: Initializing Kuromoji...")
        val t = Tokenizer()
        Log.d(TAG, "Tokenizer: Ready")
        t
    }

    data class TokenResult(
        val surface: String,      // Word as it appears in text
        val baseForm: String,     // Dictionary form
        val reading: String,      // Reading in hiragana
        val partOfSpeech: String, // Part of speech
    )

    fun tokenize(text: String): List<TokenResult> {
        val tokens = tokenizer.tokenize(text)
        return tokens
            .filter { it.surface.isNotBlank() }
            .map { token ->
                TokenResult(
                    surface = token.surface,
                    baseForm = token.baseForm ?: token.surface,
                    reading = katakanaToHiragana(token.reading ?: ""),
                    partOfSpeech = token.partOfSpeechLevel1 ?: "",
                )
            }
            .filter { isContentWord(it) }
    }

    private fun isContentWord(token: TokenResult): Boolean {
        // Filter out punctuation and symbols
        val pos = token.partOfSpeech
        if (pos == "記号") return false // symbols/punctuation

        // Filter out pure whitespace
        if (token.surface.isBlank()) return false

        return true
    }

    private fun katakanaToHiragana(katakana: String): String {
        return buildString {
            for (char in katakana) {
                if (char in '\u30A1'..'\u30F6') {
                    // Katakana to Hiragana: subtract 0x60
                    append((char.code - 0x60).toChar())
                } else {
                    append(char)
                }
            }
        }
    }
}
