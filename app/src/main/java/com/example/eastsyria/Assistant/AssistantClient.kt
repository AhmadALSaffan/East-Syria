package com.example.eastsyria.Assistant

import com.example.eastsyria.BuildConfig
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** One turn of the conversation, as the server sees it. */
data class Turn(val role: String, val content: String)

sealed interface AssistantResult {
    data class Reply(val text: String, val landmarkIds: List<String>, val remaining: Int) : AssistantResult
    data object SignedOut : AssistantResult
    data object DailyLimit : AssistantResult
    data object NotConfigured : AssistantResult
    data object Offline : AssistantResult
    data object Failed : AssistantResult
}

/**
 * Talks to the `assistant` Supabase Edge Function. The function holds the DeepSeek key; the app
 * only proves who the user is with their Firebase ID token.
 */
object AssistantClient {

    private val endpoint = "${BuildConfig.SUPABASE_URL}/functions/v1/assistant"

    suspend fun ask(history: List<Turn>, language: String): AssistantResult = withContext(Dispatchers.IO) {
        val user = FirebaseAuth.getInstance().currentUser ?: return@withContext AssistantResult.SignedOut
        val token = try {
            Tasks.await(user.getIdToken(false)).token
        } catch (e: Exception) {
            return@withContext if (e.cause is IOException) AssistantResult.Offline else AssistantResult.SignedOut
        } ?: return@withContext AssistantResult.SignedOut

        val body = JSONObject().apply {
            put("lang", language)
            put("messages", JSONArray().apply {
                history.forEach { put(JSONObject().put("role", it.role).put("content", it.content)) }
            })
        }.toString().toByteArray()

        val conn = (URL(endpoint).openConnection() as HttpURLConnection)
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15_000
            conn.readTimeout = 60_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_KEY)
            conn.setRequestProperty("x-firebase-token", token)
            conn.outputStream.use { it.write(body) }

            when (conn.responseCode) {
                200 -> {
                    val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                    val ids = json.optJSONArray("landmarks")
                    AssistantResult.Reply(
                        text = json.optString("reply"),
                        landmarkIds = (0 until (ids?.length() ?: 0)).map { ids!!.getString(it) },
                        remaining = json.optInt("remaining", -1),
                    )
                }
                401 -> AssistantResult.SignedOut
                429 -> AssistantResult.DailyLimit
                503 -> AssistantResult.NotConfigured
                else -> AssistantResult.Failed
            }
        } catch (e: IOException) {
            AssistantResult.Offline
        } catch (e: Exception) {
            AssistantResult.Failed
        } finally {
            conn.disconnect()
        }
    }
}
