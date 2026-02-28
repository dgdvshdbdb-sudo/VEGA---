package com.vega.agent

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import java.util.Calendar
import java.util.Locale

class VegaCommandEngine(private val context: Context) {

    private var tts: TextToSpeech? = null
    private val TAG = "VegaCommandEngine"

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("hi", "IN") // Hindi TTS
                tts?.setSpeechRate(0.9f)
            }
        }
    }

    fun process(command: String) {
        val cmd = command.lowercase().trim()
        Log.d(TAG, "Processing: $cmd")
        MainActivity.lastCommandCallback?.invoke("CMD: $cmd")

        when {
            // ======= CALLS =======
            cmd.contains("call") || cmd.contains("call karo") || cmd.contains("phone karo") -> handleCall(cmd)

            // ======= SMS =======
            cmd.contains("message") || cmd.contains("sms") || cmd.contains("whatsapp") -> handleSMS(cmd)

            // ======= SYSTEM — WiFi =======
            cmd.contains("wifi on") || cmd.contains("wifi chalu") -> {
                speak("WiFi settings khol raha hun")
                context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            cmd.contains("wifi off") || cmd.contains("wifi band") -> {
                speak("WiFi settings khol raha hun")
                context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }

            // ======= VOLUME =======
            cmd.contains("volume badha") || cmd.contains("volume up") || cmd.contains("aawaz badha") -> {
                val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audio.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                speak("Volume badha diya")
            }
            cmd.contains("volume kam") || cmd.contains("volume down") || cmd.contains("aawaz kam") -> {
                val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audio.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                speak("Volume ghata diya")
            }
            cmd.contains("mute") || cmd.contains("silent") || cmd.contains("chup") -> {
                val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audio.ringerMode = AudioManager.RINGER_MODE_SILENT
                speak("Phone silent kar diya")
            }
            cmd.contains("unmute") || cmd.contains("ringer on") -> {
                val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audio.ringerMode = AudioManager.RINGER_MODE_NORMAL
                speak("Phone unmute kar diya")
            }

            // ======= TORCH =======
            cmd.contains("torch") || cmd.contains("flashlight") || cmd.contains("light on") -> {
                speak("Torch command mila — accessibility se karo")
                showToast("Torch: Settings > Quick Tiles se on karo")
            }

            // ======= ALARM =======
            cmd.contains("alarm") || cmd.contains("jagana") -> handleAlarm(cmd)

            // ======= APPS =======
            cmd.contains("youtube") -> openApp("com.google.android.youtube", "YouTube")
            cmd.contains("whatsapp") -> openApp("com.whatsapp", "WhatsApp")
            cmd.contains("instagram") -> openApp("com.instagram.android", "Instagram")
            cmd.contains("maps") || cmd.contains("navigation") -> openApp("com.google.android.apps.maps", "Google Maps")
            cmd.contains("chrome") || cmd.contains("browser") -> openApp("com.android.chrome", "Chrome")
            cmd.contains("camera") -> openCamera()
            cmd.contains("calculator") || cmd.contains("calculator") -> openApp("com.google.android.calculator", "Calculator")
            cmd.contains("settings") -> context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            cmd.contains("gallery") || cmd.contains("photos") -> openApp("com.google.android.apps.photos", "Photos")
            cmd.contains("play store") -> openApp("com.android.vending", "Play Store")

            // ======= SEARCH =======
            cmd.contains("search") || cmd.contains("dhundo") || cmd.contains("google") -> handleSearch(cmd)

            // ======= TIME / DATE =======
            cmd.contains("time") || cmd.contains("samay") || cmd.contains("baj") -> {
                val cal = Calendar.getInstance()
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                val min = cal.get(Calendar.MINUTE)
                speak("Abhi $hour baj ke $min minute hue hain")
            }
            cmd.contains("date") || cmd.contains("aaj") || cmd.contains("din") -> {
                val cal = Calendar.getInstance()
                speak("Aaj ${cal.get(Calendar.DAY_OF_MONTH)} tarikh hai")
            }

            // ======= BATTERY =======
            cmd.contains("battery") || cmd.contains("charge") -> {
                speak("Battery status check karne ke liye settings dekhein")
                context.startActivity(Intent(Intent.ACTION_POWER_USAGE_SUMMARY).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }

            // ======= BLUETOOTH =======
            cmd.contains("bluetooth") -> {
                speak("Bluetooth settings khol raha hun")
                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }

            // ======= DO NOT DISTURB =======
            cmd.contains("do not disturb") || cmd.contains("disturb mat") || cmd.contains("dnd") -> {
                speak("DND settings khol raha hun")
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }

            // ======= GENERAL AI RESPONSE =======
            cmd.contains("kya hal") || cmd.contains("kaisa hai") || cmd.contains("how are you") -> {
                speak("Main bilkul theek hun, Boss! Aapki kya seva kar sakta hun?")
            }
            cmd.contains("tera naam") || cmd.contains("your name") || cmd.contains("kaun hai") -> {
                speak("Main hoon VEGA — Voice Enabled General Agent. Aapka personal AI assistant!")
            }
            cmd.contains("shukriya") || cmd.contains("thanks") || cmd.contains("thank you") || cmd.contains("dhanyawad") -> {
                speak("Koi baat nahi, Boss! Hamesha seva mein hoon.")
            }
            cmd.contains("band") || cmd.contains("stop") || cmd.contains("rukja") -> {
                speak("Theek hai, main chup ho jata hun. Zaroorat ho to bulaana.")
            }
            cmd.contains("joke") || cmd.contains("funny") -> {
                val jokes = listOf(
                    "Ek programmer restaurant gaya. Waiter ne pucha kya lenge? Usne kaha: NULL",
                    "WiFi ka password kya hai? Pehle khana order karo!",
                    "Bug free code likhna aur ghee free biryani banane mein same takleef hai!"
                )
                speak(jokes.random())
            }

            // ======= DEFAULT =======
            else -> {
                speak("Samajh nahi aaya, Boss. Phir se bolein?")
                Log.d(TAG, "Unknown command: $cmd")
            }
        }
    }

    private fun handleCall(cmd: String) {
        // Extract name from command e.g. "call karo mummy ko"
        val name = extractName(cmd, listOf("call karo", "call", "phone karo", "bulao"))
        if (name.isNotEmpty()) {
            speak("$name ko call karta hun")
            val uri = Uri.encode(name)
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:")).apply {
                putExtra(Intent.EXTRA_PHONE_NUMBER, name)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            // Try contacts search
            val contactIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(contactIntent)
        } else {
            speak("Kisko call karun? Naam batao")
        }
    }

    private fun handleSMS(cmd: String) {
        speak("Message app khol raha hun")
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_MESSAGING)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            openApp("com.android.mms", "Messages")
        }
    }

    private fun handleAlarm(cmd: String) {
        // Try to extract time e.g. "alarm lagao 7 baje"
        val hourRegex = Regex("(\\d+)\\s*(baje|bajey|baj|am|pm|:)")
        val match = hourRegex.find(cmd)
        val hour = match?.groupValues?.get(1)?.toIntOrNull()

        if (hour != null) {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, 0)
                putExtra(AlarmClock.EXTRA_MESSAGE, "VEGA Alarm")
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            speak("$hour baje ka alarm lagaa raha hun")
            context.startActivity(intent)
        } else {
            speak("Alarm set karne ke liye time batao, jaise: alarm lagao 7 baje")
            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    private fun handleSearch(cmd: String) {
        val query = cmd
            .replace("search karo", "")
            .replace("google karo", "")
            .replace("dhundo", "")
            .replace("search", "")
            .trim()

        if (query.isNotEmpty()) {
            speak("$query search kar raha hun")
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra("query", query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                val browserIntent = Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(browserIntent)
            }
        } else {
            speak("Kya search karun?")
        }
    }

    private fun openApp(packageName: String, appName: String) {
        speak("$appName khol raha hun")
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            speak("$appName install nahi hai")
            showToast("$appName not installed")
        }
    }

    private fun openCamera() {
        speak("Camera khol raha hun")
        val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun extractName(cmd: String, keywords: List<String>): String {
        var result = cmd
        for (kw in keywords) result = result.replace(kw, "")
        return result
            .replace("ko", "")
            .replace("ka", "")
            .replace("se", "")
            .trim()
    }

    private fun speak(text: String) {
        Log.d(TAG, "TTS: $text")
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "vega_tts")
        MainActivity.lastCommandCallback?.invoke("🤖 $text")
    }

    private fun showToast(text: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
    }
}
