package com.kanjilens.analysis

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kanjilens.data.models.WordEntry

class DictionaryLookup(context: Context) {

    companion object {
        private const val TAG = "KanjiLens"
    }

    // Lookup maps for fast search: kanji/surface -> [reading, meaning]
    private val byKanji: Map<String, Pair<String, String>>
    private val byReading: Map<String, Pair<String, String>>

    init {
        Log.d(TAG, "Dictionary: Loading...")
        val json = context.assets.open("dictionary.json").bufferedReader().readText()
        val type = object : TypeToken<List<List<String>>>() {}.type
        val entries: List<List<String>> = Gson().fromJson(json, type)

        // Index by kanji and by reading for fast lookup
        // Format: [kanji, reading, meaning]
        val kanjiMap = mutableMapOf<String, Pair<String, String>>()
        val readingMap = mutableMapOf<String, Pair<String, String>>()

        for (entry in entries) {
            if (entry.size < 3) continue
            val kanji = entry[0]
            val reading = entry[1]
            val meaning = entry[2]
            val pair = Pair(reading, meaning)

            kanjiMap.putIfAbsent(kanji, pair)
            readingMap.putIfAbsent(reading, pair)
        }

        byKanji = kanjiMap
        byReading = readingMap

        Log.d(TAG, "Dictionary: Loaded ${entries.size} entries")
    }

    fun lookup(surface: String, baseForm: String, reading: String): WordEntry? {
        // Try exact match on base form first (dictionary form)
        byKanji[baseForm]?.let { (r, m) ->
            return WordEntry(surface = baseForm, reading = r, meaning = m, jlptLevel = null)
        }

        // Try surface form
        byKanji[surface]?.let { (r, m) ->
            return WordEntry(surface = surface, reading = r, meaning = m, jlptLevel = null)
        }

        // Try reading match
        if (reading.isNotEmpty()) {
            byReading[reading]?.let { (r, m) ->
                return WordEntry(surface = surface, reading = r, meaning = m, jlptLevel = null)
            }
        }

        return null
    }

    fun lookupTokens(tokens: List<JapaneseTokenizer.TokenResult>): List<WordEntry> {
        return tokens.map { token ->
            lookup(token.surface, token.baseForm, token.reading)
                ?: WordEntry(
                    surface = token.surface,
                    reading = token.reading,
                    meaning = "",
                    jlptLevel = null,
                )
        }
    }
}
