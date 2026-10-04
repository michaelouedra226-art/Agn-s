package com.example.ui.image

import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.log.LogManager
import com.example.data.api.GeminiApiClient
import com.example.data.api.GoogleMediaApiClient
import com.example.data.repository.MediaRepository
import com.example.domain.model.MediaItem
import com.example.domain.model.MediaType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class ImageUiState(
    val prompt: String = "",
    val selectedStyle: String = "Cinématique",
    val selectedRatio: String = "16:9",
    val selectedQuality: String = "HD",
    val negativePrompt: String = "",
    val seed: String = "",
    val variations: Int = 1,
    val isImprovingPrompt: Boolean = false,
    val isGenerating: Boolean = false,
    val progressPercent: Int = 0,
    val progressMessage: String = "",
    val recentImages: List<MediaItem> = emptyList(),
    val errorMessage: String? = null
)

class ImageViewModel(
    private val geminiApiClient: GeminiApiClient,
    private val mediaApiClient: GoogleMediaApiClient,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImageUiState())
    val uiState: StateFlow<ImageUiState> = _uiState.asStateFlow()

    init {
        loadRecentImages()
    }

    private fun loadRecentImages() {
        viewModelScope.launch {
            mediaRepository.getMediaByType(MediaType.IMAGE).collect { images ->
                _uiState.update { it.copy(recentImages = images.take(6)) }
            }
        }
    }

    fun onPromptChange(newPrompt: String) {
        _uiState.update { it.copy(prompt = newPrompt, errorMessage = null) }
    }

    fun onStyleSelected(style: String) {
        _uiState.update { it.copy(selectedStyle = style) }
    }

    fun onRatioSelected(ratio: String) {
        _uiState.update { it.copy(selectedRatio = ratio) }
    }

    fun onQualitySelected(quality: String) {
        _uiState.update { it.copy(selectedQuality = quality) }
    }

    fun improvePrompt() {
        val currentPrompt = _uiState.value.prompt
        if (currentPrompt.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isImprovingPrompt = true) }
            try {
                val improved = geminiApiClient.improvePrompt(currentPrompt, _uiState.value.selectedStyle)
                _uiState.update { it.copy(prompt = improved, isImprovingPrompt = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isImprovingPrompt = false) }
            }
        }
    }

    fun generateImage(onSuccess: (MediaItem) -> Unit = {}) {
        val state = _uiState.value
        val prompt = state.prompt.trim()
        if (prompt.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Veuillez entrer un prompt descriptif.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGenerating = true,
                    progressPercent = 10,
                    progressMessage = "Préparation de la requête...",
                    errorMessage = null
                )
            }

            try {
                val imagePath = mediaApiClient.generateImage(
                    prompt = prompt,
                    style = state.selectedStyle,
                    aspectRatio = state.selectedRatio,
                    quality = state.selectedQuality
                ) { percent, msg ->
                    _uiState.update { it.copy(progressPercent = percent, progressMessage = msg) }
                }

                val file = File(imagePath)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(imagePath, bounds)
                val mediaItem = MediaItem(
                    id = UUID.randomUUID().toString(),
                    type = MediaType.IMAGE,
                    path = imagePath,
                    title = prompt.take(30),
                    prompt = prompt,
                    sizeBytes = file.length(),
                    width = bounds.outWidth.coerceAtLeast(1),
                    height = bounds.outHeight.coerceAtLeast(1)
                )

                mediaRepository.insertMedia(mediaItem)
                _uiState.update { it.copy(isGenerating = false, progressPercent = 100) }
                onSuccess(mediaItem)
            } catch (e: Exception) {
                LogManager.error("Image", "Erreur lors de la génération : ${e.localizedMessage}")
                _uiState.update { it.copy(isGenerating = false, errorMessage = e.localizedMessage ?: "Échec de génération") }
            }
        }
    }
}
