package com.example.eastsyria.Assistant

import android.content.Intent
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import com.example.eastsyria.Details.LandmarkDetailActivity
import com.example.eastsyria.MainPage.Data.Landmark
import com.example.eastsyria.R
import com.example.eastsyria.Ui.AppLanguage
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch

/**
 * The AI guide: a chat about travel and heritage in Syria. Replies come from the `assistant` edge
 * function (DeepSeek, grounded in the landmark catalog); landmarks it mentions show as cards that
 * open their page.
 */
class AssistantActivity : AppCompatActivity() {

    private val messages = mutableStateListOf<ChatMessage>()
    private val thinking = mutableStateOf(false)
    private val landmarks = mutableStateMapOf<String, Landmark>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        loadCatalog()
        setContent {
            AssistantScreen(
                messages = messages,
                thinking = thinking.value,
                landmarks = landmarks,
                onSend = ::send,
                onOpenLandmark = { l ->
                    startActivity(Intent(this, LandmarkDetailActivity::class.java).putExtra("LANDMARK_ID", l.id))
                },
                onBack = { finish() },
            )
        }
    }

    /** The landmark cards are drawn from the app's own catalog (kept on disk by Firebase). */
    private fun loadCatalog() {
        FirebaseDatabase.getInstance().reference.child("landmarks").get()
            .addOnSuccessListener { snap ->
                snap.children.forEach { child ->
                    val key = child.key ?: return@forEach
                    child.getValue(Landmark::class.java)?.let { it.id = key; landmarks[key] = it }
                }
            }
    }

    private fun send(text: String) {
        if (thinking.value) return
        messages += ChatMessage(fromUser = true, text = text)
        thinking.value = true
        val history = messages.filterNot { it.isError }.map { Turn(if (it.fromUser) "user" else "assistant", it.text) }

        lifecycleScope.launch {
            val result = AssistantClient.ask(history, AppLanguage.current())
            thinking.value = false
            messages += when (result) {
                is AssistantResult.Reply -> ChatMessage(false, result.text, result.landmarkIds)
                AssistantResult.SignedOut -> error(R.string.assistant_err_signed_out)
                AssistantResult.DailyLimit -> error(R.string.assistant_err_limit)
                AssistantResult.NotConfigured -> error(R.string.assistant_err_not_configured)
                AssistantResult.Offline -> error(R.string.err_offline)
                AssistantResult.Failed -> error(R.string.assistant_err_failed)
            }
        }
    }

    private fun error(res: Int) = ChatMessage(fromUser = false, text = getString(res), isError = true)
}
