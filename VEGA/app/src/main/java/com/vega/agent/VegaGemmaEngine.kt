package com.vega.agent

import android.content.Context
import android.os.Environment
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * VegaGemmaEngine — Gemma 2B on-device LLM via MediaPipe
 *
 * Model: gemma-2b-it-cpu-int4.bin (~1.5GB)
 * Download From: https://www.kaggle.com/models/google/gemma/tfLite/gemma-2b-it-cpu-int4
 *
 * Model ko phone ki Downloads folder mein rakho:
 *   /sdcard/Download/gemma-2b-it-cpu-int4.bin
 */
object VegaGemmaEngine {

    private const val TAG = "VegaGemmaEngine"
    const val MODEL_FILENAME = "gemma-2b-it-cpu-int4.bin"

    private var llm: LlmInference? = null
    private var isInitialized = false
    var onStatusUpdate: ((String) -> Unit)? = null

    private val SYSTEM_PROMPT = """<start_of_turn>user
Tu VEGA hai — ek Hinglish AI assistant jo Android phone control karta hai.
- Short jawab do (max 2-3 sentences)
- Hinglish mein bolo (Hindi + English)
- Friendly tone rakho
- "Boss" se address karo user ko
<end_of_turn>
<start_of_turn>model
Samjha! Main VEGA hun, Boss ka personal AI assistant!
<end_of_turn>
"""

    /** Check karo model file available hai ya nahi */
    fun isModelAvailable(context: Context): Boolean {
        val paths = getModelSearchPaths(context)
        return paths.any { File(it).exists() }
    }

    /** Model file ka path dhundo */
    fun getModelPath(context: Context): String? {
        return getModelSearchPaths(context).firstOrNull { File(it).exists() }
    }

    /** Saari possible model locations */
    fun getModelSearchPaths(context: Context): List<String> {
        return listOf(
            // Downloads folder (user yahaan rakhega)
            "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/$MODEL_FILENAME",
            // App private storage
            "${context.filesDir}/$MODEL_FILENAME",
            // External app storage
            "${context.getExternalFilesDir(null)}/$MODEL_FILENAME",
            // Root sdcard
            "/sdcard/$MODEL_FILENAME",
            "/sdcard/Download/$MODEL_FILENAME",
        )
    }

    /** Initialize Gemma model */
    suspend fun initialize(context: Context): InitResult {
        if (isInitialized && llm != null) return InitResult.AlreadyReady

        return withContext(Dispatchers.IO) {
            try {
                val modelPath = getModelPath(context)
                    ?: return@withContext InitResult.ModelNotFound

                onStatusUpdate?.invoke("Loading Gemma 2B... (~30 sec)")
                Log.d(TAG, "Loading model from: $modelPath")

                val options = LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .setMaxTokens(512)
                    .setTopK(40)
                    .setTemperature(0.8f)
                    .setRandomSeed(42)
                    .build()

                llm = LlmInference.createFromOptions(context, options)
                isInitialized = true
                onStatusUpdate?.invoke("Gemma 2B loaded!")
                Log.d(TAG, "Gemma loaded successfully!")
                InitResult.Success

            } catch (e: Exception) {
                Log.e(TAG, "Gemma init failed: ${e.message}")
                isInitialized = false
                llm = null
                InitResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    /** Gemma se response lo */
    suspend fun generate(userQuery: String): String {
        if (!isInitialized || llm == null) {
            return "Gemma load nahi hua Boss. Model file check karo."
        }
        return withContext(Dispatchers.IO) {
            try {
                val prompt = buildPrompt(userQuery)
                Log.d(TAG, "Generating for: $userQuery")
                val response = llm!!.generateResponse(prompt)
                // Clean response
                response.trim()
                    .removePrefix("<start_of_turn>model")
                    .removePrefix("model")
                    .removeSuffix("<end_of_turn>")
                    .trim()
                    .ifEmpty { "Kuch samjha nahi Boss, phir se puchho!" }
            } catch (e: Exception) {
                Log.e(TAG, "Generation error: ${e.message}")
                "Error aaya Boss: ${e.message?.take(50)}"
            }
        }
    }

    private fun buildPrompt(query: String): String {
        return """$SYSTEM_PROMPT<start_of_turn>user
$query
<end_of_turn>
<start_of_turn>model
"""
    }

    /** Model file size check (for download progress) */
    fun getModelFileSizeMB(context: Context): Float {
        val path = getModelPath(context) ?: return 0f
        val size = File(path).length()
        return size / (1024f * 1024f)
    }

    fun shutdown() {
        try { llm?.close() } catch (e: Exception) {}
        llm = null
        isInitialized = false
    }

    sealed class InitResult {
        object Success : InitResult()
        object AlreadyReady : InitResult()
        object ModelNotFound : InitResult()
        data class Error(val message: String) : InitResult()
    }
}
