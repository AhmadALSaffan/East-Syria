package com.example.eastsyria.Login

import com.example.eastsyria.Security.TwoFactorActivity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.eastsyria.Admin.MainPageAdminActivity
import com.example.eastsyria.MainPage.MainPageActivity
import com.example.eastsyria.R
import com.example.eastsyria.SignUp.SignUpActivity
import com.example.eastsyria.Ui.lightBarIcons
import com.example.eastsyria.Ui.showSnack
import com.example.eastsyria.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var google: GoogleSignInFlow
    private var isNavigating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        lightBarIcons()
        supportActionBar?.hide()
        auth = FirebaseAuth.getInstance()
        google = GoogleSignInFlow(
            activity = this,
            onBusy = { showLoading(it) },
            onSignedIn = { uid ->
                isNavigating = true
                navigateByRole(uid)
            },
            onMessage = { showToast(it) },
        )
        setupClickListeners()
    }

    override fun onStart() {
        super.onStart()
        if (!isNavigating && auth.currentUser != null) {
            isNavigating = true
            navigateByRole(auth.currentUser!!.uid)
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            btnLogin.setOnClickListener {
                val email = etEmail.text.toString().trim()
                val password = etPassword.text.toString().trim()
                if (validateInput(email, password)) {
                    loginWithEmail(email, password)
                }
            }

            btnGoogleSignIn.setOnClickListener {
                google.start()
            }

            tvForgot.setOnClickListener {
                val email = etEmail.text.toString().trim()
                if (email.isEmpty()) showToast(getString(R.string.login_forgot_need_email))
                else resetPassword(email)
            }

            tvSignUp.setOnClickListener {
                startActivity(Intent(this@LoginActivity, SignUpActivity::class.java))
            }
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        binding.apply {
            if (email.isEmpty()) {
                tilEmail.error = getString(R.string.err_email_required)
                etEmail.requestFocus()
                return false
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.error = getString(R.string.err_email_invalid)
                etEmail.requestFocus()
                return false
            }
            tilEmail.error = null
            if (password.isEmpty()) {
                tilPassword.error = getString(R.string.err_password_required)
                etPassword.requestFocus()
                return false
            }
            if (password.length < 6) {
                tilPassword.error = getString(R.string.err_password_short)
                etPassword.requestFocus()
                return false
            }
            tilPassword.error = null
        }
        return true
    }

    private fun loginWithEmail(email: String, password: String) {
        showLoading(true)
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    if (uid == null) {
                        showLoading(false)
                        showToast(getString(R.string.err_account_missing))
                        return@addOnCompleteListener
                    }
                    isNavigating = true
                    navigateByRole(uid)
                } else {
                    showLoading(false)
                    Log.e("LoginActivity", "Email sign-in failed: ${task.exception?.message}")
                    showToast(friendlySignInError(task.exception))
                }
            }
    }

    /** What went wrong, in words a visitor can act on. The raw message goes to the log. */
    private fun friendlySignInError(error: Exception?): String = when (error) {
        is com.google.firebase.auth.FirebaseAuthInvalidUserException,
        is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException ->
            getString(R.string.err_wrong_password)

        is com.google.firebase.FirebaseNetworkException ->
            getString(R.string.err_offline)

        is com.google.firebase.FirebaseTooManyRequestsException ->
            getString(R.string.err_too_many)

        else -> getString(R.string.err_sign_in)
    }

    private fun navigateByRole(uid: String) {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val cachedRole = prefs.getString("role_$uid", null)

        if (cachedRole != null) {
            showLoading(false)
            routeToScreen(cachedRole)
            return
        }

        showLoading(true)
        FirebaseDatabase.getInstance().getReference("users")
            .child(uid)
            .child("role")
            .get()
            .addOnSuccessListener { snapshot ->
                showLoading(false)
                val role = snapshot.getValue(String::class.java)
                if (role == null) {
                    // Never strand a signed-in user with no role — they would
                    // hit this same wall on every launch with no way out.
                    auth.signOut()
                    isNavigating = false
                    showToast(getString(R.string.err_not_set_up))
                    return@addOnSuccessListener
                }
                prefs.edit().putString("role_$uid", role).apply()
                routeToScreen(role)
            }
            .addOnFailureListener {
                showLoading(false)
                isNavigating = false
                Log.e("LoginActivity", "Failed to get role: ${it.message}")
                showToast(getString(R.string.err_load_account_offline))
            }
    }

    private fun routeToScreen(role: String) {
        when (role) {
            "admin" -> {
                TwoFactorActivity.proceedTo(this, MainPageAdminActivity::class.java)
                finish()
            }
            "user" -> {
                TwoFactorActivity.proceedTo(this, MainPageActivity::class.java)
                finish()
            }
            else -> {
                isNavigating = false
                Log.e("LoginActivity", "Unknown role: $role")
                showToast(getString(R.string.err_load_account))
            }
        }
    }

    private fun resetPassword(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) showToast(getString(R.string.login_reset_sent, email))
                else showToast(getString(R.string.login_reset_failed, task.exception?.message.orEmpty()))
            }
    }

    private fun showLoading(show: Boolean) {
        binding.apply {
            progressBar.visibility = if (show) View.VISIBLE else View.GONE
            btnLogin.text = if (show) "" else getString(R.string.login_action)
            btnLogin.isEnabled = !show
            btnGoogleSignIn.isEnabled = !show
        }
    }

    private fun showToast(message: String) {
        showSnack(binding.root, message, long = true)
    }
}