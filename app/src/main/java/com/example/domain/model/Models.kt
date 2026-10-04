package com.example.domain.model

enum class MediaType {
    IMAGE,
    VIDEO,
    FILM
}

enum class GenerationPhase(val label: String, val stepIndex: Int) {
    SCRIPT("Phase 1 : Scénario", 1),
    IMAGES("Phase 2 : Images clés", 2),
    VIDEOS("Phase 3 : Animation vidéo", 3),
    ASSEMBLY("Phase 4 : Assemblage", 4),
    COMPLETED("Terminé", 5)
}

data class FilmScene(
    val id: String,
    val filmId: String,
    val sceneIndex: Int,
    val title: String,
    val description: String,
    val dialogue: String,
    val imagePrompt: String,
    val videoPrompt: String,
    val cameraMovement: String = "Zoom in",
    val status: String = "pending", // pending, processing, done, failed, skipped
    val imagePath: String? = null,
    val videoPath: String? = null,
    val error: String? = null,
    val attempts: Int = 0
)

data class Film(
    val id: String,
    val title: String,
    val prompt: String,
    val language: String = "fr",
    val style: String = "Cinématique",
    val duration: Double = 32.0,
    val status: String = "draft", // draft, processing, done, failed, paused
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val thumbnailPath: String? = null,
    val videoPath: String? = null,
    val totalScenes: Int = 0,
    val completedScenes: Int = 0,
    val scenes: List<FilmScene> = emptyList()
)

data class MediaItem(
    val id: String,
    val type: MediaType,
    val path: String,
    val title: String,
    val prompt: String? = null,
    val sizeBytes: Long = 0L,
    val durationMs: Long? = null,
    val width: Int = 1920,
    val height: Int = 1080,
    val createdAt: Long = System.currentTimeMillis()
)
