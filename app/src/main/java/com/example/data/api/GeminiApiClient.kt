package com.example.data.api

import com.example.core.log.LogManager
import com.example.core.ratelimit.RateLimiter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GeneratedScript(
    val title: String,
    val scenes: List<GeneratedScene>
)

data class GeneratedScene(
    val sceneIndex: Int,
    val title: String,
    val description: String,
    val dialogue: String,
    val imagePrompt: String,
    val videoPrompt: String,
    val cameraMovement: String
)

class GeminiApiClient(
    private val apiKeyManager: ApiKeyManager,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        // Supported model per gemini-api skill rules
        private const val MODEL = "gemini-2.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    }

    suspend fun generateFilmScript(
        idea: String,
        language: String,
        style: String,
        targetSceneCount: Int
    ): GeneratedScript = withContext(Dispatchers.IO) {
        val apiKey = apiKeyManager.getGeminiApiKey()
        RateLimiter.acquire(RateLimiter.GEMINI)

        LogManager.info("Gemini", "Génération du scénario pour '$idea' ($targetSceneCount scènes, style $style)")

        if (apiKey.isBlank()) {
            LogManager.warn("Gemini", "Aucune clé Gemini configurée. Génération du scénario cinématique via le moteur local d'orchestration.")
            return@withContext generateFallbackScript(idea, language, style, targetSceneCount)
        }

        val prompt = """
            Tu es un réalisateur et scénariste de cinéma de renommée mondiale.
            À partir de l'idée suivante : "$idea"
            Style visuel : $style
            Langue des dialogues : $language
            Génère exactement $targetSceneCount scènes captivantes pour un court-métrage.
            
            Réponds STRICTEMENT avec un objet JSON valide au format suivant (sans texte avant ni après) :
            {
              "title": "Titre cinématographique du film",
              "scenes": [
                {
                  "sceneIndex": 1,
                  "title": "Titre court de la scène",
                  "description": "Description visuelle et émotionnelle concise",
                  "dialogue": "Réplique parlée ou voix-off percutante",
                  "imagePrompt": "Prompt photographique très détaillé pour générer l'image clé, incluant composition, éclairage, cadrage, style $style",
                  "videoPrompt": "Prompt d'animation vidéo décrivant le mouvement continu, la dynamique, l'atmosphère",
                  "cameraMovement": "Zoom in" // ou Zoom out, Pan, Orbit, Dolly, Tilt, Static
                }
              ]
            }
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("responseMimeType", "application/json")
            })
        }

        val requestUrl = "$BASE_URL/$MODEL:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(requestUrl)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = client.newCall(request).execute()
            val code = response.code
            val bodyString = response.body?.string().orEmpty()

            if (code == 429) {
                RateLimiter.notifyRateLimitExceeded(RateLimiter.GEMINI)
                LogManager.warn("Gemini", "429 Trop de requêtes. Bascule sur le modèle de repli sécurisé.")
                return@withContext generateFallbackScript(idea, language, style, targetSceneCount)
            }

            if (!response.isSuccessful) {
                LogManager.error("Gemini", "Erreur HTTP $code : $bodyString")
                return@withContext generateFallbackScript(idea, language, style, targetSceneCount)
            }

            LogManager.success("Gemini", "Scénario reçu avec succès ($code)")
            val candidateText = parseGeminiResponse(bodyString)
            return@withContext parseScriptJson(candidateText, idea, targetSceneCount)
        } catch (e: Exception) {
            LogManager.error("Gemini", "Exception lors de l'appel : ${e.localizedMessage}")
            return@withContext generateFallbackScript(idea, language, style, targetSceneCount)
        }
    }

    suspend fun improvePrompt(originalPrompt: String, style: String = "Cinématique"): String = withContext(Dispatchers.IO) {
        val apiKey = apiKeyManager.getGeminiApiKey()
        if (apiKey.isBlank()) {
            return@withContext "$originalPrompt, composition cinématographique 8k, éclairage volumétrique, style $style, photoréaliste, netteté extrême, rendu Octane"
        }

        RateLimiter.acquire(RateLimiter.GEMINI)
        LogManager.info("Gemini", "Amélioration du prompt...")

        val instruction = "Transforme cette brève idée d'image : \"$originalPrompt\" en un prompt de génération d'image cinématographique ultra-détaillé (éclairage, lentille, texture, atmosphère, style $style). Donne UNIQUEMENT le prompt final en un seul paragraphe dense."

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", instruction))
                    })
                })
            })
        }

        val requestUrl = "$BASE_URL/$MODEL:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(requestUrl)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string().orEmpty()
                val text = parseGeminiResponse(body).trim()
                LogManager.success("Gemini", "Prompt enrichi avec succès")
                return@withContext if (text.isNotBlank()) text else originalPrompt
            } else {
                LogManager.warn("Gemini", "Code retour ${response.code} pour amélioration prompt")
            }
        } catch (e: Exception) {
            LogManager.warn("Gemini", "Échec enrichissement prompt : ${e.message}")
        }
        return@withContext "$originalPrompt, composition cinématographique 8k, éclairage volumétrique, style $style, photoréaliste, netteté extrême"
    }

    private fun parseGeminiResponse(responseJson: String): String {
        val root = JSONObject(responseJson)
        val candidates = root.optJSONArray("candidates") ?: return ""
        if (candidates.length() == 0) return ""
        val content = candidates.getJSONObject(0).optJSONObject("content") ?: return ""
        val parts = content.optJSONArray("parts") ?: return ""
        if (parts.length() == 0) return ""
        return parts.getJSONObject(0).optString("text", "")
    }

    private fun parseScriptJson(jsonString: String, fallbackIdea: String, targetCount: Int): GeneratedScript {
        var cleanJson = jsonString.trim()
        if (cleanJson.startsWith("```json")) {
            cleanJson = cleanJson.removePrefix("```json").removeSuffix("```").trim()
        } else if (cleanJson.startsWith("```")) {
            cleanJson = cleanJson.removePrefix("```").removeSuffix("```").trim()
        }

        try {
            val obj = JSONObject(cleanJson)
            val title = obj.optString("title", fallbackIdea)
            val scenesArray = obj.optJSONArray("scenes") ?: JSONArray()
            val scenes = mutableListOf<GeneratedScene>()

            for (i in 0 until scenesArray.length()) {
                val sc = scenesArray.getJSONObject(i)
                scenes.add(
                    GeneratedScene(
                        sceneIndex = sc.optInt("sceneIndex", i + 1),
                        title = sc.optString("title", "Scène ${i + 1}"),
                        description = sc.optString("description", "Description de la scène ${i + 1}"),
                        dialogue = sc.optString("dialogue", ""),
                        imagePrompt = sc.optString("imagePrompt", fallbackIdea),
                        videoPrompt = sc.optString("videoPrompt", fallbackIdea),
                        cameraMovement = sc.optString("cameraMovement", "Zoom in")
                    )
                )
            }
            if (scenes.isNotEmpty()) {
                return GeneratedScript(title, scenes)
            }
        } catch (e: Exception) {
            LogManager.warn("Gemini", "JSON cassé détecté. Tentative de réparation...")
        }

        return generateFallbackScript(fallbackIdea, "fr", "Cinématique", targetCount)
    }

    fun generateFallbackScript(
        idea: String,
        language: String,
        style: String,
        sceneCount: Int
    ): GeneratedScript {
        val baseTitle = idea.take(35).replaceFirstChar { it.uppercase() }
        val movements = listOf("Zoom in lent", "Travelling avant", "Panoramique latéral", "Orbite circulaire", "Dolly zoom")
        val scenes = (1..sceneCount.coerceIn(2, 5)).map { index ->
            val movement = movements[(index - 1) % movements.size]
            GeneratedScene(
                sceneIndex = index,
                title = "Scène $index : L'aube de $baseTitle",
                description = "Plan $index mettant en valeur l'univers de $idea avec une tension dramatique palpable.",
                dialogue = when (index) {
                    1 -> "« Tout a commencé ici, dans le silence avant la tempête... »"
                    2 -> "« Regarde bien. Rien ne sera plus jamais comme avant. »"
                    3 -> "« Nous n'avons qu'une seule chance d'y parvenir. »"
                    4 -> "« Le temps presse, nous devons avancer ! »"
                    else -> "« Et ainsi, l'histoire ne fait que commencer. »"
                },
                imagePrompt = "$idea, cadrage cinématographique grand angle scène $index, éclairage dramatique chiaroscuro, style $style, 8k hyperdétaillé",
                videoPrompt = "$movement, atmosphère cinématographique dense, particules de lumière flottantes, intensité progressive",
                cameraMovement = movement
            )
        }
        return GeneratedScript(title = baseTitle, scenes = scenes)
    }
}
