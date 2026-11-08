package com.example.eastsyria.Social

import android.content.Intent
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eastsyria.Onboarding.EsType
import com.example.eastsyria.Onboarding.esColors
import com.example.eastsyria.R
import com.example.eastsyria.Security.BackHeader

/** Your conversations, newest first, with unread counts. */
class ChatListActivity : AppCompatActivity() {

    private var chats by mutableStateOf<List<ChatSummary>?>(null)
    private var stop: () -> Unit = {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        setContent {
            val c = esColors()
            Column(
                Modifier.fillMaxSize().background(c.canvas).windowInsetsPadding(WindowInsets.safeDrawing),
            ) {
                BackHeader(stringResource(R.string.chats_title)) { finish() }
                val list = chats
                when {
                    list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = c.ice, strokeWidth = 2.dp, modifier = Modifier.size(26.dp))
                    }
                    list.isEmpty() -> Column(
                        Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp).background(c.surfaceHigh, CircleShape)) {
                            Icon(painterResource(R.drawable.ic_chat), null, tint = c.ice, modifier = Modifier.size(26.dp))
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(stringResource(R.string.chats_empty_title), style = EsType.title.copy(fontSize = 18.sp), color = c.ice)
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.chats_empty_body), style = EsType.body.copy(fontSize = 15.sp), color = c.iceDim, textAlign = TextAlign.Center)
                    }
                    else -> LazyColumn(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(list, key = { it.chatId }) { chat ->
                            val prefix = if (chat.lastFromMe) getString(R.string.chats_you) + " " else ""
                            PersonRow(
                                chat.photo, chat.name, prefix + chat.lastText, online = false,
                                trailing = {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(relativeTime(chat.at), style = EsType.meta.copy(fontSize = 11.sp), color = c.iceMute)
                                        if (chat.unread > 0) {
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                chat.unread.toString(),
                                                style = EsType.label.copy(fontSize = 11.sp), color = c.onRed,
                                                modifier = Modifier.background(c.red, CircleShape).padding(horizontal = 7.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                },
                            ) {
                                startActivity(Intent(this@ChatListActivity, ChatActivity::class.java)
                                    .putExtra(ChatActivity.EXTRA_UID, chat.with)
                                    .putExtra(ChatActivity.EXTRA_NAME, chat.name)
                                    .putExtra(ChatActivity.EXTRA_PHOTO, chat.photo))
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        stop = Social.listenChats { chats = it }
    }

    override fun onStop() {
        stop()
        super.onStop()
    }
}

