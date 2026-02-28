package com.vega.agent

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/**
 * VegaTTS — Reliable TTS wrapper jo properly initialize hone ka wait karta hai
 */
class VegaTTS(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false
    private val pendingQueue = mutableListOf<String>()
    private val handler = Handler(Looper.getMainLooper())
    private val TAG = "VegaTTS"

    var onSpeakStart: (() -> Unit)? = null
    var onSpeakDone: (() -> Unit)? = null

    init {
        init()
    }

    private fun init() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Hindi try karo
                val result = tts?.setLanguage(Locale("hi", "IN"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Hindi nahi hai — English fallback
                    Log.w(TAG, "Hindi not available, falling back to English")
                    tts?.setLanguage(Locale("en", "IN"))
                }
                tts?.setSpeechRate(0.9f)
                tts?.setPitch(1.05f)

                // Utterance listener
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) { onSpeakStart?.invoke() }
                    override fun onDone(utteranceId: String?) { onSpeakDone?.invoke() }
                    override fun onError(utteranceId: String?) { Log.e(TAG, "TTS utterance error") }
                })

                isReady = true
                Log.d(TAG, "TTS initialized successfully")

                // Pending items bol do
                handler.post {
                    pendingQueue.forEach { speakNow(it) }
                    pendingQueue.clear()
                }
            } else {
                Log.e(TAG, "TTS init failed: $status")
                // Retry after delay
                handler.postDelayed({ init() }, 3000)
            }
        }
    }

    fun speak(text: String) {
        handler.post {
            if (isReady && tts != null) {
                speakNow(text)
            } else {
                // Queue karo — ready hone pe bolega
                Log.d(TAG, "TTS not ready, queuing: $text")
                pendingQueue.add(text)
            }
        }
    }

    private fun speakNow(text: String) {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return
        Log.d(TAG, "Speaking: $cleaned")
        tts?.speak(cleaned, TextToSpeech.QUEUE_ADD, null, "vega_${System.currentTimeMillis()}")
    }

    fun stop() {
        tts?.stop()
    }

    fun destroy() {
        handler.removeCallbacksAndMessages(null)
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "TTS destroy error: ${e.message}")
        }
        isReady = false
        tts = null
    }

    fun isInitialized() = isReady
}
