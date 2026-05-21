package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.AnimeSite
import com.example.data.net.NetworkScanner
import com.example.data.net.ScanResult
import com.example.data.api.GeminiClient
import com.example.data.repository.AnimeSiteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScoutViewModel(
    application: Application,
    private val repository: AnimeSiteRepository
) : AndroidViewModel(application) {

    // Cache list
    val allSites: StateFlow<List<AnimeSite>> = repository.allSites
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedSite = MutableStateFlow<AnimeSite?>(null)
    val selectedSite: StateFlow<AnimeSite?> = _selectedSite.asStateFlow()

    // URL Verifier States
    private val _urlToCheck = MutableStateFlow("")
    val urlToCheck: StateFlow<String> = _urlToCheck.asStateFlow()

    private val _isAnalyzingUrl = MutableStateFlow(false)
    val isAnalyzingUrl: StateFlow<Boolean> = _isAnalyzingUrl.asStateFlow()

    private val _lastScanResult = MutableStateFlow<ScanResult?>(null)
    val lastScanResult: StateFlow<ScanResult?> = _lastScanResult.asStateFlow()

    private val _aiScanAnalysis = MutableStateFlow<String?>(null)
    val aiScanAnalysis: StateFlow<String?> = _aiScanAnalysis.asStateFlow()

    private val _isAiAnalyzing = MutableStateFlow(false)
    val isAiAnalyzing: StateFlow<Boolean> = _isAiAnalyzing.asStateFlow()

    // Add New Custom Site States
    private val _newSiteName = MutableStateFlow("")
    val newSiteName: StateFlow<String> = _newSiteName.asStateFlow()

    private val _newSiteUrl = MutableStateFlow("")
    val newSiteUrl: StateFlow<String> = _newSiteUrl.asStateFlow()

    private val _newSiteType = MutableStateFlow("unofficial") // "legal" or "unofficial"
    val newSiteType: StateFlow<String> = _newSiteType.asStateFlow()

    // AI Adviser Chat States
    private val _aiChatMessages = MutableStateFlow<List<Pair<String, Boolean>>>(
        listOf(
            "Hello! I am your Anime Scout 2026 Security Adviser. Ask me anything about current domain landscapes, safety configurations (uBlock Origin, Stremio + Torrentio grids), or check if an anime site is safe." to false
        )
    )
    val aiChatMessages: StateFlow<List<Pair<String, Boolean>>> = _aiChatMessages.asStateFlow()

    private val _chatInputText = MutableStateFlow("")
    val chatInputText: StateFlow<String> = _chatInputText.asStateFlow()

    private val _isChatSending = MutableStateFlow(false)
    val isChatSending: StateFlow<Boolean> = _isChatSending.asStateFlow()

    // Platform Intelligence States
    private val _systemStatusMessage = MutableStateFlow("All database security profiles are synced.")
    val systemStatusMessage: StateFlow<String> = _systemStatusMessage.asStateFlow()

    init {
        // Prepopulate defaults and kick-off async site validation check on database startup
        viewModelScope.launch(Dispatchers.IO) {
            repository.prepopulateDefaultSites()
            validateCachedSites()
        }
    }

    fun setSelectedSite(site: AnimeSite?) {
        _selectedSite.value = site
    }

    fun setUrlToCheck(url: String) {
        _urlToCheck.value = url
    }

    fun setNewSiteName(name: String) {
        _newSiteName.value = name
    }

    fun setNewSiteUrl(url: String) {
        _newSiteUrl.value = url
    }

    fun setNewSiteType(type: String) {
        _newSiteType.value = type
    }

    fun setChatInputText(text: String) {
        _chatInputText.value = text
    }

    // Ping & SSL scanner on saved items
    fun validateCachedSites() {
        viewModelScope.launch(Dispatchers.IO) {
            _systemStatusMessage.value = "Scouting active directory nodes..."
            allSites.value.forEach { site ->
                val result = NetworkScanner.scanUrl(site.url)
                val updatedStatus = if (result.isOnline) "online" else "down"
                val updatedScore = if (result.isOnline) {
                    if (site.type == "legal") 100 else {
                        // calculate organic score
                        val base = 85
                        val penalty = (if (!result.sslValid) 20 else 0) + (site.virusTotalFlags * 5)
                        (base - penalty).coerceIn(10, 95)
                    }
                } else {
                    15 // down site gets severe score penalty
                }

                repository.updateSite(
                    site.copy(
                        status = updatedStatus,
                        sslValid = result.sslValid,
                        avgPingMs = result.pingMs,
                        trustScore = updatedScore,
                        lastPingTime = System.currentTimeMillis()
                    )
                )
            }
            _systemStatusMessage.value = "All localized indexes updated with real-time handshake measurements."
        }
    }

    // Ping check a specific URL in UI
    fun verifyDomain(customUrl: String? = null) {
        val targetUrl = customUrl ?: _urlToCheck.value
        if (targetUrl.isBlank()) return

        viewModelScope.launch {
            _isAnalyzingUrl.value = true
            _aiScanAnalysis.value = null
            _lastScanResult.value = null

            val result = withContext(Dispatchers.IO) {
                NetworkScanner.scanUrl(targetUrl)
            }
            _lastScanResult.value = result
            _isAnalyzingUrl.value = false

            // Automatically check local records to see if we should sync
            withContext(Dispatchers.IO) {
                val existing = repository.getSiteByUrl(targetUrl)
                if (existing != null) {
                    val updatedStatus = if (result.isOnline) "online" else "down"
                    repository.updateSite(
                        existing.copy(
                            status = updatedStatus,
                            sslValid = result.sslValid,
                            avgPingMs = result.pingMs,
                            lastPingTime = System.currentTimeMillis()
                        )
                    )
                }
            }

            // Trigger AI Safety Analysis
            requestAiSecurityAudit(targetUrl, result)
        }
    }

    // Run custom AI Security intelligence query
    private fun requestAiSecurityAudit(url: String, scan: ScanResult) {
        viewModelScope.launch {
            _isAiAnalyzing.value = true
            val systemPrompt = "You are the Anime Scout Cyber Security Intelligence agent. Identify clones, phishing threats, adblock configuration guides and mirror states in 2026."
            val userPrompt = """
                Analyze this anime streaming URL: $url
                
                Real-Time Handshake Diagnostics:
                - Active State: ${if (scan.isOnline) "Online" else "Offline"}
                - HTTP Status Code: ${scan.statusCode}
                - SSL handshake: ${if (scan.sslValid) "Secured Protocol (" + scan.tlsVersion + ")" else "Unsecured/Missing SSL"}
                - Latency: ${scan.pingMs}ms
                
                Please generate a structural intelligence audit:
                1. Domain Legitimacy: Is this an official licensed vendor, a recognized safe unofficial community project, or a potential malicious copycat/clone of larger sites?
                2. Danger Level & Adblock requirements: How aggressive is the scripts/malvertising? What adblockers are needed (e.g. uBlock Origin required, custom filters, NoScript, etc.)?
                3. Safety Rating: Give a short safety classification ("Safe", "Moderate", or "Severe Hazard") with explanation.
                4. Safe Alternatives: Recommend official channels (Tubi, Pluto, Muse, Crunchyroll) or mirrors if valid.
            """.trimIndent()

            val responseText = withContext(Dispatchers.IO) {
                GeminiClient.generateContent(userPrompt, systemPrompt)
            }
            _aiScanAnalysis.value = responseText
            _isAiAnalyzing.value = false
        }
    }

    // Toggle Favorite
    fun toggleFavorite(site: AnimeSite) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateSite(
                site.copy(isFavorite = !site.isFavorite)
            )
        }
    }

    // Save added site
    fun addCustomSite() {
        val name = _newSiteName.value.trim()
        val url = _newSiteUrl.value.trim()
        val type = _newSiteType.value

        if (name.isBlank() || url.isBlank()) return

        var formatUrl = url
        if (!formatUrl.startsWith("http://") && !formatUrl.startsWith("https://")) {
            formatUrl = "https://$formatUrl"
        }

        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getSiteByUrl(formatUrl)
            if (existing == null) {
                val category = if (type == "legal") "Safe & Legal" else "Community Mirror"
                val rawScore = if (type == "legal") 100 else 70
                val newSite = AnimeSite(
                    name = name,
                    url = formatUrl,
                    type = type,
                    status = "unknown",
                    trustScore = rawScore,
                    category = category,
                    notes = "Custom scanned user domain added to workspace."
                )
                repository.insertSite(newSite)

                // Trigger a validation ping automatically
                val scanResult = NetworkScanner.scanUrl(formatUrl)
                val freshSite = repository.getSiteByUrl(formatUrl)
                if (freshSite != null) {
                    val isOnline = scanResult.isOnline
                    val securityScore = if (isOnline) {
                        if (type == "legal") 100 else {
                            val base = 85
                            val penalty = (if (!scanResult.sslValid) 20 else 0)
                            base - penalty
                        }
                    } else 20

                    repository.updateSite(
                        freshSite.copy(
                            status = if (isOnline) "online" else "down",
                            sslValid = scanResult.sslValid,
                            avgPingMs = scanResult.pingMs,
                            trustScore = securityScore,
                            lastPingTime = System.currentTimeMillis()
                        )
                    )
                }
            }

            // Clear inputs
            _newSiteName.value = ""
            _newSiteUrl.value = ""
            _newSiteType.value = "unofficial"
        }
    }

    // Delete custom site
    fun deleteSite(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSiteById(id)
            if (_selectedSite.value?.id == id) {
                _selectedSite.value = null
            }
        }
    }

    // AI Adviser chat dispatch
    fun sendChatMessage() {
        val message = _chatInputText.value.trim()
        if (message.isBlank()) return

        // Append user prompt
        val currentHistory = _aiChatMessages.value.toMutableList()
        currentHistory.add(message to true)
        _aiChatMessages.value = currentHistory
        _chatInputText.value = ""
        _isChatSending.value = true

        viewModelScope.launch {
            val systemPrompt = """
                You are the Anime Scout Security Intelligence Adviser (2026 Edition).
                Keep anime viewers safe online by giving specific security answers regarding domains, popups, and mirrors.
                Refer to:
                1. uBlock Origin (essential for popups/redirections on unofficial pages).
                2. Stremio + Torrentio (aggregates open sources into neat Netflix-like layout, very popular in 2026).
                3. Anilab (clean APK alternative for streaming on mobile).
                4. Safe official streams: Tubi, Pluto TV, Muse Asia, Ani-One, and Crunchyroll.
                5. Highlight the "whack-a-mole" domain phenomenon where piracy links redirect to malicious phishing mimics.
                Be helpful, direct, alert, and highly professional without using heavy technobabble.
            """.trimIndent()

            val modelAnswer = withContext(Dispatchers.IO) {
                GeminiClient.generateContent(message, systemPrompt)
            }

            val updatedHistory = _aiChatMessages.value.toMutableList()
            updatedHistory.add(modelAnswer to false)
            _aiChatMessages.value = updatedHistory
            _isChatSending.value = false
        }
    }

    // Simple Factory Provider
    class Factory(
        private val application: Application,
        private val repository: AnimeSiteRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ScoutViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ScoutViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
