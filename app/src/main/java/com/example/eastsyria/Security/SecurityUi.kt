package com.example.eastsyria.Security

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import com.example.eastsyria.Onboarding.EsShapes
import com.example.eastsyria.Onboarding.EsType
import com.example.eastsyria.Onboarding.esColors
import com.example.eastsyria.R
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

/** Six digit boxes over one invisible field; digits always read left to right, even in Arabic. */
@Composable
internal fun CodeField(value: String, onChange: (String) -> Unit, error: Boolean, autoFocus: Boolean = true) {
    val c = esColors()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (autoFocus) runCatching { focus.requestFocus() } }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BasicTextField(
            value = value,
            onValueChange = { v -> onChange(v.filter { it.isDigit() }.take(6)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus),
            decorationBox = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    repeat(6) { i ->
                        val ch = value.getOrNull(i)?.toString().orEmpty()
                        val current = i == value.length
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .background(c.surface, EsShapes.md)
                                .border(
                                    if (current || error) 1.5.dp else 1.dp,
                                    when {
                                        error -> colorError()
                                        current -> c.ice
                                        else -> c.line
                                    },
                                    EsShapes.md,
                                ),
                        ) {
                            Text(ch, style = EsType.title.copy(fontSize = 22.sp), color = c.ice)
                        }
                    }
                }
            },
        )
    }
}

@Composable
internal fun colorError() = androidx.compose.ui.res.colorResource(R.color.error)

@Composable
internal fun PrimaryButton(text: String, enabled: Boolean, loading: Boolean = false, onClick: () -> Unit) {
    val c = esColors()
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .clip(EsShapes.lg)
            .background(if (enabled) c.red else c.surfaceHigh)
            .clickable(enabled = enabled && !loading, role = Role.Button, onClick = onClick),
    ) {
        if (loading) {
            CircularProgressIndicator(color = c.onRed, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        } else {
            Text(text, style = EsType.button, color = if (enabled) c.onRed else c.iceMute)
        }
    }
}

@Composable
internal fun QuietButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = esColors()
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(EsShapes.lg)
            .border(1.dp, c.line, EsShapes.lg)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
    ) {
        Text(text, style = EsType.button, color = c.ice, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun TextLink(text: String, onClick: () -> Unit) {
    val c = esColors()
    Text(
        text,
        style = EsType.button.copy(fontSize = 15.sp),
        color = c.iceDim,
        modifier = Modifier
            .clip(EsShapes.pill)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    )
}

@Composable
internal fun Card(content: @Composable () -> Unit) {
    val c = esColors()
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.surface, EsShapes.xl)
            .border(1.dp, c.line, EsShapes.xl)
            .padding(18.dp),
    ) { content() }
}

@Composable
internal fun BackHeader(title: String, onBack: () -> Unit) {
    val c = esColors()
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
        Text(title, style = EsType.display.copy(fontSize = 30.sp, lineHeight = 34.sp), color = c.ice)
    }
}

/** A QR code drawn in ink on a white tile, so any authenticator app can read it in either mode. */
@Composable
internal fun QrCode(content: String, modifier: Modifier = Modifier) {
    val bitmap = remember(content) {
        val size = 600
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.MARGIN to 1))
        Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
            val pixels = IntArray(size * size) { i -> if (matrix[i % size, i / size]) 0xFF1B1B1A.toInt() else 0xFFFFFFFF.toInt() }
            setPixels(pixels, 0, size, 0, 0, size, size)
        }
    }
    Box(
        modifier = modifier
            .clip(EsShapes.lg)
            .background(Color.White)
            .padding(10.dp),
    ) {
        Image(bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxWidth().graphicsLayer { })
    }
}
