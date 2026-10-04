package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.database.dao.FilmDao
import com.example.data.database.dao.FilmStateDao
import com.example.data.database.dao.MediaDao
import com.example.data.database.dao.SceneDao
import com.example.data.database.entity.FilmEntity
import com.example.data.database.entity.FilmStateEntity
import com.example.data.database.entity.MediaEntity
import com.example.data.database.entity.SceneEntity

@Database(
    entities = [
        FilmEntity::class,
        SceneEntity::class,
        MediaEntity::class,
        FilmStateEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun filmDao(): FilmDao
    abstract fun sceneDao(): SceneDao
    abstract fun mediaDao(): MediaDao
    abstract fun filmStateDao(): FilmStateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cineia_studio_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
