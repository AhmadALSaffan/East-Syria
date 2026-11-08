package com.example.eastsyria.Login

import android.content.Intent
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.eastsyria.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider

/**
 * Proof that the person changing an account is its owner: they sign in again.
 *
 * Email accounts re-enter their password; Google accounts pick their Google account again (it must
 * be the same one). A successful proof stays valid for [GRACE_MS], so fixing a typo and saving again
 * does not ask twice. Create it in `onCreate` (it registers an activity-result launcher).
 */
class ReauthFlow(
    private val activity: AppCompatActivity,
    private val onMessage: (String) -> Unit,
) {
    private val auth = FirebaseAuth.getInstance()
    private var pending: (() -> Unit)? = null
    private var provedAt = 0L

    private val googleLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)
                val token = account.idToken ?: return@registerForActivityResult fail(R.string.err_google_unavailable)
                reauthenticate(GoogleAuthProvider.getCredential(token, null))
            } catch (e: ApiException) {
                if (e.statusCode != GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                    Log.e(TAG, "Google re-auth failed (${e.statusCode})")
                    fail(R.string.err_google_failed)
                } else {
                    pending = null
                }
            }
        }

    /** Runs [action] once the user has proved it is them (or straight away within the grace period). */
    fun confirm(action: () -> Unit) {
        val user = auth.currentUser ?: return
        if (System.currentTimeMillis() - provedAt < GRACE_MS) {
            action()
            return
        }
        pending = action
        val providers = user.providerData.map { it.providerId }
        when {
            "password" in providers -> askPassword(user.email.orEmpty())
            "google.com" in providers -> askGoogle()
            else -> fail(R.string.reauth_unsupported)
        }
    }

    private fun askPassword(email: String) {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_reauth_password, null)
        val field = view.findViewById<TextInputLayout>(R.id.tilReauthPassword)
        val input = view.findViewById<TextInputEditText>(R.id.etReauthPassword)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        val dialog = MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_EastSyria_Dialog)
            .setTitle(R.string.reauth_title)
            .setMessage(activity.getString(R.string.reauth_password_body, email))
            .setView(view)
            .setNegativeButton(R.string.reauth_cancel) { _, _ -> pending = null }
            .setPositiveButton(R.string.reauth_confirm, null)
            .setOnCancelListener { pending = null }
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val password = input.text?.toString().orEmpty()
                if (password.isEmpty()) {
                    field.error = activity.getString(R.string.err_password_required)
                    return@setOnClickListener
                }
                dialog.dismiss()
                reauthenticate(EmailAuthProvider.getCredential(email, password))
            }
        }
        dialog.show()
    }

    private fun askGoogle() {
        MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_EastSyria_Dialog)
            .setTitle(R.string.reauth_title)
            .setMessage(R.string.reauth_google_body)
            .setNegativeButton(R.string.reauth_cancel) { _, _ -> pending = null }
            .setPositiveButton(R.string.reauth_google_action) { _, _ ->
                val client = GoogleSignIn.getClient(
                    activity,
                    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(activity.getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build(),
                )
                // Signing the client out makes the picker appear instead of silently reusing a session.
                client.signOut().addOnCompleteListener { googleLauncher.launch(client.signInIntent) }
            }
            .setOnCancelListener { pending = null }
            .show()
    }

    private fun reauthenticate(credential: AuthCredential) {
        val user = auth.currentUser ?: return
        user.reauthenticate(credential).addOnCompleteListener(activity) { task ->
            if (task.isSuccessful) {
                provedAt = System.currentTimeMillis()
                val action = pending
                pending = null
                action?.invoke()
            } else {
                Log.e(TAG, "Re-auth failed: ${task.exception?.message}")
                fail(
                    when (task.exception) {
                        is FirebaseAuthInvalidCredentialsException -> R.string.reauth_wrong
                        // A different Google account than the one signed in.
                        is FirebaseAuthInvalidUserException -> R.string.reauth_other_account
                        is FirebaseNetworkException -> R.string.err_offline
                        else -> R.string.reauth_failed
                    }
                )
            }
        }
    }

    private fun fail(res: Int) {
        pending = null
        onMessage(activity.getString(res))
    }

    private companion object {
        const val TAG = "ReauthFlow"
        const val GRACE_MS = 5 * 60 * 1000L
    }
}
