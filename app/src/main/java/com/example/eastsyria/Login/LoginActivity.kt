package com.example.eastsyria.Login

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.eastsyria.MainActivity
import com.example.eastsyria.MainPage.MainPageActivity
import com.example.eastsyria.R
import com.example.eastsyria.SignUp.SignUpActivity
import com.example.eastsyria.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    companion object {
        private const val RC_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()

        setupGoogleSignIn()

        setupClickListeners()

    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
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
                signInWithGoogle()
            }

            tvForgot.setOnClickListener {
                val email = etEmail.text.toString().trim()
                if (email.isEmpty()) {
                    showToast("Please enter your email")
                } else {
                    resetPassword(email)
                }
            }

            tvSignUp.setOnClickListener {
                navigateToSignUp()
            }
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        binding.apply {
            if (email.isEmpty()) {
                tilEmail.error = "Email is required"
                etEmail.requestFocus()
                return false
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.error = "Please enter a valid email"
                etEmail.requestFocus()
                return false
            }

            tilEmail.error = null

            if (password.isEmpty()) {
                tilPassword.error = "Password is required"
                etPassword.requestFocus()
                return false
            }

            if (password.length < 6) {
                tilPassword.error = "Password must be at least 6 characters"
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
                showLoading(false)

                if (task.isSuccessful) {
                    showToast("Login successful!")
                    navigateToMain()
                } else {
                    showToast("Authentication failed: ${task.exception?.message}")
                }
            }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleGoogleSignInResult(task)
        }
    }

    private fun handleGoogleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            showToast("Google sign in failed: ${e.message}")
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        showLoading(true)

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                showLoading(false)

                if (task.isSuccessful) {
                    showToast("Google sign in successful!")
                    navigateToMain()
                } else {
                    showToast("Authentication failed: ${task.exception?.message}")
                }
            }
    }

    private fun resetPassword(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showToast("Password reset email sent to $email")
                } else {
                    showToast("Failed to send reset email: ${task.exception?.message}")
                }
            }
    }

    private fun showLoading(show: Boolean) {
        binding.apply {
            if (show) {
                progressBar.visibility = View.VISIBLE
                btnLogin.text = ""
                btnLogin.isEnabled = false
                btnGoogleSignIn.isEnabled = false
            } else {
                progressBar.visibility = View.GONE
                btnLogin.text = "Login"
                btnLogin.isEnabled = true
                btnGoogleSignIn.isEnabled = true
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainPageActivity::class.java))
        finish()
    }

    private fun navigateToSignUp() {
        startActivity(Intent(this, SignUpActivity::class.java))
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            navigateToMain()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}