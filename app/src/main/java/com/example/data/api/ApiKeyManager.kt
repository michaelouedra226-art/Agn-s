package com.example.data.api

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

class ApiKeyManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("cineia_api_keys", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_GEMINI = "gemini_api_key"
        private const val KEY_AGNES = "agnes_api_key"
    }

    fun getGeminiApiKey(): String {
        val userKey = prefs.getString(KEY_GEMINI, "") ?: ""
        if (userKey.isNotBlank()) return userKey
        // Migrate an older installation if a Google AI Studio key was entered in the retired Agnes field.
        val legacyGoogleKey = prefs.getString(KEY_AGNES, "")?.trim().orEmpty()
        if (legacyGoogleKey.startsWith("AIza")) return legacyGoogleKey
        return try {
            val buildKey = BuildConfig.GEMINI_API_KEY
            if (buildKey.isNotBlank() && !buildKey.contains("MY_GEMINI_API_KEY")) buildKey else ""
        } catch (e: Throwable) {
            ""
        }
    }

    fun setGeminiApiKey(key: String) {
        prefs.edit().putString(KEY_GEMINI, key.trim()).apply()
    }

    fun maskKey(key: String): String {
        if (key.length <= 8) return "••••••••"
        return "${key.take(4)}••••••••${key.takeLast(4)}"
    }
}
