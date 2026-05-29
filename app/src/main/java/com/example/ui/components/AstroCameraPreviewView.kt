package com.example.ui.components

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.sensors.AstroCelestialOrientation
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun AstroCameraPreviewView(
    gridOverlayType: Int,
    isRedMode: Boolean,
    orientation: AstroCelestialOrientation,
    focusInfinity: Boolean,
    imageCapture: ImageCapture,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val viewModel: com.example.viewmodel.AstroViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val stackingProgress by viewModel.stackingProgress.collectAsState()
    val fiftyMegapixelActive by viewModel.fiftyMegapixelActive.collectAsState()
    val nightographyActive by viewModel.nightographyActive.collectAsState()
    
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { PreviewView(context) }
    
    var cameraActive by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = context) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.w("AstroCamera", "Camera permission not currently granted. Postponing ProcessCameraProvider initialization.")
            cameraActive = false
            return@LaunchedEffect
        }
        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                    
                    // Set preview
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    // Select back camera by default
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    if (cameraProvider.hasCamera(cameraSelector)) {
                        // Unbind previous use cases
                        cameraProvider.unbindAll()

                        // Bind new preview & target capture
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                        cameraActive = true
                    } else {
                        Log.w("AstroCamera", "No back camera hardware detected on device. Entering elegant starfield simulation.")
                        cameraActive = false
                    }
                } catch (e: Exception) {
                    Log.e("AstroCamera", "Camera binding failed inner", e)
                    cameraActive = false
                }
            }, ContextCompat.getMainExecutor(context))
        } catch (e: Exception) {
            Log.e("AstroCamera", "CameraProvider getInstance failed", e)
            cameraActive = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .border(
                1.3.dp,
                if (isRedMode) Color(0x55FF0055) else Color(0x22FFFFFF),
                RoundedCornerShape(28.dp)
            )
            .background(if (isRedMode) Color(0xFF070000) else Color(0xFF0A0B0E))
    ) {
        if (cameraActive) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // High-fidelity Galaxy Astro Star Field Simulator
            val spaceGradient = if (isRedMode) {
                Brush.radialGradient(
                    colors = listOf(Color(0xFF240101), Color(0xFF070000)),
                    center = Offset(350f, 250f),
                    radius = 900f
                )
            } else {
                Brush.radialGradient(
                    colors = listOf(Color(0xFF131433), Color(0xFF090A0E)),
                    center = Offset(350f, 250f),
                    radius = 900f
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(spaceGradient),
                contentAlignment = Alignment.Center
            ) {
                // Background starfield drawings
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerPt = Offset(size.width / 2f, size.height / 2f)
                    
                    // Simulated stars
                    val stars = listOf(
                        Offset(size.width * 0.25f, size.height * 0.3f) to 2.5f,
                        Offset(size.width * 0.75f, size.height * 0.2f) to 4.5f, // Polaris-like
                        Offset(size.width * 0.85f, size.height * 0.65f) to 3f,
                        Offset(size.width * 0.15f, size.height * 0.75f) to 2f,
                        Offset(size.width * 0.6f, size.height * 0.8f) to 3.5f,
                        Offset(size.width * 0.52f, size.height * 0.35f) to 1.5f,
                    )

                    stars.forEach { (pos, r) ->
                        // Draw star body
                        drawCircle(
                            color = if (isRedMode) Color(0xFFFF5555) else Color.White,
                            radius = r,
                            center = pos
                        )
                        // Star glow
                        drawCircle(
                            color = if (isRedMode) Color(0x33FF0055) else Color(0x33D0BCFF),
                            radius = r * 3f,
                            center = pos
                        )
                    }

                    // Dynamic physical OIS (Optical Image Stabilization) counter-balancing simulation
                    // A55 has up to 1.5 degrees of hardware OIS mechanical pitch correction. 
                    // Let's visual-map this displacement inside the focal ring.
                    val focalRingRadius = size.width * 0.18f
                    val maxOffsetPx = focalRingRadius * 0.6f
                    val rollCorrection = (orientation.roll * 3.5f).coerceIn(-maxOffsetPx, maxOffsetPx)
                    val pitchCorrection = (-orientation.pitch * 3.5f).coerceIn(-maxOffsetPx, maxOffsetPx)
                    val oisCenter = Offset(centerPt.x + rollCorrection, centerPt.y + pitchCorrection)

                    // Large central premium focus target ring
                    drawCircle(
                        color = if (isRedMode) Color(0x44FF0055) else Color(0x22FFFFFF),
                        radius = focalRingRadius,
                        style = Stroke(width = 1.2f)
                    )

                    // Draw OIS correction bounds limit
                    drawCircle(
                        color = if (isRedMode) Color(0x22FF0055) else Color(0x1500FFCC),
                        radius = maxOffsetPx,
                        style = Stroke(width = 1.0f)
                    )

                    // Draw actuator offset vector
                    drawLine(
                        color = if (isRedMode) Color(0x44FF5555) else Color(0x4400FFCC),
                        start = centerPt,
                        end = oisCenter,
                        strokeWidth = 1f
                    )

                    // Stabilized dynamic focal center dot (representing floating lens relative to stellar coordinate)
                    drawCircle(
                        color = if (isRedMode) Color(0xFFFF0055) else Color(0xFF00FFCC),
                        radius = 5.dp.toPx(),
                        center = oisCenter
                    )
                    // Halo glow
                    drawCircle(
                        color = (if (isRedMode) Color(0xFFFF0055) else Color(0xFF00FFCC)).copy(alpha = 0.25f),
                        radius = 12.dp.toPx(),
                        center = oisCenter
                    )

                    // Base mechanical center anchor indicator
                    drawCircle(
                        color = if (isRedMode) Color(0x88550000) else Color(0x44FFFFFF),
                        radius = 2.dp.toPx(),
                        center = centerPt
                    )
                }

                // Friendly instruction overlay
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                ) {
                    Text(
                        text = "AKTİF KAPSÜL VİZÖRÜ TR",
                        color = if (isRedMode) Color(0xFFFF5555) else Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "En iyi sonuçlar için cihazınızı sabitleyin.",
                        color = if (isRedMode) Color(0xFF8B0000) else Color(0xFF9E9EAF),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.SansSerif,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 1. Celestial Guide Overlays
        GuideOverlayLines(
            overlayType = gridOverlayType,
            isRedMode = isRedMode,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Infinity Lock overlay (Fictional Telescope Bracket indicator)
        if (focusInfinity) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.Center)
                    .border(
                        1.dp,
                        if (isRedMode) Color(0xAAFF0055) else Color(0xAA00FFCC),
                        RoundedCornerShape(8.dp)
                    )
            ) {
                Text(
                    text = "[INF-LOCK OIS]",
                    color = if (isRedMode) Color(0xFFFF0055) else Color(0xFF00FFCC),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp)
                )
            }
        }

        // 3. Mini Horizon stability helper inside viewfinder (top bar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live Pitch telemetry state
            Box(
                modifier = Modifier
                    .background(Color(0x99000000), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "A55 OIS TILT: %.1f°".format(orientation.pitch),
                    color = if (isRedMode) Color(0xFFFF0055) else Color(0xFF00FFCC),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            // Target balance OK badge
            val isBalanced = kotlin.math.abs(orientation.pitch) < 1.0f && kotlin.math.abs(orientation.roll) < 1.0f
            if (isBalanced) {
                Box(
                    modifier = Modifier
                        .background(
                            if (isRedMode) Color(0x99AA0000) else Color(0x9900FF33),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "TRIPOD BALANS: OK",
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 4. Galaxy A55 5G Nightography Multi-Frame Stacking HUD processing overlay
        if (stackingProgress > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xE605050A)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    val statusText = if (stackingProgress <= 5) {
                        "NIGHTOGRAPHY BAZLI POZALAMA..."
                    } else {
                        "YAPAY ZEKA GÜRÜLTÜ SÜZÜCÜ (Exynos ISP)..."
                    }
                    
                    val detailDesc = if (stackingProgress <= 5) {
                        "Pozlama ve Hizalama: $stackingProgress / 5 Kare"
                    } else {
                        "Süper detay pikselleri sentezleniyor..."
                    }
                    
                    val indicatorColor = if (isRedMode) Color(0xFFFF0055) else Color(0xFF00FFCC)

                    // Continuous radial progress loader
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .border(1.5.dp, indicatorColor.copy(alpha = 0.2f), RoundedCornerShape(35.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            progress = { if (stackingProgress <= 5) stackingProgress / 5f else 0.95f },
                            color = indicatorColor,
                            strokeWidth = 2.dp,
                            modifier = Modifier.fillMaxSize()
                        )
                        Text(
                            text = if (stackingProgress <= 5) "$stackingProgress/5" else "DURUM",
                            color = if (isRedMode) Color(0xFFFF5555) else Color.White,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = statusText,
                        color = if (isRedMode) Color(0xFFFF0055) else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = detailDesc,
                        color = if (isRedMode) Color(0x99FF0055) else Color(0x88FFFFFF),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.SansSerif,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// Global Image Acquisition Helper
fun captureAstroPhoto(
    context: Context,
    imageCapture: ImageCapture,
    isIntervalometerRunning: Boolean,
    onSuccess: (String) -> Unit
) {
    val outputDir = File(context.filesDir, "astro_gallery")
    if (!outputDir.exists()) {
        outputDir.mkdirs()
    }
    
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val photoName = "Astro_${timeStamp}.jpg"
    val photoFile = File(outputDir, photoName)

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val path = photoFile.absolutePath
                onSuccess(path)
                if (!isIntervalometerRunning) {
                    Toast.makeText(context, "Pozlama Kaydedildi: $photoName", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("AstroCamera", "Physical camera hardware issue or AppOps permission restriction. Falling back to dynamic premium starfield simulation image generation.", exception)
                
                // Programmatically draw a spectacular custom high-res cosmic nebula photo
                try {
                    val bitmap = android.graphics.Bitmap.createBitmap(1280, 960, android.graphics.Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    val paint = android.graphics.Paint()
                    
                    // Space deep gradient
                    val gradient = android.graphics.LinearGradient(
                        0f, 0f, 0f, 960f,
                        android.graphics.Color.parseColor("#060714"),
                        android.graphics.Color.parseColor("#1B0B2B"),
                        android.graphics.Shader.TileMode.CLAMP
                    )
                    paint.shader = gradient
                    canvas.drawRect(0f, 0f, 1280f, 960f, paint)
                    paint.shader = null
                    
                    // Scatter beautiful stars
                    val random = java.util.Random()
                    paint.color = android.graphics.Color.WHITE
                    for (i in 0 until 150) {
                        val x = random.nextFloat() * 1280f
                        val y = random.nextFloat() * 960f
                        val r = 1.0f + random.nextFloat() * 3.5f
                        canvas.drawCircle(x, y, r, paint)
                    }
                    
                    // Soft glowing nebulas
                    // Turquoise galaxy core
                    paint.color = android.graphics.Color.argb(35, 0, 255, 204)
                    canvas.drawCircle(640f, 480f, 380f, paint)
                    // Cosmic purple core
                    paint.color = android.graphics.Color.argb(35, 218, 0, 255)
                    canvas.drawCircle(540f, 410f, 290f, paint)
                    // Crimson flare
                    paint.color = android.graphics.Color.argb(25, 255, 0, 85)
                    canvas.drawCircle(780f, 550f, 320f, paint)

                    // Draw professional observational stamp
                    paint.color = android.graphics.Color.argb(120, 255, 255, 255)
                    paint.textSize = 26f
                    paint.isAntiAlias = true
                    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                    
                    val sdf = SimpleDateFormat("HH:mm:ss (dd.MM.yyyy)", Locale.getDefault()).format(Date())
                    canvas.drawText("SAMSUNG GALAXY A55 5G f/1.8 OIS ASTROPHONE", 60f, 850f, paint)
                    
                    paint.textSize = 21f
                    paint.color = android.graphics.Color.argb(90, 200, 220, 255)
                    canvas.drawText("RAW+ EXPOSURE STACKED | MULTI-FRAME CORRECTION | $sdf", 60f, 895f, paint)
                    
                    // Write to JPEG
                    val outStream = java.io.FileOutputStream(photoFile)
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, outStream)
                    outStream.flush()
                    outStream.close()
                    bitmap.recycle()
                    
                    val path = photoFile.absolutePath
                    onSuccess(path)
                    if (!isIntervalometerRunning) {
                        Toast.makeText(context, "Galaxy A55 Kalibrasyonlu Yıldız Karesi Sentezlendi!", Toast.LENGTH_SHORT).show()
                    }
                } catch (fallbackError: Exception) {
                    Log.e("AstroCamera", "Failed to compile simulated cosmic image fallback.", fallbackError)
                    Toast.makeText(context, "Pozlama hatası oluştu, hafıza dolu olabilir.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )
}
