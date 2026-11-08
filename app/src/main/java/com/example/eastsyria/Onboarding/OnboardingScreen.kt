package com.example.eastsyria.Onboarding

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.eastsyria.R
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private const val PAGE_COUNT = 4

/**
 * First-run onboarding. Each page plays a scene of the app; the last page asks the visitor in,
 * with Google first (one tap, nothing to confirm) and email as the quieter alternative.
 */
@Composable
internal fun OnboardingScreen(
    googleInFlight: Boolean,
    language: String,
    onLanguage: (String) -> Unit,
    onGoogle: () -> Unit,
    onEmail: () -> Unit,
    onSignIn: () -> Unit,
    onBrowseMap: () -> Unit,
) {
    val c = esColors()
    val pager = rememberPagerState { PAGE_COUNT }
    val scope = rememberCoroutineScope()
    val page = pager.currentPage
    val last = page == PAGE_COUNT - 1

    BackHandler(enabled = page > 0) { scope.launch { pager.animateScrollToPage(page - 1) } }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.canvas)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        TopBar(
            language = language,
            onLanguage = onLanguage,
            last = last,
            onSkip = { scope.launch { pager.animateScrollToPage(PAGE_COUNT - 1) } },
            onBrowseMap = onBrowseMap,
        )

        HorizontalPager(
            state = pager,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) { index ->
            // The words fade with the swipe; the scene plays when its page becomes current.
            val offset = ((pager.currentPage - index) + pager.currentPageOffsetFraction).absoluteValue
            val fade = (1f - offset).coerceIn(0f, 1f)
            when (index) {
                0 -> Page(R.string.onb_1_title, R.string.onb_1_body, fade) { SearchScene(active = page == 0) }
                1 -> Page(R.string.onb_2_title, R.string.onb_2_body, fade) { MapScene(active = page == 1) }
                2 -> Page(R.string.onb_3_title, R.string.onb_3_body, fade) { BilingualScene(active = page == 2) }
                else -> Page(R.string.onb_4_title, R.string.onb_4_body, fade) { SaveScene(active = page == 3) }
            }
        }

        AnimatedContent(
            targetState = last,
            transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(140)) },
            label = "actions",
        ) { isLast ->
            if (isLast) {
                StartActions(googleInFlight, onGoogle, onEmail, onSignIn)
            } else {
                PagerActions(pager, onNext = { scope.launch { pager.animateScrollToPage(page + 1) } })
            }
        }
    }
}

/** One page: the scene fills the free space, then the headline and the body. */
@Composable
private fun Page(
    @StringRes title: Int,
    @StringRes body: Int,
    fade: Float,
    scene: @Composable ColumnScope.() -> Unit,
) {
    val c = esColors()
    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 8.dp),
            verticalArrangement = Arrangement.Center,
            content = scene,
        )
        Column(
            Modifier
                .padding(start = 24.dp, end = 24.dp, top = 28.dp)
                .graphicsLayer { alpha = fade },
        ) {
            Text(
                stringResource(title),
                style = EsType.display,
                color = c.ice,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(10.dp))
            Text(stringResource(body), style = EsType.body, color = c.iceDim)
        }
    }
}

@Composable
private fun TopBar(
    language: String,
    onLanguage: (String) -> Unit,
    last: Boolean,
    onSkip: () -> Unit,
    onBrowseMap: () -> Unit,
) {
    val c = esColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(start = 24.dp, end = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(c.red, EsShapes.pill))
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.onb_wordmark), style = EsType.title, color = c.ice)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            LanguageSwitch(language, onLanguage)
            AnimatedContent(targetState = last, label = "topAction") { isLast ->
                TextAction(
                    text = stringResource(if (isLast) R.string.onboarding_open_map else R.string.onboarding_skip),
                    onClick = if (isLast) onBrowseMap else onSkip,
                )
            }
        }
    }
}

/**
 * English / العربية. Switching recreates the screen in the other language and direction, so the
 * scenes replay in Arabic, right to left.
 */
@Composable
private fun LanguageSwitch(language: String, onLanguage: (String) -> Unit) {
    val c = esColors()
    Row(
        modifier = Modifier
            .clip(EsShapes.pill)
            .border(1.dp, c.line, EsShapes.pill)
            .padding(3.dp),
    ) {
        listOf("en" to "EN", "ar" to "ع").forEach { (tag, label) ->
            val on = tag == language
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .heightIn(min = 32.dp)
                    .width(40.dp)
                    .clip(EsShapes.pill)
                    .background(if (on) c.red else Color.Transparent)
                    .clickable(enabled = !on, role = Role.Button) { onLanguage(tag) }
                    .semantics { contentDescription = if (tag == "en") "English" else "العربية" },
            ) {
                Text(
                    label,
                    style = if (tag == "ar") EsType.button.copy(fontFamily = Amiri) else EsType.label,
                    color = if (on) c.onRed else c.iceDim,
                )
            }
        }
    }
}

@Composable
private fun PagerActions(pager: PagerState, onNext: () -> Unit) {
    val c = esColors()
    val pageOf = stringResource(R.string.onb_page_of, pager.currentPage + 1, PAGE_COUNT)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.semantics {
                contentDescription = pageOf
            },
        ) {
            repeat(PAGE_COUNT) { i ->
                val on = i == pager.currentPage
                val width by animateDpAsState(if (on) 28.dp else 8.dp, label = "dot")
                Box(
                    Modifier
                        .height(8.dp)
                        .width(width)
                        .background(if (on) c.red else c.line, EsShapes.pill),
                )
            }
        }
        FilledButton(text = stringResource(R.string.onboarding_next), onClick = onNext, arrow = true)
    }
}

/** Google first; email and signing in are quieter alternatives. */
@Composable
private fun StartActions(googleInFlight: Boolean, onGoogle: () -> Unit, onEmail: () -> Unit, onSignIn: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        GoogleButton(loading = googleInFlight, onClick = onGoogle)
        Spacer(Modifier.height(4.dp))
        OutlineButton(text = stringResource(R.string.onb_continue_email), onClick = onEmail, enabled = !googleInFlight)
        TextAction(
            text = stringResource(R.string.onb_have_account),
            onClick = onSignIn,
            enabled = !googleInFlight,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun GoogleButton(loading: Boolean, onClick: () -> Unit) {
    val c = esColors()
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(EsShapes.pill)
            .background(c.red)
            .clickable(enabled = !loading, role = Role.Button, onClick = onClick),
    ) {
        if (loading) {
            CircularProgressIndicator(color = c.onRed, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        } else {
            // The G sits on its own white disc, as Google's branding asks.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(30.dp)
                    .background(Color.White, EsShapes.pill),
            ) {
                Image(painterResource(R.drawable.ic_google), null, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.onb_continue_google), style = EsType.button, color = c.onRed)
        }
    }
}

@Composable
private fun FilledButton(text: String, onClick: () -> Unit, arrow: Boolean = false) {
    val c = esColors()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .heightIn(min = 52.dp)
            .clip(EsShapes.pill)
            .background(c.red)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 24.dp, end = if (arrow) 18.dp else 24.dp),
    ) {
        Text(text, style = EsType.button, color = c.onRed)
        if (arrow) {
            Spacer(Modifier.width(8.dp))
            Icon(painterResource(R.drawable.ic_arrow_right), null, tint = c.onRed, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun OutlineButton(text: String, onClick: () -> Unit, enabled: Boolean) {
    val c = esColors()
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(EsShapes.pill)
            .border(1.dp, c.line, EsShapes.pill)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f },
    ) {
        Icon(painterResource(R.drawable.ic_email), null, tint = c.iceDim, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = EsType.button, color = c.ice)
    }
}

@Composable
private fun TextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = esColors()
    Text(
        text = text,
        style = EsType.button.copy(textAlign = TextAlign.Center),
        color = c.iceDim,
        modifier = modifier
            .clip(EsShapes.pill)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0C0E, heightDp = 860)
@Composable
private fun OnboardingPreview() {
    OnboardingScreen(
        googleInFlight = false, language = "en", onLanguage = {},
        onGoogle = {}, onEmail = {}, onSignIn = {}, onBrowseMap = {},
    )
}
