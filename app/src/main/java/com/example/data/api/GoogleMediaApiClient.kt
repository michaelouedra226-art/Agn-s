package com.example.data.api

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.core.log.LogManager
import com.example.core.ratelimit.RateLimiter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Real image/video generation through the Google Gemini API and Veo REST API. */
class GoogleMediaApiClient(
    private val context: Context,
    private val apiKeyManager: ApiKeyManager,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {
    companion object {
        private const val API_BASE = "https://generativelanguage.googleapis.com/v1beta"
        private const val IMAGE_MODEL = "gemini-3.1-flash-image"
        private const val VIDEO_MODEL = "veo-3.1-generate-preview"
        private const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"
        private const val MAX_VIDEO_WAIT_MS = 20 * 60 * 1000L
        private const val VIDEO_POLL_INTERVAL_MS = 8_000L
    }

    suspend fun generateImage(
        prompt: String,
        style: String = "Cinématique",
        aspectRatio: String = "16:9",
        quality: String = "HD",
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): String = withContext(Dispatchers.IO) {
        require(prompt.isNotBlank()) { "Décrivez l’image à générer." }
        val apiKey = requireApiKey()
        RateLimiter.acquire(RateLimiter.GEMINI_IMAGE)

        onProgress(8, "Envoi du prompt au modèle d’image Gemini…")
        LogManager.info("Gemini Image", "Génération d’image (${aspectRatio}, ${quality})")
        val detailedPrompt = "$prompt\n\nDirection artistique : $style. Composition au format $aspectRatio. " +
            "Créer une image originale détaillée, sans texte ajouté ni filigrane artificiel."
        val body = JSONObject().apply {
            put("model", IMAGE_MODEL)
            put("input", detailedPrompt)
            put("response_format", JSONObject().apply {
                put("type", "image")
                put("mime_type", "image/jpeg")
                put("aspect_ratio", aspectRatio)
                put("image_size", when (quality.lowercase()) {
                    "ultra" -> "4K"
                    "hd" -> "2K"
                    else -> "1K"
                })
            })
        }
        val request = Request.Builder()
            .url("$API_BASE/interactions")
            .header("x-goog-api-key", apiKey)
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE.toMediaType()))
            .build()

        onProgress(20, "Génération de l’image par Gemini…")
        val result = executeJson(request, "Gemini Image")
        val output = result.optJSONObject("output_image")
        val base64Data = output?.optString("data").orEmpty().ifBlank {
            findImageDataInSteps(result)
        }
        if (base64Data.isBlank()) {
            throw IOException("Gemini a répondu sans image. Vérifiez que le modèle d’image est accessible avec cette clé.")
        }

        onProgress(88, "Enregistrement de l’image…")
        val bytes = try {
            Base64.decode(base64Data, Base64.DEFAULT)
        } catch (e: IllegalArgumentException) {
            throw IOException("La réponse image de Gemini est illisible.", e)
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bytes.isEmpty() || bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException("Gemini a renvoyé des données qui ne constituent pas une image valide.")
        }

        val mimeType = output?.optString("mime_type").orEmpty()
        val extension = if (mimeType.contains("png", ignoreCase = true)) "png" else "jpg"
        val imageDir = File(context.filesDir, "images").apply { mkdirs() }
        val imageFile = File(imageDir, "gemini_${UUID.randomUUID()}.$extension")
        FileOutputStream(imageFile).use { it.write(bytes) }
        if (!imageFile.isFile || imageFile.length() == 0L) {
            imageFile.delete()
            throw IOException("Impossible d’enregistrer l’image générée.")
        }
        LogManager.success("Gemini Image", "Image générée : ${imageFile.name} (${imageFile.length() / 1024} Ko)")
        onProgress(100, "Image générée et enregistrée")
        imageFile.absolutePath
    }

    suspend fun generateVideo(
        prompt: String,
        imageSourcePath: String? = null,
        cameraMovement: String = "Zoom in",
        durationSec: Double = 8.0,
        resolution: String = "720p",
        aspectRatio: String = "16:9",
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): String = withContext(Dispatchers.IO) {
        require(prompt.isNotBlank()) { "Décrivez le mouvement ou la scène de la vidéo." }
        val apiKey = requireApiKey()
        val selectedDuration = listOf(4, 6, 8).minByOrNull { kotlin.math.abs(it - durationSec) } ?: 8
        val selectedResolution = if (resolution == "1080p" && selectedDuration != 8) "720p" else resolution
        RateLimiter.acquire(RateLimiter.GEMINI_VIDEO)

        val instance = JSONObject().put("prompt", buildString {
            append(prompt.trim())
            append(". Camera movement: ").append(cameraMovement).append('.')
        })
        val effectiveRatio = if (imageSourcePath != null) {
            val source = File(imageSourcePath)
            if (!source.isFile || source.length() == 0L) {
                throw IOException("L’image source est introuvable. Choisissez une image récente dans le studio Image.")
            }
            instance.put("image", createInlineImage(source))
            imageAspectRatio(source) ?: aspectRatio
        } else {
            aspectRatio
        }

        val parameters = JSONObject()
            .put("aspectRatio", if (effectiveRatio == "9:16") "9:16" else "16:9")
            .put("durationSeconds", selectedDuration)
            .put("resolution", if (selectedResolution == "1080p") "1080p" else "720p")
            .put("personGeneration", if (imageSourcePath != null) "allow_adult" else "allow_all")
            .put("numberOfVideos", 1)
        val requestBody = JSONObject()
            .put("instances", JSONArray().put(instance))
            .put("parameters", parameters)
        val request = Request.Builder()
            .url("$API_BASE/models/$VIDEO_MODEL:predictLongRunning")
            .header("x-goog-api-key", apiKey)
            .post(requestBody.toString().toRequestBody(JSON_MEDIA_TYPE.toMediaType()))
            .build()

        val mode = if (imageSourcePath == null) "texte vers vidéo" else "image vers vidéo"
        LogManager.info("Veo", "Démarrage de génération ($mode, ${selectedDuration}s, $selectedResolution)")
        onProgress(6, "Envoi de la demande à Veo 3.1…")
        val startedAt = System.currentTimeMillis()
        val operation = executeJson(request, "Veo 3.1")
        val operationName = operation.optString("name").trim()
        if (operationName.isBlank()) {
            throw IOException("Google n’a pas retourné d’identifiant pour la tâche vidéo.")
        }

        var latest = operation
        while (!latest.optBoolean("done", false)) {
            val elapsed = System.currentTimeMillis() - startedAt
            if (elapsed > MAX_VIDEO_WAIT_MS) {
                throw IOException("La génération vidéo dépasse 20 minutes. Réessayez plus tard depuis une connexion stable.")
            }
            val estimatedProgress = (15 + (elapsed * 70 / MAX_VIDEO_WAIT_MS)).toInt().coerceIn(15, 84)
            onProgress(estimatedProgress, "Veo rend la vidéo (la durée dépend du service)…")
            delay(VIDEO_POLL_INTERVAL_MS)
            val pollRequest = Request.Builder()
                .url("$API_BASE/${operationName.trimStart('/')}")
                .header("x-goog-api-key", apiKey)
                .get()
                .build()
            latest = executeJson(pollRequest, "Veo 3.1")
        }

        latest.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }?.let {
            throw IOException("Veo n’a pas pu générer la vidéo : $it")
        }
        val videoUri = findVideoUri(latest)
            ?: throw IOException("Veo a terminé sans fournir de fichier vidéo. Vérifiez les journaux de l’API.")

        onProgress(90, "Téléchargement du fichier MP4…")
        val videoFile = downloadVideo(videoUri, apiKey)
        LogManager.success("Veo", "Vidéo enregistrée : ${videoFile.name} (${videoFile.length() / 1024} Ko)")
        onProgress(100, "Vidéo générée et enregistrée")
        videoFile.absolutePath
    }

    suspend fun testApiKey(): String = withContext(Dispatchers.IO) {
        val apiKey = requireApiKey()
        val request = Request.Builder()
            .url("$API_BASE/models")
            .header("x-goog-api-key", apiKey)
            .get()
            .build()
        val response = executeJson(request, "Gemini")
        val count = response.optJSONArray("models")?.length() ?: 0
        "Clé reconnue par Google" + if (count > 0) " ($count modèles listés)." else "."
    }

    private fun requireApiKey(): String = apiKeyManager.getGeminiApiKey().trim().ifBlank {
        throw IOException("Ajoutez une clé Google Gemini valide dans Paramètres. Une seule clé sert aux images et aux vidéos Veo.")
    }

    private suspend fun executeJson(request: Request, service: String): JSONObject {
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                if (response.code == 429) {
                    val limiterKey = if (service == "Veo 3.1") RateLimiter.GEMINI_VIDEO else RateLimiter.GEMINI_IMAGE
                    RateLimiter.notifyRateLimitExceeded(limiterKey)
                }
                throw IOException(formatApiError(response.code, body, service))
            }
            return try {
                JSONObject(body)
            } catch (e: Exception) {
                throw IOException("Réponse JSON invalide reçue depuis $service.", e)
            }
        }
    }

    private fun formatApiError(code: Int, body: String, service: String): String {
        val message = runCatching { JSONObject(body).optJSONObject("error")?.optString("message") }.getOrNull().orEmpty()
        return when (code) {
            400 -> "Demande refusée par $service. Vérifiez les paramètres et le format du prompt.${message.takeIf { it.isNotBlank() }?.let { " Détail : $it" }.orEmpty()}"
            401, 403 -> "Accès $service refusé. Vérifiez la clé Google AI Studio, l’accès au modèle et, pour Veo, la facturation/quota.${message.takeIf { it.isNotBlank() }?.let { " Détail : $it" }.orEmpty()}"
            404 -> "$service ou le modèle demandé est indisponible pour cette clé.${message.takeIf { it.isNotBlank() }?.let { " Détail : $it" }.orEmpty()}"
            429 -> "Quota ou limite de requêtes $service atteint. Réessayez plus tard.${message.takeIf { it.isNotBlank() }?.let { " Détail : $it" }.orEmpty()}"
            else -> "$service a répondu HTTP $code.${message.takeIf { it.isNotBlank() }?.let { " Détail : $it" }.orEmpty()}"
        }
    }

    private fun findImageDataInSteps(root: JSONObject): String {
        val steps = root.optJSONArray("steps") ?: return ""
        for (i in 0 until steps.length()) {
            val content = steps.optJSONObject(i)?.optJSONArray("content") ?: continue
            for (j in 0 until content.length()) {
                val block = content.optJSONObject(j) ?: continue
                if (block.optString("type") == "image") return block.optString("data")
            }
        }
        return ""
    }

    private fun findVideoUri(operation: JSONObject): String? {
        val response = operation.optJSONObject("response") ?: return null
        val generateResponse = response.optJSONObject("generateVideoResponse")
        val sample = generateResponse?.optJSONArray("generatedSamples")?.optJSONObject(0)
            ?: response.optJSONArray("generatedVideos")?.optJSONObject(0)
        val video = sample?.optJSONObject("video") ?: sample
        return video?.optString("uri")?.takeIf { it.startsWith("https://") }
    }

    private fun createInlineImage(file: File): JSONObject {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException("Le fichier choisi n’est pas une image lisible.")
        }
        val original = file.readBytes()
        if (original.size > 18 * 1024 * 1024) {
            throw IOException("L’image source dépasse 18 Mo. Réduisez sa taille avant de l’animer.")
        }
        val mimeType = when (file.extension.lowercase()) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }
        return JSONObject().put("inlineData", JSONObject()
            .put("mimeType", mimeType)
            .put("data", Base64.encodeToString(original, Base64.NO_WRAP)))
    }

    private fun imageAspectRatio(file: File): String? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        return if (bounds.outHeight > bounds.outWidth) "9:16" else "16:9"
    }

    private fun downloadVideo(uri: String, apiKey: String): File {
        val request = Request.Builder()
            .url(uri)
            .header("x-goog-api-key", apiKey)
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val message = response.body?.string().orEmpty()
                throw IOException(formatApiError(response.code, message, "Téléchargement Veo"))
            }
            val body = response.body ?: throw IOException("Le service Veo a renvoyé une vidéo vide.")
            val videosDir = File(context.filesDir, "videos").apply { mkdirs() }
            val target = File(videosDir, "veo_${UUID.randomUUID()}.mp4")
            try {
                body.byteStream().use { input -> FileOutputStream(target).use { output -> input.copyTo(output) } }
                if (target.length() < 1024) throw IOException("Le fichier reçu est trop petit pour être une vidéo valide.")
                val header = ByteArray(16)
                val bytesRead = FileInputStream(target).use { it.read(header) }
                if (bytesRead < 8 || String(header, 4, 4, Charsets.US_ASCII) != "ftyp") {
                    throw IOException("Le téléchargement ne contient pas un MP4 valide. Réessayez.")
                }
                return target
            } catch (e: Exception) {
                target.delete()
                if (e is IOException) throw e
                throw IOException("Impossible d’enregistrer la vidéo : ${e.localizedMessage}", e)
            }
        }
    }
}
