package com.ainest.aivideolibrary.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

enum class AiProvider { GEMINI, CLAUDE }

data class AiAutoFillResult(val title: String, val hashtags: String, val keywords: String)

object AiAutoFillUtil {

    const val DEFAULT_TEMPLATE = """You are helping generate metadata for a short-form AI-generated vertical video (9:16, for TikTok/Instagram/YouTube Shorts/Facebook). Given the video prompt below, produce:
- Title: short, catchy, curiosity-driven, under 8 words
- Hashtags: exactly 4-5, relevant and commonly used for this content type
- Keywords: 10-15, each on its own line, covering subject, style, mood, and setting

Respond ONLY as strict JSON with this exact shape, no other text:
{"title": "...", "hashtags": "#tag1 #tag2 #tag3 #tag4", "keywords": "keyword1\nkeyword2\n..."}

Video prompt: "%PROMPT%""""

    suspend fun generate(
        provider: AiProvider,
        apiKey: String,
        prompt: String,
        template: String = DEFAULT_TEMPLATE
    ): Result<AiAutoFillResult> = withContext(Dispatchers.IO) {
        try {
            val fullPrompt = template.replace("%PROMPT%", prompt)
            val responseText = when (provider) {
                AiProvider.GEMINI -> callGemini(apiKey, fullPrompt)
                AiProvider.CLAUDE -> callClaude(apiKey, fullPrompt)
            }
            val json = extractJson(responseText)
            Result.success(
                AiAutoFillResult(
                    title = json.optString("title"),
                    hashtags = json.optString("hashtags"),
                    keywords = json.optString("keywords")
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun callGemini(apiKey: String, prompt: String): String {
        val url = URL(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
        )
        val body = JSONObject().apply {
            put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", prompt))
                    )
                )
            )
        }
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
            connectTimeout = 20000
            readTimeout = 20000
        }
        conn.outputStream.use { it.write(body.toString().toByteArray()) }
        val responseCode = conn.responseCode
        val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
        val text = stream.bufferedReader().use { it.readText() }
        if (responseCode !in 200..299) throw RuntimeException("Gemini error $responseCode: $text")
        val root = JSONObject(text)
        return root.getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
    }

    private fun callClaude(apiKey: String, prompt: String): String {
        val url = URL("https://api.anthropic.com/v1/messages")
        val body = JSONObject().apply {
            put("model", "claude-haiku-4-5-20251001")
            put("max_tokens", 500)
            put(
                "messages",
                JSONArray().put(
                    JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    }
                )
            )
        }
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-api-key", apiKey)
            setRequestProperty("anthropic-version", "2023-06-01")
            doOutput = true
            connectTimeout = 20000
            readTimeout = 20000
        }
        conn.outputStream.use { it.write(body.toString().toByteArray()) }
        val responseCode = conn.responseCode
        val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
        val text = stream.bufferedReader().use { it.readText() }
        if (responseCode !in 200..299) throw RuntimeException("Claude error $responseCode: $text")
        val root = JSONObject(text)
        return root.getJSONArray("content").getJSONObject(0).getString("text")
    }

    /** Models sometimes wrap JSON in markdown fences; strip those before parsing. */
    private fun extractJson(raw: String): JSONObject {
        val cleaned = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        return JSONObject(cleaned)
    }
}
