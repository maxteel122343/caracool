package com.example.service

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object GeminiAiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    /**
     * Generates a conversational custom unlock phrase using Gemini 3.5 Flash, citing the user's name
     */
    suspend fun generateConversationalGreeting(
        userName: String,
        unlockCount: Int,
        ordinalString: String,
        personality: String,
        timeOfDayGreeting: String,
        isKoolMode: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }

        val appName = if (isKoolMode) "Cara de Kool" else "Cara de Paçoca"
        val mascotCatchphrase = if (isKoolMode) "cara de kool" else "cara de paçoca"
        val defaultName = if (isKoolMode) "Cara de Kool" else "Cara de Paçoca"
        val resolvedName = if (userName.isNotBlank() && userName != "Você (Cara de Paçoca)" && userName != "Você (Cara de Kool)") userName.trim() else defaultName

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getFallbackGreeting(resolvedName, unlockCount, ordinalString, personality, isKoolMode)
        }

        try {
            val prompt = if (isKoolMode) {
                """
                    Você é a IA animada, sarcástica e divertida do aplicativo "Cara de Kool" (com tema rosa e monstrinho grumpy).
                    Gere uma frase curta (máximo 14 palavras) e muito divertida para falar quando o usuário desbloquear a tela do celular.
                    Informações essenciais:
                    - Nome do usuário: "$resolvedName". Você DEVE citar o nome "$resolvedName" na frase!
                    - É o $unlockCount º desbloqueio do dia (pela $ordinalString vez).
                    - Saudação de momento: $timeOfDayGreeting.
                    - Personalidade: $personality.
                    - REGRA ABSOLUTA: Use a expressão "cara de kool". NUNCA fale "cara de paçoca"!
                    - Retorne APENAS a frase em texto puro para ser lida por voz, sem aspas, sem formatação markdown.
                """.trimIndent()
            } else {
                """
                    Você é a IA animada e carismática do aplicativo "Cara de Paçoca".
                    Gere uma frase curta (máximo 14 palavras) e muito divertida para falar quando o usuário desbloquear a tela do celular.
                    Informações essenciais:
                    - Nome do usuário: "$resolvedName". Você DEVE citar o nome "$resolvedName" na frase!
                    - É o $unlockCount º desbloqueio do dia (pela $ordinalString vez).
                    - Saudação de momento: $timeOfDayGreeting.
                    - Personalidade: $personality (humorada, enérgica ou carinhosa).
                    - DEVE brincar com "cara de paçoca" e citar o nome "$resolvedName".
                    - Retorne APENAS a frase em texto puro para ser lida por voz, sem aspas, sem formatação markdown.
                """.trimIndent()
            }

            val jsonBody = """
                {
                    "contents": [{
                        "parts": [{
                            "text": ${moshi.adapter(String::class.java).toJson(prompt)}
                        }]
                    }],
                    "generationConfig": {
                        "temperature": 0.8,
                        "maxOutputTokens": 60
                    }
                }
            """.trimIndent()

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                val text = extractTextFromGeminiResponse(responseBody)
                if (text.isNotBlank()) {
                    return@withContext text.replace("\"", "").trim()
                }
            }
        } catch (e: Exception) {
            Log.w("GeminiAiService", "Failed to get AI phrase, using fallback: ${e.message}")
        }

        return@withContext getFallbackGreeting(resolvedName, unlockCount, ordinalString, personality, isKoolMode)
    }

    private fun extractTextFromGeminiResponse(json: String): String {
        return try {
            val regex = """"text"\s*:\s*"((?:\\.|[^"\\])*)"""".toRegex()
            val match = regex.find(json)
            match?.groups?.get(1)?.value?.replace("\\n", " ")?.replace("\\\"", "\"") ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    fun getFallbackGreeting(
        userName: String,
        count: Int,
        ordinal: String,
        personality: String,
        isKoolMode: Boolean = false
    ): String {
        val defaultName = if (isKoolMode) "Cara de Kool" else "Cara de Paçoca"
        val namePrefix = if (userName.isNotBlank() && userName != "Você (Cara de Paçoca)" && userName != "Você (Cara de Kool)" && userName != defaultName) "$userName, " else ""

        if (isKoolMode) {
            return when (personality) {
                "energetica" -> "${namePrefix}e aí cara de kool! Desbloqueado com energia rosa pela $ordinal vez!"
                "sarcastica" -> "${namePrefix}olha só essa cara de kool no celular pela $ordinal vez hoje..."
                "carinhosa" -> "${namePrefix}olá meu doce cara de kool! Desbloqueado pela $ordinal vez com muito amor!"
                else -> "${namePrefix}cara de kool desbloqueado pela $ordinal vez e contando!"
            }
        }

        return when (personality) {
            "energetica" -> "${namePrefix}e aí cara de paçoca! Desbloqueado com energia total pela $ordinal vez!"
            "sarcastica" -> "${namePrefix}olha só, o cara de paçoca não larga esse celular! Pela $ordinal vez hoje..."
            "carinhosa" -> "${namePrefix}olá meu doce cara de paçoca! Desbloqueado pela $ordinal vez com muito carinho."
            else -> "${namePrefix}cara de paçoca desbloqueado pela $ordinal vez e contando!"
        }
    }
}
