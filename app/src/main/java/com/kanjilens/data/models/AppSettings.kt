package com.kanjilens.data.models

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AppSettings(context: Context) {

    companion object {
        private const val PREFS_NAME = "kanjilens_prefs"
        private const val KEY_TEXT_SIZE = "text_size"
        private const val KEY_SHOW_PARTICLES = "show_particles"
        private const val KEY_DEEPL_API_KEY = "deepl_api_key"

        const val TEXT_SIZE_SMALL = 0
        const val TEXT_SIZE_MEDIUM = 1
        const val TEXT_SIZE_LARGE = 2
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _textSize = MutableStateFlow(prefs.getInt(KEY_TEXT_SIZE, TEXT_SIZE_MEDIUM))
    val textSize: StateFlow<Int> = _textSize

    private val _showParticles = MutableStateFlow(prefs.getBoolean(KEY_SHOW_PARTICLES, true))
    val showParticles: StateFlow<Boolean> = _showParticles

    private val _deeplApiKey = MutableStateFlow(prefs.getString(KEY_DEEPL_API_KEY, "") ?: "")
    val deeplApiKey: StateFlow<String> = _deeplApiKey

    fun setTextSize(size: Int) {
        _textSize.value = size
        prefs.edit().putInt(KEY_TEXT_SIZE, size).apply()
    }

    fun setShowParticles(show: Boolean) {
        _showParticles.value = show
        prefs.edit().putBoolean(KEY_SHOW_PARTICLES, show).apply()
    }

    fun setDeeplApiKey(key: String) {
        _deeplApiKey.value = key
        prefs.edit().putString(KEY_DEEPL_API_KEY, key).apply()
    }
}
