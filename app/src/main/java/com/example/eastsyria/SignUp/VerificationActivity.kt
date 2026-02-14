package com.example.eastsyria.SignUp

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.eastsyria.Login.LoginActivity
import com.example.eastsyria.MainPage.MainPageActivity
import com.example.eastsyria.R
import com.example.eastsyria.SignUp.Data.User
import com.example.eastsyria.databinding.ActivityVerificationBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VerificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerificationBinding
    private val auth: FirebaseAuth by lazy { Firebase.auth }
    private val database: FirebaseDatabase by lazy { Firebase.database }
    private val emailService = EmailService()

    private var fullName: String = ""
    private var email: String = ""
    private var phone: String = ""
    private var password: String = ""
    private var verificationCode: String = ""
    private var codeTimestamp: Long = 0L

    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()
        enableEdgeToEdge()
        binding = ActivityVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(
                systemBarsInsets.left,
                systemBarsInsets.top,
                systemBarsInsets.right,
                imeInsets.bottom
            )
            insets
        }





        fullName = intent.getStringExtra("FULL_NAME") ?: ""
        email = intent.getStringExtra("EMAIL") ?: ""
        phone = intent.getStringExtra("PHONE") ?: ""
        password = intent.getStringExtra("PASSWORD") ?: ""
        verificationCode = intent.getStringExtra("VERIFICATION_CODE") ?: ""
        codeTimestamp = intent.getLongExtra("CODE_TIMESTAMP", 0L)

        setupWindowInsets()
        setupOtpInputs()
        setupClickListeners()
        startResendTimer()
    }


    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(
                systemBarsInsets.left,
                systemBarsInsets.top,
                systemBarsInsets.right,
                imeInsets.bottom
            )
            insets
        }
    }

    private fun setupOtpInputs() {
        binding.apply {

            etOtp1.addTextChangedListener(createTextWatcher(etOtp2))
            etOtp2.addTextChangedListener(createTextWatcher(etOtp3))
            etOtp3.addTextChangedListener(createTextWatcher(etOtp4))
            etOtp4.addTextChangedListener(createTextWatcher(etOtp5))
            etOtp5.addTextChangedListener(createTextWatcher(etOtp6))


            etOtp2.setOnKeyListener { _, keyCode, event -> handleBackspace(etOtp1, keyCode, event) }
            etOtp3.setOnKeyListener { _, keyCode, event -> handleBackspace(etOtp2, keyCode, event) }
            etOtp4.setOnKeyListener { _, keyCode, event -> handleBackspace(etOtp3, keyCode, event) }
            etOtp5.setOnKeyListener { _, keyCode, event -> handleBackspace(etOtp4, keyCode, event) }
            etOtp6.setOnKeyListener { _, keyCode, event -> handleBackspace(etOtp5, keyCode, event) }


            etOtp1.requestFocus()
        }
    }

    private fun createTextWatcher(nextField: View): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s?.length == 1) {
                    nextField.requestFocus()
                }
            }
        }
    }

    private fun handleBackspace(previousField: View, keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
            previousField.requestFocus()
            return true
        }
        return false
    }

    private fun setupClickListeners() {
        binding.apply {
            ivBack.setOnClickListener {
                finish()
            }

            btnVerify.setOnClickListener {
                verifyCodeAndCreateAccount()
            }

            tvResendCode.setOnClickListener {
                resendVerificationCode()
            }

            tvLogin.setOnClickListener {
                finish()
            }
        }
    }

    private fun verifyCodeAndCreateAccount() {
        val code = binding.etOtp1.text.toString() +
                binding.etOtp2.text.toString() +
                binding.etOtp3.text.toString() +
                binding.etOtp4.text.toString() +
                binding.etOtp5.text.toString() +
                binding.etOtp6.text.toString()

        if (code.length != 6) {
            showToast("Please enter the 6-digit code")
            return
        }

        showLoading(true)

        // Check if code has expired (10 minutes)
        val expirationTime = codeTimestamp + 600000 // 10 minutes
        if (System.currentTimeMillis() > expirationTime) {
            showLoading(false)
            showToast("Verification code has expired. Please request a new one.")
            return
        }

        // Verify the code
        if (code == verificationCode) {
            // Code is correct, create Firebase Auth account and save to database
            createAccountAndSaveData()
        } else {
            showLoading(false)
            showToast("Invalid verification code. Please try again.")
            clearCodeFields()
        }
    }

    private fun createAccountAndSaveData() {
        // Create Firebase Authentication account
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: ""

                    // Immediately save user data to Realtime Database
                    val userData = mapOf(
                        "uid" to uid,
                        "fullName" to fullName,
                        "email" to email,
                        "phone" to phone,
                        "verified" to true,
                        "createdAt" to System.currentTimeMillis()
                    )

                    val userRef = database.reference.child("users").child(uid)

                    userRef.setValue(userData).addOnSuccessListener {
                        showLoading(false)
                        showToast("Account created successfully!")

                        // Navigate to Explore/Home page
                        navigateToExplore()

                    }.addOnFailureListener { dbError ->
                        showLoading(false)
                        showToast("Account created but failed to save profile: ${dbError.message}")
                        // Even if database save fails, auth account was created
                        navigateToExplore()
                    }

                } else {
                    showLoading(false)
                    showToast("Failed to create account: ${task.exception?.message}")
                }
            }
    }

    private fun resendVerificationCode() {
        binding.tvResendCode.isEnabled = false
        showToast("Sending new code...")

        // Generate new code
        val newCode = emailService.generateVerificationCode()

        // Update local variables
        verificationCode = newCode
        codeTimestamp = System.currentTimeMillis()

        // Send new email
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val emailSent = emailService.sendVerificationCode(email, fullName, newCode)

                withContext(Dispatchers.Main) {
                    if (emailSent) {
                        showToast("New verification code sent to $email")
                        clearCodeFields()
                        startResendTimer()
                    } else {
                        showToast("Failed to send email. Please try again.")
                        binding.tvResendCode.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Error: ${e.message}")
                    binding.tvResendCode.isEnabled = true
                }
            }
        }
    }

    private fun startResendTimer() {
        countDownTimer?.cancel()

        binding.tvResendCode.isEnabled = false

        countDownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                binding.tvResendCode.text = "Resend in ${seconds}s"
            }

            override fun onFinish() {
                binding.tvResendCode.text = "Resend Code"
                binding.tvResendCode.isEnabled = true
            }
        }.start()
    }

    private fun clearCodeFields() {
        binding.apply {
            etOtp1.text?.clear()
            etOtp2.text?.clear()
            etOtp3.text?.clear()
            etOtp4.text?.clear()
            etOtp5.text?.clear()
            etOtp6.text?.clear()
            etOtp1.requestFocus()
        }
    }

    private fun navigateToExplore() {
        val intent = Intent(this, MainPageActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showLoading(show: Boolean) {
        binding.apply {
            if (show) {
                progressBar.visibility = View.VISIBLE
                btnVerify.text = ""
                btnVerify.isEnabled = false
            } else {
                progressBar.visibility = View.GONE
                btnVerify.text = "Verify and Continue"
                btnVerify.isEnabled = true
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
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