package com.vega.agent

import android.util.Log
import java.util.Calendar

/**
 * VegaOfflineAI — 100% offline, no API, no internet needed
 * Smart pattern matching + knowledge base
 */
object VegaOfflineAI {

    private val TAG = "VegaOfflineAI"

    // ─── Knowledge Base ───────────────────────────────────────
    data class QnA(val patterns: List<String>, val answers: List<String>)

    private val knowledgeBase = listOf(

        // ── Greetings
        QnA(listOf("hello", "hi", "namaste", "namaskar", "hey", "hii"),
            listOf("Namaste Boss! Kya seva karun?", "Hello Boss! Bol kya chahiye?", "Hey Boss! Ready hun!")),

        QnA(listOf("good morning", "subah", "savera"),
            listOf("Good morning Boss! Naya din, naye sapne. Kya plan hai aaj?", "Subah bakhair Boss! Coffee pi li kya?")),

        QnA(listOf("good night", "sona", "raat"),
            listOf("Good night Boss! Aaram karo. Main yaheen hun.", "Sweet dreams Boss!")),

        QnA(listOf("good afternoon", "dopahar"),
            listOf("Good afternoon Boss! Lunch ho gaya?", "Dopahar mubarak Boss!")),

        QnA(listOf("good evening", "shaam"),
            listOf("Good evening Boss! Kaisa raha aaj ka din?", "Shaam ki dhoop mein baith ke chai pi lo Boss!")),

        // ── VEGA identity
        QnA(listOf("tera naam", "kaun hai tu", "who are you", "your name", "tum kaun"),
            listOf("Main VEGA hun — Voice Enabled General Agent. Aapka 100% offline personal AI assistant!",
                "Main VEGA hun Boss — ek smart voice assistant jo bina internet ke bhi kaam karta hai!")),

        QnA(listOf("kaisa hai", "kya hal", "how are you", "theek hai"),
            listOf("Main bilkul theek hun Boss! System fully operational. Aapki seva ke liye taiyaar!",
                "Ekdum badiya Boss! Koi kaam batao.")),

        QnA(listOf("thanks", "shukriya", "dhanyawad", "thank you", "shukriyaa"),
            listOf("Koi baat nahi Boss! Hamesha seva mein hoon.", "Bas yahi toh kaam hai mera Boss!")),

        // ── General Knowledge
        QnA(listOf("india ki rajdhani", "capital of india", "delhi"),
            listOf("India ki rajdhani New Delhi hai Boss.")),

        QnA(listOf("bharat ki jansankhya", "india population"),
            listOf("India ki aabadi lagbhag 1.4 arab se zyada hai — duniya mein sabse zyada!")),

        QnA(listOf("sabse bada", "largest", "biggest"),
            listOf("Duniya ka sabse bada desh Russia hai. India area mein 7th largest hai.")),

        QnA(listOf("android kya hai", "android kya", "what is android"),
            listOf("Android ek mobile operating system hai jo Google ne banaya hai. Linux kernel pe based hai.")),

        QnA(listOf("vega kya hai", "what is vega"),
            listOf("VEGA ek open-source voice AI agent hai jo aapke phone ko voice se control karta hai — 100% offline!")),

        QnA(listOf("ai kya hai", "artificial intelligence", "machine learning"),
            listOf("AI matlab Artificial Intelligence — computers ko insaanon ki tarah sochne ki ability. Main bhi AI hun!")),

        QnA(listOf("python", "java", "kotlin", "programming"),
            listOf("Programming ek bahut powerful skill hai Boss! Kotlin Android ke liye best hai, Python AI ke liye.")),

        // ── Fun responses
        QnA(listOf("joke", "funny", "hasao", "hasna"),
            listOf(
                "Programmer restaurant gaya, waiter ne pucha kya chahiye? Usne kaha: NULL.",
                "Bug fix karne mein 2 ghante, dhundne mein 6 ghante. Programmer ki zindagi!",
                "My code works. Main nahi jaanta kyon. Please mat chhuona!",
                "Stack Overflow ke bina programming — jaise paani ke bina swimming!",
                "Ek developer ke 3 bacche the — 0, 1 aur... uska naam 2 tha."
            )),

        QnA(listOf("shayari", "poetry", "poem", "sher"),
            listOf(
                "Code likhta hun raat ko, bugs aate hain subah ko. Phir bhi rukta nahi, programmer hun main!",
                "Keyboard pe ungliyaan naachti hain, sapne ban jaate hain code mein.",
                "Zindagi ka algorithm simple hai — error aaye toh debug karo, kaam chale toh deploy karo!"
            )),

        QnA(listOf("quote", "motivation", "inspire", "himmat", "hausla"),
            listOf(
                "Steve Jobs: Stay Hungry, Stay Foolish. Aap bhi aise hi raho Boss!",
                "Har ek expert pehle beginner tha. Chaltey raho Boss!",
                "Code karo, fail karo, seekho, repeat karo — yahi success ka formula hai!",
                "Mushkilein aati hain toh samjho — sahi raah pe ho!"
            )),

        QnA(listOf("fact", "interesting", "did you know", "kya pata hai"),
            listOf(
                "Pehla computer bug ek asli kira tha — 1947 mein ek moth computer mein ghus gayi!",
                "Google ka naam 'Googol' se aaya — 1 ke baad 100 zeros!",
                "WhatsApp ke founders pehle Facebook mein apply kar chuke the — reject ho gaye the!",
                "Instagram ka pehla photo ek kutta tha!",
                "Android ka naam 'Andy Rubin' ke nickname se aaya."
            )),

        QnA(listOf("riddle", "paheli", "puzzle"),
            listOf(
                "Paheli: Main hamesha aage jaata hun, kabhi peeche nahi aata. Kaun hun main? — Samay!",
                "Paheli: Jitna daalo utna khali ho jaata hun. Main kaun? — Akash!",
                "Paheli: Duniya mein sabse tez kya hai? — Dimag, ek pal mein sab soch leta hai!"
            )),

        QnA(listOf("story", "kahani", "batao koi"),
            listOf("Ek baar ki baat hai — ek chhota developer tha jiske paas ek bada sapna tha. Raat din code kiya. " +
                "Ek din uski app duniya bhaar mein mashoor ho gayi. Woh developer aap ho Boss — bas mehnat karte raho!")),

        // ── Health Tips
        QnA(listOf("health", "sehat", "timing", "tips"),
            listOf("Boss, phone se thodi break lo. Aankhen aaram karengi. 20-20-20 rule: har 20 min mein 20 sec door dekho.",
                "Paani piyo, stretch karo, aur neend poori lo. Yahi success ka asli formula hai Boss!")),

        QnA(listOf("neend", "sleep", "so jaana", "sona"),
            listOf("7-8 ghante ki neend zaroori hai Boss. Phone rakh do aur aaramse so jaao — main handle kar lunga!")),

        // ── Farewells
        QnA(listOf("bye", "alvida", "tata", "ok bye", "goodbye"),
            listOf("Alvida Boss! Zaroorat ho toh bulaana — main yaheen hun!", "Bye Boss! Take care!")),

        // ── Compliments to user
        QnA(listOf("main smart", "mujhe janta", "i am", "main hun"),
            listOf("Bilkul Boss! Aap bahut smart hain, isliye toh VEGA use kar rahe ho!")),

        // ── VEGA capabilities
        QnA(listOf("kya kar sakta hai", "features", "help", "madad"),
            listOf("Boss main bahut kuch kar sakta hun! Call, message, apps open, alarm, music, settings, " +
                "weather search, aur bhi bahut kuch. Kuch bolo — main karunga!")),

        // ── Time-aware
        QnA(listOf("aaj kya din", "weekday", "week"),
            listOf("Aaj ${getDayName()} hai Boss!")),

        // ── Easter eggs
        QnA(listOf("jarvis", "iron man", "tony stark"),
            listOf("Main VEGA hun Boss, Jarvis se bhi better version! 😄")),

        QnA(listOf("siri", "alexa", "cortana", "google assistant"),
            listOf("Unse better hun Boss — main offline bhi kaam karta hun, free hun, aur fully customizable hun!")),
    )

    fun respond(query: String): String {
        val q = query.lowercase().trim()
        Log.d(TAG, "Offline AI query: $q")

        // Pattern match karo
        for (item in knowledgeBase) {
            if (item.patterns.any { q.contains(it) }) {
                return item.answers.random()
            }
        }

        // Math detect karo
        val mathResult = tryMath(q)
        if (mathResult != null) return mathResult

        // Time queries
        if (q.contains("time") || q.contains("kitne baje") || q.contains("samay")) {
            return getCurrentTime()
        }

        // Date queries
        if (q.contains("date") || q.contains("aaj") || q.contains("tarikh")) {
            return getCurrentDate()
        }

        // Default fallback responses
        val defaults = listOf(
            "Ye toh pata nahi Boss — koi system command bolo!",
            "Samjha nahi Boss. App open karna ho, call karna ho, ya kuch aur?",
            "Hmm... abhi offline hun, internet wala kaam nahi hoga. Par system commands karunga!",
            "Boss, thoda clearly bolo? Main zyada samajhne ki koshish karunga.",
            "Ye mujhse zyada mushkil sawaal hai Boss. Kuch aur pucho!"
        )
        return defaults.random()
    }

    private fun tryMath(expr: String): String? {
        return try {
            val r = Regex("(\\d+\\.?\\d*)\\s*([+\\-x*×÷/])\\s*(\\d+\\.?\\d*)")
            val m = r.find(expr) ?: return null
            val a = m.groupValues[1].toDouble()
            val op = m.groupValues[2]
            val b = m.groupValues[3].toDouble()
            val result = when (op) {
                "+", "plus" -> a + b
                "-", "minus" -> a - b
                "*", "x", "×", "guna" -> a * b
                "/", "÷", "bhaag" -> if (b != 0.0) a / b else return "Zero se divide nahi ho sakta Boss!"
                else -> return null
            }
            val ans = if (result == result.toLong().toDouble()) result.toLong().toString() else "%.2f".format(result)
            "Jawab hai: $ans Boss!"
        } catch (e: Exception) { null }
    }

    private fun getCurrentTime(): String {
        val c = Calendar.getInstance()
        val h = c.get(Calendar.HOUR_OF_DAY)
        val m = c.get(Calendar.MINUTE)
        val period = when {
            h < 5 -> "raat"; h < 12 -> "subah"; h < 17 -> "dopahar"
            h < 20 -> "shaam"; else -> "raat"
        }
        return "$period ke $h baj ke $m minute hue hain Boss"
    }

    private fun getCurrentDate(): String {
        val c = Calendar.getInstance()
        val months = arrayOf("", "January","February","March","April","May","June",
            "July","August","September","October","November","December")
        return "Aaj ${c.get(Calendar.DAY_OF_MONTH)} ${months[c.get(Calendar.MONTH)+1]} ${c.get(Calendar.YEAR)} hai Boss"
    }

    private fun getDayName(): String {
        val days = arrayOf("", "Ravivar", "Somvar", "Mangalvar", "Budhvar", "Guruvar", "Shukravar", "Shanivar")
        return days[Calendar.getInstance().get(Calendar.DAY_OF_WEEK)]
    }
}
