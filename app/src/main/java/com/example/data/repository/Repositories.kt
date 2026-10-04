package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.database.AppDatabase
import com.example.data.database.entity.FilmEntity
import com.example.data.database.entity.FilmStateEntity
import com.example.data.database.entity.MediaEntity
import com.example.data.database.entity.SceneEntity
import com.example.domain.model.Film
import com.example.domain.model.FilmScene
import com.example.domain.model.MediaItem
import com.example.domain.model.MediaType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class FilmRepository(private val database: AppDatabase) {
    val allFilms: Flow<List<Film>> = database.filmDao().getAllFilms().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getActiveProcessingFilm(): Film? {
        return database.filmDao().getActiveProcessingFilm()?.toDomain()
    }

    suspend fun getFilmById(id: String): Film? {
        val filmEntity = database.filmDao().getFilmById(id) ?: return null
        val scenes = database.sceneDao().getScenesForFilmSync(id).map { it.toDomain() }
        return filmEntity.toDomain(scenes)
    }

    fun getScenesForFilm(filmId: String): Flow<List<FilmScene>> {
        return database.sceneDao().getScenesForFilm(filmId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun saveFilm(film: Film, scenes: List<FilmScene>) {
        database.filmDao().insertFilm(film.toEntity())
        database.sceneDao().insertScenes(scenes.map { it.toEntity() })
    }

    suspend fun updateFilm(film: Film) {
        database.filmDao().updateFilm(film.toEntity())
    }

    suspend fun updateScene(scene: FilmScene) {
        database.sceneDao().updateScene(scene.toEntity())
    }

    suspend fun deleteFilm(filmId: String) {
        database.filmDao().deleteFilmById(filmId)
        database.sceneDao().deleteScenesForFilm(filmId)
        database.filmStateDao().clearCheckpoint(filmId)
    }

    suspend fun saveCheckpoint(filmId: String, phase: String, currentScene: Int, checkpointJson: String) {
        database.filmStateDao().saveCheckpoint(
            FilmStateEntity(
                filmId = filmId,
                phase = phase,
                currentScene = currentScene,
                lastUpdateMs = System.currentTimeMillis(),
                checkpointJson = checkpointJson
            )
        )
    }

    suspend fun getCheckpoint(filmId: String): FilmStateEntity? {
        return database.filmStateDao().getCheckpoint(filmId)
    }

    suspend fun clearCheckpoint(filmId: String) {
        database.filmStateDao().clearCheckpoint(filmId)
    }

    private fun FilmEntity.toDomain(scenes: List<FilmScene> = emptyList()) = Film(
        id = id,
        title = title,
        prompt = prompt,
        language = language,
        style = style,
        duration = duration,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
        thumbnailPath = thumbnailPath,
        videoPath = videoPath,
        totalScenes = totalScenes,
        completedScenes = completedScenes,
        scenes = scenes
    )

    private fun Film.toEntity() = FilmEntity(
        id = id,
        title = title,
        prompt = prompt,
        language = language,
        style = style,
        duration = duration,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
        thumbnailPath = thumbnailPath,
        videoPath = videoPath,
        totalScenes = totalScenes,
        completedScenes = completedScenes
    )

    private fun SceneEntity.toDomain() = FilmScene(
        id = id,
        filmId = filmId,
        sceneIndex = sceneIndex,
        title = title,
        description = description,
        dialogue = dialogue,
        imagePrompt = imagePrompt,
        videoPrompt = videoPrompt,
        cameraMovement = cameraMovement,
        status = status,
        imagePath = imagePath,
        videoPath = videoPath,
        error = error,
        attempts = attempts
    )

    private fun FilmScene.toEntity() = SceneEntity(
        id = id,
        filmId = filmId,
        sceneIndex = sceneIndex,
        title = title,
        description = description,
        dialogue = dialogue,
        imagePrompt = imagePrompt,
        videoPrompt = videoPrompt,
        cameraMovement = cameraMovement,
        status = status,
        imagePath = imagePath,
        videoPath = videoPath,
        error = error,
        attempts = attempts
    )
}

class MediaRepository(private val database: AppDatabase) {
    val allMedia: Flow<List<MediaItem>> = database.mediaDao().getAllMedia().map { list ->
        list.map { it.toDomain() }
    }

    fun getMediaByType(type: MediaType): Flow<List<MediaItem>> {
        return database.mediaDao().getMediaByType(type.name.lowercase()).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun searchMedia(query: String): Flow<List<MediaItem>> {
        return database.mediaDao().searchMedia(query).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun insertMedia(media: MediaItem) {
        database.mediaDao().insertMedia(media.toEntity())
    }

    suspend fun deleteMedia(id: String) {
        database.mediaDao().softDeleteMedia(id)
    }

    private fun MediaEntity.toDomain() = MediaItem(
        id = id,
        type = when (type.lowercase()) {
            "video" -> MediaType.VIDEO
            "film" -> MediaType.FILM
            else -> MediaType.IMAGE
        },
        path = path,
        title = title,
        prompt = prompt,
        sizeBytes = sizeBytes,
        durationMs = durationMs,
        width = width,
        height = height,
        createdAt = createdAt
    )

    private fun MediaItem.toEntity() = MediaEntity(
        id = id,
        type = type.name.lowercase(),
        path = path,
        title = title,
        prompt = prompt,
        sizeBytes = sizeBytes,
        durationMs = durationMs,
        width = width,
        height = height,
        createdAt = createdAt,
        deletedAt = null
    )
}

data class UserSettings(
    val darkTheme: Boolean = true,
    val animatedBackgroundEnabled: Boolean = true,
    val particlesEnabled: Boolean = true,
    val defaultLanguage: String = "Français",
    val defaultStyle: String = "Cinématique",
    val defaultQuality: String = "HD",
    val notificationsEnabled: Boolean = true,
    val hapticEnabled: Boolean = true
)

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("cineia_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private fun loadSettings(): UserSettings {
        return UserSettings(
            darkTheme = prefs.getBoolean("dark_theme", true),
            animatedBackgroundEnabled = prefs.getBoolean("anim_bg", true),
            particlesEnabled = prefs.getBoolean("particles", true),
            defaultLanguage = prefs.getString("lang", "Français") ?: "Français",
            defaultStyle = prefs.getString("style", "Cinématique") ?: "Cinématique",
            defaultQuality = prefs.getString("quality", "HD") ?: "HD",
            notificationsEnabled = prefs.getBoolean("notifs", true),
            hapticEnabled = prefs.getBoolean("haptics", true)
        )
    }

    fun updateSettings(newSettings: UserSettings) {
        prefs.edit()
            .putBoolean("dark_theme", newSettings.darkTheme)
            .putBoolean("anim_bg", newSettings.animatedBackgroundEnabled)
            .putBoolean("particles", newSettings.particlesEnabled)
            .putString("lang", newSettings.defaultLanguage)
            .putString("style", newSettings.defaultStyle)
            .putString("quality", newSettings.defaultQuality)
            .putBoolean("notifs", newSettings.notificationsEnabled)
            .putBoolean("haptics", newSettings.hapticEnabled)
            .apply()
        _settings.value = newSettings
    }
}
