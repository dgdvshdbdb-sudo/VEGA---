package com.vega.agent

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        var lastCommandCallback: ((String) -> Unit)? = null
    }

    private val requiredPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.CAMERA,
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) startVega()
        else statusState.value = "⚠️ Kuch permissions nahi mili. Phir try karo."
    }

    private val statusState = mutableStateOf("Shuru karne ke liye AWAKEN dabao")
    private val isRunning = mutableStateOf(false)
    private val logMessages = mutableStateListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register callback for live updates
        lastCommandCallback = { msg ->
            runOnUiThread {
                logMessages.add(0, msg) // latest first
                if (logMessages.size > 50) logMessages.removeAt(logMessages.size - 1)
                statusState.value = msg
            }
        }
        VegaVoiceService.statusCallback = { msg ->
            runOnUiThread {
                logMessages.add(0, msg)
                if (logMessages.size > 50) logMessages.removeAt(logMessages.size - 1)
                statusState.value = msg
            }
        }

        setContent {
            VegaMainScreen(
                status = statusState.value,
                isRunning = isRunning.value,
                logs = logMessages,
                onAwaken = { checkAndStart() },
                onStop = { stopVega() },
                onAccessibility = {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            )
        }
    }

    private fun checkAndStart() {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) startVega()
        else permissionLauncher.launch(missing.toTypedArray())
    }

    private fun startVega() {
        isRunning.value = true
        statusState.value = "🟢 VEGA Active — Awaiting Commands"
        logMessages.add(0, "✅ VEGA started successfully")
        startForegroundService(Intent(this, VegaVoiceService::class.java))
    }

    private fun stopVega() {
        isRunning.value = false
        statusState.value = "🔴 VEGA Stopped"
        logMessages.add(0, "⛔ VEGA stopped")
        stopService(Intent(this, VegaVoiceService::class.java))
    }
}

@Composable
fun VegaMainScreen(
    status: String,
    isRunning: Boolean,
    logs: List<String>,
    onAwaken: () -> Unit,
    onStop: () -> Unit,
    onAccessibility: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200), repeatMode = RepeatMode.Reverse
        ), label = "glow"
    )

    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF060B18), Color(0xFF0D1F3C), Color(0xFF060B18))
    )
    val emerald = Color(0xFF10B981)
    val emeraldDark = Color(0xFF059669)

    Box(
        modifier = Modifier.fillMaxSize().background(bgGradient)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ─── Header ─────────────────────────
            Spacer(Modifier.height(40.dp))
            Text("V·E·G·A", color = emerald, fontSize = 42.sp,
                fontWeight = FontWeight.Black, letterSpacing = 8.sp)
            Text("Voice Enabled General Agent",
                color = Color(0xFF64748B), fontSize = 13.sp, letterSpacing = 2.sp)

            Spacer(Modifier.height(36.dp))

            // ─── Pulsing Core ───────────────────
            Box(contentAlignment = Alignment.Center) {
                // outer glow
                Box(modifier = Modifier
                    .size(160.dp).scale(if (isRunning) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(emerald.copy(alpha = if (isRunning) glowAlpha else 0.08f))
                )
                // inner circle
                Box(modifier = Modifier
                    .size(95.dp).clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(emerald, emeraldDark, Color(0xFF064E3B)))
                    )
                    .border(2.dp, emerald.copy(0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (isRunning) "🎙️" else "😴", fontSize = 34.sp)
                }
            }

            Spacer(Modifier.height(20.dp))

            // ─── Status Chip ────────────────────
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isRunning) emerald.copy(0.15f) else Color(0xFF1E293B),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(status, color = if (isRunning) emerald else Color.LightGray,
                    fontSize = 13.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Spacer(Modifier.height(28.dp))

            // ─── Buttons ────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!isRunning) {
                    Button(
                        onClick = onAwaken,
                        colors = ButtonDefaults.buttonColors(containerColor = emerald),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(52.dp).weight(1f)
                    ) {
                        Text("⚡ AWAKEN", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                } else {
                    Button(
                        onClick = onStop,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(52.dp).weight(1f)
                    ) {
                        Text("⛔ STOP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
                OutlinedButton(
                    onClick = onAccessibility,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, emerald.copy(0.5f)),
                    modifier = Modifier.height(52.dp).weight(1f)
                ) {
                    Text("♿ Access", color = emerald, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(24.dp))

            // ─── Command Tips ───────────────────
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0F172A),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("💡 Commands", color = emerald, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))
                    val tips = listOf(
                        "\"Call karo [naam]\"",
                        "\"YouTube kholo\"",
                        "\"Volume badha do\"",
                        "\"7 baje alarm lagao\"",
                        "\"Mute karo\"",
                        "\"Weather search karo\""
                    )
                    tips.forEach {
                        Text("  • $it", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ─── Live Log ───────────────────────
            Text("📋 Live Log", color = emerald, fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0A0E1A),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                LazyColumn(Modifier.padding(10.dp)) {
                    if (logs.isEmpty()) {
                        item {
                            Text("  VEGA awaiting activation...",
                                color = Color(0xFF334155), fontSize = 12.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        }
                    }
                    items(logs) { msg ->
                        Text("  > $msg", color = Color(0xFF4ADE80), fontSize = 11.5.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 1.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
