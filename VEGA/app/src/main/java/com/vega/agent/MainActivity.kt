package com.vega.agent

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VegaUiScreen(onStartAI = {
                Toast.makeText(this, "VEGA: Need Android Permissions...", Toast.LENGTH_SHORT).show()
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            })
        }
    }
}

@Composable
fun VegaUiScreen(onStartAI: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val glowSize by infiniteTransition.animateFloat(
        initialValue = 100f,
        targetValue = 140f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)), 
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "V.E.G.A",
            color = Color(0xFF10B981), // Emerald Green for VEGA
            fontSize = 45.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 6.sp
        )
        
        Text(
            text = "Advanced Offline Agent",
            color = Color.LightGray,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(70.dp))

        // Glowing Core
        Box(
            modifier = Modifier
                .size(glowSize.dp)
                .clip(CircleShape)
                .background(Color(0xFF10B981).copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF059669)) 
            )
        }

        Spacer(modifier = Modifier.height(70.dp))

        Button(
            onClick = onStartAI,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            shape = RoundedCornerShape(25.dp),
            modifier = Modifier.height(60.dp).padding(horizontal = 40.dp)
        ) {
            Text(
                "AWAKEN VEGA",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}
