package com.example.gemini

import android.util.Log
import com.example.data.model.TranscriptSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SmartPostProcessor(
    private val apiKey: String
) {
    companion object {
        private const val TAG = "SmartPostProcessor"
        private const val REST_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun polishTranscript(
        rawText: String,
        settings: TranscriptSettings
    ): Result<String> = withContext(Dispatchers.IO) {
        if (rawText.isBlank()) {
            return@withContext Result.success("")
        }

        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Gemini API key is not configured."))
        }

        try {
            val url = "$REST_URL?key=$apiKey"

            val langHint = when {
                settings.isAutoLanguageDetection -> "Automatically detect the spoken language. If mixed languages or loan words are present, format them naturally."
                settings.languageCode == "bn" -> "Language: Bengali (বাংলা). Preserve authentic phrasing."
                settings.languageCode == "hi" -> "Language: Hindi (हिन्दी). Preserve authentic phrasing."
                settings.languageCode == "en" -> "Language: English."
                else -> "Language code: ${settings.languageCode}."
            }

            val vocabInstruction = if (settings.customVocabulary.isNotEmpty()) {
                "Custom vocabulary terms (spell exactly as specified if mentioned): ${settings.customVocabulary.joinToString(", ")}."
            } else ""

            val prompt = buildString {
                appendLine("You are the Gemini SMART Speech-to-Text Post-Processor.")
                appendLine("Your task is to take the provided raw spoken transcript and produce a clean, publication-quality text output while strictly preserving the speaker's original meaning and intent.")
                appendLine()
                appendLine("RULES:")
                if (settings.isAutoPunctuation) {
                    appendLine("1. Add standard grammatical punctuation, appropriate capitalization, and sentence boundaries.")
                }
                if (settings.isRemoveFillerWords) {
                    appendLine("2. Strip unnecessary filler words (e.g., 'um', 'uh', 'er', 'ah', 'like', 'you know', 'basically', 'so yeah', etc.).")
                    appendLine("3. Eliminate stuttering, false starts, and duplicate repeated words.")
                }
                appendLine("4. Resolve spoken self-corrections naturally into the speaker's intended statement.")
                appendLine("   Example: 'uh I think we should meet tomorrow at three pm actually no Wednesday at three'")
                appendLine("   Output: 'I think we should meet on Wednesday at 3:00 PM.'")
                appendLine("5. If the speaker dictates a list (e.g., 'item one...', 'first...', 'bullet point...'), format it neatly as a markdown list.")
                appendLine("6. Break long monologues into clean, logical paragraphs.")
                appendLine("7. Strictly NEVER invent facts, names, or statements that were not expressed.")
                appendLine("8. $langHint")
                if (vocabInstruction.isNotBlank()) {
                    appendLine("9. $vocabInstruction")
                }
                appendLine("10. Return ONLY the polished final text. Do NOT include markdown code blocks, preamble, notes, or explanations.")
                appendLine()
                appendLine("RAW TRANSCRIPT:")
                appendLine(rawText)
            }

            val requestJson = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject()

            partObj.put("text", prompt)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            requestJson.put("contents", contentsArray)

            val genConfig = JSONObject()
            genConfig.put("temperature", 0.1)
            requestJson.put("generationConfig", genConfig)

            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Smart polish request failed with code ${response.code}: $responseBody")
                return@withContext Result.failure(Exception("API Error (${response.code})"))
            }

            val responseJson = JSONObject(responseBody)
            val candidates = responseJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val polishedText = parts?.optJSONObject(0)?.optString("text")?.trim()

            if (!polishedText.isNullOrBlank()) {
                Result.success(polishedText)
            } else {
                Result.success(rawText)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during smart polish", e)
            Result.failure(e)
        }
    }
}
