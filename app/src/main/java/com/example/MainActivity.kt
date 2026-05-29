package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.db.AstroLog
import com.example.ui.components.*
import com.example.viewmodel.ApiResponseState
import com.example.viewmodel.AstroPreset
import com.example.viewmodel.AstroViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AstroCamApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AstroCamApp() {
    val context = LocalContext.current
    val viewModel: AstroViewModel = viewModel()
    
    val currentTheme by viewModel.currentTheme.collectAsState()
    val isRedMode = currentTheme == com.example.viewmodel.AstroTheme.COSMIC_RED
    val orientation by viewModel.sensorOrientation.collectAsState()

    // Screen selection inside simple view Navigation
    var activeTab by remember { mutableStateOf(0) } // 0: CAMERA, 1: SAT RADAR, 2: AI ADVISOR, 3: JOURNAL

    // CameraX helper
    val imageCapture = remember { ImageCapture.Builder().build() }

    // Stellar / Cosmic Color Overrides (Dynamic Cosmic Theme mapping)
    val cosmicSlateBlack = when (currentTheme) {
        com.example.viewmodel.AstroTheme.COSMIC_RED -> Color(0xFF080101)
        com.example.viewmodel.AstroTheme.AMBER_HORIZON -> Color(0xFF0C0905)
        com.example.viewmodel.AstroTheme.SUPERNOVA_GOLD -> Color(0xFF100F0A)
        else -> Color(0xFF0A0B0E)
    }
    val cosmicCardBg = Color(currentTheme.cardBgHex)
    val primaryColor = Color(currentTheme.primaryColorHex)
    val accentColor = Color(currentTheme.accentColorHex)
    val textColor = Color(currentTheme.textColorHex)
    val textMutedColor = Color(currentTheme.textMutedHex)
    val gridBorderColor = if (isRedMode) Color(0xFF330000) else Color(0x1AFFFFFF)

    // Camera permission states
    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        cameraPermissionGranted = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Astrosever, yıldızları yakalamak için kamera izni gerekiyor!", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionGranted) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(cosmicSlateBlack),
        containerColor = cosmicSlateBlack,
        topBar = {
            Column(
                modifier = Modifier
                    .background(cosmicSlateBlack)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isRedMode) Color(0x33FF0055) else Color(0x11E2E2E6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = "Astro Mode",
                                tint = primaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "ASTROCAM",
                                color = textColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = "GALAXY A55 ASTRO",
                                color = textMutedColor,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Top Bar Badges and Mode Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(30.dp))
                                .background(if (isRedMode) Color(0xFF220101) else Color(0x1BFFFFFF))
                                .border(1.dp, if (isRedMode) Color(0x44FF0055) else Color(0x22FFFFFF), RoundedCornerShape(30.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "RAW+",
                                color = if (isRedMode) Color(0xFFFF5555) else textMutedColor,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(30.dp))
                                .background(if (isRedMode) Color(0xFF4C001B) else primaryColor)
                                .clickable { viewModel.toggleNightModeRed() }
                                .border(1.dp, if (isRedMode) Color(0xFFFF0055) else Color.Transparent, RoundedCornerShape(30.dp))
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = if (isRedMode) "RED ACTIVE" else "LIVE",
                                color = if (isRedMode) Color(0xFFFF4D4D) else Color(0xFF381E72),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Divider(
                    color = gridBorderColor,
                    modifier = Modifier.padding(top = 10.dp),
                    thickness = 1.dp
                )
            }
        },
        bottomBar = {
            // Elegant Cosmic Navigation Bar
            NavigationBar(
                containerColor = cosmicSlateBlack,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier.border(BorderStroke(1.dp, gridBorderColor), RoundedCornerShape(12.dp, 12.dp, 0.dp, 0.dp))
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = "Kamera") },
                    label = { Text("Kamera", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = textColor,
                        indicatorColor = primaryColor,
                        unselectedIconColor = textMutedColor,
                        unselectedTextColor = textMutedColor
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(imageVector = Icons.Default.CompassCalibration, contentDescription = "Teleskop Radarı") },
                    label = { Text("Konumla", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = textColor,
                        indicatorColor = primaryColor,
                        unselectedIconColor = textMutedColor,
                        unselectedTextColor = textMutedColor
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI Asistan") },
                    label = { Text("AI Cevap", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = textColor,
                        indicatorColor = primaryColor,
                        unselectedIconColor = textMutedColor,
                        unselectedTextColor = textMutedColor
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = { Icon(imageVector = Icons.Default.Book, contentDescription = "Log Defteri") },
                    label = { Text("Günce", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = textColor,
                        indicatorColor = primaryColor,
                        unselectedIconColor = textMutedColor,
                        unselectedTextColor = textMutedColor
                    )
                )
            }
        }
    ) { innerPadding ->
        
        AnimatedContent(
            targetState = activeTab,
            transitionSpec = {
                slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width } + fadeOut()
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { targetTab ->
            when (targetTab) {
                0 -> { // KAMERA TAB
                    CameraViewTab(
                        viewModel = viewModel,
                        cameraPermissionGranted = cameraPermissionGranted,
                        imageCapture = imageCapture,
                        primaryColor = primaryColor,
                        accentColor = accentColor,
                        textColor = textColor,
                        textMutedColor = textMutedColor,
                        cosmicCardBg = cosmicCardBg,
                        gridBorderColor = gridBorderColor,
                        isRedMode = isRedMode,
                        orientation = orientation,
                        onRequestPermission = { launcher.launch(Manifest.permission.CAMERA) }
                    )
                }
                1 -> { // RADAR LEVELER TAB
                    RadarViewTab(
                        orientation = orientation,
                        isRedMode = isRedMode,
                        primaryColor = primaryColor,
                        accentColor = accentColor,
                        textColor = textColor,
                        textMutedColor = textMutedColor
                    )
                }
                2 -> { // AI ADVISOR TAB
                    AiAdvisorTab(
                        viewModel = viewModel,
                        isRedMode = isRedMode,
                        primaryColor = primaryColor,
                        accentColor = accentColor,
                        textColor = textColor,
                        textMutedColor = textMutedColor,
                        cosmicCardBg = cosmicCardBg,
                        gridBorderColor = gridBorderColor
                    )
                }
                3 -> { // LOG JOURNAL TAB
                    JournalTab(
                        viewModel = viewModel,
                        isRedMode = isRedMode,
                        primaryColor = primaryColor,
                        accentColor = accentColor,
                        textColor = textColor,
                        textMutedColor = textMutedColor,
                        cosmicCardBg = cosmicCardBg,
                        gridBorderColor = gridBorderColor
                    )
                }
            }
        }
    }
}

@Composable
fun CameraViewTab(
    viewModel: AstroViewModel,
    cameraPermissionGranted: Boolean,
    imageCapture: ImageCapture,
    primaryColor: Color,
    accentColor: Color,
    textColor: Color,
    textMutedColor: Color,
    cosmicCardBg: Color,
    gridBorderColor: Color,
    isRedMode: Boolean,
    orientation: com.example.sensors.AstroCelestialOrientation,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    val preset by viewModel.selectedPreset.collectAsState()
    val iso by viewModel.currentIso.collectAsState()
    val shutter by viewModel.currentShutter.collectAsState()
    val focusInfinity by viewModel.manualFocusActive.collectAsState()
    val overlayType by viewModel.gridOverlayType.collectAsState()
    
    // Premium Galaxy A55 states
    val fiftyMpActive by viewModel.fiftyMegapixelActive.collectAsState()
    val nightographyActive by viewModel.nightographyActive.collectAsState()
    val currentTheme by viewModel.currentTheme.collectAsState()
    
    // Intervalometer state
    val intervalometerRunning by viewModel.intervalometerActive.collectAsState()
    val totalShots by viewModel.intervalCount.collectAsState()
    val shotsIndex by viewModel.intervalShotsTaken.collectAsState()
    val delayBetweenShots by viewModel.intervalDelay.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Space header panel
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "UZAY TELEFON BİLEŞENLERİ",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = primaryColor,
                letterSpacing = 1.sp
            )
            Text(
                text = "OIS HARDWARE: AKTİF",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Large Real Viewfinder (Dynamic Redesigned Camera UI)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, gridBorderColor, RoundedCornerShape(16.dp))
        ) {
            if (cameraPermissionGranted) {
                AstroCameraPreviewView(
                    gridOverlayType = overlayType,
                    isRedMode = isRedMode,
                    orientation = orientation,
                    focusInfinity = focusInfinity,
                    imageCapture = imageCapture,
                    modifier = Modifier.fillMaxSize()
                )

                // Floating top-bar indicator inside preview
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Small active theme indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = currentTheme.titleTurkish,
                            color = Color.White,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Gear button to open Settings dialog
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .border(0.5.dp, primaryColor.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Kamera Ayarları",
                            tint = primaryColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Active parameters bottom HUD overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (fiftyMpActive) "50MP RAW" else "12.5MP BINNED",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Text(
                        text = "EXYNOS ISP • ${if (nightographyActive) "STACKING" else "SINGLE"}",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(cosmicCardBg)
                        .clickable { onRequestPermission() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = "Camera Icon",
                            tint = primaryColor,
                            modifier = Modifier.size(50.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "GÖKYÜZÜNÜ GÖRMEK İÇİN İZİN GEREKİR",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Buraya dokunarak kamera yetkisini onaylayın.",
                            fontSize = 11.sp,
                            color = textMutedColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Dynamic Premium Multi-Theme & Camera hardware popup Dialog
        if (showSettingsDialog) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showSettingsDialog = false }
            ) {
                androidx.compose.material3.Card(
                    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = cosmicCardBg),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.5.dp, primaryColor.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(18.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Cosmic Settings",
                                    tint = primaryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ASTRO KONTROL PANELİ",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            IconButton(
                                onClick = { showSettingsDialog = false },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Kapat",
                                    tint = textMutedColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Title Section 1: Themes (UYGULAMA TEMALARI)
                        Text(
                            text = "UYGULAMA TEMALARI",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Selectable themes row items
                        com.example.viewmodel.AstroTheme.values().forEach { theme ->
                            val isThemeSelected = currentTheme == theme
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isThemeSelected) Color(theme.primaryColorHex).copy(alpha = 0.12f) else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (isThemeSelected) Color(theme.primaryColorHex) else Color(0x10FFFFFF),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.setTheme(theme) }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color(theme.primaryColorHex))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = theme.titleTurkish,
                                    color = Color(theme.textColorHex),
                                    fontSize = 11.sp,
                                    fontWeight = if (isThemeSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isThemeSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Seçili",
                                        tint = Color(theme.accentColorHex),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        androidx.compose.material3.Divider(
                            color = if (isRedMode) Color(0xFF2C0202) else Color(0x15FFFFFF),
                            modifier = Modifier.padding(vertical = 10.dp)
                        )

                        // Title Section 2: Sensor Modes
                        Text(
                            text = "A55 GELİŞMİŞ DONANIM MODLARI",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        
                        Spacer(modifier = Modifier.height(10.dp))

                        // Resolution Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "Sensör Çözünürlüğü",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = textColor
                                )
                                Text(
                                    text = if (fiftyMpActive) "50MP RAW (Detay, 1.0µm)" else "12.5MP Binned (Işık, 2.0µm)",
                                    fontSize = 9.sp,
                                    color = textMutedColor
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .background(Color(0xFF0F1014), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF1F222B), RoundedCornerShape(8.dp))
                                    .padding(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (!fiftyMpActive) primaryColor else Color.Transparent)
                                        .clickable { if (fiftyMpActive) viewModel.toggleFiftyMegapixel() }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "12.5MP",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!fiftyMpActive) Color.Black else textColor
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (fiftyMpActive) primaryColor else Color.Transparent)
                                        .clickable { if (!fiftyMpActive) viewModel.toggleFiftyMegapixel() }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "50MP RAW",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (fiftyMpActive) Color.Black else textColor
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Nightography switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "Nightography Stacking",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = textColor
                                )
                                Text(
                                    text = if (nightographyActive) "Gürültü giderici çoklu-kare" else "Hızlı tekil kare",
                                    fontSize = 9.sp,
                                    color = textMutedColor
                                )
                            }

                            Switch(
                                checked = nightographyActive,
                                onCheckedChange = { viewModel.toggleNightography() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = accentColor,
                                    uncheckedBorderColor = Color(0x22FFFFFF)
                                )
                            )
                        }

                        androidx.compose.material3.Divider(
                            color = if (isRedMode) Color(0xFF2C0202) else Color(0x15FFFFFF),
                            modifier = Modifier.padding(vertical = 10.dp)
                        )

                        // Title Section 3: Grids
                        Text(
                            text = "KILAVUZ ÇİZGİLER",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("YOK", "ÜÇLER", "ALTIN", "KILAVUZ").forEachIndexed { index, label ->
                                val isSelected = overlayType == index
                                Text(
                                    text = label,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) primaryColor else Color.Transparent)
                                        .border(1.dp, if (isSelected) primaryColor else Color(0x15FFFFFF), RoundedCornerShape(6.dp))
                                        .clickable { viewModel.setGridOverlay(index) }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    color = if (isSelected) Color.Black else textColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Focus infinity lock toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "Sonsuz Odak Kilidi",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = textColor
                                )
                                Text(
                                    text = "Uzay odaklamasını pürüzsüzleştirir kılavuzlar.",
                                    fontSize = 9.sp,
                                    color = textMutedColor
                                )
                            }

                            Switch(
                                checked = focusInfinity,
                                onCheckedChange = { viewModel.toggleManualFocus() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = accentColor,
                                    uncheckedBorderColor = Color(0x22FFFFFF)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showSettingsDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "AYARLARI UYGULA", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // Ayarlar Quick Trigger Card Under viewfinder
        androidx.compose.material3.Card(
            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = cosmicCardBg),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, gridBorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showSettingsDialog = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Cosmic Settings Gear",
                        tint = primaryColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "A55 DONANIM & TEMA AYARLARI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Temayı değiştir, 50MP, Nightography ve Kılavuz çizgileri yönet",
                            fontSize = 9.sp,
                            color = textMutedColor
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Aç",
                    tint = primaryColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Space Preset Chips
        Text(
            text = "ASTRO PRO REÇETELERİ",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = primaryColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(AstroPreset.values()) { p ->
                val isSelected = preset == p
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) primaryColor else cosmicCardBg)
                        .border(
                            1.dp,
                            if (isSelected) primaryColor else gridBorderColor,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { viewModel.selectPreset(p) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text(
                            text = p.titleTurkish,
                            color = if (isSelected) Color.Black else textColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ISO ${p.defaultIso} • ${p.defaultShutter}",
                            color = if (isSelected) Color.DarkGray else textMutedColor,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Hardware exposure fine adjustments (Enables in custom or shows active specs)
        Text(
            text = "MANUEL DİJİTAL ÖLÇÜMLER",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = primaryColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cosmicCardBg)
                .border(1.dp, gridBorderColor, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            // ISO Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Hassasiyet (ISO)", color = textColor, fontSize = 13.sp)
                Text(text = "$iso", color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Slider(
                value = iso.toFloat(),
                onValueChange = { viewModel.setIso(it.toInt()) },
                valueRange = 100f..3200f,
                steps = 6,
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = primaryColor,
                    inactiveTrackColor = gridBorderColor
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Shutter select speed
            val enstantaneValues = listOf("1/1000s", "1/250s", "1s", "4s", "8s", "10s", "15s")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Pozlama Süresi (Shutter)", color = textColor, fontSize = 13.sp)
                Text(text = shutter, color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                enstantaneValues.forEach { speed ->
                    val isShutterSelected = shutter == speed
                    Box(
                        modifier = Modifier
                            .background(
                                if (isShutterSelected) accentColor else Color.Transparent,
                                RoundedCornerShape(4.dp)
                            )
                            .border(1.dp, gridBorderColor, RoundedCornerShape(4.dp))
                            .clickable { viewModel.setShutter(speed) }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = speed,
                            color = if (isShutterSelected) Color.Black else textColor,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Manual focus switch (Infinity lock representation)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Sonsuz Odak Kilidi (Telescope INF)", color = textColor, fontSize = 13.sp)
                    Text(text = "Gökyüzü puslu olmaması için otomatik odağı aşar ve sonsuza kilitler.", color = textMutedColor, fontSize = 10.sp)
                }
                Switch(
                    checked = focusInfinity,
                    onCheckedChange = { viewModel.toggleManualFocus() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = accentColor,
                        uncheckedBorderColor = gridBorderColor
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Intervalometer config (Yıldız İzleri ve Zaman Aşımı için)
        Text(
            text = "YILDIZ İZİ ARALIKÖLÇER (Intervalometer)",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = primaryColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cosmicCardBg)
                .border(1.dp, gridBorderColor, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Çekilecek Kare Sayısı", color = textColor, fontSize = 13.sp)
                Text(text = "$totalShots Poz", color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Slider(
                value = totalShots.toFloat(),
                onValueChange = { viewModel.setIntervalCount(it.toInt()) },
                valueRange = 5f..60f,
                steps = 11,
                colors = SliderDefaults.colors(
                    thumbColor = primaryColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = gridBorderColor
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "İki Poz Arası Bekleme (Süre)", color = textColor, fontSize = 13.sp)
                Text(text = "$delayBetweenShots sn", color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Slider(
                value = delayBetweenShots.toFloat(),
                onValueChange = { viewModel.setIntervalDelay(it.toInt()) },
                valueRange = 1f..15f,
                steps = 14,
                colors = SliderDefaults.colors(
                    thumbColor = primaryColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = gridBorderColor
                )
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Active Interactive Shutter Central Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (intervalometerRunning) {
                // Abort action
                Button(
                    onClick = { viewModel.stopIntervalometer() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                ) {
                    Icon(imageVector = Icons.Default.Cancel, contentDescription = "Durdur", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SIRALI POZLAMAYI DURDUR ($shotsIndex / $totalShots)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                // Central Astrophotography Shutter triggers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Shutter 1: Single Shot
                    Button(
                        onClick = {
                            if (cameraPermissionGranted) {
                                viewModel.triggerStackingAnimation {
                                    captureAstroPhoto(context, imageCapture, false) { path ->
                                        viewModel.onPhotoCaptured(path)
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Kamera izni olmadan çekim yapılamaz!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Pozla", tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "TEKİL POZ", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    // Shutter 2: Start Intervalometer sequence
                    Button(
                        onClick = {
                            if (cameraPermissionGranted) {
                                viewModel.startIntervalometer {
                                    captureAstroPhoto(context, imageCapture, true) { path ->
                                        viewModel.onPhotoCaptured(path)
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Kamera izni olmadan çekim yapılamaz!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, accentColor),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Timelapse, contentDescription = "Seri", tint = accentColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "SIRALI BAŞLAT", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun RadarViewTab(
    orientation: com.example.sensors.AstroCelestialOrientation,
    isRedMode: Boolean,
    primaryColor: Color,
    accentColor: Color,
    textColor: Color,
    textMutedColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CİHAZ RADAR VE SEVİYE SENSÖRLERİ",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = primaryColor,
                letterSpacing = 1.sp
            )
            Icon(
                imageVector = Icons.Default.CompassCalibration,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Radar view includes leveled tracker bubble directly
        TacticalCompassLevelRadar(
            orientation = orientation,
            isRedMode = isRedMode,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Celestial Target Alignment Guidelines
        Text(
            text = "HIZALAMA VE KOORDİNAT REHBERİ",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = primaryColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isRedMode) Color(0xFF140101) else Color(0xFF13131F))
                .border(1.dp, if (isRedMode) Color(0xFF330000) else Color(0xFF1E1E34), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            AlignmentGuideRow("Kuzey Kutup Yıldızı (Polaris)", "Azimut: 0.0° (Tam Kuzey) | İdeal Yükselim: Enleminiz kadar", accentColor, textColor, textMutedColor)
            Spacer(modifier = Modifier.height(10.dp))
            AlignmentGuideRow("Samanyolu Merkez Core", "Azimut: 130° - 190° (Güney-Doğu/Güney) | Yaz aylarında dikey yükselir", accentColor, textColor, textMutedColor)
            Spacer(modifier = Modifier.height(10.dp))
            AlignmentGuideRow("Andromeda Galaksisi (M31)", "Azimut: 45° (Güz gökyüzünde belirginleşir) | 41° yükseklik kadranı", accentColor, textColor, textMutedColor)
            Spacer(modifier = Modifier.height(10.dp))
            AlignmentGuideRow("Orion Nebulası (M42)", "Azimut: 180° (Tam Güney kış semalarında) | Kemere kilitlenin", accentColor, textColor, textMutedColor)
        }
    }
}

@Composable
fun AlignmentGuideRow(
    targetName: String,
    tipText: String,
    accentColor: Color,
    textColor: Color,
    textMutedColor: Color
) {
    Row {
        Icon(
            imageVector = Icons.Default.LocationSearching,
            contentDescription = "Target icon",
            tint = accentColor,
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = targetName, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(text = tipText, color = textMutedColor, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun AiAdvisorTab(
    viewModel: AstroViewModel,
    isRedMode: Boolean,
    primaryColor: Color,
    accentColor: Color,
    textColor: Color,
    textMutedColor: Color,
    cosmicCardBg: Color,
    gridBorderColor: Color
) {
    val preset by viewModel.selectedPreset.collectAsState()
    val bortleScale by viewModel.bortleScale.collectAsState()
    val apiResponseState by viewModel.aiAdviceState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "YAPAY ZEKA CELESTIAL ASİSTAN",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = primaryColor,
                letterSpacing = 1.sp
            )
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Selection variables
        Text(
            text = "Yapay Zeka Analiz Parametreleri",
            fontSize = 13.sp,
            color = textColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cosmicCardBg)
                .border(1.dp, gridBorderColor, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Hedef Gökyüzü Reçetesi:", color = textColor, fontSize = 12.sp)
                Text(
                    text = preset.titleTurkish,
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Divider(color = gridBorderColor)

            Spacer(modifier = Modifier.height(12.dp))

            // Bortle Scale slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Bortle Sınıfı (Işık Kirliliği)", color = textColor, fontSize = 12.sp)
                Text(
                    text = "Sınıf $bortleScale (${getBortleClassDescription(bortleScale)})",
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Slider(
                value = bortleScale.toFloat(),
                onValueChange = { viewModel.setBortleScale(it.toInt()) },
                valueRange = 1f..9f,
                steps = 7,
                colors = SliderDefaults.colors(
                    thumbColor = primaryColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = gridBorderColor
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Get advice button
        Button(
            onClick = { viewModel.requestAstroAiAdvice() },
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Sor", tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "GALAXY A55 5G OPTİMİZASYON TAVSİYESİ AL",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Result container
        Text(
            text = "UZMAN GÖKBİLİMCİ TAVSİYESİ",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = primaryColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .background(cosmicCardBg)
                .border(2.dp, if (apiResponseState is ApiResponseState.Success) accentColor else gridBorderColor, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            when (val state = apiResponseState) {
                is ApiResponseState.Idle -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Parametreleri seçip analiz butonuna tıklayın.\nGalaxy A55 OIS donanımını koruyup en sarsıntısız yıldızları yakalamanız için gereken matematiksel hesapları yapacaktır.",
                            color = textMutedColor,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
                is ApiResponseState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = accentColor)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "AI Studio Uzayı Analiz Ediyor...",
                            color = textMutedColor,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                is ApiResponseState.Success -> {
                    Text(
                        text = state.advice,
                        color = textColor,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is ApiResponseState.Error -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = "Hata", tint = Color.Red)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = state.errorMessage, color = Color.Red, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

fun getBortleClassDescription(bortle: Int): String {
    return when (bortle) {
        1 -> "Kusursuz Karanlık Gökyüzü"
        2 -> "Çok Karanlık Doğal Gökyüzü"
        3 -> "Mavi Işık Kirliliği Gökyüzü"
        4 -> "Kırsal/Geçiş Gökyüzü"
        5 -> "Kırsal-Banliyö Gökyüzü"
        6 -> "Banliyö Parlak Gökyüzü"
        7 -> "Banliyö-Şehir Geçişi"
        8 -> "Şehir İçi Parlak Gökyüzü"
        9 -> "Şehir Merkezi Işık Denizi"
        else -> "Bilinmeyen Sınıf"
    }
}

@Composable
fun JournalTab(
    viewModel: AstroViewModel,
    isRedMode: Boolean,
    primaryColor: Color,
    accentColor: Color,
    textColor: Color,
    textMutedColor: Color,
    cosmicCardBg: Color,
    gridBorderColor: Color
) {
    val logs by viewModel.astroLogs.collectAsState()
    val galleryImages by viewModel.capturedPhotoPaths.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ASTRO SEYİR GÜNCESİ (Journal)",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = primaryColor,
                letterSpacing = 1.sp
            )
            Icon(
                imageVector = Icons.Default.Book,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Statistics bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(cosmicCardBg)
                .border(1.dp, gridBorderColor, RoundedCornerShape(12.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "TOPLAM GÖZLEM", color = textMutedColor, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Text(text = "${logs.size}", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Divider(modifier = Modifier.height(24.dp).width(1.dp), color = gridBorderColor)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "ORTALAMA PUAN", color = textMutedColor, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                val avgRating = if (logs.isNotEmpty()) logs.averageOf { it.rating.toDouble() } else 0.0
                Text(text = "%.1f ★".format(avgRating), color = accentColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Divider(modifier = Modifier.height(24.dp).width(1.dp), color = gridBorderColor)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "ÇEKİLEN DOSYA", color = textMutedColor, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Text(text = "${galleryImages.size} JPG", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Gallery strip
        if (galleryImages.isNotEmpty()) {
            Text(
                text = "BU SEANSTA ÇEKİLEN RESİMLER (Visual Registry)",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = primaryColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(galleryImages) { path ->
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.DarkGray)
                            .border(1.dp, gridBorderColor, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Quick indicator placeholder for simplicity since Coil.compose can read it, but placeholder keeps it lightweight!
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.InsertDriveFile, contentDescription = "Photo file", tint = accentColor, modifier = Modifier.size(18.dp))
                            Text(text = "Astro.jpg", color = textColor, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        Text(
            text = "KAYITLI KOORDİNAT VE POZ SEYİR DEFTERİ",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = primaryColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.MenuBook, contentDescription = "Empty", tint = gridBorderColor, modifier = Modifier.size(50.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "KAYITLI GÖZLEM BULUNMAMAKTADIR", color = textMutedColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text(text = "Kamera sayfasından pozlama aldıkça buraya kaydedilecektir.", color = textMutedColor, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(logs) { log ->
                    StargazerLogbookItem(
                        log = log,
                        isRedMode = isRedMode,
                        onDelete = { viewModel.deleteLog(log) }
                    )
                }
            }
        }
    }
}

// Simple extension helper
inline fun <T> List<T>.averageOf(selector: (T) -> Double): Double {
    if (isEmpty()) return 0.0
    var sum = 0.0
    for (element in this) {
        sum += selector(element)
    }
    return sum / size
}
