package com.vega.agent

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
        Manifest.permission.CAMERA,
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) startVega()
        else statusState.value = "⚠️ Kuch permissions nahi mili. Phir try karo."
    }

    private val statusState = mutableStateOf("Shuru karne ke liye AWAKEN dabao")
    private val isRunning = mutableStateOf(false)
    private val logMessages = mutableStateListOf<String>()

    // Gemma download state
    private val downloadProgress = mutableStateOf(0)
    private val downloadMB = mutableStateOf(0f)
    private val totalMB = mutableStateOf(0f)
    private val downloadSpeed = mutableStateOf(0f)
    private val isDownloading = mutableStateOf(false)
    private val isModelReady = mutableStateOf(false)
    private val gemmaStatus = mutableStateOf("")

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Callbacks
        lastCommandCallback = { msg -> runOnUiThread {
            logMessages.add(0, msg)
            if (logMessages.size > 60) logMessages.removeAt(logMessages.size - 1)
            statusState.value = msg
        }}
        VegaVoiceService.statusCallback = { msg -> runOnUiThread {
            logMessages.add(0, msg)
            if (logMessages.size > 60) logMessages.removeAt(logMessages.size - 1)
            statusState.value = msg
        }}

        // Check model on start
        checkModelStatus()

        setContent {
            VegaMainScreen(
                status = statusState.value,
                isRunning = isRunning.value,
                logs = logMessages,
                downloadPercent = downloadProgress.value,
                downloadedMB = downloadMB.value,
                totalMB = totalMB.value,
                speedKBps = downloadSpeed.value,
                isDownloading = isDownloading.value,
                isModelReady = isModelReady.value,
                gemmaStatus = gemmaStatus.value,
                onAwaken = { checkAndStart() },
                onStop = { stopVega() },
                onDownloadModel = { startModelDownload() },
                onLoadModel = { loadGemma() },
                onAccessibility = { startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            )
        }
    }

    private fun checkModelStatus() {
        isModelReady.value = VegaModelDownloader.isDownloaded(this)
        gemmaStatus.value = when {
            isModelReady.value -> "✅ Gemma 2B ready (${VegaModelDownloader.getFileSizeMB(this).toInt()} MB)"
            else -> "⬇️ Gemma 2B not downloaded"
        }
    }

    private fun startModelDownload() {
        if (isDownloading.value) return
        isDownloading.value = true
        gemmaStatus.value = "Downloading Gemma 2B..."
        logMessages.add(0, "⬇️ Gemma 2B download shuru...")

        scope.launch {
            VegaModelDownloader.download(
                context = this@MainActivity,
                onProgress = { p ->
                    downloadProgress.value = p.percent
                    downloadMB.value = p.downloadedMB
                    totalMB.value = p.totalMB
                    downloadSpeed.value = p.speedKBps
                    gemmaStatus.value = "Downloading... ${p.percent}%"
                },
                onComplete = {
                    isDownloading.value = false
                    isModelReady.value = true
                    gemmaStatus.value = "✅ Download complete! Loading..."
                    logMessages.add(0, "✅ Gemma 2B downloaded!")
                    loadGemma()
                },
                onError = { err ->
                    isDownloading.value = false
                    gemmaStatus.value = "❌ Download failed: $err"
                    logMessages.add(0, "❌ Download error: $err")
                }
            )
        }
    }

    private fun loadGemma() {
        gemmaStatus.value = "Loading Gemma 2B on-device..."
        logMessages.add(0, "🧠 Gemma loading (30-60 sec)...")
        VegaGemmaEngine.onStatusUpdate = { s -> runOnUiThread { gemmaStatus.value = s } }
        scope.launch(Dispatchers.IO) {
            when (val result = VegaGemmaEngine.initialize(this@MainActivity)) {
                is VegaGemmaEngine.InitResult.Success -> runOnUiThread {
                    gemmaStatus.value = "🧠 Gemma 2B active — Full AI mode!"
                    logMessages.add(0, "🧠 Gemma online!")
                }
                is VegaGemmaEngine.InitResult.ModelNotFound -> runOnUiThread {
                    gemmaStatus.value = "⬇️ Model nahi mila — Download karo"
                    isModelReady.value = false
                }
                is VegaGemmaEngine.InitResult.Error -> runOnUiThread {
                    gemmaStatus.value = "❌ Load failed: ${result.message}"
                }
                else -> {}
            }
        }
    }

    private fun checkAndStart() {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) startVega() else permissionLauncher.launch(missing.toTypedArray())
    }

    private fun startVega() {
        isRunning.value = true
        statusState.value = "🟢 VEGA Active"
        logMessages.add(0, "✅ VEGA started!")
        startForegroundService(Intent(this, VegaVoiceService::class.java))
    }

    private fun stopVega() {
        isRunning.value = false
        statusState.value = "🔴 VEGA Stopped"
        stopService(Intent(this, VegaVoiceService::class.java))
    }
}

// ══════════════════════════════════════════════════════
//  UI
// ══════════════════════════════════════════════════════
@Composable
fun VegaMainScreen(
    status: String, isRunning: Boolean, logs: List<String>,
    downloadPercent: Int, downloadedMB: Float, totalMB: Float, speedKBps: Float,
    isDownloading: Boolean, isModelReady: Boolean, gemmaStatus: String,
    onAwaken: () -> Unit, onStop: () -> Unit,
    onDownloadModel: () -> Unit, onLoadModel: () -> Unit, onAccessibility: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "glow"
    )

    val emerald = Color(0xFF10B981)
    val bgGradient = Brush.verticalGradient(listOf(Color(0xFF060B18), Color(0xFF0D1F3C), Color(0xFF060B18)))

    Box(modifier = Modifier.fillMaxSize().background(bgGradient)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(30.dp))

            // ─── Title ───
            Text("V·E·G·A", color = emerald, fontSize = 38.sp,
                fontWeight = FontWeight.Black, letterSpacing = 8.sp)
            Text("Voice Enabled General Agent", color = Color(0xFF64748B),
                fontSize = 12.sp, letterSpacing = 2.sp)

            Spacer(Modifier.height(24.dp))

            // ─── Orb ───
            Box(contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(150.dp)
                    .scale(if (isRunning) pulseScale else 1f).clip(CircleShape)
                    .background(emerald.copy(if (isRunning) glowAlpha else 0.07f)))
                Box(modifier = Modifier.size(85.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(emerald, Color(0xFF059669), Color(0xFF064E3B))))
                    .border(2.dp, emerald.copy(0.6f), CircleShape),
                    contentAlignment = Alignment.Center) {
                    Text(if (isRunning) "🎙️" else "😴", fontSize = 30.sp)
                }
            }

            Spacer(Modifier.height(14.dp))

            // ─── Status chip ───
            Surface(shape = RoundedCornerShape(50),
                color = if (isRunning) emerald.copy(0.15f) else Color(0xFF1E293B)) {
                Text(status, color = if (isRunning) emerald else Color.LightGray,
                    fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Spacer(Modifier.height(16.dp))

            // ─── Buttons ───
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = if (!isRunning) onAwaken else onStop,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isRunning) emerald else Color(0xFFEF4444)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text(if (!isRunning) "⚡ AWAKEN" else "⛔ STOP",
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                OutlinedButton(
                    onClick = onAccessibility, shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, emerald.copy(0.5f)),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) { Text("♿ Access", color = emerald, fontSize = 13.sp) }
            }

            Spacer(Modifier.height(14.dp))

            // ─── Gemma AI Card ───
            Surface(shape = RoundedCornerShape(14.dp),
                color = Color(0xFF0F172A), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🧠", fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Gemma 2B — On-Device AI",
                                color = emerald, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(gemmaStatus, color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                    }

                    if (isDownloading) {
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = downloadPercent / 100f,
                            color = emerald, trackColor = Color(0xFF1E293B),
                            strokeCap = StrokeCap.Round,
                            modifier = Modifier.fillMaxWidth().height(6.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${downloadPercent}% — ${"%.0f".format(downloadedMB)}/${"%.0f".format(totalMB)} MB",
                                color = Color(0xFF64748B), fontSize = 10.sp)
                            Text("${"%.0f".format(speedKBps)} KB/s",
                                color = Color(0xFF64748B), fontSize = 10.sp)
                        }
                    } else {
                        Spacer(Modifier.height(10.dp))
                        if (!isModelReady) {
                            Button(onClick = onDownloadModel,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().height(38.dp)) {
                                Text("⬇️ Download Gemma 2B (1.5 GB)", fontSize = 12.sp, color = Color.White)
                            }
                        } else {
                            Button(onClick = onLoadModel,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().height(38.dp)) {
                                Text("🚀 Load Gemma AI", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ─── Commands tip ───
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF0A0E1A),
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp)) {
                    Text("💡 Bol ke try karo", color = emerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    listOf("\"YouTube kholo\"", "\"7 baje alarm\"", "\"Joke sunao\"",
                        "\"Volume badha do\"", "\"Call karo mummy\"").forEach {
                        Text("• $it", color = Color(0xFF64748B), fontSize = 11.sp)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ─── Live log ───
            Text("📋 Live", color = emerald, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF060B18),
                modifier = Modifier.fillMaxWidth().weight(1f)) {
                LazyColumn(Modifier.padding(8.dp)) {
                    if (logs.isEmpty()) item {
                        Text("  VEGA awaiting activation...", color = Color(0xFF1E293B),
                            fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    items(logs) { msg ->
                        Text("  > $msg", color = Color(0xFF4ADE80), fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 0.5.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
