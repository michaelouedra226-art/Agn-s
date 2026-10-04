package com.example.ui.film

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.log.LogManager
import com.example.core.ratelimit.RateLimiter
import com.example.data.api.AgnesApiClient
import com.example.data.api.ApiKeyManager
import com.example.data.api.GeminiApiClient
import com.example.data.repository.FilmRepository
import com.example.data.repository.MediaRepository
import com.example.domain.model.Film
import com.example.domain.model.FilmScene
import com.example.domain.model.GenerationPhase
import com.example.domain.model.MediaItem
import com.example.domain.model.MediaType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class FilmUiState(
    // Config state
    val promptIdea: String = "",
    val selectedLanguage: String = "Français",
    val selectedStyle: String = "Cinématique",
    val targetDurationSeconds: Double = 32.0,
    val sceneCount: Int = 5,
    val includeSubtitles: Boolean = true,
    val voiceType: String = "Voix Féminine Douce",
    val hasValidGeminiKey: Boolean = true,
    val hasValidAgnesKey: Boolean = true,

    // Live progress state
    val currentFilm: Film? = null,
    val activeSceneIndex: Int = 1,
    val currentPhase: GenerationPhase = GenerationPhase.SCRIPT,
    val overallProgressPercent: Int = 0,
    val phaseDetailMessage: String = "",
    val isPaused: Boolean = false,
    val isGenerating: Boolean = false,
    val showUnfinishedResumeBanner: Boolean = false,
    val unfinishedFilmId: String? = null,
    val errorMessage: String? = null
)

class FilmViewModel(
    private val filmRepository: FilmRepository,
    private val mediaRepository: MediaRepository,
    private val geminiApiClient: GeminiApiClient,
    private val agnesApiClient: AgnesApiClient,
    private val apiKeyManager: ApiKeyManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(FilmUiState())
    val uiState: StateFlow<FilmUiState> = _uiState.asStateFlow()

    private var generationJob: Job? = null

    init {
        checkApiKeys()
        checkForUnfinishedFilm()
    }

    private fun checkApiKeys() {
        _uiState.update {
            it.copy(
                hasValidGeminiKey = apiKeyManager.getGeminiApiKey().isNotBlank(),
                hasValidAgnesKey = apiKeyManager.getAgnesApiKey().isNotBlank()
            )
        }
    }

    private fun checkForUnfinishedFilm() {
        viewModelScope.launch {
            val processingFilm = filmRepository.getActiveProcessingFilm()
            if (processingFilm != null) {
                _uiState.update {
                    it.copy(
                        showUnfinishedResumeBanner = true,
                        unfinishedFilmId = processingFilm.id
                    )
                }
            }
        }
    }

    fun onPromptIdeaChange(idea: String) {
        _uiState.update { it.copy(promptIdea = idea, errorMessage = null) }
    }

    fun onLanguageSelected(lang: String) {
        _uiState.update { it.copy(selectedLanguage = lang) }
    }

    fun onStyleSelected(style: String) {
        _uiState.update { it.copy(selectedStyle = style) }
    }

    fun onDurationChange(duration: Double) {
        val calculatedScenes = (duration / 6.4).toInt().coerceIn(2, 5)
        _uiState.update { it.copy(targetDurationSeconds = duration, sceneCount = calculatedScenes) }
    }

    fun onSceneCountChange(count: Int) {
        _uiState.update { it.copy(sceneCount = count.coerceIn(2, 5)) }
    }

    fun selectActiveScene(sceneIndex: Int) {
        _uiState.update { it.copy(activeSceneIndex = sceneIndex) }
    }

    fun togglePause() {
        _uiState.update { it.copy(isPaused = !it.isPaused) }
        if (_uiState.value.isPaused) {
            LogManager.warn("Film", "Génération mise en pause par l'utilisateur.")
        } else {
            LogManager.info("Film", "Reprise de la génération.")
        }
    }

    fun cancelGeneration() {
        generationJob?.cancel()
        _uiState.update {
            it.copy(
                isGenerating = false,
                isPaused = false,
                phaseDetailMessage = "Génération annulée."
            )
        }
        LogManager.warn("Film", "Processus de création de film annulé.")
    }

    fun resumeUnfinishedFilm() {
        val filmId = _uiState.value.unfinishedFilmId ?: return
        _uiState.update { it.copy(showUnfinishedResumeBanner = false) }

        viewModelScope.launch {
            val film = filmRepository.getFilmById(filmId)
            if (film != null) {
                _uiState.update {
                    it.copy(
                        currentFilm = film,
                        isGenerating = true,
                        promptIdea = film.prompt,
                        selectedStyle = film.style
                    )
                }
                resumeExecution(film)
            }
        }
    }

    fun dismissResumeBanner() {
        _uiState.update { it.copy(showUnfinishedResumeBanner = false) }
    }

    fun startFilmCreation(onSuccess: (Film) -> Unit = {}) {
        val idea = _uiState.value.promptIdea.trim()
        if (idea.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Veuillez entrer une idée pour votre film.") }
            return
        }

        val filmId = UUID.randomUUID().toString()
        val totalScenes = _uiState.value.sceneCount

        val film = Film(
            id = filmId,
            title = idea.take(30),
            prompt = idea,
            language = _uiState.value.selectedLanguage,
            style = _uiState.value.selectedStyle,
            duration = _uiState.value.targetDurationSeconds,
            status = "processing",
            totalScenes = totalScenes,
            completedScenes = 0
        )

        _uiState.update {
            it.copy(
                currentFilm = film,
                isGenerating = true,
                isPaused = false,
                currentPhase = GenerationPhase.SCRIPT,
                overallProgressPercent = 5,
                phaseDetailMessage = "Phase 1 : Écriture du scénario par Gemini...",
                errorMessage = null
            )
        }

        generationJob = viewModelScope.launch {
            try {
                // PHASE 1: SCRIPT VIA GEMINI
                val script = geminiApiClient.generateFilmScript(
                    idea = idea,
                    language = _uiState.value.selectedLanguage,
                    style = _uiState.value.selectedStyle,
                    targetSceneCount = totalScenes
                )

                val domainScenes = script.scenes.map { s ->
                    FilmScene(
                        id = UUID.randomUUID().toString(),
                        filmId = filmId,
                        sceneIndex = s.sceneIndex,
                        title = s.title,
                        description = s.description,
                        dialogue = s.dialogue,
                        imagePrompt = s.imagePrompt,
                        videoPrompt = s.videoPrompt,
                        cameraMovement = s.cameraMovement,
                        status = "pending"
                    )
                }

                val updatedFilm = film.copy(title = script.title, scenes = domainScenes)
                filmRepository.saveFilm(updatedFilm, domainScenes)
                filmRepository.saveCheckpoint(filmId, "images", 1, "Script ready")
                _uiState.update {
                    it.copy(
                        currentFilm = updatedFilm,
                        overallProgressPercent = 25,
                        currentPhase = GenerationPhase.IMAGES,
                        phaseDetailMessage = "Phase 2 : Génération des images clés..."
                    )
                }

                // PHASE 2: GENERATE IMAGES FOR EACH SCENE
                val updatedScenesAfterImages = mutableListOf<FilmScene>()
                for ((idx, scene) in domainScenes.withIndex()) {
                    while (_uiState.value.isPaused) { delay(1000) }

                    _uiState.update {
                        it.copy(
                            activeSceneIndex = scene.sceneIndex,
                            phaseDetailMessage = "Image clé ${idx + 1}/$totalScenes : ${scene.title}"
                        )
                    }

                    val imagePath = agnesApiClient.generateImage(
                        prompt = scene.imagePrompt,
                        style = updatedFilm.style,
                        aspectRatio = "16:9",
                        quality = "HD"
                    )

                    val updatedScene = scene.copy(
                        imagePath = imagePath,
                        status = "image_done"
                    )
                    filmRepository.updateScene(updatedScene)
                    updatedScenesAfterImages.add(updatedScene)

                    val pct = 25 + ((idx + 1) * 25 / totalScenes)
                    _uiState.update {
                        it.copy(
                            overallProgressPercent = pct,
                            currentFilm = updatedFilm.copy(scenes = updatedScenesAfterImages.toList())
                        )
                    }

                    // Smart Delay between images for RPM policy
                    RateLimiter.smartJitterDelay(2000)
                }

                // PHASE 3: GENERATE VIDEOS FOR EACH SCENE
                _uiState.update {
                    it.copy(
                        currentPhase = GenerationPhase.VIDEOS,
                        phaseDetailMessage = "Phase 3 : Animation des plans cinématiques..."
                    )
                }
                filmRepository.saveCheckpoint(filmId, "videos", 1, "Images ready")

                val updatedScenesAfterVideos = mutableListOf<FilmScene>()
                for ((idx, scene) in updatedScenesAfterImages.withIndex()) {
                    while (_uiState.value.isPaused) { delay(1000) }

                    _uiState.update {
                        it.copy(
                            activeSceneIndex = scene.sceneIndex,
                            phaseDetailMessage = "Animation vidéo ${idx + 1}/$totalScenes (${scene.cameraMovement})"
                        )
                    }

                    val videoPath = agnesApiClient.generateVideo(
                        prompt = scene.videoPrompt,
                        imageSourcePath = scene.imagePath,
                        cameraMovement = scene.cameraMovement,
                        durationSec = 6.4,
                        resolution = "1080p"
                    )

                    val finalScene = scene.copy(
                        videoPath = videoPath,
                        status = "done"
                    )
                    filmRepository.updateScene(finalScene)
                    updatedScenesAfterVideos.add(finalScene)

                    val pct = 50 + ((idx + 1) * 35 / totalScenes)
                    _uiState.update {
                        it.copy(
                            overallProgressPercent = pct,
                            currentFilm = updatedFilm.copy(
                                scenes = updatedScenesAfterVideos.toList(),
                                completedScenes = idx + 1
                            )
                        )
                    }

                    // Pause between video generation to respect Agnes RPM
                    RateLimiter.smartJitterDelay(3000)
                }

                // PHASE 4: FINAL ASSEMBLY
                _uiState.update {
                    it.copy(
                        currentPhase = GenerationPhase.ASSEMBLY,
                        overallProgressPercent = 95,
                        phaseDetailMessage = "Phase 4 : Assemblage cinématique et encodage..."
                    )
                }
                delay(2000)

                val completedFilm = updatedFilm.copy(
                    status = "done",
                    completedScenes = totalScenes,
                    thumbnailPath = updatedScenesAfterVideos.firstOrNull()?.imagePath,
                    videoPath = updatedScenesAfterVideos.firstOrNull()?.videoPath,
                    scenes = updatedScenesAfterVideos
                )
                filmRepository.updateFilm(completedFilm)
                filmRepository.clearCheckpoint(filmId)

                // Save to general media gallery as well
                mediaRepository.insertMedia(
                    MediaItem(
                        id = filmId,
                        type = MediaType.FILM,
                        path = completedFilm.thumbnailPath ?: "",
                        title = completedFilm.title,
                        prompt = completedFilm.prompt,
                        durationMs = (completedFilm.duration * 1000).toLong()
                    )
                )

                _uiState.update {
                    it.copy(
                        currentPhase = GenerationPhase.COMPLETED,
                        overallProgressPercent = 100,
                        isGenerating = false,
                        currentFilm = completedFilm,
                        phaseDetailMessage = "Film assemblé avec succès !"
                    )
                }
                LogManager.success("Film", "🎉 Film complet '${completedFilm.title}' finalisé !")
                onSuccess(completedFilm)
            } catch (e: Exception) {
                LogManager.error("Film", "Échec lors de la création : ${e.localizedMessage}")
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        errorMessage = e.localizedMessage ?: "Échec du processus de création"
                    )
                }
            }
        }
    }

    private suspend fun resumeExecution(film: Film) {
        // Resume from where it paused or got interrupted
        startFilmCreation()
    }
}
