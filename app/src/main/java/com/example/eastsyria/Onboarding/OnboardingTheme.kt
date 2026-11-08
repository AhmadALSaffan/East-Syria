package com.example.eastsyria.Onboarding

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eastsyria.R

/** The app's colour tokens (res/values/colors.xml), so Compose and the XML screens never drift. */
@Immutable
internal data class EsColors(
    val canvas: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val line: Color,
    val ice: Color,
    val iceDim: Color,
    val iceMute: Color,
    val red: Color,
    val redSoft: Color,
    val onRed: Color,
    val river: Color,
)

@Composable
@ReadOnlyComposable
internal fun esColors() = EsColors(
    canvas = colorResource(R.color.canvas),
    surface = colorResource(R.color.surface),
    surfaceHigh = colorResource(R.color.surface_high),
    line = colorResource(R.color.line),
    ice = colorResource(R.color.ivory),
    iceDim = colorResource(R.color.ivory_dim),
    iceMute = colorResource(R.color.ivory_mute),
    red = colorResource(R.color.terracotta),
    redSoft = colorResource(R.color.terracotta_soft),
    onRed = colorResource(R.color.on_accent),
    river = colorResource(R.color.wayfinding),
)

internal val Amiri = FontFamily(
    Font(R.font.amiri_regular, FontWeight.Normal),
    Font(R.font.amiri_bold, FontWeight.Bold),
)

internal object EsType {
    val display = TextStyle(fontFamily = Amiri, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp)
    val body = TextStyle(fontSize = 16.sp, lineHeight = 24.sp)
    val title = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium)
    val meta = TextStyle(fontSize = 13.sp, lineHeight = 18.sp)
    val label = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp)
    val button = TextStyle(fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
}

internal object EsShapes {
    val sm = RoundedCornerShape(8.dp)
    val md = RoundedCornerShape(12.dp)
    val lg = RoundedCornerShape(16.dp)
    val xl = RoundedCornerShape(24.dp)
    val pill = RoundedCornerShape(50)
}
