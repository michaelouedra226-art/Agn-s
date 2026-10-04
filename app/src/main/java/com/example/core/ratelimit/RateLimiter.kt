package com.example.core.ratelimit

import com.example.core.log.LogManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

data class RateLimitRule(
    val serviceName: String,
    val rpm: Int,
    val rpd: Int,
    val burst: Int,
    val cooldownMs: Long
)

data class QuotaStatus(
    val serviceName: String,
    val requestsThisMinute: Int,
    val maxRpm: Int,
    val requestsToday: Int,
    val maxRpd: Int,
    val isCoolingDown: Boolean
)

object RateLimiter {
    private val mutex = Mutex()

    val GEMINI = "gemini"
    val AGNES_IMAGE = "agnes_image"
    val AGNES_VIDEO = "agnes_video"

    private val rules = mapOf(
        GEMINI to RateLimitRule(GEMINI, rpm = 10, rpd = 250, burst = 3, cooldownMs = 30_000),
        AGNES_IMAGE to RateLimitRule(AGNES_IMAGE, rpm = 4, rpd = 200, burst = 2, cooldownMs = 60_000),
        AGNES_VIDEO to RateLimitRule(AGNES_VIDEO, rpm = 1, rpd = 100, burst = 1, cooldownMs = 90_000)
    )

    private val requestTimestamps = mutableMapOf<String, MutableList<Long>>()
    private val cooldownUntil = mutableMapOf<String, Long>()

    private val _quotaState = MutableStateFlow<Map<String, QuotaStatus>>(emptyMap())
    val quotaState: StateFlow<Map<String, QuotaStatus>> = _quotaState.asStateFlow()

    init {
        updateQuotaSnapshot()
    }

    suspend fun acquire(service: String, priority: Int = 1) {
        val rule = rules[service] ?: RateLimitRule(service, rpm = 5, rpd = 100, burst = 1, cooldownMs = 30_000)

        while (true) {
            val waitTime = mutex.withLock {
                val now = System.currentTimeMillis()

                // Check active cooldown
                val coolUntil = cooldownUntil[service] ?: 0L
                if (now < coolUntil) {
                    return@withLock coolUntil - now
                }

                val timestamps = requestTimestamps.getOrPut(service) { mutableListOf() }
                // Remove timestamps older than 60s
                timestamps.removeAll { now - it > 60_000 }

                if (timestamps.size < rule.rpm) {
                    timestamps.add(now)
                    updateQuotaSnapshot()
                    return@withLock 0L
                }

                // Need to wait until the oldest request in the 60s window expires
                val oldest = timestamps.firstOrNull() ?: now
                val requiredWait = 60_000 - (now - oldest) + 500
                requiredWait.coerceAtLeast(1000L)
            }

            if (waitTime <= 0L) {
                break
            } else {
                LogManager.warn("RateLimiter", "File d'attente RPM [$service] : pause de ${waitTime / 1000}s...")
                delay(waitTime)
            }
        }
    }

    suspend fun notifyRateLimitExceeded(service: String, retryAfterSeconds: Long? = null) {
        val rule = rules[service]
        val cooldown = if (retryAfterSeconds != null && retryAfterSeconds > 0) {
            retryAfterSeconds * 1000L
        } else {
            rule?.cooldownMs ?: 45_000L
        }

        mutex.withLock {
            val until = System.currentTimeMillis() + cooldown
            cooldownUntil[service] = until
            updateQuotaSnapshot()
        }

        LogManager.warn("RateLimiter", "HTTP 429 Rate limit atteint pour $service. Cooldown de ${cooldown / 1000}s en cours.")
    }

    suspend fun smartJitterDelay(baseMs: Long) {
        val jitter = Random.nextLong(-2000, 2000)
        val finalDelay = (baseMs + jitter).coerceAtLeast(500L)
        delay(finalDelay)
    }

    private fun updateQuotaSnapshot() {
        val now = System.currentTimeMillis()
        val snapshot = mutableMapOf<String, QuotaStatus>()
        rules.forEach { (key, rule) ->
            val timestamps = requestTimestamps[key]?.filter { now - it <= 60_000 } ?: emptyList()
            val isCooling = (cooldownUntil[key] ?: 0L) > now
            snapshot[key] = QuotaStatus(
                serviceName = key,
                requestsThisMinute = timestamps.size,
                maxRpm = rule.rpm,
                requestsToday = timestamps.size, // in-memory session count
                maxRpd = rule.rpd,
                isCoolingDown = isCooling
            )
        }
        _quotaState.value = snapshot
    }
}
