package com.kanjilens.translate

import android.graphics.Bitmap
import android.util.Base64
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.kanjilens.data.models.AppSettings
import com.kanjilens.ocr.TextRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

sealed class TranslateResult {
    data class Success(val text: String) : TranslateResult()
    data class Error(val message: String) : TranslateResult()
}

class ScreenTranslator(
    private val textRecognizer: TextRecognizer,
) {

    companion object {
        const val STYLE_AUTO = 0
        const val STYLE_TRANSLATE_ONLY = 1
        const val STYLE_TRANSLATE_AND_EXPLAIN = 2
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private var mlKitTranslator: com.google.mlkit.nl.translate.Translator? = null
    private var mlKitCurrentTargetLang: String? = null

    private fun mlKitLanguageCode(appLangCode: String): String {
        return when (appLangCode) {
            AppSettings.LANG_ENGLISH -> TranslateLanguage.ENGLISH
            AppSettings.LANG_SPANISH -> TranslateLanguage.SPANISH
            AppSettings.LANG_PORTUGUESE -> TranslateLanguage.PORTUGUESE
            AppSettings.LANG_FRENCH -> TranslateLanguage.FRENCH
            AppSettings.LANG_GERMAN -> TranslateLanguage.GERMAN
            AppSettings.LANG_ITALIAN -> TranslateLanguage.ITALIAN
            AppSettings.LANG_CHINESE -> TranslateLanguage.CHINESE
            AppSettings.LANG_KOREAN -> TranslateLanguage.KOREAN
            AppSettings.LANG_RUSSIAN -> TranslateLanguage.RUSSIAN
            else -> TranslateLanguage.ENGLISH
        }
    }

    suspend fun ensureOfflineModelReady(targetLang: String = AppSettings.LANG_ENGLISH) {
        val mlKitLang = mlKitLanguageCode(targetLang)
        if (mlKitTranslator != null && mlKitCurrentTargetLang == mlKitLang) return
        withContext(Dispatchers.IO) {
            mlKitTranslator?.close()
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.JAPANESE)
                .setTargetLanguage(mlKitLang)
                .build()
            val translator = Translation.getClient(options)
            val conditions = DownloadConditions.Builder().build()
            translator.downloadModelIfNeeded(conditions).await()
            mlKitTranslator = translator
            mlKitCurrentTargetLang = mlKitLang
        }
    }

    suspend fun translateScreen(
        bitmap: Bitmap,
        apiKey: String,
        style: Int = STYLE_AUTO,
        model: Int = AppSettings.MODEL_GPT4O_MINI,
        outputLanguage: String = AppSettings.LANG_ENGLISH,
        ollamaUrl: String = "",
        ollamaModel: String = "",
        ollamaVision: Boolean = true,
        customUrl: String = "",
        customApiKey: String = "",
        customModel: String = "",
        customVision: Boolean = true,
        onDownloading: (() -> Unit)? = null,
    ): TranslateResult {
        return withContext(Dispatchers.IO) {
            try {
                if (model == AppSettings.MODEL_MLKIT_OFFLINE) {
                    return@withContext translateOffline(bitmap, outputLanguage, onDownloading)
                }

                if (model == AppSettings.MODEL_OLLAMA || model == AppSettings.MODEL_CUSTOM) {
                    val endpoint = if (model == AppSettings.MODEL_OLLAMA) {
                        "${ollamaUrl.trimEnd('/')}/v1/chat/completions"
                    } else {
                        "${customUrl.trimEnd('/')}/v1/chat/completions"
                    }
                    val key = if (model == AppSettings.MODEL_CUSTOM) customApiKey else ""
                    val modelName = if (model == AppSettings.MODEL_OLLAMA) ollamaModel else customModel
                    val vision = if (model == AppSettings.MODEL_OLLAMA) ollamaVision else customVision
                    val prompt = getSystemPrompt(style, outputLanguage)

                    val base64 = if (vision) bitmapToBase64(bitmap) else ""
                    val ocrText = if (!vision) textRecognizer.recognizeText(bitmap) else null

                    val result = callOpenAICompatible(base64, endpoint, key, modelName, vision, prompt, ocrText)
                    return@withContext if (result != null) {
                        TranslateResult.Success(result)
                    } else {
                        TranslateResult.Error("Translation failed. Check your endpoint and model settings.")
                    }
                }

                val base64Image = bitmapToBase64(bitmap)
                val prompt = getSystemPrompt(style, outputLanguage)

                val result = when (model) {
                    AppSettings.MODEL_GEMINI_FLASH -> callGemini(base64Image, apiKey, prompt)
                    else -> callOpenAI(base64Image, apiKey, prompt)
                }

                if (result != null) {
                    TranslateResult.Success(result)
                } else {
                    TranslateResult.Error("Translation failed. Check your API key.")
                }
            } catch (e: UnknownHostException) {
                TranslateResult.Error("No internet connection")
            } catch (e: java.net.SocketTimeoutException) {
                TranslateResult.Error("Connection timed out. Try again.")
            } catch (e: Exception) {
                e.printStackTrace()
                TranslateResult.Error("Translation failed: ${e.message ?: "unknown error"}")
            }
        }
    }

    private suspend fun translateOffline(
        bitmap: Bitmap,
        outputLanguage: String = AppSettings.LANG_ENGLISH,
        onDownloading: (() -> Unit)? = null,
    ): TranslateResult {
        val blocks = textRecognizer.recognizeTextBlocks(bitmap)
            ?: return TranslateResult.Error("No text found in screenshot")

        if (blocks.isEmpty()) {
            return TranslateResult.Error("No text found in screenshot")
        }

        val needsDownload = mlKitTranslator == null || mlKitCurrentTargetLang != mlKitLanguageCode(outputLanguage)
        if (needsDownload) {
            withContext(Dispatchers.Main) { onDownloading?.invoke() }
        }

        try {
            ensureOfflineModelReady(outputLanguage)
        } catch (e: Exception) {
            return TranslateResult.Error("Download the offline model first. Connect to WiFi and try again.")
        }

        val translator = mlKitTranslator
            ?: return TranslateResult.Error("Offline translator not available")

        return try {
            val result = StringBuilder()
            for (block in blocks) {
                val translated = translator.translate(block).await()
                result.appendLine(block)
                result.appendLine(translated)
                result.appendLine()
            }
            TranslateResult.Success(result.toString().trimEnd())
        } catch (e: Exception) {
            TranslateResult.Error("Offline translation failed: ${e.message ?: "unknown error"}")
        }
    }

    private fun callOpenAI(base64Image: String, apiKey: String, systemPrompt: String): String? {
        val body = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("max_tokens", 1000)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", "Translate this game screen.")
                        })
                        put(JSONObject().apply {
                            put("type", "image_url")
                            put("image_url", JSONObject().apply {
                                put("url", "data:image/jpeg;base64,$base64Image")
                                put("detail", "low")
                            })
                        })
                    })
                })
            })
        }

        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return null
        if (!response.isSuccessful) return null

        val json = JSONObject(responseBody)
        val choices = json.getJSONArray("choices")
        return if (choices.length() > 0) {
            choices.getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        } else null
    }

    private fun callGemini(base64Image: String, apiKey: String, systemPrompt: String): String? {
        val body = JSONObject().apply {
            put("system_instruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemPrompt) })
                })
            })
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "Translate this game screen.")
                        })
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("maxOutputTokens", 1000)
            })
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return null
        if (!response.isSuccessful) return null

        val json = JSONObject(responseBody)
        val candidates = json.optJSONArray("candidates") ?: return null
        if (candidates.length() == 0) return null

        return candidates.getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
    }

    private fun callOpenAICompatible(
        base64Image: String,
        endpoint: String,
        apiKey: String,
        model: String,
        vision: Boolean,
        systemPrompt: String,
        ocrText: String? = null,
    ): String? {
        val userContent = if (vision) {
            JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "text")
                    put("text", "Translate this game screen.")
                })
                put(JSONObject().apply {
                    put("type", "image_url")
                    put("image_url", JSONObject().apply {
                        put("url", "data:image/jpeg;base64,$base64Image")
                        put("detail", "low")
                    })
                })
            }
        } else {
            ocrText ?: return null
        }

        val body = JSONObject().apply {
            put("model", model)
            put("max_tokens", 1000)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    if (vision) {
                        put("content", userContent)
                    } else {
                        put("content", "Translate this game screen text:\n\n$userContent")
                    }
                })
            })
        }

        val requestBuilder = Request.Builder()
            .url(endpoint)
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))

        if (apiKey.isNotEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $apiKey")
        }

        val response = client.newCall(requestBuilder.build()).execute()
        val responseBody = response.body?.string() ?: return null
        if (!response.isSuccessful) return null

        val json = JSONObject(responseBody)
        val choices = json.optJSONArray("choices") ?: return null
        return if (choices.length() > 0) {
            choices.getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        } else null
    }

    suspend fun fetchOllamaModels(baseUrl: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/api/tags")
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()
            val body = response.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(body)
            val models = json.optJSONArray("models") ?: return@withContext emptyList()
            (0 until models.length()).map { models.getJSONObject(it).getString("name") }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun fetchOpenAIModels(baseUrl: String, apiKey: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val requestBuilder = Request.Builder().url("$baseUrl/v1/models")
            if (apiKey.isNotEmpty()) requestBuilder.addHeader("Authorization", "Bearer $apiKey")
            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) return@withContext emptyList()
            val body = response.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(body)
            val data = json.optJSONArray("data") ?: return@withContext emptyList()
            (0 until data.length()).map { data.getJSONObject(it).getString("id") }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        val scaled = if (bitmap.width > 1024) {
            val ratio = 1024f / bitmap.width
            Bitmap.createScaledBitmap(
                bitmap,
                1024,
                (bitmap.height * ratio).toInt(),
                true,
            )
        } else {
            bitmap
        }
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    private fun getSystemPrompt(style: Int, outputLanguage: String = AppSettings.LANG_ENGLISH): String {
        val langName = AppSettings.languageDisplayName(outputLanguage)
        val baseRules = "Never be conversational. No greetings, no questions, no \"feel free to ask\", no \"let me know\". No markdown formatting."

        return when (style) {
            STYLE_TRANSLATE_ONLY -> {
                "You translate game screenshots to $langName. The text may be in any language (Japanese, Chinese, Korean, etc). " +
                    "Translate all visible text on screen. " +
                    "For menus, list each option translated. " +
                    "For dialogue, translate naturally. " +
                    "For stats, translate the labels and values. " +
                    "Only translate, do not explain or give advice. " +
                    "Always respond in $langName. " +
                    baseRules
            }
            STYLE_TRANSLATE_AND_EXPLAIN -> {
                "You are a game assistant helping someone play a game that's not in their language. The screen may be in any language (Japanese, Chinese, Korean, etc).\n\n" +
                    "Rules:\n" +
                    "- First: translate all text on screen to $langName\n" +
                    "- Then: explain what you're looking at and what you should do to progress\n" +
                    "- For menus: translate each option and recommend which to pick\n" +
                    "- For dialogue/story: translate naturally, then summarize what's happening\n" +
                    "- For gameplay/instructions: translate and explain what the game wants you to do\n" +
                    "- For stats/progress: explain the key numbers and what they mean\n" +
                    "- Talk directly to the user using \"you\" (e.g. \"you need to select...\", \"your stats are...\")\n" +
                    "- Keep it concise but useful\n" +
                    "- Always respond in $langName\n" +
                    "- $baseRules"
            }
            else -> { // AUTO
                "You are a game assistant helping someone play a game that's not in their language. The screen may be in any language (Japanese, Chinese, Korean, etc).\n\n" +
                    "Always do both:\n" +
                    "1. Translate all text on screen to $langName\n" +
                    "2. Briefly explain what you're seeing and what to do next\n\n" +
                    "- For menus: translate each option and say which one to pick to progress\n" +
                    "- For dialogue/story: translate naturally, then summarize what's happening\n" +
                    "- For gameplay/instructions: translate and explain what the game wants you to do\n" +
                    "- For stats/progress: explain the key numbers and what they mean\n" +
                    "- Talk directly to the user using \"you\" (e.g. \"you need to select...\", \"your health is...\")\n" +
                    "- Keep it concise — you just want to keep playing\n" +
                    "- Always respond in $langName\n" +
                    "- $baseRules"
            }
        }
    }
}
