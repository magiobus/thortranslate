package com.kanjilens.translate

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class OpenAITranslator {

    companion object {
        const val STYLE_AUTO = 0
        const val STYLE_TRANSLATE_ONLY = 1
        const val STYLE_TRANSLATE_AND_EXPLAIN = 2
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun translateScreen(bitmap: Bitmap, apiKey: String, style: Int = STYLE_AUTO): String? {
        return withContext(Dispatchers.IO) {
            try {
                val base64Image = bitmapToBase64(bitmap)
                val requestBody = buildRequest(base64Image, style)

                val request = Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string()

                if (!response.isSuccessful || body == null) {
                    return@withContext null
                }

                val json = JSONObject(body)
                val choices = json.getJSONArray("choices")
                if (choices.length() > 0) {
                    choices.getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        // Scale down if too large to save tokens
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
            else -> { // AUTO - always translate + explain
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

    private fun buildRequest(base64Image: String, style: Int): JSONObject {
        val systemMessage = JSONObject().apply {
            put("role", "system")
            put("content", getSystemPrompt(style))
        }

        val imageContent = JSONArray().apply {
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

        val userMessage = JSONObject().apply {
            put("role", "user")
            put("content", imageContent)
        }

        return JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("messages", JSONArray().apply {
                put(systemMessage)
                put(userMessage)
            })
            put("max_tokens", 1000)
        }
    }
}
