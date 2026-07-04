package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit

data class GeminiMember(
    var tempId: Long,
    var firstName: String,
    var lastName: String = "",
    var gender: String, // "Male" / "Female" / "Other"
    var fatherTempId: Long? = null,
    var motherTempId: Long? = null,
    var spouseTempId: Long? = null
)

object GeminiService {
    private const val TAG = "GeminiService"
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // Helper to read and prepare image as base64
    private fun getBase64Image(imagePath: String): String? {
        return try {
            val file = File(imagePath)
            if (!file.exists()) return null
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
            
            // Resize if it is very large to save tokens & upload time while keeping it readable (e.g. max 1024px)
            val maxDim = 1024
            val resized = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                val (w, h) = if (ratio > 1) {
                    Pair(maxDim, (maxDim / ratio).toInt())
                } else {
                    Pair((maxDim * ratio).toInt(), maxDim)
                }
                Bitmap.createScaledBitmap(bitmap, w, h, true)
            } else {
                bitmap
            }

            val byteArrayOutputStream = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream)
            val bytes = byteArrayOutputStream.toByteArray()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding image to base64", e)
            null
        }
    }

    suspend fun scanLineageTreeImage(context: Context, imagePath: String): List<GeminiMember> = withContext(Dispatchers.IO) {
        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey.contains("PLACEHOLDER")) {
            Log.e(TAG, "Gemini API key is missing or is placeholder!")
            return@withContext emptyList()
        }

        val base64Data = getBase64Image(imagePath)
        if (base64Data == null) {
            Log.e(TAG, "Failed to load/encode image of handmade tree.")
            return@withContext emptyList()
        }

        val promptText = """
            You are an expert genealogist and OCR family tree diagram analyzer.
            You are looking at an image containing a handwritten or drawn family tree diagram or lineage list.
            
            Please analyze the image carefully to extract every member and their relationships.
            Translate or keep the names in clean Hindi Script (e.g. चिराम, राम, शर्मा) or English. Prefer Hindi as the user is Hindi speaking.
            For each person, extract:
            1. Name (firstName, optionally lastName).
            2. Gender ("Male" or "Female").
            3. Relationships (Father, Mother, Spouse).
            
            To link parents and spouses correctly in JSON:
            - Assign each unique person a starting numeric ID ("tempId" starting from 1).
            - Use the assigned "tempId" to reference relationships:
              - "fatherTempId": the tempId of their father.
              - "motherTempId": the tempId of their mother.
              - "spouseTempId": the tempId of their husband or wife.

            Return ONLY a valid JSON object matching the following structure under a root field "members":
            {
              "members": [
                {
                  "tempId": 1,
                  "firstName": "राम",
                  "lastName": "शर्मा",
                  "gender": "Male",
                  "fatherTempId": null,
                  "motherTempId": null,
                  "spouseTempId": 2
                },
                {
                  "tempId": 2,
                  "firstName": "सीता",
                  "lastName": "शर्मा",
                  "gender": "Female",
                  "fatherTempId": null,
                  "motherTempId": null,
                  "spouseTempId": 1
                }
              ]
            }
            Do not write any markdown codeblocks or conversational text in the response. Return raw JSON text only.
        """.trimIndent()

        val requestJson = """
            {
              "contents": [{
                "parts": [
                  { "text": ${escapeJsonString(promptText)} },
                  {
                    "inlineData": {
                      "mimeType": "image/jpeg",
                      "data": "$base64Data"
                    }
                  }
                ]
              }],
              "generationConfig": {
                "responseMimeType": "application/json"
              }
            }
        """.trimIndent()

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val requestBody = requestJson.toRequestBody("application/json".toMediaType())
        val httpRequest = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .build()

        try {
            val response: Response = okHttpClient.newCall(httpRequest).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API call failed: code = ${response.code}, message = ${response.message}")
                return@withContext emptyList()
            }
            val bodyString = response.body?.string() ?: ""
            Log.d(TAG, "Gemini Raw Response: $bodyString")
            
            val textContent = extractTextFromGeminiResponse(bodyString)
            if (textContent.isBlank()) {
                Log.e(TAG, "No candidate content returned or empty response.")
                return@withContext emptyList()
            }

            Log.d(TAG, "Extracted Text (JSON): $textContent")
            val parsed = parseGeminiMembersJson(textContent)
            return@withContext parsed
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini API", e)
            return@withContext emptyList()
        }
    }

    private fun escapeJsonString(input: String): String {
        return moshi.adapter(String::class.java).toJson(input)
    }

    private fun extractTextFromGeminiResponse(rawJson: String): String {
        return try {
            val adapter = moshi.adapter(Map::class.java)
            val root = adapter.fromJson(rawJson) ?: return ""
            val candidates = root["candidates"] as? List<*> ?: return ""
            if (candidates.isEmpty()) return ""
            val candidate = candidates[0] as? Map<*, *> ?: return ""
            val content = candidate["content"] as? Map<*, *> ?: return ""
            val parts = content["parts"] as? List<*> ?: return ""
            if (parts.isEmpty()) return ""
            val part = parts[0] as? Map<*, *> ?: return ""
            (part["text"] as? String) ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text content from Gemini responseJson", e)
            ""
        }
    }

    private fun parseGeminiMembersJson(jsonText: String): List<GeminiMember> {
        return try {
            val adapter = moshi.adapter(Map::class.java)
            val parsedMap = adapter.fromJson(jsonText) ?: return emptyList()
            val membersList = parsedMap["members"] as? List<*> ?: return emptyList()
            
            val result = mutableListOf<GeminiMember>()
            for (item in membersList) {
                val m = item as? Map<*, *> ?: continue
                val tempId = when (val id = m["tempId"]) {
                    is Double -> id.toLong()
                    is Int -> id.toLong()
                    is String -> id.toLongOrNull() ?: 0L
                    else -> 0L
                }
                val firstName = (m["firstName"] as? String) ?: ""
                val lastName = (m["lastName"] as? String) ?: ""
                val gender = (m["gender"] as? String) ?: "Male"
                
                val fatherTempId = when (val id = m["fatherTempId"]) {
                    is Double -> id.toLong()
                    is Int -> id.toLong()
                    is String -> id.toLongOrNull()
                    else -> null
                }
                val motherTempId = when (val id = m["motherTempId"]) {
                    is Double -> id.toLong()
                    is Int -> id.toLong()
                    is String -> id.toLongOrNull()
                    else -> null
                }
                val spouseTempId = when (val id = m["spouseTempId"]) {
                    is Double -> id.toLong()
                    is Int -> id.toLong()
                    is String -> id.toLongOrNull()
                    else -> null
                }
                
                if (firstName.isNotBlank()) {
                    result.add(
                        GeminiMember(
                            tempId = tempId,
                            firstName = firstName,
                            lastName = lastName,
                            gender = gender,
                            fatherTempId = fatherTempId,
                            motherTempId = motherTempId,
                            spouseTempId = spouseTempId
                        )
                    )
                }
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing extracted JSON", e)
            emptyList()
        }
    }
}
