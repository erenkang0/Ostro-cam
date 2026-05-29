package com.example.ui.components

import android.graphics.Canvas
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.AstroLog
import com.example.sensors.AstroCelestialOrientation
import com.example.viewmodel.AstroPreset
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun StargazerLevelBubble(
    orientation: AstroCelestialOrientation,
    isRedMode: Boolean,
    modifier: Modifier = Modifier
) {
    val accentColor = if (isRedMode) Color(0xFFFF0055) else Color(0xFF00FFCC)
    val gridColor = if (isRedMode) Color(0xFF550000) else Color(0x3300FFCC)

    Box(
        modifier = modifier
            .border(1.dp, gridColor, CircleShape)
            .background(Color(0x2205050A), CircleShape)
            .padding(4.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.width / 2f
            
            // Draw crosshairs
            drawLine(
                color = gridColor,
                start = Offset(0f, center.y),
                end = Offset(size.width, center.y),
                strokeWidth = 1f
            )
            drawLine(
                color = gridColor,
                start = Offset(center.x, 0f),
                end = Offset(center.x, size.height),
                strokeWidth = 1f
            )

            // Draw center target circle
            drawCircle(
                color = gridColor,
                radius = outerRadius * 0.25f,
                style = Stroke(width = 1f)
            )

            // Calculate bubble position based on pitch & roll
            // Maximum tilt we track dynamically is around 15 degrees
            val maxTilt = 15f
            val pitchClamped = orientation.pitch.coerceIn(-maxTilt, maxTilt)
            val rollClamped = orientation.roll.coerceIn(-maxTilt, maxTilt)

            val xPct = rollClamped / maxTilt
            val yPct = -pitchClamped / maxTilt // Invert because screen coords increase downwards

            val bubbleX = center.x + (xPct * (outerRadius - 15.dp.toPx()))
            val bubbleY = center.y + (yPct * (outerRadius - 15.dp.toPx()))

            // Check if level is perfectly balanced (safe tripod hold!)
            val isBalanced = kotlin.math.abs(orientation.pitch) < 1.0f && kotlin.math.abs(orientation.roll) < 1.0f
            val bubbleColor = if (isBalanced) {
                if (isRedMode) Color(0xFFFF3333) else Color(0xFF00FF77)
            } else {
                accentColor
            }

            // Draw level bubble
            drawCircle(
                color = bubbleColor,
                radius = 8.dp.toPx(),
                center = Offset(bubbleX, bubbleY)
            )
            
            // Center ring glow
            if (isBalanced) {
                drawCircle(
                    color = bubbleColor.copy(alpha = 0.3f),
                    radius = 16.dp.toPx(),
                    center = Offset(bubbleX, bubbleY)
                )
            }
        }
    }
}

@Composable
fun TacticalCompassLevelRadar(
    orientation: AstroCelestialOrientation,
    isRedMode: Boolean,
    modifier: Modifier = Modifier
) {
    val primaryColor = if (isRedMode) Color(0xFFFF0055) else Color(0xFF9d00ff)
    val accentColor = if (isRedMode) Color(0xFFFF5555) else Color(0xFF00FFCC)
    val gridColor = if (isRedMode) Color(0xFF470000) else Color(0x44FFFFFF)
    val textColor = if (isRedMode) Color(0xFFFF3333) else Color.White

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isRedMode) Color(0xFF0D0000) else Color(0xFF0D0D15), RoundedCornerShape(16.dp))
            .border(1.dp, gridColor, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "YILDIZ KONUMLAMA TELESKOP RADARI",
            color = primaryColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f

                // Outer Radar Rings
                drawCircle(color = gridColor, radius = radius, style = Stroke(width = 1f))
                drawCircle(color = gridColor, radius = radius * 0.7f, style = Stroke(width = 1f))
                drawCircle(color = gridColor, radius = radius * 0.4f, style = Stroke(width = 1f))

                // Compass Direction Markers
                val cardinalPoints = listOf("N" to 270f, "E" to 0f, "S" to 90f, "W" to 180f)
                cardinalPoints.forEach { (label, angle) ->
                    val angleRad = Math.toRadians(angle.toDouble())
                    val textDist = radius - 14.dp.toPx()
                    val x = center.x + (textDist * cos(angleRad)).toFloat()
                    val y = center.y + (textDist * sin(angleRad)).toFloat()

                    drawCircle(
                        color = gridColor,
                        radius = 2.dp.toPx(),
                        center = Offset(center.x + (radius * cos(angleRad)).toFloat(), center.y + (radius * sin(angleRad)).toFloat())
                    )
                }

                // Grid subdivisions
                for (a in 0 until 360 step 45) {
                    val angleRad = Math.toRadians(a.toDouble())
                    drawLine(
                        color = gridColor.copy(alpha = 0.5f),
                        start = center,
                        end = Offset(
                            center.x + (radius * cos(angleRad)).toFloat(),
                            center.y + (radius * sin(angleRad)).toFloat()
                        ),
                        strokeWidth = 1f
                    )
                }

                // Rotated Compass Needle (pointing North)
                // Offset by heading angle (azimuth)
                val headingRad = Math.toRadians((orientation.azimuth - 90).toDouble())
                val needleLength = radius * 0.85f
                val needleX = center.x + (needleLength * cos(headingRad)).toFloat()
                val needleY = center.y + (needleLength * sin(headingRad)).toFloat()

                // Draw needle line
                drawLine(
                    color = accentColor,
                    start = center,
                    end = Offset(needleX, needleY),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )

                // Highlight North tip
                drawCircle(
                    color = accentColor,
                    radius = 5.dp.toPx(),
                    center = Offset(needleX, needleY)
                )
            }

            // Overlay the balance level dead-center so user gets dual compass + pitch/roll feedback!
            StargazerLevelBubble(
                orientation = orientation,
                isRedMode = isRedMode,
                modifier = Modifier
                    .size(70.dp)
                    .align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Telemetry Readouts (Futuristic Military HUD)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "AZİMUT (Compass)", 
                    color = primaryColor, 
                    fontSize = 10.sp, 
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "%.1f° %s".format(
                        orientation.azimuth,
                        getCardinalDirection(orientation.azimuth)
                    ),
                    color = textColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Divider(
                modifier = Modifier
                    .height(30.dp)
                    .width(1.dp),
                color = gridColor
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "EĞİM (Pitch)", 
                    color = primaryColor, 
                    fontSize = 10.sp, 
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "%.1f°".format(orientation.pitch),
                    color = textColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Divider(
                modifier = Modifier
                    .height(30.dp)
                    .width(1.dp),
                color = gridColor
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "DENGE (Roll)", 
                    color = primaryColor, 
                    fontSize = 10.sp, 
                    fontFamily = FontFamily.Monospace
                )
                var rollText = "%.1f°".format(orientation.roll)
                if (kotlin.math.abs(orientation.roll) < 1f) rollText += " (OK)"
                Text(
                    text = rollText,
                    color = if (kotlin.math.abs(orientation.roll) < 1f) accentColor else textColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

fun getCardinalDirection(azimuth: Float): String {
    return when (azimuth) {
        in 337.5..360.0 -> "K"
        in 0.0..22.5 -> "K"
        in 22.5..67.5 -> "KD"
        in 67.5..112.5 -> "D"
        in 112.5..157.5 -> "GD"
        in 157.5..202.5 -> "G"
        in 202.5..247.5 -> "GB"
        in 247.5..292.5 -> "B"
        in 292.5..337.5 -> "KB"
        else -> "K"
    }
}

@Composable
fun GuideOverlayLines(
    overlayType: Int,
    isRedMode: Boolean,
    modifier: Modifier = Modifier
) {
    val strokeColor = if (isRedMode) Color(0x66FF0055) else Color(0x66FFFFFF)
    
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        when (overlayType) {
            1 -> { // Rule of Thirds
                // Vertical lines
                drawLine(color = strokeColor, start = Offset(w / 3f, 0f), end = Offset(w / 3f, h), strokeWidth = 1.5f)
                drawLine(color = strokeColor, start = Offset(w * 2f / 3f, 0f), end = Offset(w * 2f / 3f, h), strokeWidth = 1.5f)
                // Horizontal lines
                drawLine(color = strokeColor, start = Offset(0f, h / 3f), end = Offset(w, h / 3f), strokeWidth = 1.5f)
                drawLine(color = strokeColor, start = Offset(0f, h * 2f / 3f), end = Offset(w, h * 2f / 3f), strokeWidth = 1.5f)
            }
            2 -> { // Golden Spiral (Aesthetic composition)
                val path = Path()
                // Outer rectangle lines of the golden ratio sequence
                path.moveTo(0f, 0f)
                // Approximate representation of Golden Fibonacci Spiral
                var currentSize = h
                var x = 0f
                var y = 0f
                drawArc(
                    color = strokeColor,
                    startAngle = 180f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(0f, 0f),
                    size = Size(w * 2, h * 2),
                    style = Stroke(width = 1.5f)
                )
                drawLine(color = strokeColor, start = Offset(w * 0.618f, 0f), end = Offset(w * 0.618f, h), strokeWidth = 1f)
                drawLine(color = strokeColor, start = Offset(0f, h * 0.618f), end = Offset(w, h * 0.618f), strokeWidth = 1f)
            }
            3 -> { // Polaris Crosshair Target Circle (Radial)
                drawCircle(color = strokeColor, radius = w * 0.35f, style = Stroke(width = 1f))
                drawCircle(color = strokeColor, radius = w * 0.15f, style = Stroke(width = 1f))
                drawLine(color = strokeColor, start = Offset(w / 2f, 0f), end = Offset(w / 2f, h), strokeWidth = 1f)
                drawLine(color = strokeColor, start = Offset(0f, h / 2f), end = Offset(w, h / 2f), strokeWidth = 1f)
            }
        }
    }
}

@Composable
fun StargazerLogbookItem(
    log: AstroLog,
    isRedMode: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = if (isRedMode) Color(0xFFFF0055) else Color(0xFF9d00ff)
    val cardBg = if (isRedMode) Color(0xFF140101) else Color(0xFF1E1E28)
    val dividerColor = if (isRedMode) Color(0xFF2C0202) else Color(0xFF2C2C3F)
    val textColor = if (isRedMode) Color(0xFFFF5555) else Color.White
    val textMuted = if (isRedMode) Color(0xFF8B0000) else Color.Gray

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .border(1.dp, dividerColor, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FilterHdr,
                        contentDescription = "Target",
                        tint = primaryColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = log.target,
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Sil",
                        tint = if (isRedMode) Color(0xFFAA0000) else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Tarih: ${log.date}",
                    color = textMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Bortle Sınıfı: ${log.bortleScale}",
                    color = textMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = dividerColor
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Info block
                Row {
                    Box(
                        modifier = Modifier
                            .background(if (isRedMode) Color(0xFF2B0000) else Color(0x339D00FF), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ISO ${log.iso}",
                            color = primaryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier
                            .background(if (isRedMode) Color(0xFF2B0000) else Color(0x2200FFCC), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = log.shutterSpeed,
                            color = if (isRedMode) Color(0xFFFF5555) else Color(0xFF00FFCC),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Stars display
                Row {
                    for (i in 1..5) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Star",
                            tint = if (i <= log.rating) {
                                if (isRedMode) Color(0xFFFF0055) else Color(0xFFFFB300)
                            } else {
                                if (isRedMode) Color(0xFF220000) else Color(0xFF333333)
                            },
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            if (log.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = log.notes,
                    color = textColor.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
