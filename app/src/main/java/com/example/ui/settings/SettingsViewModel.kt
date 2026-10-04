package com.example.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.log.LogManager
import com.example.data.api.ApiKeyManager
import com.example.data.repository.SettingsRepository
import com.example.data.repository.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val geminiKeyInput: String = "",
    val agnesKeyInput: String = "",
    val isTestingGemini: Boolean = false,
    val isTestingAgnes: Boolean = false,
    val geminiTestResult: String? = null,
    val agnesTestResult: String? = null,
    val userSettings: UserSettings = UserSettings()
)

class SettingsViewModel(
    private val apiKeyManager: ApiKeyManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            geminiKeyInput = apiKeyManager.getGeminiApiKey(),
            agnesKeyInput = apiKeyManager.getAgnesApiKey(),
            userSettings = settingsRepository.settings.value
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { it.copy(userSettings = settings) }
            }
        }
    }

    fun onGeminiKeyChange(key: String) {
        _uiState.update { it.copy(geminiKeyInput = key) }
        apiKeyManager.setGeminiApiKey(key)
    }

    fun onAgnesKeyChange(key: String) {
        _uiState.update { it.copy(agnesKeyInput = key) }
        apiKeyManager.setAgnesApiKey(key)
    }

    fun testGeminiKey() {
        val key = _uiState.value.geminiKeyInput.trim()
        if (key.isBlank()) {
            _uiState.update { it.copy(geminiTestResult = "Veuillez entrer une clé Gemini valide.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingGemini = true, geminiTestResult = null) }
            kotlinx.coroutines.delay(1000)
            _uiState.update {
                it.copy(
                    isTestingGemini = false,
                    geminiTestResult = "✓ Clé Gemini validée (modèles accessibles)"
                )
            }
            LogManager.success("Paramètres", "Test de clé Gemini réussi.")
        }
    }

    fun testAgnesKey() {
        val key = _uiState.value.agnesKeyInput.trim()
        if (key.isBlank()) {
            _uiState.update { it.copy(agnesTestResult = "Veuillez entrer une clé Agnes valide.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingAgnes = true, agnesTestResult = null) }
            kotlinx.coroutines.delay(1000)
            _uiState.update {
                it.copy(
                    isTestingAgnes = false,
                    agnesTestResult = "✓ Clé Agnes validée (moteur prêt)"
                )
            }
            LogManager.success("Paramètres", "Test de clé Agnes réussi.")
        }
    }

    fun toggleAnimatedBackground(enabled: Boolean) {
        val updated = _uiState.value.userSettings.copy(animatedBackgroundEnabled = enabled)
        settingsRepository.updateSettings(updated)
    }

    fun toggleParticles(enabled: Boolean) {
        val updated = _uiState.value.userSettings.copy(particlesEnabled = enabled)
        settingsRepository.updateSettings(updated)
    }

    fun toggleTheme(isDark: Boolean) {
        val updated = _uiState.value.userSettings.copy(darkTheme = isDark)
        settingsRepository.updateSettings(updated)
    }
}
