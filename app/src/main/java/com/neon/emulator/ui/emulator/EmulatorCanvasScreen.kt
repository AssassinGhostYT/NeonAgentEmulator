package com.neon.emulator.ui.emulator

import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.neon.emulator.model.PhoneModel

@Composable
fun EmulatorCanvasScreen(
    selectedModel: PhoneModel,
    onWebViewCreated: (WebView) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "neonGlow")
    val glowColor by infiniteTransition.animateColor(
        initialValue = Color(0xFF0EA5E9),
        targetValue = Color(0xFFEC4899),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowColor"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        // 📲 EL MARCO/CHASIS DEL TELÉFONO ES PERMANENTE Y FIJO (NO SE MUEVE)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(selectedModel.aspectRatioWidth)
                .shadow(20.dp, RoundedCornerShape(selectedModel.cornerRadius.dp))
                .border(3.dp, glowColor, RoundedCornerShape(selectedModel.cornerRadius.dp))
                .clip(RoundedCornerShape(selectedModel.cornerRadius.dp))
                .background(Color(0xFF0F172A))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                
                // 📡 1. BARRA DE NOTIFICACIONES REAL
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "12:45",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    when (selectedModel) {
                        PhoneModel.IPHONE_15_PRO_MAX -> {
                            Box(
                                modifier = Modifier
                                    .width(85.dp)
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0F172A))
                                    .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(10.dp))
                            )
                        }
                        PhoneModel.SAMSUNG_A50 -> {
                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                                    .background(Color(0xFF0F172A))
                                    .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                            )
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0F172A))
                                    .border(1.5.dp, Color(0xFF38BDF8), CircleShape)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SignalCellular4Bar, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Wifi, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.BatteryFull, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                // 🖥️ 2. PANTALLA INTERNA DEL PROYECTO (AQUÍ SE MUEVEN Y REORDENAN LOS ELEMENTOS CON EL DEDO EN TIEMPO REAL)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.allowFileAccess = true
                                settings.allowContentAccess = true
                                settings.allowUniversalAccessFromFileURLs = true
                                webViewClient = WebViewClient()
                                webChromeClient = WebChromeClient()

                                onWebViewCreated(this)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 📱 3. BOTONES INFERIORES DE NAVEGACIÓN ANDROID
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .background(Color(0xFF1E293B)),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .border(1.5.dp, Color.White, RoundedCornerShape(2.dp))
                    )
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    Text(
                        text = "◀",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
