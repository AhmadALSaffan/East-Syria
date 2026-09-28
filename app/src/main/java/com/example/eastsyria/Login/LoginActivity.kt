package com.example.eastsyria.Login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.eastsyria.Admin.MainPageAdminActivity
import com.example.eastsyria.MainPage.MainPageActivity
import com.example.eastsyria.R
import com.example.eastsyria.SignUp.SignUpActivity
import com.example.eastsyria.Ui.lightBarIcons
import com.example.eastsyria.Ui.showSnack
import com.example.eastsyria.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
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

    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        lightBarIcons()
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

        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleGoogleSignInResult(GoogleSignIn.getSignedInAccountFromIntent(result.data))
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
                signInWithGoogle()
            }

            tvForgot.setOnClickListener {
                val email = etEmail.text.toString().trim()
                if (email.isEmpty()) showToast("Enter your email first, then tap Forgot.")
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
                        showToast("We couldn't find that account. Try again.")
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
            "That email and password don't match. Check them and try again."

        is com.google.firebase.FirebaseNetworkException ->
            "No connection. Check your network and try again."

        is com.google.firebase.FirebaseTooManyRequestsException ->
            "Too many attempts. Wait a moment and try again."

        else -> "We couldn't sign you in. Try again in a moment."
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
                    showToast("Your account isn't set up yet. Please sign up first.")
                    return@addOnSuccessListener
                }
                prefs.edit().putString("role_$uid", role).apply()
                routeToScreen(role)
            }
            .addOnFailureListener {
                showLoading(false)
                isNavigating = false
                Log.e("LoginActivity", "Failed to get role: ${it.message}")
                showToast("We couldn't load your account. Check your connection and try again.")
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
                Log.e("LoginActivity", "Unknown role: $role")
                showToast("We couldn't load your account. Try again in a moment.")
            }
        }
    }

    private fun signInWithGoogle() {
        // Sign the Google client out first. Otherwise it silently reuses the
        // last account and the picker never appears, so nobody can switch.
        googleSignInClient.signOut().addOnCompleteListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    private fun handleGoogleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken == null) {
                Log.e("LoginActivity", "Google returned no ID token; check this build's SHA-1 in Firebase")
                showToast("Google sign-in isn't available right now. Try your email instead.")
                return
            }
            firebaseAuthWithGoogle(idToken, account)
        } catch (e: ApiException) {
            when (e.statusCode) {
                // Backing out of the picker is not an error worth reporting.
                GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> Unit
                GoogleSignInStatusCodes.NETWORK_ERROR ->
                    showToast("No connection. Check your network and try again.")
                GoogleSignInStatusCodes.DEVELOPER_ERROR -> {
                    Log.e("LoginActivity", "Google sign-in not configured for this build; add its SHA-1 to Firebase")
                    showToast("Google sign-in isn't available right now. Try your email instead.")
                }
                else -> {
                    Log.e("LoginActivity", "Google sign-in failed (code ${e.statusCode})")
                    showToast("Google sign-in didn't work. Try again or use your email.")
                }
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String, account: GoogleSignInAccount) {
        showLoading(true)
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (!task.isSuccessful) {
                    showLoading(false)
                    Log.e("LoginActivity", "Google credential sign-in failed: ${task.exception?.message}")
                    showToast(friendlySignInError(task.exception))
                    return@addOnCompleteListener
                }
                val uid = auth.currentUser?.uid ?: run {
                    showLoading(false)
                    showToast("We couldn't sign you in. Try again in a moment.")
                    return@addOnCompleteListener
                }
                ensureUserRecord(uid, account)
            }
    }

    /**
     * A first-time Google user gets a Firebase account but no users/{uid} node,
     * so the role lookup returns null and the app dead-ends — permanently, since
     * they stay signed in and hit the same wall on every launch. Email sign-up
     * writes this record in VerificationActivity; the Google path never did.
     *
     * Same field shape as VerificationActivity.createAccountAndSaveData so both
     * routes produce identical user records.
     */
    private fun ensureUserRecord(uid: String, account: GoogleSignInAccount) {
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(uid)

        userRef.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists() && snapshot.child("role").exists()) {
                    isNavigating = true
                    navigateByRole(uid)
                    return@addOnSuccessListener
                }

                val userData = mapOf(
                    "uid" to uid,
                    "fullName" to (account.displayName ?: ""),
                    "email" to (account.email ?: ""),
                    "phone" to "",
                    // Google has already verified the address.
                    "verified" to true,
                    "city" to "",
                    "role" to "user",
                    "createdAt" to System.currentTimeMillis()
                )

                userRef.setValue(userData)
                    .addOnSuccessListener {
                        isNavigating = true
                        navigateByRole(uid)
                    }
                    .addOnFailureListener { error ->
                        showLoading(false)
                        auth.signOut()
                        Log.e("LoginActivity", "Account setup failed: ${error.message}")
                        showToast("We couldn't finish setting up your account. Try again.")
                    }
            }
            .addOnFailureListener { error ->
                showLoading(false)
                auth.signOut()
                Log.e("LoginActivity", "Database unreachable: ${error.message}")
                showToast("We couldn't reach the server. Check your connection and try again.")
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
            btnLogin.text = if (show) "" else getString(R.string.login_action)
            btnLogin.isEnabled = !show
            btnGoogleSignIn.isEnabled = !show
        }
    }

    private fun showToast(message: String) {
        showSnack(binding.root, message, long = true)
    }
}