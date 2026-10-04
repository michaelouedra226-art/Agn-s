package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "films")
data class FilmEntity(
    @PrimaryKey val id: String,
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
    val completedScenes: Int = 0
)

@Entity(tableName = "scenes")
data class SceneEntity(
    @PrimaryKey val id: String,
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

@Entity(tableName = "media")
data class MediaEntity(
    @PrimaryKey val id: String,
    val type: String, // image, video, film
    val path: String,
    val title: String,
    val prompt: String? = null,
    val sizeBytes: Long = 0L,
    val durationMs: Long? = null,
    val width: Int = 1920,
    val height: Int = 1080,
    val createdAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

@Entity(tableName = "film_states")
data class FilmStateEntity(
    @PrimaryKey val filmId: String,
    val phase: String, // script, images, videos, assembly
    val currentScene: Int,
    val lastUpdateMs: Long = System.currentTimeMillis(),
    val checkpointJson: String
)
