package com.example.eastsyria.Social

import android.content.Context
import android.text.format.DateUtils
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.example.eastsyria.Onboarding.EsShapes
import com.example.eastsyria.Onboarding.EsType
import com.example.eastsyria.Onboarding.esColors
import com.example.eastsyria.R

/** A round photo, or the person's initial on a raised disc when there is none. */
@Composable
internal fun Avatar(photo: String, name: String, size: Dp = 48.dp, online: Boolean = false) {
    val c = esColors()
    Box {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(size).clip(CircleShape).background(c.surfaceHigh),
        ) {
            Text(name.trim().firstOrNull()?.uppercase().orEmpty(), style = EsType.title.copy(fontSize = (size.value * 0.38f).sp), color = c.ice)
            if (photo.isNotBlank()) {
                AndroidView(
                    factory = { ctx -> ImageView(ctx).apply { scaleType = ImageView.ScaleType.CENTER_CROP } },
                    update = { v -> Glide.with(v).load(photo).circleCrop().into(v) },
                    modifier = Modifier.size(size),
                )
            }
        }
        if (online) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.28f)
                    .background(c.canvas, CircleShape)
                    .padding(2.dp)
                    .background(colorOnline(), CircleShape),
            )
        }
    }
}

@Composable
internal fun colorOnline() = androidx.compose.ui.graphics.Color(0xFF2E9E5B)

/** "~3 km away", or "Less than 1 km away" for anything closer. */
internal fun distanceLabel(context: Context, km: Double): String =
    if (km < 1.0) context.getString(R.string.nearby_under_km)
    else context.getString(R.string.nearby_km, Math.round(km).toInt())

internal fun relativeTime(at: Long): String =
    if (at <= 0L) "" else DateUtils.getRelativeTimeSpanString(at, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE).toString()

@Composable
internal fun PersonRow(
    photo: String,
    name: String,
    line: String,
    online: Boolean,
    trailing: @Composable () -> Unit = {
        Icon(painterResource(R.drawable.ic_arrow_right), null, tint = esColors().iceMute, modifier = Modifier.size(20.dp))
    },
    onClick: () -> Unit,
) {
    val c = esColors()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(EsShapes.lg)
            .background(c.surface)
            .border(1.dp, c.line, EsShapes.lg)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(12.dp),
    ) {
        Avatar(photo, name, 52.dp, online)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(name.ifBlank { "—" }, style = EsType.title, color = c.ice, maxLines = 1)
            Text(line, style = EsType.meta, color = c.iceMute, maxLines = 1)
        }
        trailing()
    }
}
