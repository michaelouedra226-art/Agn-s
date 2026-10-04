package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.database.entity.FilmEntity
import com.example.data.database.entity.FilmStateEntity
import com.example.data.database.entity.MediaEntity
import com.example.data.database.entity.SceneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FilmDao {
    @Query("SELECT * FROM films ORDER BY createdAt DESC")
    fun getAllFilms(): Flow<List<FilmEntity>>

    @Query("SELECT * FROM films WHERE id = :id LIMIT 1")
    suspend fun getFilmById(id: String): FilmEntity?

    @Query("SELECT * FROM films WHERE status = 'processing' LIMIT 1")
    suspend fun getActiveProcessingFilm(): FilmEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFilm(film: FilmEntity)

    @Update
    suspend fun updateFilm(film: FilmEntity)

    @Query("DELETE FROM films WHERE id = :id")
    suspend fun deleteFilmById(id: String)
}

@Dao
interface SceneDao {
    @Query("SELECT * FROM scenes WHERE filmId = :filmId ORDER BY sceneIndex ASC")
    fun getScenesForFilm(filmId: String): Flow<List<SceneEntity>>

    @Query("SELECT * FROM scenes WHERE filmId = :filmId ORDER BY sceneIndex ASC")
    suspend fun getScenesForFilmSync(filmId: String): List<SceneEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScenes(scenes: List<SceneEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScene(scene: SceneEntity)

    @Update
    suspend fun updateScene(scene: SceneEntity)

    @Query("DELETE FROM scenes WHERE filmId = :filmId")
    suspend fun deleteScenesForFilm(filmId: String)
}

@Dao
interface MediaDao {
    @Query("SELECT * FROM media WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun getAllMedia(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media WHERE type = :type AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun getMediaByType(type: String): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media WHERE deletedAt IS NULL AND (title LIKE '%' || :query || '%' OR prompt LIKE '%' || :query || '%') ORDER BY createdAt DESC")
    fun searchMedia(query: String): Flow<List<MediaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: MediaEntity)

    @Query("UPDATE media SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteMedia(id: String, deletedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM media WHERE id = :id")
    suspend fun hardDeleteMedia(id: String)
}

@Dao
interface FilmStateDao {
    @Query("SELECT * FROM film_states WHERE filmId = :filmId LIMIT 1")
    suspend fun getCheckpoint(filmId: String): FilmStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCheckpoint(state: FilmStateEntity)

    @Query("DELETE FROM film_states WHERE filmId = :filmId")
    suspend fun clearCheckpoint(filmId: String)
}
