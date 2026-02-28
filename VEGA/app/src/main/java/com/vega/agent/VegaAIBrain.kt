package com.vega.agent

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * VegaAIBrain — Groq free API se real AI response leta hai
 * Groq = Ultra-fast inference, FREE tier available
 * Model: llama3-8b-8192 (Ollama ka hi model, cloud pe free!)
 *
 * FREE API key lene ke liye: https://console.groq.com/keys
 */
object VegaAIBrain {

    private const val TAG = "VegaAIBrain"
    private const val GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions"

    // ⚠️ Apni free Groq API key yahan daalo
    // console.groq.com pe signup karo - bilkul free hai
    private var apiKey = ""

    private val conversationHistory = mutableListOf<JSONObject>()

    private val SYSTEM_PROMPT = """
        Tu VEGA hai — Voice Enabled General Agent. Ek Hindi aur English (Hinglish) mein baat karne wala personal AI assistant.
        - Hamesha short aur helpful jawab do (1-2 sentences max jab tak detail na maangi ho)
        - Hinglish mein baat karo (Hindi + English mix)
        - Friendly aur smart tone rakho
        - Agar koi system command ho (call, open app, alarm etc.) toh sirf "COMMAND:[action]" format mein respond karo
        - User ka naam "Boss" se part karo
    """.trimIndent()

    fun setApiKey(key: String) {
        apiKey = key
    }

    fun hasApiKey() = apiKey.isNotBlank()

    suspend fun ask(userMessage: String): AIResponse {
        if (apiKey.isBlank()) {
            return AIResponse(
                text = "Boss, Groq API key set nahi ki. App settings mein jaao ya kaho 'api key set karo'",
                isError = true
            )
        }
        return withContext(Dispatchers.IO) {
            try {
                // Conversation history mein add karo
                conversationHistory.add(JSONObject().apply {
                    put("role", "user")
                    put("content", userMessage)
                })

                // Keep last 10 messages only (memory limit)
                if (conversationHistory.size > 10) {
                    conversationHistory.removeAt(0)
                }

                val requestBody = JSONObject().apply {
                    put("model", "llama3-8b-8192")
                    put("max_tokens", 200)
                    put("temperature", 0.7)
                    val messages = JSONArray()
                    // System message
                    messages.put(JSONObject().apply {
                        put("role", "system")
                        put("content", SYSTEM_PROMPT)
                    })
                    // History
                    conversationHistory.forEach { messages.put(it) }
                    put("messages", messages)
                }

                val url = URL(GROQ_API_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Authorization", "Bearer $apiKey")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 10000
                conn.readTimeout = 15000

                OutputStreamWriter(conn.outputStream).use { it.write(requestBody.toString()) }

                val responseCode = conn.responseCode
                val response = if (responseCode == 200) {
                    conn.inputStream.bufferedReader().readText()
                } else {
                    val err = conn.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                    Log.e(TAG, "API error $responseCode: $err")
                    return@withContext AIResponse(text = "Server error $responseCode", isError = true)
                }

                val json = JSONObject(response)
                val reply = json
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim()

                Log.d(TAG, "AI reply: $reply")

                // Add assistant reply to history
                conversationHistory.add(JSONObject().apply {
                    put("role", "assistant")
                    put("content", reply)
                })

                AIResponse(text = reply, isError = false)

            } catch (e: Exception) {
                Log.e(TAG, "AI request failed: ${e.message}")
                AIResponse(
                    text = "Network se baat nahi ho pa rahi Boss. Internet check karo.",
                    isError = true
                )
            }
        }
    }

    fun clearHistory() {
        conversationHistory.clear()
    }

    data class AIResponse(val text: String, val isError: Boolean)
}
