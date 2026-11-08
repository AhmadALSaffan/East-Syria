package com.example.eastsyria.Social

import android.Manifest
import android.content.Intent
import android.location.Location
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eastsyria.Onboarding.EsShapes
import com.example.eastsyria.Onboarding.EsType
import com.example.eastsyria.Onboarding.esColors
import com.example.eastsyria.R
import com.example.eastsyria.Security.BackHeader

/** Who else has the app open near you, for people who chose to be iAmVisible. */
class NearbyActivity : AppCompatActivity() {

    private enum class State { OFF, NEED_PERMISSION, LOCATING, NO_FIX, LIST }

    private var iAmVisible by mutableStateOf(false)
    private var state by mutableStateOf(State.OFF)
    private var people by mutableStateOf(listOf<NearbyPerson>())
    private var farther by mutableIntStateOf(0)
    private var unread by mutableIntStateOf(0)
    private var stopNearby: () -> Unit = {}
    private var stopUnread: () -> Unit = {}

    private val askLocation = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) turnOn() else { iAmVisible = false; state = State.NEED_PERMISSION }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        iAmVisible = Social.isVisible(this) && Social.hasLocationPermission(this)
        setContent { Screen() }
    }

    override fun onStart() {
        super.onStart()
        stopUnread = Social.listenUnreadTotal { unread = it }
        if (iAmVisible) startListening() else state = State.OFF
    }

    override fun onStop() {
        stopNearby(); stopUnread()
        super.onStop()
    }

    private fun onToggle(on: Boolean) {
        if (!on) {
            Social.setVisible(this, false)
            iAmVisible = false; stopNearby(); people = emptyList(); state = State.OFF
            return
        }
        if (Social.hasLocationPermission(this)) turnOn() else askLocation.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    private fun turnOn() {
        Social.setVisible(this, true)
        iAmVisible = true
        startListening()
    }

    private fun startListening() {
        state = State.LOCATING
        Presence.locate(this) { here: Location? ->
            if (here == null) { state = State.NO_FIX; return@locate }
            Presence.publish(this, here)
            stopNearby()
            stopNearby = Social.listenNearby(here) { list, far -> people = list; farther = far; state = State.LIST }
        }
    }

    private fun openChat(p: NearbyPerson) {
        startActivity(Intent(this, ChatActivity::class.java)
            .putExtra(ChatActivity.EXTRA_UID, p.uid)
            .putExtra(ChatActivity.EXTRA_NAME, p.name)
            .putExtra(ChatActivity.EXTRA_PHOTO, p.photo))
    }

    @Composable
    private fun Screen() {
        val c = esColors()
        Column(
            Modifier
                .fillMaxSize()
                .background(c.canvas)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { BackHeader(stringResource(R.string.nearby_title)) { finish() } }
                MessagesButton()
                Spacer(Modifier.width(16.dp))
            }
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item { VisibilityCard() }
                when (state) {
                    State.OFF -> item { Note(R.string.nearby_off_title, R.string.nearby_off_body) }
                    State.NEED_PERMISSION -> item { Note(R.string.nearby_perm_title, R.string.nearby_perm_body) }
                    State.LOCATING -> item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = c.ice, strokeWidth = 2.dp, modifier = Modifier.size(26.dp))
                        }
                    }
                    State.NO_FIX -> item { Note(R.string.nearby_nofix_title, R.string.nearby_nofix_body) }
                    State.LIST -> {
                        if (people.isNotEmpty()) item {
                            Text(
                                pluralStringResource(R.plurals.nearby_count, people.size),
                                style = EsType.label.copy(fontSize = 13.sp), color = c.iceMute,
                                modifier = Modifier.padding(start = 6.dp, top = 12.dp, bottom = 2.dp),
                            )
                        }
                        if (people.isEmpty()) item { Note(R.string.nearby_empty_title, R.string.nearby_empty_body) }
                        if (people.isEmpty() && farther > 0) item {
                            Text(
                                pluralStringResource(R.plurals.nearby_farther, farther),
                                style = EsType.meta.copy(fontSize = 13.sp), color = c.iceMute,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                            )
                        }
                        items(people, key = { it.uid }) { p ->
                            PersonRow(
                                p.photo, p.name,
                                distanceLabel(this@NearbyActivity, p.km) + "  ·  " + getString(R.string.nearby_active_now),
                                online = true,
                            ) { openChat(p) }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun pluralStringResource(id: Int, n: Int) = resources.getQuantityString(id, n, n)

    @Composable
    private fun MessagesButton() {
        val c = esColors()
        Box {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(1.dp, c.line, CircleShape)
                    .clickable(role = Role.Button) { startActivity(Intent(this@NearbyActivity, ChatListActivity::class.java)) }
                    .semantics { contentDescription = getString(R.string.chats_title) },
            ) {
                Icon(painterResource(R.drawable.ic_chat), null, tint = c.ice, modifier = Modifier.size(22.dp))
            }
            if (unread > 0) {
                Text(
                    if (unread > 9) "9+" else unread.toString(),
                    style = EsType.label.copy(fontSize = 10.sp), color = c.onRed,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(c.red, CircleShape)
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
        }
    }

    @Composable
    private fun VisibilityCard() {
        val c = esColors()
        Column(
            Modifier
                .fillMaxWidth()
                .background(c.surface, EsShapes.xl)
                .border(1.dp, c.line, EsShapes.xl)
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.nearby_visible), style = EsType.title, color = c.ice)
                    Text(
                        stringResource(if (iAmVisible) R.string.nearby_visible_on else R.string.nearby_visible_off),
                        style = EsType.meta, color = c.iceMute,
                    )
                }
                Switch(
                    checked = iAmVisible,
                    onCheckedChange = ::onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = c.onRed, checkedTrackColor = c.red,
                        uncheckedThumbColor = c.iceMute, uncheckedTrackColor = c.surfaceHigh, uncheckedBorderColor = c.line,
                    ),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.nearby_privacy), style = EsType.meta.copy(fontSize = 13.sp, lineHeight = 19.sp), color = c.iceDim)
        }
    }

    @Composable
    private fun Note(title: Int, body: Int) {
        val c = esColors()
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp).background(c.surfaceHigh, CircleShape)) {
                Icon(painterResource(R.drawable.ic_people), null, tint = c.ice, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(stringResource(title), style = EsType.title.copy(fontSize = 18.sp), color = c.ice)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(body), style = EsType.body.copy(fontSize = 15.sp), color = c.iceDim,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
