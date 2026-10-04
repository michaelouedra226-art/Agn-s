package com.example.ui.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.log.LogManager
import com.example.data.api.GoogleMediaApiClient
import com.example.data.repository.MediaRepository
import com.example.domain.model.MediaItem
import com.example.domain.model.MediaType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
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
    val durationSeconds: Double = 8.0,
    val resolution: String = "720p",
    val isGenerating: Boolean = false,
    val progressPercent: Int = 0,
    val progressMessage: String = "",
    val recentImages: List<MediaItem> = emptyList(),
    val recentVideos: List<MediaItem> = emptyList(),
    val errorMessage: String? = null
)

class VideoViewModel(
    private val mediaApiClient: GoogleMediaApiClient,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoUiState())
    val uiState: StateFlow<VideoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            mediaRepository.getMediaByType(MediaType.IMAGE).collect { images ->
                _uiState.update { it.copy(recentImages = images.take(12)) }
            }
        }
        viewModelScope.launch {
            mediaRepository.getMediaByType(MediaType.VIDEO).collect { videos ->
                _uiState.update { it.copy(recentVideos = videos.take(6)) }
            }
        }
    }

    fun setMode(mode: VideoGenerationMode) {
        _uiState.update { it.copy(mode = mode, errorMessage = null) }
    }

    fun setImageSource(path: String?) {
        _uiState.update {
            it.copy(
                selectedImageSourcePath = path,
                mode = VideoGenerationMode.IMAGE_TO_VIDEO,
                errorMessage = null
            )
        }
    }

    fun setTextPrompt(prompt: String) {
        _uiState.update { it.copy(textPrompt = prompt, errorMessage = null) }
    }

    fun setCameraMovement(movement: String) {
        _uiState.update { it.copy(cameraMovement = movement) }
    }

    fun setMovementIntensity(intensity: Float) {
        _uiState.update { it.copy(movementIntensity = intensity.coerceIn(1f, 10f)) }
    }

    fun setDuration(duration: Double) {
        val supported = listOf(4.0, 6.0, 8.0).minByOrNull { kotlin.math.abs(it - duration) } ?: 8.0
        _uiState.update {
            it.copy(
                durationSeconds = supported,
                resolution = if (supported == 8.0) it.resolution else "720p"
            )
        }
    }

    fun setResolution(res: String) {
        _uiState.update {
            it.copy(
                resolution = if (res == "1080p") "1080p" else "720p",
                durationSeconds = if (res == "1080p") 8.0 else it.durationSeconds
            )
        }
    }

    fun generateVideo(onSuccess: (MediaItem) -> Unit = {}) {
        val state = _uiState.value
        if (state.isGenerating) return
        val sourcePath = if (state.mode == VideoGenerationMode.IMAGE_TO_VIDEO) state.selectedImageSourcePath else null
        if (state.mode == VideoGenerationMode.IMAGE_TO_VIDEO && sourcePath.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Choisissez une image récente ou générez-en une dans l’onglet Image.") }
            return
        }
        if (sourcePath != null && (!File(sourcePath).isFile || File(sourcePath).length() == 0L)) {
            _uiState.update { it.copy(errorMessage = "L’image source n’existe plus. Sélectionnez une autre image.") }
            return
        }
        val rawPrompt = if (state.mode == VideoGenerationMode.IMAGE_TO_VIDEO) {
            "Animate the supplied image while preserving its main subject and visual style. " +
                "Camera movement: ${state.cameraMovement}. Motion intensity: ${state.movementIntensity.toInt()} out of 10."
        } else {
            state.textPrompt.trim()
        }
        if (rawPrompt.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Veuillez entrer une description pour votre vidéo.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGenerating = true,
                    progressPercent = 4,
                    progressMessage = "Préparation de la génération Veo…",
                    errorMessage = null
                )
            }

            try {
                val videoPath = mediaApiClient.generateVideo(
                    prompt = rawPrompt,
                    imageSourcePath = sourcePath,
                    cameraMovement = state.cameraMovement,
                    durationSec = state.durationSeconds,
                    resolution = state.resolution
                ) { percent, message ->
                    _uiState.update { it.copy(progressPercent = percent, progressMessage = message) }
                }

                val file = File(videoPath)
                if (!file.isFile || file.length() < 1024) {
                    throw IllegalStateException("Le service n’a pas retourné de fichier vidéo exploitable.")
                }
                val mediaItem = MediaItem(
                    id = UUID.randomUUID().toString(),
                    type = MediaType.VIDEO,
                    path = videoPath,
                    title = if (sourcePath != null) "Animation ${state.cameraMovement}" else rawPrompt.take(30),
                    prompt = rawPrompt,
                    sizeBytes = file.length(),
                    durationMs = (state.durationSeconds * 1000).toLong()
                )

                mediaRepository.insertMedia(mediaItem)
                _uiState.update { it.copy(isGenerating = false, progressPercent = 100, progressMessage = "Vidéo prête") }
                onSuccess(mediaItem)
            } catch (e: CancellationException) {
                _uiState.update { it.copy(isGenerating = false, progressMessage = "Génération annulée.") }
                throw e
            } catch (e: Exception) {
                val message = e.localizedMessage ?: "Échec de génération vidéo."
                LogManager.error("Veo", "Erreur de génération vidéo : $message")
                _uiState.update { it.copy(isGenerating = false, errorMessage = message) }
            }
        }
    }
}
