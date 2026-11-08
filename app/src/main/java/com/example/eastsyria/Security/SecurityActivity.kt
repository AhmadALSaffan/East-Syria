package com.example.eastsyria.Security

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.eastsyria.Onboarding.EsShapes
import com.example.eastsyria.Onboarding.EsType
import com.example.eastsyria.Onboarding.esColors
import com.example.eastsyria.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/**
 * Security: how you sign in, the authenticator-app second step (set up, recovery codes, turn off),
 * and a password reset for email accounts.
 */
class SecurityActivity : AppCompatActivity() {

    private enum class Stage { LOADING, OFF, SETUP, CODES, ON, TURNING_OFF, ERROR }

    private var stage by mutableStateOf(Stage.LOADING)
    private var secret by mutableStateOf("")
    private var uri by mutableStateOf("")
    private var code by mutableStateOf("")
    private var codes by mutableStateOf(listOf<String>())
    private var busy by mutableStateOf(false)
    private var message by mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        loadStatus()
        setContent { Screen() }
    }

    private fun loadStatus() {
        stage = Stage.LOADING
        lifecycleScope.launch {
            stage = when (val r = SecurityClient.status()) {
                is SecurityResult.Status -> if (r.enabled) Stage.ON else Stage.OFF
                else -> Stage.ERROR
            }
        }
    }

    @Composable
    private fun Screen() {
        val c = esColors()
        val user = FirebaseAuth.getInstance().currentUser
        val google = user?.providerData?.any { it.providerId == "google.com" } == true
        val password = user?.providerData?.any { it.providerId == "password" } == true

        Column(
            Modifier
                .fillMaxSize()
                .background(c.canvas)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding(),
        ) {
            BackHeader(stringResource(R.string.security_title)) { finish() }
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
            ) {
                // ---------------------------------------------------- how you sign in
                Label(R.string.security_signin)
                Card {
                    InfoRow(R.drawable.ic_person, stringResource(R.string.security_method),
                        stringResource(if (google) R.string.security_method_google else R.string.security_method_email))
                    InfoRow(R.drawable.ic_email, stringResource(R.string.personal_email), user?.email.orEmpty())
                    user?.metadata?.lastSignInTimestamp?.let {
                        InfoRow(R.drawable.ic_clock, stringResource(R.string.security_last_signin),
                            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it)))
                    }
                }

                // ---------------------------------------------------- two-step sign-in
                Label(R.string.security_twofa)
                Card { TwoStep() }

                // ---------------------------------------------------- password
                if (password) {
                    Label(R.string.security_password)
                    Card {
                        Text(stringResource(R.string.security_password_body), style = EsType.body.copy(fontSize = 15.sp), color = c.iceDim)
                        Spacer(Modifier.height(14.dp))
                        QuietButton(stringResource(R.string.security_password_action), onClick = {
                            val email = user?.email ?: return@QuietButton
                            FirebaseAuth.getInstance().sendPasswordResetEmail(email).addOnCompleteListener { t ->
                                toast(if (t.isSuccessful) getString(R.string.login_reset_sent, email) else getString(R.string.err_offline))
                            }
                        }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }

    @Composable
    private fun TwoStep() {
        val c = esColors()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(40.dp).background(c.surfaceHigh, EsShapes.md),
            ) { Icon(painterResource(R.drawable.ic_lock), null, tint = c.ice, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.security_app), style = EsType.title, color = c.ice)
                Text(stringResource(R.string.security_app_hint), style = EsType.meta, color = c.iceMute)
            }
            val on = stage == Stage.ON || stage == Stage.CODES || stage == Stage.TURNING_OFF
            Text(
                stringResource(if (on) R.string.security_on else R.string.security_off),
                style = EsType.label,
                color = if (on) c.onRed else c.iceDim,
                modifier = Modifier
                    .background(if (on) c.red else c.surfaceHigh, EsShapes.pill)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        Spacer(Modifier.height(16.dp))

        when (stage) {
            Stage.LOADING -> CircularProgressIndicator(color = c.ice, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
            Stage.ERROR -> {
                Text(stringResource(R.string.err_offline), style = EsType.body.copy(fontSize = 15.sp), color = c.iceDim)
                Spacer(Modifier.height(12.dp))
                QuietButton(stringResource(R.string.action_retry), onClick = ::loadStatus)
            }
            Stage.OFF -> {
                Text(stringResource(R.string.security_off_body), style = EsType.body.copy(fontSize = 15.sp), color = c.iceDim)
                Spacer(Modifier.height(16.dp))
                PrimaryButton(stringResource(R.string.security_setup), enabled = true, loading = busy) { startSetup() }
            }
            Stage.SETUP -> Setup()
            Stage.CODES -> RecoveryCodes()
            Stage.ON -> {
                Text(stringResource(R.string.security_on_body), style = EsType.body.copy(fontSize = 15.sp), color = c.iceDim)
                Spacer(Modifier.height(16.dp))
                QuietButton(stringResource(R.string.security_turn_off), onClick = { code = ""; message = null; stage = Stage.TURNING_OFF },
                    modifier = Modifier.fillMaxWidth())
            }
            Stage.TURNING_OFF -> {
                Text(stringResource(R.string.security_turn_off_body), style = EsType.body.copy(fontSize = 15.sp), color = c.iceDim)
                Spacer(Modifier.height(14.dp))
                CodeField(code, { code = it; message = null }, error = message != null)
                Error()
                Spacer(Modifier.height(14.dp))
                PrimaryButton(stringResource(R.string.security_turn_off), enabled = code.length == 6, loading = busy) { turnOff() }
                TextLink(stringResource(R.string.reauth_cancel)) { stage = Stage.ON }
            }
        }
    }

    @Composable
    private fun Setup() {
        val c = esColors()
        Step(1, R.string.security_step_scan)
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            QrCode(uri, Modifier.widthIn(max = 220.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.security_key_hint), style = EsType.meta, color = c.iceMute)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                secret.chunked(4).joinToString(" "),
                style = EsType.title.copy(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                color = c.ice,
                modifier = Modifier.weight(1f),
            )
            TextLink(stringResource(R.string.security_copy)) { copy(secret) }
        }
        QuietButton(stringResource(R.string.security_open_app), onClick = {
            runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri))) }
                .onFailure { toast(getString(R.string.security_no_app)) }
        }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(20.dp))
        Step(2, R.string.security_step_code)
        Spacer(Modifier.height(12.dp))
        CodeField(code, { code = it; message = null }, error = message != null, autoFocus = false)
        Error()
        Spacer(Modifier.height(14.dp))
        PrimaryButton(stringResource(R.string.security_turn_on), enabled = code.length == 6, loading = busy) { turnOn() }
        TextLink(stringResource(R.string.reauth_cancel)) { stage = Stage.OFF }
    }

    @Composable
    private fun RecoveryCodes() {
        val c = esColors()
        Text(stringResource(R.string.security_codes_title), style = EsType.title, color = c.ice)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.security_codes_body), style = EsType.body.copy(fontSize = 15.sp), color = c.iceDim)
        Spacer(Modifier.height(14.dp))
        codes.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                pair.forEach {
                    Text(
                        it,
                        style = EsType.title.copy(fontFamily = FontFamily.Monospace, fontSize = 15.sp),
                        color = c.ice,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                            .background(c.surfaceHigh, EsShapes.sm)
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        QuietButton(stringResource(R.string.security_copy_all), onClick = { copy(codes.joinToString("\n")) }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        PrimaryButton(stringResource(R.string.security_saved_codes), enabled = true) { codes = emptyList(); stage = Stage.ON }
    }

    @Composable
    private fun Step(n: Int, text: Int) {
        val c = esColors()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(26.dp).background(c.red, EsShapes.pill),
            ) { Text(n.toString(), style = EsType.label, color = c.onRed) }
            Spacer(Modifier.width(10.dp))
            Text(stringResource(text), style = EsType.title.copy(fontSize = 15.sp), color = c.ice)
        }
    }

    @Composable
    private fun Error() {
        message?.let {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(it), style = EsType.meta.copy(fontSize = 14.sp), color = colorError())
        }
    }

    @Composable
    private fun Label(text: Int) {
        val c = esColors()
        Text(stringResource(text), style = EsType.label.copy(fontSize = 13.sp), color = c.iceMute,
            modifier = Modifier.padding(start = 12.dp, top = 12.dp))
    }

    @Composable
    private fun InfoRow(icon: Int, label: String, value: String) {
        val c = esColors()
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
            Icon(painterResource(icon), null, tint = c.iceDim, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = EsType.meta, color = c.iceMute)
                Text(value, style = EsType.body.copy(fontSize = 15.sp), color = c.ice, maxLines = 1)
            }
        }
    }

    // ---------------------------------------------------------------- actions

    private fun startSetup() {
        busy = true
        lifecycleScope.launch {
            val r = SecurityClient.setup()
            busy = false
            if (r is SecurityResult.Setup) {
                secret = r.secret; uri = r.uri; code = ""; message = null; stage = Stage.SETUP
            } else toast(getString(R.string.twofa_failed))
        }
    }

    private fun turnOn() {
        busy = true
        lifecycleScope.launch {
            val r = SecurityClient.enable(code)
            busy = false
            when (r) {
                is SecurityResult.Enabled -> {
                    // This phone just proved it holds the authenticator; don't ask again at next launch.
                    FirebaseAuth.getInstance().currentUser?.uid?.let { TwoFactorTrust.trust(this@SecurityActivity, it) }
                    codes = r.recoveryCodes; secret = ""; uri = ""; stage = Stage.CODES
                }
                SecurityResult.WrongCode -> { message = R.string.twofa_wrong; code = "" }
                SecurityResult.Locked -> message = R.string.twofa_locked
                SecurityResult.Offline -> message = R.string.err_offline
                else -> message = R.string.twofa_failed
            }
        }
    }

    private fun turnOff() {
        busy = true
        lifecycleScope.launch {
            val r = SecurityClient.disable(code)
            busy = false
            when (r) {
                SecurityResult.Disabled -> { code = ""; stage = Stage.OFF; toast(getString(R.string.security_turned_off)) }
                SecurityResult.WrongCode -> { message = R.string.twofa_wrong; code = "" }
                SecurityResult.Locked -> message = R.string.twofa_locked
                SecurityResult.Offline -> message = R.string.err_offline
                else -> message = R.string.twofa_failed
            }
        }
    }

    private fun copy(text: String) {
        (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("East Syria", text))
        toast(getString(R.string.security_copied))
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}
