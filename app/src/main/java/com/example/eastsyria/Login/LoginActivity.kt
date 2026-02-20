package com.example.eastsyria.Login

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.eastsyria.Admin.MainPageAdminActivity
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
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private var isNavigating = false

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

    override fun onStart() {
        super.onStart()
        if (!isNavigating && auth.currentUser != null) {
            isNavigating = true
            navigateByRole(auth.currentUser!!.uid)
        }
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
                if (email.isEmpty()) showToast("Please enter your email")
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
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    if (uid == null) {
                        showLoading(false)
                        showToast("Login failed: user not found")
                        return@addOnCompleteListener
                    }
                    isNavigating = true
                    navigateByRole(uid)
                } else {
                    showLoading(false)
                    showToast("Authentication failed: ${task.exception?.message}")
                }
            }
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
                    showToast("Role not found, contact support")
                    isNavigating = false
                    return@addOnSuccessListener
                }
                prefs.edit().putString("role_$uid", role).apply()
                routeToScreen(role)
            }
            .addOnFailureListener {
                showLoading(false)
                isNavigating = false
                showToast("Failed to get role: ${it.message}")
            }
    }

    private fun routeToScreen(role: String) {
        when (role) {
            "admin" -> {
                startActivity(Intent(this, MainPageAdminActivity::class.java))
                finish()
            }
            "user" -> {
                startActivity(Intent(this, MainPageActivity::class.java))
                finish()
            }
            else -> {
                isNavigating = false
                showToast("Unknown role: $role")
            }
        }
    }

    private fun signInWithGoogle() {
        startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
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
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: run {
                        showLoading(false)
                        showToast("Sign in failed")
                        return@addOnCompleteListener
                    }
                    isNavigating = true
                    navigateByRole(uid)
                } else {
                    showLoading(false)
                    showToast("Authentication failed: ${task.exception?.message}")
                }
            }
    }

    private fun resetPassword(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) showToast("Password reset email sent to $email")
                else showToast("Failed to send reset email: ${task.exception?.message}")
            }
    }

    private fun showLoading(show: Boolean) {
        binding.apply {
            progressBar.visibility = if (show) View.VISIBLE else View.GONE
            btnLogin.text = if (show) "" else "Login"
            btnLogin.isEnabled = !show
            btnGoogleSignIn.isEnabled = !show
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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