package com.example.viewmodel

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.api.AstroGeminiClient
import com.example.data.api.GeminiContent
import com.example.data.api.GeminiPart
import com.example.data.api.GeminiRequest
import com.example.data.db.AstroDatabase
import com.example.data.db.AstroLog
import com.example.sensors.AstroCelestialOrientation
import com.example.sensors.AstroSensorTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AstroPreset(
    val titleTurkish: String,
    val defaultIso: Int,
    val defaultShutter: String,
    val description: String
) {
    MILKY_WAY("Samanyolu Çekimi", 1600, "10s", "Gökadamızın toz koridorlarını yakalamak için yüksek hassasiyet."),
    STAR_TRAILS("Yıldız İzleri", 800, "15s", "Sıralı çekim yaparak gökyüzünün dairesel dönüşünü kaydetme."),
    MOON("Dolunay / Krater", 100, "1/250s", "Parlak ay yüzeyi için düşük ISO ve ultra hızlı enstantane hızı."),
    METEOR("Meteor Yağmuru", 1600, "8s", "Uzun süreli seri çekimlerle atmosfere giren kayan yıldızları yakalama."),
    DEEP_SKY("Derin Uzay Bulutsusu", 3200, "15s", "Takımyıldız ve sönük gaz bulutlarını ortaya çıkarmak için ekstrem mod."),
    CUSTOM("Manuel Ayarlar", 400, "2s", "Kendi astro-fotoğraf parametrelerinizi tamamen özelleştirin.")
}

sealed interface ApiResponseState {
    object Idle : ApiResponseState
    object Loading : ApiResponseState
    data class Success(val advice: String) : ApiResponseState
    data class Error(val errorMessage: String) : ApiResponseState
}

enum class AstroTheme(
    val id: Int,
    val titleTurkish: String,
    val primaryColorHex: Long,
    val accentColorHex: Long,
    val cardBgHex: Long,
    val textColorHex: Long,
    val textMutedHex: Long
) {
    DEEP_NEBULA(0, "Derin Nebula (Mavi)", 0xFFD0BCFF, 0xFF00FFCC, 0xFF12131A, 0xFFE2E2E6, 0x99E2E2E6),
    COSMIC_RED(1, "Astro Kırmızı (Night Vision)", 0xFFFF0055, 0xFFFF4D4D, 0xFF120303, 0xFFFF5555, 0xFF880000),
    AMBER_HORIZON(2, "Kehribar Ufku (Exynos)", 0xFFFFD54F, 0xFFFF9100, 0xFF14110C, 0xFFFFF8E1, 0xFFC97F00),
    SUPERNOVA_GOLD(3, "Süpernova Altın (Gold)", 0xFFE5A93B, 0xFFFFF176, 0xFF161510, 0xFFFFF9C4, 0xFFA1887F)
}

class AstroViewModel(application: Application) : AndroidViewModel(application) {

    private val db = androidx.room.Room.databaseBuilder(
        application.applicationContext,
        AstroDatabase::class.java, "astro_db"
    ).build()
    
    private val astroLogDao = db.astroLogDao()
    private val sensorTracker = AstroSensorTracker(application.applicationContext)

    // Room Database Logflow
    val astroLogs: StateFlow<List<AstroLog>> = astroLogDao.getAllLogs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Sensor state
    val sensorOrientation: StateFlow<AstroCelestialOrientation> = sensorTracker.orientation

    // UI Configuration States
    private val _currentTheme = MutableStateFlow(AstroTheme.COSMIC_RED) // Star with premium Night Vision red
    val currentTheme: StateFlow<AstroTheme> = _currentTheme.asStateFlow()

    private val _nightModeRed = MutableStateFlow(true) // Start with astronomical night-vision red!
    val nightModeRed: StateFlow<Boolean> = _nightModeRed.asStateFlow()

    private val _selectedPreset = MutableStateFlow(AstroPreset.MILKY_WAY)
    val selectedPreset: StateFlow<AstroPreset> = _selectedPreset.asStateFlow()

    private val _currentIso = MutableStateFlow(1600)
    val currentIso: StateFlow<Int> = _currentIso.asStateFlow()

    private val _currentShutter = MutableStateFlow("10s")
    val currentShutter: StateFlow<String> = _currentShutter.asStateFlow()

    private val _manualFocusActive = MutableStateFlow(true)
    val manualFocusActive: StateFlow<Boolean> = _manualFocusActive.asStateFlow()

    private val _gridOverlayType = MutableStateFlow(0) // 0: None, 1: Rule of Thirds, 2: Golden Spiral, 3: Circular Astro Horizon
    val gridOverlayType: StateFlow<Int> = _gridOverlayType.asStateFlow()

    // Light pollution state (Bortle-scale: 1 Excellent dark sky to 9 Inner-city bright light)
    private val _bortleScale = MutableStateFlow(4)
    val bortleScale: StateFlow<Int> = _bortleScale.asStateFlow()

    // Intervalometer states (for long shooting / star trails)
    private val _intervalometerActive = MutableStateFlow(false)
    val intervalometerActive: StateFlow<Boolean> = _intervalometerActive.asStateFlow()

    private val _intervalCount = MutableStateFlow(15) // shots count
    val intervalCount: StateFlow<Int> = _intervalCount.asStateFlow()

    private val _intervalDelay = MutableStateFlow(5) // seconds between shots
    val intervalDelay: StateFlow<Int> = _intervalDelay.asStateFlow()

    private val _intervalShotsTaken = MutableStateFlow(0)
    val intervalShotsTaken: StateFlow<Int> = _intervalShotsTaken.asStateFlow()

    private val _capturedPhotoPaths = MutableStateFlow<List<String>>(emptyList())
    val capturedPhotoPaths: StateFlow<List<String>> = _capturedPhotoPaths.asStateFlow()

    // Galaxy A55 Premium Camera States
    private val _fiftyMegapixelActive = MutableStateFlow(false) // false = 12.5MP Binned (pixel-folded), true = 50MP High-Res RAW
    val fiftyMegapixelActive: StateFlow<Boolean> = _fiftyMegapixelActive.asStateFlow()

    private val _nightographyActive = MutableStateFlow(true) // Multi-frame noise stacking
    val nightographyActive: StateFlow<Boolean> = _nightographyActive.asStateFlow()

    private val _stackingProgress = MutableStateFlow(0) // 0 = idle, 1..5 = current stacking frame processing
    val stackingProgress: StateFlow<Int> = _stackingProgress.asStateFlow()

    // Gemini AI Advice State
    private val _aiAdviceState = MutableStateFlow<ApiResponseState>(ApiResponseState.Idle)
    val aiAdviceState: StateFlow<ApiResponseState> = _aiAdviceState.asStateFlow()

    private var intervalJob: Job? = null

    init {
        // Automatically start the physical trackers for pitch level stability
        sensorTracker.startTracking()
        loadLocalGallery()
    }

    private fun loadLocalGallery() {
        val galleryDir = File(getApplication<Application>().filesDir, "astro_gallery")
        if (galleryDir.exists()) {
            val files = galleryDir.listFiles()?.filter { it.isFile && it.name.endsWith(".jpg") }
            files?.let { list ->
                _capturedPhotoPaths.value = list.map { it.absolutePath }.sortedDescending()
            }
        }
    }

    fun toggleNightModeRed() {
        _nightModeRed.value = !_nightModeRed.value
        if (_nightModeRed.value) {
            _currentTheme.value = AstroTheme.COSMIC_RED
        } else if (_currentTheme.value == AstroTheme.COSMIC_RED) {
            _currentTheme.value = AstroTheme.DEEP_NEBULA
        }
    }

    fun setTheme(theme: AstroTheme) {
        _currentTheme.value = theme
        _nightModeRed.value = (theme == AstroTheme.COSMIC_RED)
    }

    fun selectPreset(preset: AstroPreset) {
        _selectedPreset.value = preset
        if (preset != AstroPreset.CUSTOM) {
            _currentIso.value = preset.defaultIso
            _currentShutter.value = preset.defaultShutter
        }
    }

    fun setIso(iso: Int) {
        _currentIso.value = iso
        _selectedPreset.value = AstroPreset.CUSTOM
    }

    fun setShutter(shutter: String) {
        _currentShutter.value = shutter
        _selectedPreset.value = AstroPreset.CUSTOM
    }

    fun toggleManualFocus() {
        _manualFocusActive.value = !_manualFocusActive.value
    }

    fun toggleFiftyMegapixel() {
        _fiftyMegapixelActive.value = !_fiftyMegapixelActive.value
        // If 50MP is active, we don't do full Nightography pixel-binned HDR tracking, but we can keep it optional.
    }

    fun toggleNightography() {
        _nightographyActive.value = !_nightographyActive.value
    }

    // High fidelity stacking sequence simulator (OIS aligned)
    fun triggerStackingAnimation(onFinished: () -> Unit) {
        if (_stackingProgress.value != 0) return // already in progress

        viewModelScope.launch(Dispatchers.Main) {
            if (_nightographyActive.value) {
                // Simulate multi-frame HDR capture (5 frames)
                for (frame in 1..5) {
                    _stackingProgress.value = frame
                    delay(220) // Simulated physical exposure burst time of A55
                }
                // Simulated ISP digital alignment on Exynos 1480
                _stackingProgress.value = 6 
                delay(350)
                _stackingProgress.value = 0
            }
            onFinished()
        }
    }

    fun setGridOverlay(type: Int) {
        _gridOverlayType.value = type
    }

    fun setBortleScale(scale: Int) {
        _bortleScale.value = scale
    }

    fun setIntervalCount(count: Int) {
        _intervalCount.value = count
    }

    fun setIntervalDelay(delaySecs: Int) {
        _intervalDelay.value = delaySecs
    }

    // Capture triggered natively via CameraX trigger
    fun onPhotoCaptured(filePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            val dateStr = sdf.format(Date())
            
            val resolutionText = if (_fiftyMegapixelActive.value) "50MP Ultra-Res RAW (8160 x 6120, 1.0µm)" else "12.5MP Binned Super-Pixel (4080 x 3060, 2.0µm)"
            val processingText = if (_nightographyActive.value) "Multi-Frame Nightography Astro Stacking (5x aligned)" else "Single RAW Shoot"
            val targetName = when (_selectedPreset.value) {
                AstroPreset.MILKY_WAY -> "Samanyolu Çekirdeği"
                AstroPreset.STAR_TRAILS -> "Yıldız Döngüsü (Trail)"
                AstroPreset.MOON -> "Ay / Kraterler"
                AstroPreset.METEOR -> "Perseid / Geminid Meteor"
                AstroPreset.DEEP_SKY -> "Messier / Nebula"
                AstroPreset.CUSTOM -> "Özel Gözlem Hedefi"
            }

            val dynamicNotes = buildString {
                append("Samsung Galaxy A55 5G f/1.8 OIS Lens ile yakalandı.\n")
                append("• Çözünürlük: $resolutionText\n")
                append("• İşleme: $processingText\n")
                append("• Hedef: $targetName | Bortle Ölçeği: Sınıf ${_bortleScale.value}\n")
                append("• Manuel Odak: Sonsuz Odak Kilidi (Telescope INF) Aktif\n")
                append("• Optik Stabilizasyon: Donanımsal OIS Süspansiyon sarsıntı engelleme uygulandı.")
            }

            val log = AstroLog(
                filePath = filePath,
                date = dateStr,
                target = targetName,
                iso = _currentIso.value,
                shutterSpeed = _currentShutter.value,
                bortleScale = _bortleScale.value,
                notes = dynamicNotes,
                rating = 5
            )
            astroLogDao.insertLog(log)
            loadLocalGallery()
        }
    }

    fun deleteLog(log: AstroLog) {
        viewModelScope.launch(Dispatchers.IO) {
            astroLogDao.deleteLog(log)
            // Delete file also
            val file = File(log.filePath)
            if (file.exists()) {
                file.delete()
            }
            loadLocalGallery()
        }
    }

    // Trigger sequential capturing (Intervalometer simulate)
    fun startIntervalometer(onCaptureTrigger: () -> Unit) {
        if (_intervalometerActive.value) return
        _intervalometerActive.value = true
        _intervalShotsTaken.value = 0

        intervalJob = viewModelScope.launch(Dispatchers.Main) {
            val count = _intervalCount.value
            val delayMillis = _intervalDelay.value * 1000L

            for (i in 1..count) {
                if (!_intervalometerActive.value) break
                
                // Trigger camera hardware trigger in the view layer
                onCaptureTrigger()
                _intervalShotsTaken.value = i
                
                if (i < count) {
                    delay(delayMillis)
                }
            }
            _intervalometerActive.value = false
        }
    }

    fun stopIntervalometer() {
        _intervalometerActive.value = false
        intervalJob?.cancel()
        intervalJob = null
    }

    // Ask Gemini astronomical recommendation advisor based on device features (OIS, f/1.8 setup of A55 G)
    fun requestAstroAiAdvice() {
        val targetName = when (_selectedPreset.value) {
            AstroPreset.MILKY_WAY -> "Samanyolu Gökada Çekirdeği"
            AstroPreset.STAR_TRAILS -> "Kuzey Yıldızı Yıldız İzleri"
            AstroPreset.MOON -> "Dolunay Yakınlaştırma"
            AstroPreset.METEOR -> "Aktif Meteor Yağmuru"
            AstroPreset.DEEP_SKY -> "Orion Nebulası / Sönük Gaz Bulutları"
            AstroPreset.CUSTOM -> "Genel Yıldızlı Gece Gözlemi"
        }
        val currentBortle = _bortleScale.value
        val is50Mp = _fiftyMegapixelActive.value
        val isNightography = _nightographyActive.value

        _aiAdviceState.value = ApiResponseState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    val modeDesc = if (is50Mp) "50MP Ultra-Res RAW Modu (Detay odaklı, 1.0µm)" else "12.5MP Pixel-Binning Super-Pixel Modu (Işık hassas, 2.0µm)"
                    val stackDesc = if (isNightography) "Çoklu-Kare Astro Stacking Aktif (Gürültü azaltma)" else "Tekil Kare Çekim"
                    _aiAdviceState.value = ApiResponseState.Success(
                        "⚠️ AI Studio Secrets panelinden 'GEMINI_API_KEY' bulunamadı, çevrimdışı motor yanıt hazırladı.\n\n" +
                        "**Samsung Galaxy A55 5G Astrofotografi Analizi (f/1.8 fokal oran):**\n" +
                        "• **Hedef:** $targetName | **Gökyüzü Kalitesi:** Bortle Sınıfı $currentBortle\n" +
                        "• **Seçili Sensör Rejimi:** $modeDesc\n" +
                        "• **Çekim İşleme Motoru:** $stackDesc\n\n" +
                        "**A55 5G f/1.8 Donanımsal Öneriler:**\n" +
                        "1. **ISO/Shutter Dengesi:** Bölgenizin ışık kalitesi (Bortle $currentBortle) düşünüldüğünde, " +
                        (if (is50Mp) "50MP modunda sensör yüzeyi başına ışık gücü düşeceği için ISO'yu 1600'ün altına indirmeyin ve enstantane süresini uzatın." else "12.5MP modunda birleştirilmiş pikseller sayesinde ışık emilimi yüksektir. ISO 800 - 1600 arası temiz bir gökyüzü verir.") + "\n" +
                        "2. **Astro Hizalama:** OIS donanımı çekim yaparken parmak titremesini sönümler ancak astrofotografide saniyeler süren pozlarda mutlaka tripod gerekir.\n" +
                        "3. **Pratik Bilgi:** Okyanus mavisi veya mor gaz bulutlarını ortaya çıkarmak için RAW karesini hafifçe histogramda sola yaslayın."
                    )
                    return@launch
                }

                val systemPrompt = "Sen Samsung Galaxy A55 5G f/1.8 ana sensör kamera özelliklerine hakim, Türkçe konuşan profesyonel bir Astrofotografi yapay zeka asistanı ve meteoroloji/gökbilim uzmanısın."
                val userPrompt = """
                    Kullanıcının Samsung Galaxy A55 G telefonu var (50MP Ana Kamera f/1.8, OIS özellikli).
                    Mevcut Ayarlar:
                    - Seçili Hedef Gök Cisim: $targetName
                    - Işık Kirliliği Seviyesi: Bortle Sınıfı $currentBortle
                    - Sensör Piksel Modu: ${if (is50Mp) "50MP Ultra-Res Modu" else "12.5MP Pixel-Binning Modu (4-in-1 birleştiricili)"}
                    - İşleme Seçeneği: ${if (isNightography) "Multi-Frame Astro Stacking Gürültü Engelleme Aktif" else "Tek Kare RAW Çekim"}
                    
                    Lütfen Galaxy A55 5G donanım limitlerini ve bu detayları göz önünde bulundurarak şu detayları Türkçe açıkla:
                    1. Bu seçili mod kombinasyonuna göre en uygun ISO ve Shutter (Enstantane) süresi nedir? (Örn: 50MP vs 12.5MP farkı)
                    2. Mevcut Bortle $currentBortle ışık seviyesine göre kontrast artırma ve gürültü azaltma tavsiyesi.
                    3. Yıldızların dairesel çizgileşmesini önlemek için 500 Kuralı hesabı (f/1.8 lens için)
                    4. OIS donanımının etkisi ve çekim gecikmesi önerisi.
                    
                    Yanıtı markdown formatında, kısa gökbilimci jargonuyla profesyonelce ver.
                """.trimIndent()

                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(
                            parts = listOf(
                                GeminiPart(text = userPrompt)
                             )
                        )
                    )
                )

                val response = AstroGeminiClient.service.generateContent(apiKey, request)
                val advice = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                
                withContext(Dispatchers.Main) {
                    if (advice != null) {
                        _aiAdviceState.value = ApiResponseState.Success(advice)
                    } else {
                        _aiAdviceState.value = ApiResponseState.Error("Yapay zekadan boş yanıt döndü.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _aiAdviceState.value = ApiResponseState.Error("Ağ Hatası: ${e.message}")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sensorTracker.stopTracking()
        stopIntervalometer()
    }
}
