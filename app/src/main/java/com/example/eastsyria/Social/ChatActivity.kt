package com.example.eastsyria.Social

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eastsyria.Onboarding.EsShapes
import com.example.eastsyria.Onboarding.EsType
import com.example.eastsyria.Onboarding.esColors
import com.example.eastsyria.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import java.text.DateFormat
import java.util.Date

/** A one-to-one conversation. Block from the menu; a blocked person can no longer write to you. */
class ChatActivity : AppCompatActivity() {

    private lateinit var otherUid: String
    private var otherName = ""
    private var otherPhoto = ""
    private val myUid get() = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    private var messages by mutableStateOf(listOf<Message>())
    private var blocked by mutableStateOf(false)
    private var input by mutableStateOf("")
    private var sending by mutableStateOf(false)
    private var error by mutableStateOf(false)
    private var stopMessages: () -> Unit = {}
    private var stopBlocked: () -> Unit = {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        otherUid = intent.getStringExtra(EXTRA_UID).orEmpty()
        otherName = intent.getStringExtra(EXTRA_NAME).orEmpty()
        otherPhoto = intent.getStringExtra(EXTRA_PHOTO).orEmpty()
        if (otherUid.isBlank() || myUid.isBlank()) return finish()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        setContent { Screen() }
    }

    override fun onStart() {
        super.onStart()
        val id = Social.chatId(myUid, otherUid)
        stopMessages = Social.listenMessages(id) { messages = it; Social.markRead(id) }
        stopBlocked = Social.listenBlocked(otherUid) { blocked = it }
    }

    override fun onStop() {
        stopMessages(); stopBlocked()
        super.onStop()
    }

    private fun send() {
        val text = input.trim()
        if (text.isEmpty() || sending) return
        sending = true; error = false
        Social.send(otherUid, otherName, otherPhoto, text) { ok ->
            sending = false
            if (ok) input = "" else error = true
        }
    }

    private fun confirmBlock() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.chat_block_title, otherName))
            .setMessage(R.string.chat_block_body)
            .setNegativeButton(R.string.reauth_cancel, null)
            .setPositiveButton(R.string.chat_block) { _, _ -> Social.setBlocked(otherUid, true) }
            .show()
    }

    @Composable
    private fun Screen() {
        val c = esColors()
        val list = rememberLazyListState()
        var menu by remember { mutableStateOf(false) }
        LaunchedEffect(messages.size) { if (messages.isNotEmpty()) list.animateScrollToItem(messages.size - 1) }

        Column(
            Modifier
                .fillMaxSize()
                .background(c.canvas)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding(),
        ) {
            // ------------------------------------------------------------ header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp).clip(CircleShape).border(1.dp, c.line, CircleShape)
                        .clickable(role = Role.Button) { finish() }
                        .semantics { contentDescription = "back" },
                ) { Icon(painterResource(R.drawable.ic_back), null, tint = c.ice, modifier = Modifier.size(22.dp)) }
                Spacer(Modifier.width(12.dp))
                Avatar(otherPhoto, otherName, 40.dp)
                Spacer(Modifier.width(12.dp))
                Text(otherName.ifBlank { "—" }, style = EsType.title.copy(fontSize = 17.sp), color = c.ice, maxLines = 1, modifier = Modifier.weight(1f))
                Box {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button) { menu = true }
                            .semantics { contentDescription = getString(R.string.chat_more) },
                    ) { Icon(painterResource(R.drawable.ic_more_vert), null, tint = c.ice, modifier = Modifier.size(22.dp)) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(if (blocked) R.string.chat_unblock else R.string.chat_block)) },
                            onClick = { menu = false; if (blocked) Social.setBlocked(otherUid, false) else confirmBlock() },
                        )
                    }
                }
            }
            Box(Modifier.fillMaxWidth().padding(0.dp).heightIn(min = 1.dp).background(c.line))

            // ------------------------------------------------------------ messages
            LazyColumn(
                state = list,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                if (messages.isEmpty()) item {
                    Text(
                        stringResource(R.string.chat_empty, otherName),
                        style = EsType.body.copy(fontSize = 15.sp), color = c.iceMute, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp, start = 24.dp, end = 24.dp),
                    )
                }
                items(messages, key = { it.id }) { m -> Bubble(m) }
            }

            // ------------------------------------------------------------ input
            if (blocked) {
                Text(
                    stringResource(R.string.chat_blocked_note),
                    style = EsType.meta.copy(fontSize = 14.sp), color = c.iceDim, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                )
            } else {
                if (error) {
                    Text(stringResource(R.string.chat_send_failed), style = EsType.meta, color = c.iceDim,
                        modifier = Modifier.padding(horizontal = 24.dp))
                }
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                            .background(c.surface, EsShapes.xl)
                            .border(1.dp, c.line, EsShapes.xl)
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                    ) {
                        if (input.isEmpty()) Text(stringResource(R.string.chat_hint), style = EsType.body, color = c.iceMute)
                        BasicTextField(
                            value = input,
                            onValueChange = { if (it.length <= 1000) input = it },
                            textStyle = EsType.body.copy(color = c.ice),
                            cursorBrush = SolidColor(c.ice),
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    val can = input.isNotBlank() && !sending
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(52.dp).clip(CircleShape)
                            .background(if (can) c.red else c.surfaceHigh)
                            .clickable(enabled = can, role = Role.Button) { send() }
                            .semantics { contentDescription = "send" },
                    ) { Icon(painterResource(R.drawable.ic_send), null, tint = if (can) c.onRed else c.iceMute, modifier = Modifier.size(22.dp)) }
                }
            }
        }
    }

    @Composable
    private fun Bubble(m: Message) {
        val c = esColors()
        val mine = m.from == myUid
        val shape = if (mine) RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp) else RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
        Box(Modifier.fillMaxWidth(), contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
            Column(
                horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
                modifier = Modifier
                    .widthIn(max = 290.dp)
                    .background(if (mine) c.red else c.surfaceHigh, shape)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            ) {
                Text(m.text, style = EsType.body.copy(fontSize = 15.sp, lineHeight = 21.sp), color = if (mine) c.onRed else c.ice)
                if (m.at > 0) {
                    Text(
                        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(m.at)),
                        style = EsType.meta.copy(fontSize = 10.sp), color = if (mine) c.onRed.copy(alpha = 0.7f) else c.iceMute,
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_UID = "chat_uid"
        const val EXTRA_NAME = "chat_name"
        const val EXTRA_PHOTO = "chat_photo"
    }
}
