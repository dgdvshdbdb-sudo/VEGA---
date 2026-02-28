package com.vega.agent

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random

class VegaCommandEngine(private val context: Context) {

    private val tts = VegaTTS(context)   // ← Fixed TTS (queue + fallback)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val TAG = "VegaCommandEngine"
    private var lastSpokenText = ""

    companion object {
        // Ye keys detect karo command mein — AI ko mat bhejo
        val SYSTEM_KEYWORDS = listOf(
            "call", "open", "kholo", "alarm", "timer", "volume", "mute",
            "wifi", "bluetooth", "screenshot", "back", "home", "scroll",
            "search", "youtube", "whatsapp", "instagram", "facebook",
            "netflix", "spotify", "paytm", "maps", "camera", "selfie",
            "calculator", "weather", "news", "battery", "settings"
        )
    }

    fun process(command: String) {
        val cmd = command.lowercase().trim()
        Log.d(TAG, "CMD: $cmd")
        MainActivity.lastCommandCallback?.invoke("🎤 $cmd")

        // System command check
        val isSystemCmd = SYSTEM_KEYWORDS.any { cmd.contains(it) }

        if (isSystemCmd) {
            handleSystemCommand(cmd)
        } else {
            // Unknown — Groq AI ko bhejo
            askAI(cmd)
        }
    }

    private fun handleSystemCommand(cmd: String) {
        when {
            // ═══ CALLS ═══
            cmd.contains("call") || cmd.contains("phone karo") -> handleCall(cmd)
            cmd.contains("emergency") || cmd.contains("ambulance") -> { speak("Emergency 112 dial kar raha hun!"); dialNumber("112") }
            cmd.contains("police") -> { speak("Police 100 dial kar raha hun!"); dialNumber("100") }
            cmd.contains("fire") -> { speak("Fire brigade 101 dial kar raha hun!"); dialNumber("101") }

            // ═══ MESSAGING ═══
            cmd.contains("whatsapp") -> openApp("com.whatsapp", "WhatsApp")
            cmd.contains("telegram") -> openApp("org.telegram.messenger", "Telegram")
            cmd.contains("instagram") -> openApp("com.instagram.android", "Instagram")
            cmd.contains("facebook") -> openApp("com.facebook.katana", "Facebook")
            cmd.contains("twitter") || cmd.contains("x app") -> openApp("com.twitter.android", "Twitter")
            cmd.contains("gmail") || cmd.contains("email") -> openApp("com.google.android.gm", "Gmail")
            cmd.contains("linkedin") -> openApp("com.linkedin.android", "LinkedIn")
            cmd.contains("snapchat") -> openApp("com.snapchat.android", "Snapchat")

            // ═══ VOLUME ═══
            cmd.contains("volume badha") || cmd.contains("aawaz badha") || cmd.contains("louder") ->
                adjustVolume(AudioManager.ADJUST_RAISE, "Volume badha diya ✓")
            cmd.contains("volume kam") || cmd.contains("aawaz kam") ->
                adjustVolume(AudioManager.ADJUST_LOWER, "Volume ghata diya ✓")
            cmd.contains("max volume") || cmd.contains("full volume") -> {
                val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC), AudioManager.FLAG_SHOW_UI)
                speak("Maximum volume set!")
            }
            cmd.contains("mute") || cmd.contains("silent") ->
                setRinger(AudioManager.RINGER_MODE_SILENT, "Phone silent kar diya ✓")
            cmd.contains("vibrate") -> setRinger(AudioManager.RINGER_MODE_VIBRATE, "Vibrate mode on ✓")
            cmd.contains("unmute") || cmd.contains("ringer on") ->
                setRinger(AudioManager.RINGER_MODE_NORMAL, "Sound on kar diya ✓")

            // ═══ MUSIC ═══
            cmd.contains("spotify") -> openApp("com.spotify.music", "Spotify")
            cmd.contains("youtube music") -> openApp("com.google.android.apps.youtube.music", "YouTube Music")
            cmd.contains("pause") -> { sendMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_PAUSE); speak("Paused") }
            cmd.contains("next song") || cmd.contains("agla gaana") ->
                { sendMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_NEXT); speak("Next!") }
            cmd.contains("previous") || cmd.contains("pichla") ->
                { sendMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS); speak("Previous!") }

            // ═══ APPS ═══
            cmd.contains("youtube") -> openApp("com.google.android.youtube", "YouTube")
            cmd.contains("netflix") -> openApp("com.netflix.mediaclient", "Netflix")
            cmd.contains("hotstar") -> openApp("in.startv.hotstar", "Hotstar")
            cmd.contains("amazon") && !cmd.contains("echo") -> openApp("in.amazon.mShop.android.shopping", "Amazon")
            cmd.contains("flipkart") -> openApp("com.flipkart.android", "Flipkart")
            cmd.contains("swiggy") -> openApp("in.swiggy.android", "Swiggy")
            cmd.contains("zomato") -> openApp("com.application.zomato", "Zomato")
            cmd.contains("paytm") -> openApp("net.one97.paytm", "Paytm")
            cmd.contains("phonepe") -> openApp("com.phonepe.app", "PhonePe")
            cmd.contains("gpay") || cmd.contains("google pay") -> openApp("com.google.android.apps.nbu.paisa.user", "Google Pay")
            cmd.contains("ola") -> openApp("com.olacabs.customer", "Ola")
            cmd.contains("uber") -> openApp("com.ubercab", "Uber")
            cmd.contains("chrome") || cmd.contains("browser") -> openApp("com.android.chrome", "Chrome")
            cmd.contains("play store") -> openApp("com.android.vending", "Play Store")
            cmd.contains("drive") -> openApp("com.google.android.apps.docs", "Drive")
            cmd.contains("maps") || cmd.contains("navigation") -> openApp("com.google.android.apps.maps", "Maps")
            cmd.contains("calculator") -> openApp("com.google.android.calculator", "Calculator")
            cmd.contains("calendar") -> openApp("com.google.android.calendar", "Calendar")
            cmd.contains("contacts") -> openApp("com.google.android.contacts", "Contacts")
            cmd.contains("camera") && !cmd.contains("selfie") -> openCamera(front = false)
            cmd.contains("selfie") -> openCamera(front = true)
            cmd.contains("gallery") || cmd.contains("photos") -> openApp("com.google.android.apps.photos", "Photos")

            // ═══ SETTINGS ═══
            cmd.contains("wifi") -> openSettings(Settings.ACTION_WIFI_SETTINGS)
            cmd.contains("bluetooth") -> openSettings(Settings.ACTION_BLUETOOTH_SETTINGS)
            cmd.contains("settings") -> openSettings(Settings.ACTION_SETTINGS)
            cmd.contains("battery") -> openSettings(Intent.ACTION_POWER_USAGE_SUMMARY)
            cmd.contains("display") || cmd.contains("brightness") -> openSettings(Settings.ACTION_DISPLAY_SETTINGS)
            cmd.contains("hotspot") -> openSettings(Settings.ACTION_WIRELESS_SETTINGS)
            cmd.contains("language") -> openSettings(Settings.ACTION_LOCALE_SETTINGS)
            cmd.contains("accessibility") -> openSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS)

            // ═══ ACCESSIBILITY CONTROLS ═══
            cmd.contains("back") || cmd.contains("wapas") ->
                { VegaAccessibilityService.instance?.pressBack(); speak("Back ✓") }
            cmd.contains("home") -> { VegaAccessibilityService.instance?.pressHome(); speak("Home ✓") }
            cmd.contains("recent") -> { VegaAccessibilityService.instance?.pressRecents(); speak("Recent apps ✓") }
            cmd.contains("notification") -> { VegaAccessibilityService.instance?.openNotifications(); speak("Notifications ✓") }
            cmd.contains("quick settings") -> { VegaAccessibilityService.instance?.openQuickSettings(); speak("Quick settings ✓") }
            cmd.contains("scroll upar") || cmd.contains("scroll up") ->
                { VegaAccessibilityService.instance?.scrollUp(); speak("Upar scroll ✓") }
            cmd.contains("scroll neeche") || cmd.contains("scroll down") ->
                { VegaAccessibilityService.instance?.scrollDown(); speak("Neeche scroll ✓") }
            cmd.contains("screenshot") ->
                { VegaAccessibilityService.instance?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT); speak("Screenshot ✓") }

            // ═══ ALARM / TIMER ═══
            cmd.contains("alarm") -> handleAlarm(cmd)
            cmd.contains("timer") -> handleTimer(cmd)

            // ═══ TIME / DATE ═══
            cmd.contains("time") || cmd.contains("baj") || cmd.contains("samay") -> tellTime()
            cmd.contains("date") || cmd.contains("aaj") -> tellDate()

            // ═══ SEARCH / WEB ═══
            cmd.contains("search") || cmd.contains("dhundo") || cmd.contains("google") -> handleSearch(cmd)
            cmd.contains("weather") || cmd.contains("mausam") -> handleWeather(cmd)
            cmd.contains("news") -> openUrl("https://news.google.com", "News")

            // ═══ FUN ═══
            cmd.contains("coin") || cmd.contains("toss") -> { val r = if (Random.nextBoolean()) "Heads!" else "Tails!"; speak(r) }
            cmd.contains("dice") || cmd.contains("pasa") -> speak("${Random.nextInt(1, 7)} aaya!")
            cmd.contains("random") -> speak("Random number: ${Random.nextInt(1, 101)}")

            // ═══ DEVICE INFO ═══
            cmd.contains("android version") || cmd.contains("phone model") ->
                speak("Ye ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} hai, Android ${android.os.Build.VERSION.RELEASE} pe")

            // ═══ STOP ═══
            cmd.contains("band kar") || cmd.contains("chup") -> { tts.stop(); speak("Ok Boss") }

            // ═══ AI KEY SET ═══
            cmd.contains("api key") -> speak("Boss, app ke andar settings mein API key set karo ya developer se baat karo")

            else -> askAI(cmd)
        }
    }

    // ════════════════════════════════════════════
    // AI BRAIN — Groq API
    // ════════════════════════════════════════════
    private fun askAI(query: String) {
        if (!VegaAIBrain.hasApiKey()) {
            // No API key — fallback responses
            val responses = listOf(
                "Samjha nahi Boss — thoda clearly bolo?",
                "Ye command mujhe nahi pata, kuch aur try karo",
                "Hmm, iske baare mein nahi jaanta. Kuch aur pucho!",
                "Boss, AI mode ke liye Groq API key chahiye"
            )
            speak(responses.random())
            return
        }
        speak("Soch raha hun...")
        scope.launch {
            val response = VegaAIBrain.ask(query)
            speak(response.text)
        }
    }

    // ════════════════════════════════════════════
    // HELPERS
    // ════════════════════════════════════════════

    private fun adjustVolume(dir: Int, msg: String) {
        (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager)
            .adjustVolume(dir, AudioManager.FLAG_SHOW_UI)
        speak(msg)
    }

    private fun setRinger(mode: Int, msg: String) {
        try {
            (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).ringerMode = mode
            speak(msg)
        } catch (e: Exception) {
            speak("Permission nahi mili Boss. DND settings check karo")
        }
    }

    private fun sendMediaKey(keyCode: Int) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val down = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode)
        val up = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode)
        audio.dispatchMediaKeyEvent(down)
        audio.dispatchMediaKeyEvent(up)
    }

    private fun handleCall(cmd: String) {
        val name = cmd.replace("call karo", "").replace("call", "").replace("ko", "").trim()
        speak(if (name.length > 1) "$name ko call karta hun" else "Dialer khol raha hun")
        context.startActivity(Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun dialNumber(number: String) {
        context.startActivity(
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun handleAlarm(cmd: String) {
        val hr = Regex("(\\d+)\\s*(baje|am|pm)").find(cmd)?.groupValues?.get(1)?.toIntOrNull()
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (hr != null) {
            intent.putExtra(AlarmClock.EXTRA_HOUR, hr)
                .putExtra(AlarmClock.EXTRA_MINUTES, 0)
                .putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            speak("$hr baje alarm set!")
        } else speak("Alarm app khola")
        context.startActivity(intent)
    }

    private fun handleTimer(cmd: String) {
        val m = Regex("(\\d+)\\s*(minute|min|second|sec)").find(cmd)
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (m != null) {
            val n = m.groupValues[1].toIntOrNull() ?: 0
            val secs = if (m.groupValues[2].startsWith("min")) n * 60 else n
            intent.putExtra(AlarmClock.EXTRA_LENGTH, secs).putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            speak("$n ${m.groupValues[2]} timer set!")
        } else speak("Timer app khola")
        context.startActivity(intent)
    }

    private fun handleSearch(cmd: String) {
        val q = cmd.replace("search karo", "").replace("search", "").replace("google karo", "")
            .replace("google", "").replace("dhundo", "").trim()
        if (q.isNotEmpty()) {
            speak("$q search kar raha hun")
            openUrl("https://www.google.com/search?q=${Uri.encode(q)}", "Search")
        } else speak("Kya search karun Boss?")
    }

    private fun handleWeather(cmd: String) {
        val city = cmd.replace("weather", "").replace("mausam", "").trim().ifEmpty { "India" }
        speak("$city ka mausam dekh raha hun")
        openUrl("https://www.google.com/search?q=weather+${Uri.encode(city)}", "Weather")
    }

    private fun tellTime() {
        val c = Calendar.getInstance()
        val h = c.get(Calendar.HOUR_OF_DAY); val m = c.get(Calendar.MINUTE)
        val period = if (h < 12) "subah" else if (h < 17) "dopahar" else if (h < 20) "shaam" else "raat"
        speak("$period ke $h baj ke $m minute hue hain Boss")
    }

    private fun tellDate() {
        val c = Calendar.getInstance()
        val months = arrayOf("", "January","February","March","April","May","June","July","August","September","October","November","December")
        speak("Aaj ${c.get(Calendar.DAY_OF_MONTH)} ${months[c.get(Calendar.MONTH)+1]} ${c.get(Calendar.YEAR)} hai Boss")
    }

    private fun openCamera(front: Boolean) {
        speak(if (front) "Selfie mode on!" else "Camera on!")
        val i = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (front) i.putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
        try { context.startActivity(i) } catch (e: Exception) { speak("Camera error") }
    }

    private fun openApp(pkg: String, name: String) {
        speak("$name khol raha hun")
        val i = context.packageManager.getLaunchIntentForPackage(pkg)
        if (i != null) context.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        else speak("$name install nahi hai Boss")
    }

    private fun openUrl(url: String, label: String = "") {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun openSettings(action: String) {
        context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun speak(text: String) {
        lastSpokenText = text
        Log.d(TAG, "Speak: $text")
        tts.speak(text)                                   // ← VegaTTS se bolega
        MainActivity.lastCommandCallback?.invoke("🤖 $text")
    }

    fun destroy() { tts.destroy(); scope.coroutineContext[kotlinx.coroutines.Job]?.cancel() }
}
