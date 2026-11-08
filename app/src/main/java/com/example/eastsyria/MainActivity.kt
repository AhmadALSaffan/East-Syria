package com.example.eastsyria

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.eastsyria.Admin.MainPageAdminActivity
import com.example.eastsyria.Login.GoogleSignInFlow
import com.example.eastsyria.Login.LoginActivity
import com.example.eastsyria.MainPage.MainPageActivity
import com.example.eastsyria.Onboarding.OnboardingScreen
import com.example.eastsyria.Security.TwoFactorActivity
import com.example.eastsyria.SignUp.SignUpActivity
import com.example.eastsyria.Ui.AppLanguage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

/**
 * The launcher. A signed-in user is routed straight to their home (by role). Everyone else sees
 * the onboarding once: four animated scenes of the app, then Google sign-in as the main way in,
 * with email as the alternative. After that first run it forwards to the login screen.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var google: GoogleSignInFlow
    private var googleInFlight by mutableStateOf(false)
    private var routing = false

    private val prefs by lazy { getSharedPreferences(PREFS, MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Bar icons follow the day/night colours.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        auth = FirebaseAuth.getInstance()

        google = GoogleSignInFlow(
            activity = this,
            onBusy = { googleInFlight = it },
            onSignedIn = {
                markOnboarded()
                navigateToMain()
            },
            onMessage = { Toast.makeText(this, it, Toast.LENGTH_LONG).show() },
        )

        setContent {
            OnboardingScreen(
                googleInFlight = googleInFlight,
                language = AppLanguage.current(),
                onLanguage = { AppLanguage.set(it) },
                onGoogle = {
                    if (auth.currentUser != null) {
                        markOnboarded()
                        navigateToMain()
                    } else {
                        google.start()
                    }
                },
                onEmail = {
                    markOnboarded()
                    startActivity(Intent(this, SignUpActivity::class.java))
                    finish()
                },
                onSignIn = {
                    markOnboarded()
                    goToLogin()
                },
                onBrowseMap = {
                    markOnboarded()
                    startActivityWithMap()
                },
            )
        }
    }

    override fun onStart() {
        super.onStart()
        // Settings → "Watch the intro again" opens the onboarding even for a signed-in user.
        if (routing || googleInFlight || intent.getBooleanExtra(EXTRA_REPLAY, false)) return
        if (auth.currentUser != null) {
            navigateToMain()
        } else if (prefs.getBoolean(KEY_ONBOARDED, false)) {
            goToLogin()
        }
    }

    private fun markOnboarded() {
        prefs.edit().putBoolean(KEY_ONBOARDED, true).apply()
    }

    // -------------------------------------------------------------- routing

    private fun navigateToMain() {
        val uid = auth.currentUser?.uid ?: return goToLogin()
        routing = true

        val cachedRole = prefs.getString("role_$uid", null)
        if (cachedRole != null) {
            navigateByRole(cachedRole)
            refreshRoleInBackground(uid)
            return
        }

        FirebaseDatabase.getInstance().getReference("users")
            .child(uid)
            .child("role")
            .get()
            .addOnSuccessListener { snapshot ->
                val role = snapshot.getValue(String::class.java) ?: return@addOnSuccessListener goToLogin()
                prefs.edit().putString("role_$uid", role).apply()
                navigateByRole(role)
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.err_load_account), Toast.LENGTH_LONG).show()
                goToLogin()
            }
    }

    private fun navigateByRole(role: String) {
        // The authenticator-app step (if the account has one) comes before the home screen.
        when (role) {
            "admin" -> TwoFactorActivity.proceedTo(this, MainPageAdminActivity::class.java)
            "user" -> TwoFactorActivity.proceedTo(this, MainPageActivity::class.java)
            else -> return goToLogin()
        }
        finish()
    }

    private fun refreshRoleInBackground(uid: String) {
        FirebaseDatabase.getInstance().getReference("users")
            .child(uid)
            .child("role")
            .get()
            .addOnSuccessListener { snapshot ->
                val role = snapshot.getValue(String::class.java) ?: return@addOnSuccessListener
                prefs.edit().putString("role_$uid", role).apply()
            }
    }

    private fun goToLogin() {
        routing = true
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun startActivityWithMap() {
        val destination = if (auth.currentUser != null) {
            Intent(this, MainPageActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }
        destination.putExtra("OPEN_MAP", true)
        startActivity(destination)
        finish()
    }

    companion object {
        const val EXTRA_REPLAY = "replay_onboarding"
        private const val PREFS = "user_prefs"
        private const val KEY_ONBOARDED = "onboarding_seen"
    }
}
