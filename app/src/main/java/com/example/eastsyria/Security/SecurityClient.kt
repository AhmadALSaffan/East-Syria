package com.example.eastsyria.Security

import android.content.Context
import com.example.eastsyria.BuildConfig
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Results from the `security` edge function (authenticator-app second step). */
sealed interface SecurityResult {
    data class Status(val enabled: Boolean) : SecurityResult
    data class Setup(val secret: String, val uri: String) : SecurityResult
    data class Enabled(val recoveryCodes: List<String>) : SecurityResult
    data class Verified(val usedRecovery: Boolean, val recoveryLeft: Int) : SecurityResult
    data object Disabled : SecurityResult
    data object WrongCode : SecurityResult
    data object Locked : SecurityResult
    data object SignedOut : SecurityResult
    data object Offline : SecurityResult
    data object Failed : SecurityResult
}

object SecurityClient {

    private val endpoint = "${BuildConfig.SUPABASE_URL}/functions/v1/security"

    suspend fun status() = call("status")
    suspend fun setup() = call("setup")
    suspend fun enable(code: String) = call("enable", code)
    suspend fun verify(code: String) = call("verify", code)
    suspend fun disable(code: String) = call("disable", code)

    private suspend fun call(action: String, code: String? = null): SecurityResult = withContext(Dispatchers.IO) {
        val user = FirebaseAuth.getInstance().currentUser ?: return@withContext SecurityResult.SignedOut
        val token = try {
            Tasks.await(user.getIdToken(false)).token
        } catch (e: Exception) {
            return@withContext SecurityResult.Offline
        } ?: return@withContext SecurityResult.SignedOut

        val body = JSONObject().put("action", action).apply { if (code != null) put("code", code) }
            .toString().toByteArray()
        val conn = URL(endpoint).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_KEY)
            conn.setRequestProperty("x-firebase-token", token)
            conn.outputStream.use { it.write(body) }
            val status = conn.responseCode
            val text = (if (status in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = runCatching { JSONObject(text) }.getOrDefault(JSONObject())
            when {
                status == 401 -> SecurityResult.SignedOut
                status == 429 -> SecurityResult.Locked
                status == 400 && json.optString("error") == "wrong_code" -> SecurityResult.WrongCode
                status !in 200..299 -> SecurityResult.Failed
                action == "status" -> SecurityResult.Status(json.optBoolean("enabled"))
                action == "setup" -> SecurityResult.Setup(json.getString("secret"), json.getString("uri"))
                action == "enable" -> {
                    val arr = json.optJSONArray("recoveryCodes")
                    SecurityResult.Enabled((0 until (arr?.length() ?: 0)).map { arr!!.getString(it) })
                }
                action == "disable" -> SecurityResult.Disabled
                else -> SecurityResult.Verified(json.optBoolean("usedRecovery"), json.optInt("recoveryLeft", -1))
            }
        } catch (e: IOException) {
            SecurityResult.Offline
        } catch (e: Exception) {
            SecurityResult.Failed
        } finally {
            conn.disconnect()
        }
    }
}

/**
 * Remembers that this phone passed the second step, so the code is asked after a fresh sign-in
 * (or once a month), not on every launch. Signing out forgets it.
 */
object TwoFactorTrust {
    private const val PREFS = "user_prefs"
    private const val TRUST_MS = 30L * 24 * 60 * 60 * 1000

    fun isTrusted(context: Context, uid: String): Boolean {
        val at = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong("2fa_ok_$uid", 0L)
        return System.currentTimeMillis() - at < TRUST_MS
    }

    fun trust(context: Context, uid: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong("2fa_ok_$uid", System.currentTimeMillis()).apply()
    }

    fun forget(context: Context, uid: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove("2fa_ok_$uid").apply()
    }
}
