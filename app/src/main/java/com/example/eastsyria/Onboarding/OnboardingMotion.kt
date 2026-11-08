package com.example.eastsyria.Onboarding

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp

/*
 * Each onboarding page plays a short scene of the real app (search, the map, a bilingual entry,
 * saving a landmark) as live Compose UI, the way Decivio's onboarding replays its product videos.
 * A scene runs on a timeline in milliseconds while its page is shown and restarts when the page
 * comes back. With animations turned off on the phone, the timeline jumps to the finished frame.
 */

/** Milliseconds since the page became current; holds at [durationMs]. Previews show the end. */
@Composable
internal fun rememberTimeline(active: Boolean, durationMs: Int): Float {
    val inspection = LocalInspectionMode.current
    val context = LocalContext.current
    val reduceMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val still = inspection || reduceMotion
    val time = remember { Animatable(if (still) durationMs.toFloat() else 0f) }
    LaunchedEffect(active) {
        if (still) return@LaunchedEffect
        if (active) {
            time.snapTo(0f)
            time.animateTo(durationMs.toFloat(), tween(durationMs, easing = LinearEasing))
        } else {
            time.snapTo(0f)
        }
    }
    return time.value
}

/** 0 → 1 with a calm spring once [shown] turns true (and back when the timeline restarts). */
@Composable
internal fun appear(shown: Boolean): Float {
    val value by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow),
        label = "appear",
    )
    return value
}

/** A small pop for pins, badges and the bookmark: overshoots a touch, then settles. */
@Composable
internal fun pop(shown: Boolean): Float {
    val value by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "pop",
    )
    return value
}

/** Rises into place as [progress] goes 0 → 1. */
@Composable
internal fun Modifier.rise(progress: Float, distanceDp: Int = 18): Modifier {
    val distance = with(LocalDensity.current) { distanceDp.dp.toPx() }
    return graphicsLayer {
        alpha = progress.coerceIn(0f, 1f)
        translationY = (1f - progress) * distance
    }
}

/** Linear 0 → 1 between [start] and [end] on the timeline. */
internal fun span(time: Float, start: Int, end: Int): Float =
    ((time - start) / (end - start)).coerceIn(0f, 1f)

/** A caret that blinks about twice a second on the timeline. */
internal fun caretOn(time: Float): Boolean = (time / 450f).toInt() % 2 == 0
