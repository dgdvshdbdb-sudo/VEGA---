package com.vega.agent

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat

class VegaVoiceService : Service() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var commandEngine: VegaCommandEngine
    private var isListening = false

    companion object {
        const val CHANNEL_ID = "vega_listening_channel"
        const val NOTIF_ID = 1
        const val TAG = "VegaVoiceService"
        var statusCallback: ((String) -> Unit)? = null
    }

    override fun onCreate() {
        super.onCreate()
        commandEngine = VegaCommandEngine(this)
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification("🎙️ VEGA sun raha hai..."))
        initSpeechRecognizer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isListening) startListening()
        return START_STICKY // Phone restart pe bhi chale
    }

    private fun initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            updateStatus("❌ Speech recognition available nahi hai")
            return
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                updateStatus("👂 Bol...")
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val command = matches?.firstOrNull() ?: return
                Log.d(TAG, "Command: $command")
                updateStatus("🎤 Suna: $command")
                commandEngine.process(command)
                // Auto restart listening
                restartListening()
            }

            override fun onError(error: Int) {
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "Samjha nahi, phir bolo"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Kuch nahi bola gaya"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    else -> "Error: $error"
                }
                updateStatus("⚠️ $msg")
                restartListening()
            }

            override fun onBeginningOfSpeech() { updateStatus("🔴 Recording...") }
            override fun onEndOfSpeech() { updateStatus("⏳ Processing...") }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (partial != null) updateStatus("🎤 $partial")
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN") // Hindi primary
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN") // English fallback
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        try {
            speechRecognizer.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "startListening failed: ${e.message}")
        }
    }

    private fun restartListening() {
        isListening = false
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (!isListening) startListening()
        }, 800)
    }

    private fun updateStatus(status: String) {
        statusCallback?.invoke(status)
        updateNotification(status)
    }

    private fun updateNotification(text: String) {
        val notif = buildNotification(text)
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, notif)
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("V.E.G.A — Active")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "VEGA Voice Listener",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "VEGA background voice service" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onDestroy() {
        speechRecognizer.destroy()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
