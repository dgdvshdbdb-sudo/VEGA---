package com.vega.agent

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat

class VegaVoiceService : Service() {

    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var commandEngine: VegaCommandEngine
    private var isListening = false
    private val handler = Handler(Looper.getMainLooper())

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
        startForeground(NOTIF_ID, buildNotification("VEGA initialized..."))
        initSpeechRecognizer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isListening) {
            handler.postDelayed({ startListening() }, 500)
        }
        return START_STICKY
    }

    private fun initSpeechRecognizer() {
        // isRecognitionAvailable check hata diya — kuch devices pe false deta hai
        // even jab actually available hota hai
        try {
            // First try: on-device recognizer (Android 13+)
            speechRecognizer = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
            } else {
                // Fallback: Google speech recognizer
                SpeechRecognizer.createSpeechRecognizer(this)
            }
            setupRecognitionListener()
            updateStatus("🎙️ VEGA ready — bol!")
        } catch (e: Exception) {
            Log.e(TAG, "SpeechRecognizer init failed: ${e.message}")
            updateStatus("⚠️ Google App install/update karo — speech need hai")
            // Retry after delay
            handler.postDelayed({ initSpeechRecognizer() }, 5000)
        }
    }

    private fun setupRecognitionListener() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                updateStatus("👂 Bol Boss...")
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val command = matches?.firstOrNull() ?: return
                Log.d(TAG, "Recognized: $command")
                updateStatus("✅ Suna: $command")
                commandEngine.process(command)
                scheduleRestart(800)
            }

            override fun onError(error: Int) {
                isListening = false
                val (msg, delay) = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> Pair("Samjha nahi, phir bolo", 500L)
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> Pair("Timeout — phir se sun raha hun", 300L)
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> Pair("Busy, thoda ruko...", 2000L)
                    SpeechRecognizer.ERROR_NETWORK -> Pair("Network error — offline mode limited", 3000L)
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> Pair("Network slow — retry...", 3000L)
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> Pair("Mic permission chahiye!", 5000L)
                    SpeechRecognizer.ERROR_SERVER -> Pair("Server error — retry...", 3000L)
                    SpeechRecognizer.ERROR_CLIENT -> {
                        // Recognizer needs reset
                        resetRecognizer()
                        Pair("Reset ho raha hun...", 2000L)
                    }
                    7 -> Pair("Mic nahi mila, phir try...", 2000L) // ERROR_NO_MATCH alt
                    else -> Pair("Error $error — retry...", 1500L)
                }
                updateStatus("⚠️ $msg")
                scheduleRestart(delay)
            }

            override fun onBeginningOfSpeech() { updateStatus("🔴 Rec...") }
            override fun onEndOfSpeech() {
                isListening = false
                updateStatus("⏳ Process ho raha hai...")
            }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                if (!partial.isNullOrBlank()) updateStatus("🎤 $partial")
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startListening() {
        if (isListening) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 300L)
        }
        try {
            speechRecognizer?.startListening(intent)
            Log.d(TAG, "Listening started")
        } catch (e: Exception) {
            Log.e(TAG, "startListening error: ${e.message}")
            updateStatus("❌ Recognizer error — reset ho raha hai")
            resetRecognizer()
        }
    }

    private fun scheduleRestart(delayMs: Long) {
        handler.postDelayed({
            if (!isListening) startListening()
        }, delayMs)
    }

    private fun resetRecognizer() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) { Log.e(TAG, "destroy error: ${e.message}") }
        handler.postDelayed({
            initSpeechRecognizer()
            handler.postDelayed({ startListening() }, 800)
        }, 1500)
    }

    private fun updateStatus(status: String) {
        Log.d(TAG, "Status: $status")
        statusCallback?.invoke(status)
        updateNotification(status)
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, buildNotification(text))
    }

    private fun buildNotification(text: String): Notification {
        val launchIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("V.E.G.A — Active")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(launchIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "VEGA Voice Listener", NotificationManager.IMPORTANCE_LOW
        ).apply { description = "VEGA background voice service" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        try { speechRecognizer?.destroy() } catch (e: Exception) {}
        commandEngine.destroy()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
