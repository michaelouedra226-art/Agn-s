package com.example.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.log.LogManager
import com.example.data.api.ApiKeyManager
import com.example.data.api.GeminiApiClient
import com.example.data.repository.SettingsRepository
import com.example.data.repository.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val geminiKeyInput: String = "",
    val isTestingGemini: Boolean = false,
    val geminiTestResult: String? = null,
    val userSettings: UserSettings = UserSettings()
)

class SettingsViewModel(
    private val apiKeyManager: ApiKeyManager,
    private val settingsRepository: SettingsRepository,
    private val geminiApiClient: GeminiApiClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            geminiKeyInput = apiKeyManager.getGeminiApiKey(),
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
        _uiState.update { it.copy(geminiKeyInput = key, geminiTestResult = null) }
        apiKeyManager.setGeminiApiKey(key)
    }

    fun testGeminiKey() {
        if (_uiState.value.isTestingGemini) return
        if (_uiState.value.geminiKeyInput.isBlank()) {
            _uiState.update { it.copy(geminiTestResult = "Entrez d’abord une clé Google AI Studio.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingGemini = true, geminiTestResult = null) }
            try {
                val result = geminiApiClient.testApiKey()
                _uiState.update { it.copy(isTestingGemini = false, geminiTestResult = "✓ $result") }
                LogManager.success("Paramètres", "Clé Google Gemini vérifiée auprès de l’API.")
            } catch (e: Exception) {
                val reason = e.localizedMessage?.takeIf { it.isNotBlank() } ?: "Connexion à Google impossible."
                _uiState.update { it.copy(isTestingGemini = false, geminiTestResult = "Échec : $reason") }
                LogManager.error("Paramètres", "Échec de vérification Gemini : $reason")
            }
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
