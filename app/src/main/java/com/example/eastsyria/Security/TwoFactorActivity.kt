package com.example.eastsyria.Security

import android.content.Intent
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.eastsyria.Login.LoginActivity
import com.example.eastsyria.Onboarding.EsShapes
import com.example.eastsyria.Onboarding.EsType
import com.example.eastsyria.Onboarding.esColors
import com.example.eastsyria.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * The second step after signing in. It first asks the server whether this account has an
 * authenticator app; if not, it continues straight away. If it does, the user types the 6-digit
 * code (or a recovery code) before the home screen opens.
 */
class TwoFactorActivity : AppCompatActivity() {

    private var checking by mutableStateOf(true)
    private var code by mutableStateOf("")
    private var recovery by mutableStateOf("")
    private var useRecovery by mutableStateOf(false)
    private var busy by mutableStateOf(false)
    private var message by mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return signOut()
        if (TwoFactorTrust.isTrusted(this, uid)) return proceed()

        lifecycleScope.launch {
            when (val r = SecurityClient.status()) {
                is SecurityResult.Status -> if (r.enabled) checking = false else {
                    TwoFactorTrust.trust(this@TwoFactorActivity, uid); proceed()
                }
                SecurityResult.SignedOut -> signOut()
                else -> { checking = false; message = R.string.err_offline }
            }
        }

        setContent {
            BackHandler { signOut() }
            Screen()
        }
    }

    @androidx.compose.runtime.Composable
    private fun Screen() {
        val c = esColors()
        Box(
            Modifier
                .fillMaxSize()
                .background(c.canvas)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding(),
        ) {
            if (checking) {
                CircularProgressIndicator(color = c.ice, strokeWidth = 2.dp, modifier = Modifier.align(Alignment.Center).size(28.dp))
                return@Box
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(56.dp).background(c.surfaceHigh, CircleShape),
                ) {
                    Icon(painterResource(R.drawable.ic_lock), null, tint = c.ice, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.twofa_title), style = EsType.display.copy(fontSize = 30.sp, lineHeight = 36.sp), color = c.ice)
                Text(
                    stringResource(if (useRecovery) R.string.twofa_body_recovery else R.string.twofa_body),
                    style = EsType.body, color = c.iceDim,
                )
                Spacer(Modifier.height(8.dp))
                if (useRecovery) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(c.surface, EsShapes.md)
                            .border(1.dp, if (message != null) colorError() else c.line, EsShapes.md)
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                    ) {
                        if (recovery.isEmpty()) Text("XXXX-XXXX", style = EsType.title, color = c.iceMute)
                        BasicTextField(
                            value = recovery,
                            onValueChange = { recovery = it.uppercase().take(9); message = null },
                            textStyle = EsType.title.copy(color = c.ice, letterSpacing = 2.sp),
                            cursorBrush = SolidColor(c.ice),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    CodeField(code, { code = it; message = null; if (it.length == 6) submit() }, error = message != null)
                }
                message?.let { Text(stringResource(it), style = EsType.meta.copy(fontSize = 14.sp), color = colorError()) }
                Spacer(Modifier.height(6.dp))
                PrimaryButton(
                    stringResource(R.string.twofa_verify),
                    enabled = if (useRecovery) recovery.length >= 8 else code.length == 6,
                    loading = busy,
                    onClick = ::submit,
                )
                TextLink(stringResource(if (useRecovery) R.string.twofa_use_app else R.string.twofa_use_recovery)) {
                    useRecovery = !useRecovery; message = null
                }
                TextLink(stringResource(R.string.profile_sign_out)) { signOut() }
            }
        }
    }

    private fun submit() {
        if (busy) return
        val value = if (useRecovery) recovery else code
        busy = true
        lifecycleScope.launch {
            val r = SecurityClient.verify(value)
            busy = false
            when (r) {
                is SecurityResult.Verified -> {
                    FirebaseAuth.getInstance().currentUser?.uid?.let { TwoFactorTrust.trust(this@TwoFactorActivity, it) }
                    proceed()
                }
                SecurityResult.WrongCode -> { message = R.string.twofa_wrong; code = "" }
                SecurityResult.Locked -> { message = R.string.twofa_locked; code = "" }
                SecurityResult.SignedOut -> signOut()
                SecurityResult.Offline -> message = R.string.err_offline
                else -> message = R.string.twofa_failed
            }
        }
    }

    private fun proceed() {
        val next = intent.getStringExtra(EXTRA_NEXT)?.let { runCatching { Class.forName(it) }.getOrNull() }
        if (next != null) startActivity(Intent(this, next).putExtras(intent.getBundleExtra(EXTRA_NEXT_EXTRAS) ?: Bundle()))
        finish()
    }

    private fun signOut() {
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    companion object {
        const val EXTRA_NEXT = "next_activity"
        const val EXTRA_NEXT_EXTRAS = "next_extras"

        /** Opens [target] now if this phone already passed the second step, otherwise asks first. */
        fun proceedTo(activity: android.app.Activity, target: Class<*>, extras: Bundle? = null) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null && TwoFactorTrust.isTrusted(activity, uid)) {
                activity.startActivity(Intent(activity, target).apply { extras?.let { putExtras(it) } })
            } else {
                activity.startActivity(
                    Intent(activity, TwoFactorActivity::class.java)
                        .putExtra(EXTRA_NEXT, target.name)
                        .putExtra(EXTRA_NEXT_EXTRAS, extras),
                )
            }
        }
    }
}
