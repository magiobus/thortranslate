package com.kanjilens.data.models

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AppSettings(context: Context) {

    companion object {
        private const val PREFS_NAME = "kanjilens_prefs"
        private const val KEY_TEXT_SIZE = "text_size"
        private const val KEY_OPENAI_API_KEY = "openai_api_key"
        private const val KEY_APP_MODE = "app_mode"
        private const val KEY_TRANSLATE_STYLE = "translate_style"

        const val TEXT_SIZE_SMALL = 0
        const val TEXT_SIZE_MEDIUM = 1
        const val TEXT_SIZE_LARGE = 2

        const val MODE_DICTIONARY = 0
        const val MODE_TRANSLATE = 1

        const val TRANSLATE_STYLE_AUTO = 0
        const val TRANSLATE_STYLE_TRANSLATE_ONLY = 1
        const val TRANSLATE_STYLE_TRANSLATE_AND_EXPLAIN = 2
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _textSize = MutableStateFlow(prefs.getInt(KEY_TEXT_SIZE, TEXT_SIZE_MEDIUM))
    val textSize: StateFlow<Int> = _textSize

    private val _openaiApiKey = MutableStateFlow(prefs.getString(KEY_OPENAI_API_KEY, "") ?: "")
    val openaiApiKey: StateFlow<String> = _openaiApiKey

    private val _appMode = MutableStateFlow(prefs.getInt(KEY_APP_MODE, MODE_TRANSLATE))
    val appMode: StateFlow<Int> = _appMode

    private val _translateStyle = MutableStateFlow(prefs.getInt(KEY_TRANSLATE_STYLE, TRANSLATE_STYLE_AUTO))
    val translateStyle: StateFlow<Int> = _translateStyle

    fun setTextSize(size: Int) {
        _textSize.value = size
        prefs.edit().putInt(KEY_TEXT_SIZE, size).apply()
    }

    fun setOpenaiApiKey(key: String) {
        _openaiApiKey.value = key
        prefs.edit().putString(KEY_OPENAI_API_KEY, key).apply()
    }

    fun setAppMode(mode: Int) {
        _appMode.value = mode
        prefs.edit().putInt(KEY_APP_MODE, mode).apply()
    }

    fun setTranslateStyle(style: Int) {
        _translateStyle.value = style
        prefs.edit().putInt(KEY_TRANSLATE_STYLE, style).apply()
    }
}
