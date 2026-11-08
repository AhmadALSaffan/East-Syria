package com.example.eastsyria.Assistant

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.example.eastsyria.MainPage.Data.Landmark
import com.example.eastsyria.Onboarding.EsShapes
import com.example.eastsyria.Onboarding.EsType
import com.example.eastsyria.Onboarding.esColors
import com.example.eastsyria.R
import com.example.eastsyria.Ui.localCity
import com.example.eastsyria.Ui.localName
import android.widget.ImageView
import androidx.compose.ui.platform.LocalContext

/** A message on screen. Assistant messages may carry landmarks to open. */
data class ChatMessage(
    val fromUser: Boolean,
    val text: String,
    val landmarkIds: List<String> = emptyList(),
    val isError: Boolean = false,
)

@Composable
internal fun AssistantScreen(
    messages: List<ChatMessage>,
    thinking: Boolean,
    remaining: Int?,
    landmarks: Map<String, Landmark>,
    onSend: (String) -> Unit,
    onOpenLandmark: (Landmark) -> Unit,
    onBack: () -> Unit,
) {
    val c = esColors()
    var input by remember { mutableStateOf("") }
    val list = rememberLazyListState()
    val send = {
        val text = input.trim()
        if (text.isNotEmpty() && !thinking) {
            onSend(text)
            input = ""
        }
    }

    // Keep the newest message (or the typing dots) in view.
    LaunchedEffect(messages.size, thinking) {
        val count = list.layoutInfo.totalItemsCount
        if (messages.isNotEmpty() && count > 0) list.animateScrollToItem(count - 1)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.canvas)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding(),
    ) {
        // ------------------------------------------------------------ top bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(1.dp, c.line, CircleShape)
                    .clickable(role = Role.Button, onClick = onBack)
                    .semantics { contentDescription = "back" },
            ) {
                Icon(painterResource(R.drawable.ic_back), null, tint = c.ice, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .background(c.red, CircleShape),
            ) {
                Icon(painterResource(R.drawable.ic_sparkle), null, tint = c.onRed, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(stringResource(R.string.assistant_title), style = EsType.title.copy(fontSize = 17.sp), color = c.ice)
                val subtitle = stringResource(R.string.assistant_subtitle)
                val left = remaining?.let {
                    androidx.compose.ui.platform.LocalContext.current.resources
                        .getQuantityString(R.plurals.assistant_left, it, it)
                }
                Text(if (left == null) subtitle else "$subtitle · $left", style = EsType.meta, color = c.iceMute)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))

        // ------------------------------------------------------------ conversation
        LazyColumn(
            state = list,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (messages.isEmpty()) {
                item { Welcome(onPick = { onSend(it) }) }
            }
            items(messages) { m ->
                if (m.fromUser) UserBubble(m.text) else AssistantBubble(m, landmarks, onOpenLandmark)
            }
            if (thinking) item { Thinking() }
            item { Spacer(Modifier.height(1.dp)) }
        }

        // ------------------------------------------------------------ input
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
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
                if (input.isEmpty()) {
                    Text(stringResource(R.string.assistant_hint), style = EsType.body, color = c.iceMute)
                }
                BasicTextField(
                    value = input,
                    onValueChange = { if (it.length <= 1000) input = it },
                    textStyle = EsType.body.copy(color = c.ice),
                    cursorBrush = SolidColor(c.red),
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.width(10.dp))
            val canSend = input.isNotBlank() && !thinking
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(if (canSend) c.red else c.surfaceHigh)
                    .clickable(enabled = canSend, role = Role.Button) { send() }
                    .semantics { contentDescription = "send" },
            ) {
                Icon(
                    painterResource(R.drawable.ic_send),
                    null,
                    tint = if (canSend) c.onRed else c.iceMute,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun Welcome(onPick: (String) -> Unit) {
    val c = esColors()
    val suggestions = listOf(
        stringResource(R.string.assistant_suggest_1),
        stringResource(R.string.assistant_suggest_2),
        stringResource(R.string.assistant_suggest_3),
        stringResource(R.string.assistant_suggest_4),
    )
    Column(Modifier.padding(top = 12.dp)) {
        Text(stringResource(R.string.assistant_welcome_title), style = EsType.display.copy(fontSize = 32.sp), color = c.ice)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.assistant_welcome_body), style = EsType.body, color = c.iceDim)
        Spacer(Modifier.height(24.dp))
        suggestions.forEach { s ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(bottom = 10.dp)
                    .fillMaxWidth()
                    .clip(EsShapes.lg)
                    .background(c.surface)
                    .border(1.dp, c.line, EsShapes.lg)
                    .clickable(role = Role.Button) { onPick(s) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Icon(painterResource(R.drawable.ic_sparkle), null, tint = c.redSoft, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                Text(s, style = EsType.body.copy(fontSize = 15.sp), color = c.ice, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun UserBubble(text: String) {
    val c = esColors()
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Text(
            text,
            style = EsType.body,
            color = c.ice,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(c.surfaceHigh, EsShapes.lg)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun AssistantBubble(m: ChatMessage, landmarks: Map<String, Landmark>, onOpen: (Landmark) -> Unit) {
    val c = esColors()
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    AnimatedVisibility(visible = shown, enter = fadeIn() + slideInVertically { it / 6 }) {
        Column(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(28.dp)
                        .background(if (m.isError) c.surfaceHigh else c.red, CircleShape),
                ) {
                    Icon(painterResource(R.drawable.ic_sparkle), null, tint = c.onRed, modifier = Modifier.size(15.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    m.text,
                    style = EsType.body.copy(lineHeight = 25.sp),
                    color = if (m.isError) c.iceMute else c.ice,
                    modifier = Modifier.weight(1f),
                )
            }
            val cards = m.landmarkIds.mapNotNull { landmarks[it] }
            if (cards.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(start = 40.dp),
                ) {
                    items(cards) { LandmarkChip(it, onOpen) }
                }
            }
        }
    }
}

@Composable
private fun LandmarkChip(landmark: Landmark, onOpen: (Landmark) -> Unit) {
    val c = esColors()
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .width(230.dp)
            .clip(EsShapes.lg)
            .background(c.surface)
            .border(1.dp, c.line, EsShapes.lg)
            .clickable(role = Role.Button) { onOpen(landmark) }
            .padding(8.dp),
    ) {
        AndroidView(
            factory = { ctx -> ImageView(ctx).apply { scaleType = ImageView.ScaleType.CENTER_CROP } },
            update = { view ->
                Glide.with(view).load(landmark.imageUrl).placeholder(R.drawable.placeholder_dark)
                    .error(R.drawable.placeholder_dark).into(view)
            },
            modifier = Modifier.size(52.dp).clip(EsShapes.md),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(landmark.localName(context), style = EsType.title.copy(fontSize = 14.sp), color = c.ice, maxLines = 1)
            Text(landmark.localCity(context), style = EsType.meta.copy(fontSize = 12.sp), color = c.iceMute, maxLines = 1)
        }
        Icon(painterResource(R.drawable.ic_arrow_right), null, tint = c.redSoft, modifier = Modifier.size(18.dp))
    }
}

/** Three dots breathing while the guide writes. */
@Composable
private fun Thinking() {
    val c = esColors()
    val t = rememberInfiniteTransition(label = "thinking")
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 2.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(28.dp).background(c.red, CircleShape),
        ) {
            Icon(painterResource(R.drawable.ic_sparkle), null, tint = c.onRed, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.width(14.dp))
        repeat(3) { i ->
            val a by t.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(520, delayMillis = i * 160), RepeatMode.Reverse),
                label = "dot$i",
            )
            Box(
                Modifier
                    .padding(end = 6.dp)
                    .size(8.dp)
                    .graphicsLayer { alpha = a }
                    .background(c.iceDim, CircleShape),
            )
        }
    }
}
