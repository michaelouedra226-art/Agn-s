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

    fun getAgnesApiKey(): String {
        val userKey = prefs.getString(KEY_AGNES, "") ?: ""
        if (userKey.isNotBlank()) return userKey
        return try {
            val buildKey = BuildConfig.AGNES_API_KEY
            if (buildKey.isNotBlank() && !buildKey.contains("MY_AGNES_API_KEY")) buildKey else ""
        } catch (e: Throwable) {
            ""
        }
    }

    fun setAgnesApiKey(key: String) {
        prefs.edit().putString(KEY_AGNES, key.trim()).apply()
    }

    fun maskKey(key: String): String {
        if (key.length <= 8) return "••••••••"
        return "${key.take(4)}••••••••${key.takeLast(4)}"
    }
}
