package com.vega.agent

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.AlarmClock
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random

class VegaCommandEngine(private val context: Context) {

    private var tts: TextToSpeech? = null
    private val TAG = "VegaCommandEngine"
    private var lastSpokenText = ""

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("hi", "IN")
                tts?.setSpeechRate(0.92f)
                tts?.setPitch(1.0f)
            }
        }
    }

    fun process(command: String) {
        val cmd = command.lowercase().trim()
        Log.d(TAG, "CMD: $cmd")
        MainActivity.lastCommandCallback?.invoke("🎤 $cmd")

        when {

            // ════════════════════════════════════════════════
            //  📞 CALLS
            // ════════════════════════════════════════════════
            cmd.contains("call karo") || cmd.contains("phone karo") || cmd.contains("call kr") ->
                handleCall(cmd)
            cmd.contains("missed call") || cmd.contains("miss call") -> {
                speak("Recent calls khol raha hun")
                openApp("com.android.dialer", "Phone")
            }
            cmd.contains("dial") -> handleCall(cmd)
            cmd.contains("emergency") || cmd.contains("ambulance") || cmd.contains("police") -> {
                speak("Emergency call kar raha hun — 112")
                dialNumber("112")
            }
            cmd.contains("fire brigade") || cmd.contains("aag lagi") -> {
                speak("Fire brigade call kar raha hun — 101")
                dialNumber("101")
            }

            // ════════════════════════════════════════════════
            //  💬 MESSAGING
            // ════════════════════════════════════════════════
            cmd.contains("whatsapp") && (cmd.contains("kholo") || cmd.contains("open")) ->
                openApp("com.whatsapp", "WhatsApp")
            cmd.contains("telegram") -> openApp("org.telegram.messenger", "Telegram")
            cmd.contains("message") || cmd.contains("sms") -> handleSMS(cmd)
            cmd.contains("gmail") || cmd.contains("email") -> openApp("com.google.android.gm", "Gmail")
            cmd.contains("instagram") -> openApp("com.instagram.android", "Instagram")
            cmd.contains("facebook") -> openApp("com.facebook.katana", "Facebook")
            cmd.contains("twitter") || cmd.contains("एक्स") || cmd.contains("x app") ->
                openApp("com.twitter.android", "Twitter")
            cmd.contains("snapchat") -> openApp("com.snapchat.android", "Snapchat")
            cmd.contains("linkedin") -> openApp("com.linkedin.android", "LinkedIn")

            // ════════════════════════════════════════════════
            //  🔊 VOLUME & AUDIO
            // ════════════════════════════════════════════════
            cmd.contains("volume badha") || cmd.contains("aawaz badha") || cmd.contains("louder") ->
                adjustVolume(AudioManager.ADJUST_RAISE, "Volume badha diya")
            cmd.contains("volume kam") || cmd.contains("aawaz kam") || cmd.contains("quieter") ->
                adjustVolume(AudioManager.ADJUST_LOWER, "Volume ghata diya")
            cmd.contains("mute") || cmd.contains("silent") || cmd.contains("chup kar") ->
                setRinger(AudioManager.RINGER_MODE_SILENT, "Phone silent kar diya")
            cmd.contains("vibrate") || cmd.contains("vibration") ->
                setRinger(AudioManager.RINGER_MODE_VIBRATE, "Vibrate mode on kar diya")
            cmd.contains("unmute") || cmd.contains("ringer on") || cmd.contains("sound on") ->
                setRinger(AudioManager.RINGER_MODE_NORMAL, "Sound on kar diya")
            cmd.contains("max volume") || cmd.contains("full volume") || cmd.contains("poori aawaz") -> {
                val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audio.setStreamVolume(AudioManager.STREAM_RING,
                    audio.getStreamMaxVolume(AudioManager.STREAM_RING), AudioManager.FLAG_SHOW_UI)
                speak("Maximum volume set kar diya")
            }

            // ════════════════════════════════════════════════
            //  🎵 MUSIC
            // ════════════════════════════════════════════════
            cmd.contains("music") || cmd.contains("gaana") || cmd.contains("song") ->
                openApp("com.spotify.music", "Spotify").also {
                    if (context.packageManager.getLaunchIntentForPackage("com.spotify.music") == null)
                        openApp("com.google.android.music", "Music")
                }
            cmd.contains("spotify") -> openApp("com.spotify.music", "Spotify")
            cmd.contains("youtube music") -> openApp("com.google.android.apps.youtube.music", "YouTube Music")
            cmd.contains("pause") -> sendMediaButton(android.view.KeyEvent.KEYCODE_MEDIA_PAUSE)
            cmd.contains("play") && !cmd.contains("store") -> sendMediaButton(android.view.KeyEvent.KEYCODE_MEDIA_PLAY)
            cmd.contains("next song") || cmd.contains("agla gaana") || cmd.contains("skip") ->
                sendMediaButton(android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
            cmd.contains("previous song") || cmd.contains("pichla gaana") ->
                sendMediaButton(android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS)

            // ════════════════════════════════════════════════
            //  🌐 APPS — ENTERTAINMENT
            // ════════════════════════════════════════════════
            cmd.contains("youtube") -> openApp("com.google.android.youtube", "YouTube")
            cmd.contains("netflix") -> openApp("com.netflix.mediaclient", "Netflix")
            cmd.contains("hotstar") || cmd.contains("disney") ->
                openApp("in.startv.hotstar", "Hotstar")
            cmd.contains("amazon prime") || cmd.contains("prime video") ->
                openApp("com.amazon.avod.thirdpartyclient", "Prime Video")
            cmd.contains("zee5") -> openApp("com.zenga.zee5", "Zee5")
            cmd.contains("mx player") -> openApp("com.mxtech.videoplayer.ad", "MX Player")

            // ════════════════════════════════════════════════
            //  🛒 SHOPPING
            // ════════════════════════════════════════════════
            cmd.contains("amazon") && !cmd.contains("prime") -> openApp("in.amazon.mShop.android.shopping", "Amazon")
            cmd.contains("flipkart") -> openApp("com.flipkart.android", "Flipkart")
            cmd.contains("meesho") -> openApp("com.meesho.supply", "Meesho")
            cmd.contains("myntra") -> openApp("com.myntra.android", "Myntra")

            // ════════════════════════════════════════════════
            //  💳 PAYMENTS
            // ════════════════════════════════════════════════
            cmd.contains("paytm") -> openApp("net.one97.paytm", "Paytm")
            cmd.contains("phonepe") || cmd.contains("phone pe") -> openApp("com.phonepe.app", "PhonePe")
            cmd.contains("google pay") || cmd.contains("gpay") -> openApp("com.google.android.apps.nbu.paisa.user", "Google Pay")
            cmd.contains("bhim") -> openApp("in.org.npci.upiapp", "BHIM UPI")

            // ════════════════════════════════════════════════
            //  🍕 FOOD
            // ════════════════════════════════════════════════
            cmd.contains("swiggy") -> openApp("in.swiggy.android", "Swiggy")
            cmd.contains("zomato") -> openApp("com.application.zomato", "Zomato")
            cmd.contains("dominos") || cmd.contains("pizza") -> openApp("com.dominospizza", "Dominos")

            // ════════════════════════════════════════════════
            //  🚗 TRANSPORT
            // ════════════════════════════════════════════════
            cmd.contains("ola") -> openApp("com.olacabs.customer", "Ola")
            cmd.contains("uber") -> openApp("com.ubercab", "Uber")
            cmd.contains("rapido") -> openApp("com.rapido.passenger", "Rapido")
            cmd.contains("irctc") || cmd.contains("train ticket") -> openApp("ctrail.hartron.com.ctrail", "IRCTC")
            cmd.contains("makemytrip") || cmd.contains("flight") -> openApp("com.makemytrip", "MakeMyTrip")
            cmd.contains("google maps") || cmd.contains("maps") || cmd.contains("navigation") ->
                openApp("com.google.android.apps.maps", "Maps")
            cmd.contains("navigate to") || cmd.contains("directions to") || cmd.contains("rasta batao") ->
                handleNavigation(cmd)

            // ════════════════════════════════════════════════
            //  ⚙️ SYSTEM — SETTINGS
            // ════════════════════════════════════════════════
            cmd.contains("settings") -> openSettings(Settings.ACTION_SETTINGS)
            cmd.contains("wifi") -> openSettings(Settings.ACTION_WIFI_SETTINGS)
            cmd.contains("bluetooth") -> openSettings(Settings.ACTION_BLUETOOTH_SETTINGS)
            cmd.contains("hotspot") || cmd.contains("tethering") ->
                openSettings(Settings.ACTION_WIRELESS_SETTINGS)
            cmd.contains("location") && cmd.contains("on") ->
                openSettings(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            cmd.contains("data") && (cmd.contains("on") || cmd.contains("off")) ->
                openSettings(Settings.ACTION_DATA_ROAMING_SETTINGS)
            cmd.contains("do not disturb") || cmd.contains("dnd") ->
                openSettings(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            cmd.contains("battery") ->
                openSettings(Intent.ACTION_POWER_USAGE_SUMMARY)
            cmd.contains("storage") || cmd.contains("memory") ->
                openSettings(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
            cmd.contains("display") || cmd.contains("brightness") ->
                openSettings(Settings.ACTION_DISPLAY_SETTINGS)
            cmd.contains("sound settings") ->
                openSettings(Settings.ACTION_SOUND_SETTINGS)
            cmd.contains("language") ->
                openSettings(Settings.ACTION_LOCALE_SETTINGS)
            cmd.contains("date time") || cmd.contains("time zone") ->
                openSettings(Settings.ACTION_DATE_SETTINGS)
            cmd.contains("accessibility") ->
                openSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            cmd.contains("developer") -> {
                speak("Developer options khol raha hun")
                openSettings(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            }
            cmd.contains("about phone") || cmd.contains("phone info") -> {
                speak("Phone ke baare mein info")
                openSettings(Settings.ACTION_DEVICE_INFO_SETTINGS)
            }
            cmd.contains("app settings") || cmd.contains("app permission") -> {
                speak("App settings khol raha hun")
                openSettings(Settings.ACTION_APPLICATION_SETTINGS)
            }

            // ════════════════════════════════════════════════
            //  📸 CAMERA
            // ════════════════════════════════════════════════
            cmd.contains("selfie") || cmd.contains("front camera") -> openCamera(front = true)
            cmd.contains("camera") -> openCamera(front = false)
            cmd.contains("video record") || cmd.contains("video lo") -> openVideoCamera()
            cmd.contains("screenshot") || cmd.contains("screen capture") || cmd.contains("capture karo") -> {
                VegaAccessibilityService.instance?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
                speak("Screenshot le liya")
            }
            cmd.contains("gallery") || cmd.contains("photos") ->
                openApp("com.google.android.apps.photos", "Photos")

            // ════════════════════════════════════════════════
            //  🧭 NAVIGATION (Accessibility)
            // ════════════════════════════════════════════════
            cmd.contains("wapas jaoo") || cmd.contains("back karo") || cmd.contains("go back") -> {
                VegaAccessibilityService.instance?.pressBack()
                speak("Wapas gaya")
            }
            cmd.contains("home jaoo") || cmd.contains("home screen") -> {
                VegaAccessibilityService.instance?.pressHome()
                speak("Home pe aa gaya")
            }
            cmd.contains("recent apps") || cmd.contains("switch apps") -> {
                VegaAccessibilityService.instance?.pressRecents()
                speak("Recent apps dikha raha hun")
            }
            cmd.contains("notification") && !cmd.contains("off") -> {
                VegaAccessibilityService.instance?.openNotifications()
                speak("Notifications khol diye")
            }
            cmd.contains("quick settings") || cmd.contains("quick panel") -> {
                VegaAccessibilityService.instance?.openQuickSettings()
                speak("Quick settings khola")
            }
            cmd.contains("scroll upar") || cmd.contains("scroll up") -> {
                VegaAccessibilityService.instance?.scrollUp()
                speak("Upar scroll kiya")
            }
            cmd.contains("scroll neeche") || cmd.contains("scroll down") -> {
                VegaAccessibilityService.instance?.scrollDown()
                speak("Neeche scroll kiya")
            }

            // ════════════════════════════════════════════════
            //  ⏰ TIME, ALARM, TIMER
            // ════════════════════════════════════════════════
            cmd.contains("time") || cmd.contains("samay") || cmd.contains("baj") -> tellTime()
            cmd.contains("date") || cmd.contains("aaj") && cmd.contains("din") -> tellDate()
            cmd.contains("alarm") || cmd.contains("jagaa do") -> handleAlarm(cmd)
            cmd.contains("timer") -> handleTimer(cmd)
            cmd.contains("stopwatch") -> {
                speak("Stopwatch shuru kar raha hun")
                val i = Intent(AlarmClock.ACTION_SET_TIMER).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(i)
            }
            cmd.contains("kal") || cmd.contains("tomorrow") -> {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, 1)
                speak("Kal ${cal.get(Calendar.DAY_OF_MONTH)} tarikh hai")
            }

            // ════════════════════════════════════════════════
            //  🔍 SEARCH & WEB
            // ════════════════════════════════════════════════
            cmd.contains("search") || cmd.contains("dhundo") || cmd.contains("google") ->
                handleSearch(cmd)
            cmd.contains("wikipedia") -> {
                val q = cmd.replace("wikipedia", "").trim()
                openUrl("https://en.wikipedia.org/w/index.php?search=${Uri.encode(q)}", "Wikipedia")
            }
            cmd.contains("news") || cmd.contains("khabar") ->
                openApp("com.google.android.apps.magazines", "Google News").also {
                    if (context.packageManager.getLaunchIntentForPackage("com.google.android.apps.magazines") == null)
                        openUrl("https://news.google.com", "News")
                }
            cmd.contains("website") || cmd.contains("site kholo") -> handleWebsite(cmd)
            cmd.contains("youtube search") -> {
                val q = cmd.replace("youtube search", "").replace("youtube pe search", "").trim()
                openUrl("https://www.youtube.com/results?search_query=${Uri.encode(q)}", "YouTube search")
            }

            // ════════════════════════════════════════════════
            //  📐 MATH & TOOLS
            // ════════════════════════════════════════════════
            cmd.contains("calculator") || cmd.contains("calculate") || cmd.contains("hisab") ->
                openApp("com.google.android.calculator", "Calculator")
            cmd.contains("coin flip") || cmd.contains("toss") || cmd.contains("heads or tails") -> {
                val result = if (Random.nextBoolean()) "Heads — Chit!" else "Tails — Pat!"
                speak("Coin toss result: $result")
            }
            cmd.contains("dice") || cmd.contains("pasa") -> {
                val roll = Random.nextInt(1, 7)
                speak("Pasa fenka — $roll aaya!")
            }
            cmd.contains("random number") || cmd.contains("random no") -> {
                val n = Random.nextInt(1, 101)
                speak("Random number hai: $n")
            }
            cmd.matches(Regex(".*\\d+\\s*[+\\-×x*÷/]\\s*\\d+.*")) -> handleMath(cmd)

            // ════════════════════════════════════════════════
            //  📂 FILES & APPS
            // ════════════════════════════════════════════════
            cmd.contains("file manager") || cmd.contains("files") ->
                openApp("com.google.android.apps.nbu.files", "Files")
            cmd.contains("downloads") -> {
                speak("Downloads khol raha hun")
                val i = Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(i)
            }
            cmd.contains("contacts") -> openApp("com.google.android.contacts", "Contacts")
            cmd.contains("calendar") -> openApp("com.google.android.calendar", "Calendar")
            cmd.contains("chrome") || cmd.contains("browser") -> openApp("com.android.chrome", "Chrome")
            cmd.contains("play store") -> openApp("com.android.vending", "Play Store")
            cmd.contains("google drive") || cmd.contains("drive") ->
                openApp("com.google.android.apps.docs", "Drive")
            cmd.contains("google docs") -> openApp("com.google.android.apps.docs", "Docs")
            cmd.contains("google sheets") -> openApp("com.google.android.apps.spreadsheets", "Sheets")
            cmd.contains("google meet") -> openApp("com.google.android.apps.meetings", "Meet")
            cmd.contains("zoom") -> openApp("us.zoom.videomeetings", "Zoom")
            cmd.contains("clock") -> openApp("com.google.android.deskclock", "Clock")
            cmd.contains("maps") -> openApp("com.google.android.apps.maps", "Maps")
            cmd.contains("phone") && !cmd.contains("phonepe") ->
                openApp("com.google.android.dialer", "Phone")
            cmd.contains("qr code") || cmd.contains("barcode") -> {
                speak("QR scanner khol raha hun")
                openApp("com.google.zxing.client.android", "QR Scanner").also {
                    if (context.packageManager.getLaunchIntentForPackage("com.google.zxing.client.android") == null)
                        openCamera(front = false)
                }
            }

            // ════════════════════════════════════════════════
            //  💡 FUN & AI
            // ════════════════════════════════════════════════
            cmd.contains("joke") || cmd.contains("funny") || cmd.contains("hasao") -> tellJoke()
            cmd.contains("sher") || cmd.contains("shayari") -> tellShayari()
            cmd.contains("fact") || cmd.contains("interesting") -> tellFact()
            cmd.contains("motivation") || cmd.contains("inspire") || cmd.contains("himmat do") ->
                tellMotivation()
            cmd.contains("ek baar aur") || cmd.contains("repeat") || cmd.contains("dobara bolo") -> {
                speak(lastSpokenText)
            }
            cmd.contains("tera naam") || cmd.contains("tum kaun") || cmd.contains("who are you") ->
                speak("Main VEGA hun — Voice Enabled General Agent. Aapka personal AI assistant, hamesha seva mein!")
            cmd.contains("kya hal") || cmd.contains("kaisa hai") || cmd.contains("how are you") ->
                speak("Main bilkul theek hun Boss! System fully operational. Aapki kya seva karun?")
            cmd.contains("thanks") || cmd.contains("shukriya") || cmd.contains("dhanyawad") ->
                speak("Koi baat nahi Boss! Hamesha seva mein hoon. Aur kuch chahiye?")
            cmd.contains("namaste") || cmd.contains("hello") || cmd.contains("hi vega") ->
                speak("Namaste Boss! VEGA at your service. Bolo kya chahiye?")
            cmd.contains("good morning") || cmd.contains("subah") ->
                speak("Good morning Boss! Aaj ka din acha rahe. Kya plan hai aaj ka?")
            cmd.contains("good night") || cmd.contains("sone") ->
                speak("Good night Boss! Aaram karo. Main yaheen hun jab zaroorat ho.")
            cmd.contains("good afternoon") || cmd.contains("dopahar") ->
                speak("Good afternoon Boss! Lunch ho gaya? Kuch chahiye?")
            cmd.contains("good evening") || cmd.contains("shaam") ->
                speak("Good evening Boss! Kaisa raha aaj ka din?")
            cmd.contains("happy birthday") ->
                speak("Happy Birthday! Ye din aapke liye khushion bhara ho!")
            cmd.contains("rock paper scissors") || cmd.contains("pathar kaagaz") -> playRPS(cmd)
            cmd.contains("riddle") || cmd.contains("paheli") -> tellRiddle()
            cmd.contains("gana gao") || cmd.contains("nursery") -> speak("Twinkle twinkle little star, how I wonder what you are!")
            cmd.contains("story") || cmd.contains("kahani") -> tellStory()
            cmd.contains("roast") -> speak("Boss, aap itne smart hain ki main aapko roast bhi nahi kar sakta! 😄")

            // ════════════════════════════════════════════════
            //  📍 LOCATION & WEATHER
            // ════════════════════════════════════════════════
            cmd.contains("weather") || cmd.contains("mausam") -> {
                val q = cmd.replace("weather", "").replace("mausam", "").trim()
                val city = if (q.isEmpty()) "Delhi" else q
                openUrl("https://www.google.com/search?q=weather+$city", "Weather")
                speak("$city ka mausam dekh raha hun")
            }
            cmd.contains("kahan hun") || cmd.contains("my location") || cmd.contains("location batao") -> {
                speak("Location map mein dikha raha hun")
                openUrl("https://maps.google.com/maps?q=my+location", "My Location")
            }
            cmd.contains("nearby") || cmd.contains("paas mein") || cmd.contains("nearest") ->
                handleNearby(cmd)

            // ════════════════════════════════════════════════
            //  🔋 SYSTEM STATUS
            // ════════════════════════════════════════════════
            cmd.contains("ip address") -> {
                speak("Network settings mein IP mila jayega")
                openSettings(Settings.ACTION_WIFI_SETTINGS)
            }
            cmd.contains("android version") || cmd.contains("os version") -> {
                speak("Ye phone Android ${android.os.Build.VERSION.RELEASE} pe chal raha hai, ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            }
            cmd.contains("phone model") || cmd.contains("device") ->
                speak("Ye device hai ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")

            // ════════════════════════════════════════════════
            //  ⛔ STOP
            // ════════════════════════════════════════════════
            cmd.contains("band kar") || cmd.contains("stop") || cmd.contains("rukja") || cmd.contains("chup") -> {
                speak("Theek hai Boss, chup ho jata hun")
                tts?.stop()
            }

            // ════════════════════════════════════════════════
            //  ❓ DEFAULT
            // ════════════════════════════════════════════════
            else -> {
                // Try web search as fallback
                speak("Yeh command samjha nahi — Google pe search karta hun")
                val i = Intent(Intent.ACTION_WEB_SEARCH).apply {
                    putExtra("query", cmd)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try { context.startActivity(i) } catch (e: Exception) {
                    openUrl("https://www.google.com/search?q=${Uri.encode(cmd)}", "Search")
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    //  HELPER FUNCTIONS
    // ══════════════════════════════════════════════════════════

    private fun adjustVolume(direction: Int, msg: String) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.adjustVolume(direction, AudioManager.FLAG_SHOW_UI)
        speak(msg)
    }

    private fun setRinger(mode: Int, msg: String) {
        try {
            val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audio.ringerMode = mode
            speak(msg)
        } catch (e: Exception) {
            speak("Permission nahi hai ringer change karne ki. Settings se DND off karo.")
        }
    }

    private fun sendMediaButton(keyCode: Int) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode))
        audio.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode))
    }

    private fun handleCall(cmd: String) {
        val name = extractTarget(cmd, listOf("call karo", "call kr", "call", "phone karo", "dial"))
        if (name.isNotEmpty() && name.length > 1) {
            speak("$name ko call karta hun")
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:")
                putExtra(Intent.EXTRA_PHONE_NUMBER, name)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } else {
            speak("Dialer khol raha hun")
            val intent = Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    private fun dialNumber(number: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun handleSMS(cmd: String) {
        speak("Messaging app khol raha hun")
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_MESSAGING)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try { context.startActivity(intent) }
        catch (e: Exception) { openApp("com.android.mms", "Messages") }
    }

    private fun handleAlarm(cmd: String) {
        val hourRegex = Regex("(\\d+)\\s*(baje|bajey|baj|am|pm|o'clock|:)")
        val match = hourRegex.find(cmd)
        val hour = match?.groupValues?.get(1)?.toIntOrNull()
        if (hour != null) {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, if (cmd.contains("pm") && hour < 12) hour + 12 else hour)
                putExtra(AlarmClock.EXTRA_MINUTES, 0)
                putExtra(AlarmClock.EXTRA_MESSAGE, "VEGA Alarm")
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            speak("$hour baje ka alarm set kar raha hun")
            context.startActivity(intent)
        } else {
            speak("Alarm clock khol raha hun")
            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    private fun handleTimer(cmd: String) {
        val minRegex = Regex("(\\d+)\\s*(minute|min|second|sec)")
        val match = minRegex.find(cmd)
        val amount = match?.groupValues?.get(1)?.toIntOrNull()
        val unit = match?.groupValues?.get(2) ?: "minute"
        if (amount != null) {
            val secs = if (unit.startsWith("min")) amount * 60 else amount
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, secs)
                putExtra(AlarmClock.EXTRA_MESSAGE, "VEGA Timer")
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            speak("$amount ${if (unit.startsWith("min")) "minute" else "second"} ka timer lagaa raha hun")
            context.startActivity(intent)
        } else {
            speak("Timer app khol raha hun")
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    private fun handleSearch(cmd: String) {
        val q = cmd.replace("search karo", "").replace("google karo", "")
            .replace("dhundo", "").replace("search", "").replace("google", "").trim()
        if (q.isNotEmpty()) {
            speak("$q search kar raha hun")
            openUrl("https://www.google.com/search?q=${Uri.encode(q)}", "Search")
        } else speak("Kya search karun Boss?")
    }

    private fun handleNavigation(cmd: String) {
        val place = cmd.replace("navigate to", "").replace("directions to", "")
            .replace("rasta batao", "").trim()
        if (place.isNotEmpty()) {
            speak("$place ka rasta dikha raha hun")
            openUrl("https://maps.google.com/maps?daddr=${Uri.encode(place)}", "Navigation")
        } else {
            speak("Kahan jaana hai?")
            openApp("com.google.android.apps.maps", "Maps")
        }
    }

    private fun handleWebsite(cmd: String) {
        val url = cmd.replace("website kholo", "").replace("site kholo", "")
            .replace("website", "").replace("open", "").trim()
        val fullUrl = if (url.startsWith("http")) url else "https://$url"
        speak("$url khol raha hun")
        openUrl(fullUrl, url)
    }

    private fun handleNearby(cmd: String) {
        val q = cmd.replace("nearby", "").replace("paas mein", "").replace("nearest", "").trim()
        val query = if (q.isEmpty()) "shops" else q
        speak("Aaspaas mein $query dhund raha hun")
        openUrl("https://maps.google.com/maps?q=${Uri.encode("$query near me")}", "Nearby $query")
    }

    private fun handleMath(cmd: String) {
        try {
            val expr = cmd.replace("×", "*").replace("x", "*").replace("÷", "/")
            val regex = Regex("(\\d+\\.?\\d*)\\s*([+\\-*/])\\s*(\\d+\\.?\\d*)")
            val match = regex.find(expr) ?: return
            val a = match.groupValues[1].toDouble()
            val op = match.groupValues[2]
            val b = match.groupValues[3].toDouble()
            val result = when (op) {
                "+" -> a + b
                "-" -> a - b
                "*" -> a * b
                "/" -> if (b != 0.0) a / b else Double.NaN
                else -> Double.NaN
            }
            val ans = if (result == result.toLong().toDouble()) result.toLong().toString() else result.toString()
            speak("Jawab hai $ans")
        } catch (e: Exception) {
            openApp("com.google.android.calculator", "Calculator")
        }
    }

    private fun openCamera(front: Boolean = false) {
        speak(if (front) "Front camera khol raha hun" else "Camera khol raha hun")
        val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
            if (front) putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try { context.startActivity(intent) }
        catch (e: Exception) { showToast("Camera not available") }
    }

    private fun openVideoCamera() {
        speak("Video camera khol raha hun")
        val intent = Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { context.startActivity(intent) }
        catch (e: Exception) { showToast("Video camera error") }
    }

    private fun openApp(packageName: String, appName: String): Boolean {
        speak("$appName khol raha hun")
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        return if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } else {
            speak("$appName install nahi hai, Play Store pe dekho")
            false
        }
    }

    private fun openUrl(url: String, label: String = "") {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun openSettings(action: String) {
        context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun extractTarget(cmd: String, keywords: List<String>): String {
        var r = cmd
        for (kw in keywords) r = r.replace(kw, "")
        return r.replace("ko", "").replace("ka", "").replace("se", "").trim()
    }

    private fun tellTime() {
        val cal = Calendar.getInstance()
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        val period = if (h < 12) "subah" else if (h < 17) "dopahar" else if (h < 20) "shaam" else "raat"
        speak("Abhi $period ke $h baj ke $m minute hue hain")
    }

    private fun tellDate() {
        val cal = Calendar.getInstance()
        val days = arrayOf("", "Ravivar", "Somvar", "Mangalvar", "Budhvar", "Guruvar", "Shukravar", "Shanivar")
        val months = arrayOf("", "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December")
        val day = days[cal.get(Calendar.DAY_OF_WEEK)]
        val date = cal.get(Calendar.DAY_OF_MONTH)
        val month = months[cal.get(Calendar.MONTH) + 1]
        val year = cal.get(Calendar.YEAR)
        speak("Aaj $day hai, $date $month $year")
    }

    private fun tellJoke() {
        val jokes = listOf(
            "Programmer restaurant gaya, waiter ne pucha kya chahiye? Usne kaha: NULL.",
            "Bug dhundne mein 2 ghante lage, fix karne mein 2 second. Zindagi hai yahi.",
            "Mera code pehli baar chal gaya — main samjha computer ne kuch galat kar diya!",
            "WiFi ka password bata do. Pehle order karo!",
            "Ek developer ne Google pe search kiya — aur StackOverflow ne usse bachaya. Phir se.",
            "My code works. I have no idea why. Please don't touch it.",
            "Boss ne kaha feature add karo. Main ne stackoverflow khola. Zindagi chali.",
            "Duniya mein 10 type ke log hain — binary samjhne wale aur na samjhne wale!",
            "Software 90% done hai. Baaki 90% bhi done hai. Sirf testing bachi hai.",
            "Kisi ne mujhse pucha LinkedIn pe kya likhun? Maine kaha: Currently breathing."
        )
        speak(jokes.random())
    }

    private fun tellShayari() {
        val shayari = listOf(
            "Code likhta hun, bugs aate hain. Phir bhi rukta nahi, yahi programmer ka andaz hai.",
            "Zindagi ka algorithm simple hai — input de, output lo, error aaye toh debug karo.",
            "Raat ko sona cha'hun, par production mein bug hai. Coffee pi lo, yahi zindagi hai.",
            "Jo deadline se darta nahi, woh asli developer hai.",
            "Keyboard pe ungliyaan chalatein hain, dreams ko code mein dhaltein hain."
        )
        speak(shayari.random())
    }

    private fun tellFact() {
        val facts = listOf(
            "Kya aap jaante hain? Pehla computer bug ek asli kira tha — 1947 mein ek moth computer mein ghus gayi thi!",
            "Google ka naam 'Googol' se aaya hai, jo ek aur number hai — 1 ke baad 100 zeros.",
            "Pehle mobile phone ka weight 1.1 kilogram tha — bilkul ek brick ki tarah!",
            "Instagram ka pehla photo ek kutta tha — Josh Systrom ka dog.",
            "WhatsApp ke founders ne pehle Facebook mein job ke liye apply kiya tha — reject ho gaye the!"
        )
        speak(facts.random())
    }

    private fun tellMotivation() {
        val quotes = listOf(
            "Boss, hone do galti — sikhno ki path yaheen se shuru hoti hai!",
            "Ek choti si coding aaj, ek badi duniya kal. Chaltey raho!",
            "VEGA aapke saath hai — mushkil code woh bhi, aur mushkil din bhi!",
            "Steve Jobs ne kaha: Stay Hungry, Stay Foolish. Main bolunga: Stay Coding!",
            "Har bug ek naya lesson hai. Aap seekh rahe ho — ye hi success hai!"
        )
        speak(quotes.random())
    }

    private fun tellRiddle() {
        val riddles = listOf(
            "Paheli suno: Main hamesha aage daud ta hun, kabhi peeche nahi aata. Main kaun hun? — Answer: Samay!",
            "Jitna bhar lo utna khali ho jaata hun. Main kaun hun? — Answer: Akash!",
            "Mujhe sabse zyada poor log use karte hain, ameer log kabhi nahi. Main kaun hun? — Answer: Kuch nahi!"
        )
        speak(riddles.random())
    }

    private fun playRPS(cmd: String) {
        val opts = listOf("Pathar", "Kaagaz", "Qainchi")
        val vegaChoice = opts.random()
        val userChoice = when {
            cmd.contains("pathar") || cmd.contains("rock") || cmd.contains("stone") -> "Pathar"
            cmd.contains("kaagaz") || cmd.contains("paper") -> "Kaagaz"
            else -> "Qainchi"
        }
        val result = when {
            userChoice == vegaChoice -> "Barabar!"
            (userChoice == "Pathar" && vegaChoice == "Qainchi") ||
            (userChoice == "Kaagaz" && vegaChoice == "Pathar") ||
            (userChoice == "Qainchi" && vegaChoice == "Kaagaz") -> "Aap jeete!"
            else -> "Main jeeta! Ha ha!"
        }
        speak("Maine $vegaChoice chuna. Aapne $userChoice chuna. $result")
    }

    private fun tellStory() {
        speak("Ek baar ki baat hai — ek developer tha jiska naam VEGA tha. Wo raat din code karta tha. " +
            "Ek din usne ek aisi app banayi jo poori duniya badal de. Aur wo developer bhi tum ho! Bas mehnat karte raho Boss.")
    }

    private fun speak(text: String) {
        lastSpokenText = text
        Log.d(TAG, "TTS: $text")
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "vega_tts_${System.currentTimeMillis()}")
        MainActivity.lastCommandCallback?.invoke("🤖 $text")
    }

    private fun showToast(msg: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
    }
}

// Keep AccessibilityService reference accessible
private typealias AccessibilityService = android.accessibilityservice.AccessibilityService
