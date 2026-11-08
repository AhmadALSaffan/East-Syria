package com.example.eastsyria.Login

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import com.example.eastsyria.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase

/**
 * "Continue with Google", shared by the onboarding and the login screen.
 *
 * Create it in `onCreate` (it registers an activity-result launcher). It shows Google's account
 * picker, signs in to Firebase, and makes sure a first-time Google user gets a `users/{uid}`
 * record with the `user` role, the same shape email sign-up writes in VerificationActivity.
 */
class GoogleSignInFlow(
    private val activity: ComponentActivity,
    private val onBusy: (Boolean) -> Unit,
    private val onSignedIn: (uid: String) -> Unit,
    private val onMessage: (String) -> Unit,
) {
    private val auth = FirebaseAuth.getInstance()

    private val client: GoogleSignInClient = GoogleSignIn.getClient(
        activity,
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(activity.getString(R.string.default_web_client_id))
            .requestEmail()
            .build(),
    )

    private val launcher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleResult(result.data)
        }

    fun start() {
        onBusy(true)
        // Sign the Google client out first. Otherwise it silently reuses the last account and
        // the picker never appears, so nobody can switch accounts.
        client.signOut().addOnCompleteListener { launcher.launch(client.signInIntent) }
    }

    private fun handleResult(data: Intent?) {
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken == null) {
                Log.e(TAG, "Google returned no ID token; check this build's SHA-1 in Firebase")
                fail(activity.getString(R.string.err_google_unavailable))
                return
            }
            signInToFirebase(idToken, account)
        } catch (e: ApiException) {
            when (e.statusCode) {
                // Backing out of the picker is not an error worth reporting.
                GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> onBusy(false)
                GoogleSignInStatusCodes.NETWORK_ERROR -> fail(activity.getString(R.string.err_offline))
                GoogleSignInStatusCodes.DEVELOPER_ERROR -> {
                    Log.e(TAG, "Google sign-in not configured for this build; add its SHA-1 to Firebase")
                    fail(activity.getString(R.string.err_google_unavailable))
                }
                else -> {
                    Log.e(TAG, "Google sign-in failed (code ${e.statusCode})")
                    fail(activity.getString(R.string.err_google_failed))
                }
            }
        }
    }

    private fun signInToFirebase(idToken: String, account: GoogleSignInAccount) {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
            .addOnCompleteListener(activity) { task ->
                val uid = auth.currentUser?.uid
                if (!task.isSuccessful || uid == null) {
                    Log.e(TAG, "Google credential sign-in failed: ${task.exception?.message}")
                    fail(
                        if (task.exception is FirebaseNetworkException) activity.getString(R.string.err_offline)
                        else activity.getString(R.string.err_sign_in),
                    )
                    return@addOnCompleteListener
                }
                ensureUserRecord(uid, account)
            }
    }

    /**
     * A first-time Google user has a Firebase account but no users/{uid} node, so the role lookup
     * would return null and strand them on every launch. Write the record before routing.
     */
    private fun ensureUserRecord(uid: String, account: GoogleSignInAccount) {
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(uid)
        userRef.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists() && snapshot.child("role").exists()) {
                    onBusy(false)
                    onSignedIn(uid)
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
                    "createdAt" to System.currentTimeMillis(),
                )
                userRef.setValue(userData)
                    .addOnSuccessListener {
                        onBusy(false)
                        onSignedIn(uid)
                    }
                    .addOnFailureListener { error ->
                        auth.signOut()
                        Log.e(TAG, "Account setup failed: ${error.message}")
                        fail(activity.getString(R.string.err_account_setup))
                    }
            }
            .addOnFailureListener { error ->
                auth.signOut()
                Log.e(TAG, "Database unreachable: ${error.message}")
                fail(activity.getString(R.string.err_server))
            }
    }

    private fun fail(message: String) {
        onBusy(false)
        onMessage(message)
    }

    private companion object {
        const val TAG = "GoogleSignInFlow"
    }
}
