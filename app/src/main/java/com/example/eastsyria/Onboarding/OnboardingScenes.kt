package com.example.eastsyria.Onboarding

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eastsyria.R

// ---------------------------------------------------------------------------------------------
// Page 1 — Search: the query types itself, three landmarks rise, the best one is picked.
// ---------------------------------------------------------------------------------------------

private const val SEARCH_MS = 3800
private const val TYPE_START = 300
private const val TYPE_MS_PER_CHAR = 70

private data class Result(@DrawableRes val photo: Int, val name: Int, val meta: Int)

private val results = listOf(
    Result(R.drawable.onb_qalaat_jabar, R.string.onb_search_r1, R.string.onb_search_r1_meta),
    Result(R.drawable.onb_al_rahba, R.string.onb_search_r2, R.string.onb_search_r2_meta),
    Result(R.drawable.onb_dura_europos, R.string.onb_search_r3, R.string.onb_search_r3_meta),
)

@Composable
internal fun SearchScene(active: Boolean) {
    val c = esColors()
    val time = rememberTimeline(active, SEARCH_MS)
    val query = stringResource(R.string.onb_search_query)
    val typed = ((time - TYPE_START) / TYPE_MS_PER_CHAR).toInt().coerceIn(0, query.length)
    val typingEnds = TYPE_START + query.length * TYPE_MS_PER_CHAR

    val countShown = appear(time >= typingEnds + 200)
    val rows = results.indices.map { appear(time >= typingEnds + 300 + it * 160) }
    val picked = time >= typingEnds + 1300
    val names = results.map { stringResource(it.name) }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = "$query: ${names.joinToString()}"
        },
    ) {
        // The search field, as on the Search screen.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(c.surface, EsShapes.pill)
                .border(1.dp, if (typed > 0) c.red.copy(alpha = 0.7f) else c.line, EsShapes.pill)
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Icon(painterResource(R.drawable.ic_search), null, tint = c.iceDim, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                text = if (typed == 0) stringResource(R.string.onb_search_hint) else query.take(typed),
                style = EsType.body,
                color = if (typed == 0) c.iceMute else c.ice,
                maxLines = 1,
            )
            Box(
                Modifier
                    .padding(start = 2.dp)
                    .width(2.dp)
                    .height(20.dp)
                    .background(if (typed < query.length || caretOn(time)) c.red else Color.Transparent),
            )
        }

        Text(
            text = stringResource(R.string.onb_search_count),
            style = EsType.label,
            color = c.iceMute,
            modifier = Modifier
                .padding(start = 6.dp, top = 6.dp)
                .rise(countShown, 8),
        )

        results.forEachIndexed { i, r ->
            ResultRow(r, picked = picked && i == 0, modifier = Modifier.rise(rows[i]))
        }
    }
}

@Composable
private fun ResultRow(r: Result, picked: Boolean, modifier: Modifier = Modifier) {
    val c = esColors()
    val border by animateColorAsState(if (picked) c.red else c.line, tween(300), label = "rowBorder")
    val chevron = pop(picked)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(if (picked) c.surfaceHigh else c.surface, EsShapes.lg)
            .border(if (picked) 1.5.dp else 1.dp, border, EsShapes.lg)
            .padding(10.dp),
    ) {
        Image(
            painter = painterResource(r.photo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(52.dp)
                .clip(EsShapes.md),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(r.name), style = EsType.title, color = c.ice, maxLines = 1)
            Text(stringResource(r.meta), style = EsType.meta, color = c.iceMute, maxLines = 1)
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .scale(chevron.coerceAtLeast(0f))
                .background(c.red, CircleShape),
        ) {
            Icon(painterResource(R.drawable.ic_arrow_right), null, tint = c.onRed, modifier = Modifier.size(16.dp))
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Page 2 — Map: the Euphrates draws itself, landmarks drop onto it, one opens.
// ---------------------------------------------------------------------------------------------

private const val MAP_MS = 4400
private const val RIVER_START = 150
private const val RIVER_END = 1500
private val PIN_AT = listOf(1250, 1500, 1750, 2000)
private val PIN_ALONG = listOf(0.16f, 0.42f, 0.66f, 0.88f)
private const val SELECT_AT = 2500
private const val CARD_AT = 2750

@Composable
internal fun MapScene(active: Boolean) {
    val c = esColors()
    val time = rememberTimeline(active, MAP_MS)
    val river = span(time, RIVER_START, RIVER_END).let { 1f - (1f - it) * (1f - it) } // ease out
    val pins = PIN_AT.map { pop(time >= it) }
    val selected = pop(time >= SELECT_AT)
    val card = appear(time >= CARD_AT)
    val labels = listOf(
        stringResource(R.string.onb_map_pin1),
        stringResource(R.string.onb_map_pin2),
        stringResource(R.string.onb_map_pin3),
        stringResource(R.string.onb_map_pin4),
    )
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 11.sp, color = c.iceDim)
    val path = remember { Path() }
    val measure = remember { PathMeasure() }
    val segment = remember { Path() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(330.dp)
            .clip(EsShapes.xl)
            .background(c.surface)
            .border(1.dp, c.line, EsShapes.xl)
            .clearAndSetSemantics {
                contentDescription = labels.joinToString() + ". " + "Dura-Europos"
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // A quiet grid, like a printed survey map.
            val step = 28.dp.toPx()
            var x = step
            while (x < size.width) {
                drawLine(c.line.copy(alpha = 0.45f), Offset(x, 0f), Offset(x, size.height), 1f); x += step
            }
            var y = step
            while (y < size.height) {
                drawLine(c.line.copy(alpha = 0.45f), Offset(0f, y), Offset(size.width, y), 1f); y += step
            }

            // The Euphrates, from the north-west down to the south-east.
            val w = size.width
            val h = size.height
            path.reset()
            path.moveTo(w * 0.06f, h * 0.12f)
            path.cubicTo(w * 0.30f, h * 0.12f, w * 0.22f, h * 0.32f, w * 0.44f, h * 0.34f)
            path.cubicTo(w * 0.66f, h * 0.36f, w * 0.58f, h * 0.50f, w * 0.74f, h * 0.52f)
            path.cubicTo(w * 0.86f, h * 0.54f, w * 0.84f, h * 0.60f, w * 0.96f, h * 0.63f)
            measure.setPath(path, false)
            val length = measure.length
            segment.reset()
            measure.getSegment(0f, length * river, segment, true)
            drawPath(segment, c.river.copy(alpha = 0.22f), style = Stroke(14.dp.toPx(), cap = StrokeCap.Round))
            drawPath(segment, c.river, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))

            PIN_ALONG.forEachIndexed { i, along ->
                val p = pins[i]
                if (p <= 0.01f) return@forEachIndexed
                val at = measure.getPosition(length * along)
                val isSelected = i == 2
                val r = 7.dp.toPx() * p * (if (isSelected) 1f + 0.35f * selected else 1f)
                if (isSelected && selected > 0f) {
                    drawCircle(c.red.copy(alpha = 0.18f * selected), r * 2.6f, at)
                }
                drawCircle(c.canvas, r + 3.dp.toPx() * p, at)
                drawCircle(if (isSelected) c.red else c.ice, r, at)
                val text = measurer.measure(labels[i], labelStyle)
                val padX = 6.dp.toPx()
                val padY = 2.dp.toPx()
                val left = (at.x - text.size.width / 2f).coerceIn(padX, size.width - text.size.width - padX)
                val top = at.y - r - 10.dp.toPx() - text.size.height
                val a = p.coerceIn(0f, 1f)
                drawRoundRect(
                    color = c.canvas.copy(alpha = 0.85f * a),
                    topLeft = Offset(left - padX, top - padY),
                    size = androidx.compose.ui.geometry.Size(text.size.width + padX * 2, text.size.height + padY * 2),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                )
                drawText(textLayoutResult = text, topLeft = Offset(left, top), alpha = a)
            }
        }

        // The pin's card, as on the Map screen.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
                .fillMaxWidth()
                .rise(card, 28)
                .background(c.surfaceHigh, EsShapes.lg)
                .border(1.dp, c.line, EsShapes.lg)
                .padding(10.dp),
        ) {
            Image(
                painterResource(R.drawable.onb_dura_europos), null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(EsShapes.md),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.onb_search_r3), style = EsType.title, color = c.ice, maxLines = 1)
                Text(stringResource(R.string.onb_map_card_meta), style = EsType.meta, color = c.iceMute, maxLines = 1)
            }
            Text(
                stringResource(R.string.onb_map_open),
                style = EsType.label,
                color = c.onRed,
                modifier = Modifier
                    .background(c.red, EsShapes.pill)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Page 3 — Bilingual: the English name, then the Arabic, the facts, and the reviewed mark.
// ---------------------------------------------------------------------------------------------

private const val ENTRY_MS = 3600
private const val EN_AT = 250
private const val AR_AT = 800
private val FACT_AT = listOf(1400, 1600, 1800)
private const val REVIEWED_AT = 2500

@Composable
internal fun BilingualScene(active: Boolean) {
    val c = esColors()
    val time = rememberTimeline(active, ENTRY_MS)
    val en = appear(time >= EN_AT)
    val ar = appear(time >= AR_AT)
    val facts = FACT_AT.map { appear(time >= it) }
    val reviewed = pop(time >= REVIEWED_AT)
    val nameEn = stringResource(R.string.onboarding_plate_en)
    val nameAr = stringResource(R.string.onboarding_plate_ar)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(EsShapes.xl)
            .background(c.surface)
            .border(1.dp, c.line, EsShapes.xl)
            .clearAndSetSemantics { contentDescription = "$nameEn, $nameAr" },
    ) {
        Box(Modifier.height(150.dp)) {
            Image(
                painterResource(R.drawable.onb_tell_halaf), null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = 1.08f - 0.08f * en; scaleY = scaleX },
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to c.surface)),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .scale(reviewed.coerceAtLeast(0f))
                    .background(c.canvas.copy(alpha = 0.8f), EsShapes.pill)
                    .border(1.dp, c.red, EsShapes.pill)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Box(Modifier.size(6.dp).background(c.red, CircleShape))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.onb_entry_reviewed), style = EsType.label, color = c.ice)
            }
        }
        Column(Modifier.padding(start = 18.dp, end = 18.dp, bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    nameEn,
                    style = EsType.display.copy(fontSize = 28.sp, lineHeight = 32.sp),
                    color = c.ice,
                    modifier = Modifier.weight(1f).rise(en),
                )
                Text(
                    nameAr,
                    style = TextStyle(fontFamily = Amiri, fontSize = 26.sp, textDirection = TextDirection.Rtl),
                    color = c.redSoft,
                    modifier = Modifier.rise(ar),
                )
            }
            Spacer(Modifier.height(8.dp))
            FactRow(R.string.onb_entry_era, R.string.onb_entry_era_value, facts[0])
            FactRow(R.string.onb_entry_region, R.string.onb_entry_region_value, facts[1])
            FactRow(R.string.onb_entry_entry, R.string.onb_entry_entry_value, facts[2], last = true)
        }
    }
}

@Composable
private fun FactRow(label: Int, value: Int, shown: Float, last: Boolean = false) {
    val c = esColors()
    Column(Modifier.rise(shown, 10)) {
        Row(Modifier.padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(label), style = EsType.meta, color = c.iceMute, modifier = Modifier.weight(1f))
            Text(stringResource(value), style = EsType.title.copy(fontSize = 14.sp), color = c.ice)
        }
        if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
    }
}

// ---------------------------------------------------------------------------------------------
// Page 4 — Save: the bookmark is tapped, the collection grows, a new landmark is announced.
// ---------------------------------------------------------------------------------------------

private const val SAVE_MS = 4200
private const val TAP_AT = 900
private const val SAVED_AT = 1050
private const val NOTE_AT = 2200

@Composable
internal fun SaveScene(active: Boolean) {
    val c = esColors()
    val time = rememberTimeline(active, SAVE_MS)
    val ripple = span(time, TAP_AT, TAP_AT + 450)
    val saved = time >= SAVED_AT
    val mark = pop(saved)
    val note = appear(time >= NOTE_AT)
    val tint by animateColorAsState(if (saved) c.red else c.ice, tween(200), label = "bookmark")
    val name = stringResource(R.string.onb_save_name)

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = "$name. " + "Mari Ancient City"
        },
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(EsShapes.xl),
        ) {
            Image(
                painterResource(R.drawable.onb_palmyra), null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to c.canvas.copy(alpha = 0.92f))),
            )
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(18.dp),
            ) {
                Text(name, style = EsType.display.copy(fontSize = 30.sp, lineHeight = 34.sp), color = c.ice)
                Text(stringResource(R.string.onb_save_meta), style = EsType.meta, color = c.iceDim)
            }

            // The bookmark button, tapped by an invisible finger.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .size(48.dp),
            ) {
                if (ripple in 0.001f..0.999f) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .scale(0.6f + 0.9f * ripple)
                            .background(c.ice.copy(alpha = 0.35f * (1f - ripple)), CircleShape),
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .scale(if (saved) 0.85f + 0.15f * mark else 1f)
                        .background(c.canvas.copy(alpha = 0.7f), CircleShape),
                ) {
                    Icon(
                        painterResource(if (saved) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_border),
                        null, tint = tint, modifier = Modifier.size(22.dp),
                    )
                }
            }

            // The notification, sliding in over the photo.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, end = 72.dp, top = 12.dp)
                    .graphicsLayer {
                        alpha = note
                        translationY = (1f - note) * -40.dp.toPx()
                    }
                    .background(c.surfaceHigh.copy(alpha = 0.96f), EsShapes.lg)
                    .border(1.dp, c.line, EsShapes.lg)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .background(c.red, CircleShape),
                ) {
                    Icon(painterResource(R.drawable.ic_notification_bell), null, tint = c.onRed, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(stringResource(R.string.onb_save_note_title), style = EsType.label, color = c.iceMute, maxLines = 1)
                    Text(stringResource(R.string.onb_save_note_body), style = EsType.title.copy(fontSize = 14.sp), color = c.ice, maxLines = 1)
                }
            }
        }

        // The collection count, ticking up as the bookmark lands.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(c.surface, EsShapes.lg)
                .border(1.dp, c.line, EsShapes.lg)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Icon(painterResource(R.drawable.ic_bookmark_filled), null, tint = c.red, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.onb_save_collection), style = EsType.title, color = c.ice, modifier = Modifier.weight(1f))
            AnimatedContent(
                targetState = if (saved) 3 else 2,
                transitionSpec = {
                    (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                },
                label = "count",
            ) { count ->
                Text(
                    count.toString(),
                    style = EsType.title,
                    color = c.onRed,
                    modifier = Modifier
                        .background(lerp(c.surfaceHigh, c.red, mark.coerceIn(0f, 1f)), EsShapes.pill)
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                )
            }
        }
    }
}
