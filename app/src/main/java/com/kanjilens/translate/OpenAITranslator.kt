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
    private var mlKitModelReady = false

    suspend fun ensureOfflineModelReady() {
        if (mlKitModelReady && mlKitTranslator != null) return
        withContext(Dispatchers.IO) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.JAPANESE)
                .setTargetLanguage(TranslateLanguage.ENGLISH)
                .build()
            val translator = Translation.getClient(options)
            val conditions = DownloadConditions.Builder().build()
            translator.downloadModelIfNeeded(conditions).await()
            mlKitTranslator = translator
            mlKitModelReady = true
        }
    }

    suspend fun translateScreen(
        bitmap: Bitmap,
        apiKey: String,
        style: Int = STYLE_AUTO,
        model: Int = AppSettings.MODEL_GPT4O_MINI,
    ): TranslateResult {
        return withContext(Dispatchers.IO) {
            try {
                if (model == AppSettings.MODEL_MLKIT_OFFLINE) {
                    return@withContext translateOffline(bitmap)
                }

                val base64Image = bitmapToBase64(bitmap)
                val prompt = getSystemPrompt(style)

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

    private suspend fun translateOffline(bitmap: Bitmap): TranslateResult {
        val blocks = textRecognizer.recognizeTextBlocks(bitmap)
            ?: return TranslateResult.Error("No text found in screenshot")

        if (blocks.isEmpty()) {
            return TranslateResult.Error("No text found in screenshot")
        }

        try {
            ensureOfflineModelReady()
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

    private fun getSystemPrompt(style: Int): String {
        val baseRules = "Never be conversational. No greetings, no questions, no \"feel free to ask\", no \"let me know\". No markdown formatting."

        return when (style) {
            STYLE_TRANSLATE_ONLY -> {
                "You translate game screenshots to English. The text may be in any language (Japanese, Chinese, Korean, etc). " +
                    "Translate all visible text on screen. " +
                    "For menus, list each option translated. " +
                    "For dialogue, translate naturally. " +
                    "For stats, translate the labels and values. " +
                    "Only translate, do not explain or give advice. " +
                    baseRules
            }
            STYLE_TRANSLATE_AND_EXPLAIN -> {
                "You are a game assistant helping someone play a game that's not in their language. The screen may be in any language (Japanese, Chinese, Korean, etc).\n\n" +
                    "Rules:\n" +
                    "- First: translate all text on screen to English\n" +
                    "- Then: explain what you're looking at and what you should do to progress\n" +
                    "- For menus: translate each option and recommend which to pick\n" +
                    "- For dialogue/story: translate naturally, then summarize what's happening\n" +
                    "- For gameplay/instructions: translate and explain what the game wants you to do\n" +
                    "- For stats/progress: explain the key numbers and what they mean\n" +
                    "- Talk directly to the user using \"you\" (e.g. \"you need to select...\", \"your stats are...\")\n" +
                    "- Keep it concise but useful\n" +
                    "- $baseRules"
            }
            else -> { // AUTO
                "You are a game assistant helping someone play a game that's not in their language. The screen may be in any language (Japanese, Chinese, Korean, etc).\n\n" +
                    "Always do both:\n" +
                    "1. Translate all text on screen to English\n" +
                    "2. Briefly explain what you're seeing and what to do next\n\n" +
                    "- For menus: translate each option and say which one to pick to progress\n" +
                    "- For dialogue/story: translate naturally, then summarize what's happening\n" +
                    "- For gameplay/instructions: translate and explain what the game wants you to do\n" +
                    "- For stats/progress: explain the key numbers and what they mean\n" +
                    "- Talk directly to the user using \"you\" (e.g. \"you need to select...\", \"your health is...\")\n" +
                    "- Keep it concise — you just want to keep playing\n" +
                    "- $baseRules"
            }
        }
    }
}
