package com.example.ui.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.log.LogManager
import com.example.data.api.AgnesApiClient
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

enum class VideoGenerationMode {
    IMAGE_TO_VIDEO,
    TEXT_TO_VIDEO
}

data class VideoUiState(
    val mode: VideoGenerationMode = VideoGenerationMode.IMAGE_TO_VIDEO,
    val selectedImageSourcePath: String? = null,
    val textPrompt: String = "",
    val cameraMovement: String = "Zoom in",
    val movementIntensity: Float = 5f,
    val durationSeconds: Double = 6.4,
    val resolution: String = "1080p",
    val isGenerating: Boolean = false,
    val progressPercent: Int = 0,
    val progressMessage: String = "",
    val recentVideos: List<MediaItem> = emptyList(),
    val errorMessage: String? = null
)

class VideoViewModel(
    private val agnesApiClient: AgnesApiClient,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoUiState())
    val uiState: StateFlow<VideoUiState> = _uiState.asStateFlow()

    init {
        loadRecentVideos()
    }

    private fun loadRecentVideos() {
        viewModelScope.launch {
            mediaRepository.getMediaByType(MediaType.VIDEO).collect { videos ->
                _uiState.update { it.copy(recentVideos = videos.take(6)) }
            }
        }
    }

    fun setMode(mode: VideoGenerationMode) {
        _uiState.update { it.copy(mode = mode) }
    }

    fun setImageSource(path: String?) {
        _uiState.update { it.copy(selectedImageSourcePath = path, mode = VideoGenerationMode.IMAGE_TO_VIDEO) }
    }

    fun setTextPrompt(prompt: String) {
        _uiState.update { it.copy(textPrompt = prompt) }
    }

    fun setCameraMovement(movement: String) {
        _uiState.update { it.copy(cameraMovement = movement) }
    }

    fun setMovementIntensity(intensity: Float) {
        _uiState.update { it.copy(movementIntensity = intensity) }
    }

    fun setDuration(duration: Double) {
        _uiState.update { it.copy(durationSeconds = duration) }
    }

    fun setResolution(res: String) {
        _uiState.update { it.copy(resolution = res) }
    }

    fun generateVideo(onSuccess: (MediaItem) -> Unit = {}) {
        val state = _uiState.value
        val effectivePrompt = if (state.mode == VideoGenerationMode.IMAGE_TO_VIDEO) {
            "Animation caméra ${state.cameraMovement} (intensité ${state.movementIntensity.toInt()}/10)"
        } else {
            state.textPrompt.trim()
        }

        if (state.mode == VideoGenerationMode.TEXT_TO_VIDEO && effectivePrompt.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Veuillez entrer une description pour votre vidéo.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGenerating = true,
                    progressPercent = 10,
                    progressMessage = "Démarrage du pipeline temporel...",
                    errorMessage = null
                )
            }

            try {
                val videoPath = agnesApiClient.generateVideo(
                    prompt = effectivePrompt,
                    imageSourcePath = if (state.mode == VideoGenerationMode.IMAGE_TO_VIDEO) state.selectedImageSourcePath else null,
                    cameraMovement = state.cameraMovement,
                    durationSec = state.durationSeconds,
                    resolution = state.resolution
                ) { percent, msg ->
                    _uiState.update { it.copy(progressPercent = percent, progressMessage = msg) }
                }

                val file = File(videoPath)
                val mediaItem = MediaItem(
                    id = UUID.randomUUID().toString(),
                    type = MediaType.VIDEO,
                    path = videoPath,
                    title = if (state.mode == VideoGenerationMode.IMAGE_TO_VIDEO) "Animation ${state.cameraMovement}" else effectivePrompt.take(30),
                    prompt = effectivePrompt,
                    sizeBytes = file.length(),
                    durationMs = (state.durationSeconds * 1000).toLong()
                )

                mediaRepository.insertMedia(mediaItem)
                _uiState.update { it.copy(isGenerating = false, progressPercent = 100) }
                onSuccess(mediaItem)
            } catch (e: Exception) {
                LogManager.error("Video", "Erreur lors de la génération vidéo : ${e.localizedMessage}")
                _uiState.update { it.copy(isGenerating = false, errorMessage = e.localizedMessage ?: "Échec de génération") }
            }
        }
    }
}
