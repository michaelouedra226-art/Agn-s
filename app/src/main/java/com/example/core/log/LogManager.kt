package com.example.core.log

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object LogManager {
    private const val MAX_LOGS = 2000

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _hasUnreadError = MutableStateFlow(false)
    val hasUnreadError: StateFlow<Boolean> = _hasUnreadError.asStateFlow()

    private val _hasRecentSuccess = MutableStateFlow(false)
    val hasRecentSuccess: StateFlow<Boolean> = _hasRecentSuccess.asStateFlow()

    fun log(level: LogLevel, source: String, message: String, details: String? = null) {
        val entry = LogEntry(
            level = level,
            source = source,
            message = message,
            details = details
        )
        _logs.update { current ->
            (listOf(entry) + current).take(MAX_LOGS)
        }
        _unreadCount.update { it + 1 }

        if (level == LogLevel.ERROR) {
            _hasUnreadError.value = true
        } else if (level == LogLevel.SUCCESS) {
            _hasRecentSuccess.value = true
        }
    }

    fun info(source: String, message: String, details: String? = null) = log(LogLevel.INFO, source, message, details)
    fun success(source: String, message: String, details: String? = null) = log(LogLevel.SUCCESS, source, message, details)
    fun warn(source: String, message: String, details: String? = null) = log(LogLevel.WARN, source, message, details)
    fun error(source: String, message: String, details: String? = null) = log(LogLevel.ERROR, source, message, details)
    fun debug(source: String, message: String, details: String? = null) = log(LogLevel.DEBUG, source, message, details)

    fun markRead() {
        _unreadCount.value = 0
        _hasUnreadError.value = false
        _hasRecentSuccess.value = false
    }

    fun clear() {
        _logs.value = emptyList()
        _unreadCount.value = 0
        _hasUnreadError.value = false
        _hasRecentSuccess.value = false
    }

    fun exportAsText(): String {
        return _logs.value.reversed().joinToString("\n") { entry ->
            "[${entry.formattedTime()}] [${entry.level}] [${entry.source}] ${entry.message} ${entry.details ?: ""}"
        }
    }
}
