package com.kanjilens.data.models

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AppSettings(context: Context) {

    companion object {
        private const val PREFS_NAME = "kanjilens_prefs"
        private const val KEY_TEXT_SIZE = "text_size"
        private const val KEY_OPENAI_API_KEY = "openai_api_key_v2"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_APP_MODE = "app_mode"
        private const val KEY_TRANSLATE_STYLE = "translate_style"
        private const val KEY_AI_MODEL = "ai_model"

        const val TEXT_SIZE_SMALL = 0
        const val TEXT_SIZE_MEDIUM = 1
        const val TEXT_SIZE_LARGE = 2

        const val MODE_DICTIONARY = 0
        const val MODE_TRANSLATE = 1

        const val TRANSLATE_STYLE_AUTO = 0
        const val TRANSLATE_STYLE_TRANSLATE_ONLY = 1
        const val TRANSLATE_STYLE_TRANSLATE_AND_EXPLAIN = 2

        const val MODEL_GPT4O_MINI = 0
        const val MODEL_GEMINI_FLASH = 1
        const val MODEL_MLKIT_OFFLINE = 2
        const val MODEL_MLKIT_OFFLINE_AUTO = 3
        const val MODEL_OLLAMA = 4
        const val MODEL_CUSTOM = 5

        private const val KEY_OUTPUT_LANGUAGE = "output_language"
        private const val KEY_CROP_LEFT = "crop_left"
        private const val KEY_CROP_TOP = "crop_top"
        private const val KEY_CROP_RIGHT = "crop_right"
        private const val KEY_CROP_BOTTOM = "crop_bottom"
        private const val KEY_CROP_ENABLED = "crop_enabled"
        private const val KEY_OLLAMA_URL = "ollama_url"
        private const val KEY_OLLAMA_MODEL = "ollama_model"
        private const val KEY_OLLAMA_VISION = "ollama_vision"
        private const val KEY_CUSTOM_URL = "custom_url"
        private const val KEY_CUSTOM_API_KEY = "custom_api_key"
        private const val KEY_CUSTOM_MODEL = "custom_model"
        private const val KEY_CUSTOM_VISION = "custom_vision"

        const val LANG_ENGLISH = "en"
        const val LANG_SPANISH = "es"
        const val LANG_PORTUGUESE = "pt"
        const val LANG_FRENCH = "fr"
        const val LANG_GERMAN = "de"
        const val LANG_ITALIAN = "it"
        const val LANG_CHINESE = "zh"
        const val LANG_KOREAN = "ko"
        const val LANG_RUSSIAN = "ru"

        val OUTPUT_LANGUAGES = listOf(
            LANG_ENGLISH to "English",
            LANG_SPANISH to "Spanish",
            LANG_PORTUGUESE to "Portuguese",
            LANG_FRENCH to "French",
            LANG_GERMAN to "German",
            LANG_ITALIAN to "Italian",
            LANG_CHINESE to "Chinese",
            LANG_KOREAN to "Korean",
            LANG_RUSSIAN to "Russian",
        )

        fun languageDisplayName(code: String): String =
            OUTPUT_LANGUAGES.firstOrNull { it.first == code }?.second ?: "English"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _textSize = MutableStateFlow(prefs.getInt(KEY_TEXT_SIZE, TEXT_SIZE_MEDIUM))
    val textSize: StateFlow<Int> = _textSize

    private val _openaiApiKey = MutableStateFlow(prefs.getString(KEY_OPENAI_API_KEY, "") ?: "")
    val openaiApiKey: StateFlow<String> = _openaiApiKey

    private val _geminiApiKey = MutableStateFlow(prefs.getString(KEY_GEMINI_API_KEY, "") ?: "")
    val geminiApiKey: StateFlow<String> = _geminiApiKey

    private val _ollamaUrl = MutableStateFlow(prefs.getString(KEY_OLLAMA_URL, "http://192.168.1.x:11434") ?: "http://192.168.1.x:11434")
    val ollamaUrl: StateFlow<String> = _ollamaUrl

    private val _ollamaModel = MutableStateFlow(prefs.getString(KEY_OLLAMA_MODEL, "") ?: "")
    val ollamaModel: StateFlow<String> = _ollamaModel

    private val _ollamaVision = MutableStateFlow(prefs.getBoolean(KEY_OLLAMA_VISION, true))
    val ollamaVision: StateFlow<Boolean> = _ollamaVision

    private val _customUrl = MutableStateFlow(prefs.getString(KEY_CUSTOM_URL, "") ?: "")
    val customUrl: StateFlow<String> = _customUrl

    private val _customApiKey = MutableStateFlow(prefs.getString(KEY_CUSTOM_API_KEY, "") ?: "")
    val customApiKey: StateFlow<String> = _customApiKey

    private val _customModel = MutableStateFlow(prefs.getString(KEY_CUSTOM_MODEL, "") ?: "")
    val customModel: StateFlow<String> = _customModel

    private val _customVision = MutableStateFlow(prefs.getBoolean(KEY_CUSTOM_VISION, true))
    val customVision: StateFlow<Boolean> = _customVision

    private val _appMode = MutableStateFlow(prefs.getInt(KEY_APP_MODE, MODE_TRANSLATE))
    val appMode: StateFlow<Int> = _appMode

    private val _translateStyle = MutableStateFlow(prefs.getInt(KEY_TRANSLATE_STYLE, TRANSLATE_STYLE_AUTO))
    val translateStyle: StateFlow<Int> = _translateStyle

    private val _aiModel = MutableStateFlow(prefs.getInt(KEY_AI_MODEL, MODEL_MLKIT_OFFLINE))
    val aiModel: StateFlow<Int> = _aiModel

    private val _outputLanguage = MutableStateFlow(prefs.getString(KEY_OUTPUT_LANGUAGE, LANG_ENGLISH) ?: LANG_ENGLISH)
    val outputLanguage: StateFlow<String> = _outputLanguage

    // Crop region stored as percentages (0f..1f)
    private val _cropEnabled = MutableStateFlow(prefs.getBoolean(KEY_CROP_ENABLED, false))
    val cropEnabled: StateFlow<Boolean> = _cropEnabled

    private val _cropLeft = MutableStateFlow(prefs.getFloat(KEY_CROP_LEFT, 0f))
    private val _cropTop = MutableStateFlow(prefs.getFloat(KEY_CROP_TOP, 0f))
    private val _cropRight = MutableStateFlow(prefs.getFloat(KEY_CROP_RIGHT, 1f))
    private val _cropBottom = MutableStateFlow(prefs.getFloat(KEY_CROP_BOTTOM, 1f))

    data class CropRegion(val left: Float, val top: Float, val right: Float, val bottom: Float)

    val cropRegion: CropRegion
        get() = CropRegion(_cropLeft.value, _cropTop.value, _cropRight.value, _cropBottom.value)

    fun setCropRegion(left: Float, top: Float, right: Float, bottom: Float) {
        _cropEnabled.value = true
        _cropLeft.value = left
        _cropTop.value = top
        _cropRight.value = right
        _cropBottom.value = bottom
        prefs.edit()
            .putBoolean(KEY_CROP_ENABLED, true)
            .putFloat(KEY_CROP_LEFT, left)
            .putFloat(KEY_CROP_TOP, top)
            .putFloat(KEY_CROP_RIGHT, right)
            .putFloat(KEY_CROP_BOTTOM, bottom)
            .apply()
    }

    fun clearCropRegion() {
        _cropEnabled.value = false
        prefs.edit().putBoolean(KEY_CROP_ENABLED, false).apply()
    }

    /** Returns the API key for the currently selected model (empty for offline) */
    val activeApiKey: String
        get() = when (_aiModel.value) {
            MODEL_GEMINI_FLASH -> _geminiApiKey.value
            MODEL_MLKIT_OFFLINE, MODEL_MLKIT_OFFLINE_AUTO -> ""
            MODEL_OLLAMA -> ""
            MODEL_CUSTOM -> _customApiKey.value
            else -> _openaiApiKey.value
        }

    fun setTextSize(size: Int) {
        _textSize.value = size
        prefs.edit().putInt(KEY_TEXT_SIZE, size).apply()
    }

    fun setOpenaiApiKey(key: String) {
        _openaiApiKey.value = key
        prefs.edit().putString(KEY_OPENAI_API_KEY, key).apply()
    }

    fun setGeminiApiKey(key: String) {
        _geminiApiKey.value = key
        prefs.edit().putString(KEY_GEMINI_API_KEY, key).apply()
    }

    fun setAppMode(mode: Int) {
        _appMode.value = mode
        prefs.edit().putInt(KEY_APP_MODE, mode).apply()
    }

    fun setTranslateStyle(style: Int) {
        _translateStyle.value = style
        prefs.edit().putInt(KEY_TRANSLATE_STYLE, style).apply()
    }

    fun setAiModel(model: Int) {
        _aiModel.value = model
        prefs.edit().putInt(KEY_AI_MODEL, model).apply()
    }

    fun setOutputLanguage(lang: String) {
        _outputLanguage.value = lang
        prefs.edit().putString(KEY_OUTPUT_LANGUAGE, lang).apply()
    }

    fun setOllamaUrl(url: String) {
        _ollamaUrl.value = url
        prefs.edit().putString(KEY_OLLAMA_URL, url).apply()
    }

    fun setOllamaModel(model: String) {
        _ollamaModel.value = model
        prefs.edit().putString(KEY_OLLAMA_MODEL, model).apply()
    }

    fun setOllamaVision(vision: Boolean) {
        _ollamaVision.value = vision
        prefs.edit().putBoolean(KEY_OLLAMA_VISION, vision).apply()
    }

    fun setCustomUrl(url: String) {
        _customUrl.value = url
        prefs.edit().putString(KEY_CUSTOM_URL, url).apply()
    }

    fun setCustomApiKey(key: String) {
        _customApiKey.value = key
        prefs.edit().putString(KEY_CUSTOM_API_KEY, key).apply()
    }

    fun setCustomModel(model: String) {
        _customModel.value = model
        prefs.edit().putString(KEY_CUSTOM_MODEL, model).apply()
    }

    fun setCustomVision(vision: Boolean) {
        _customVision.value = vision
        prefs.edit().putBoolean(KEY_CUSTOM_VISION, vision).apply()
    }
}
