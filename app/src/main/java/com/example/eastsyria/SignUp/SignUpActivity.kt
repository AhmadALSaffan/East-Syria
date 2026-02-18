package com.example.eastsyria.SignUp

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.eastsyria.R
import com.example.eastsyria.databinding.ActivitySignUpBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private val auth: FirebaseAuth by lazy { Firebase.auth }
    private val database: FirebaseDatabase by lazy { Firebase.database }
    private val emailService = EmailService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()
        enableEdgeToEdge()
        binding = ActivitySignUpBinding.inflate(layoutInflater)
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

        setupFocusListeners()
        setupWindowInsets()
        setupClickListeners()
        setupCityDropdown()
    }

    private fun setupCityDropdown() {
        val cities = resources.getStringArray(R.array.syria_cities)
        val adapter = ArrayAdapter(this, R.layout.dropdown_item, cities)
        binding.actvCity.setAdapter(adapter)
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

                if (imeInsets.bottom > 0) {
                    currentFocus?.let { focusedView ->
                        binding.root.post {
                            scrollToView(focusedView)
                        }
                    }
                }

                insets
            }
        }

        private fun setupFocusListeners() {
            binding.apply {
                etFullName.setOnFocusChangeListener { view, hasFocus ->
                    if (hasFocus) scrollToView(tilFullName)
                }

                etEmail.setOnFocusChangeListener { view, hasFocus ->
                    if (hasFocus) scrollToView(tilEmail)
                }

                etPhone.setOnFocusChangeListener { view, hasFocus ->
                    if (hasFocus) scrollToView(tilPhone)
                }

                etPassword.setOnFocusChangeListener { view, hasFocus ->
                    if (hasFocus) scrollToView(tilPassword)
                }

                etFullName.setOnEditorActionListener { _, _, _ ->
                    etEmail.requestFocus()
                    true
                }

                etEmail.setOnEditorActionListener { _, _, _ ->
                    etPhone.requestFocus()
                    true
                }

                etPhone.setOnEditorActionListener { _, _, _ ->
                    etPassword.requestFocus()
                    true
                }

                etPassword.setOnEditorActionListener { _, _, _ ->
                    btnCreateAccount.performClick()
                    true
                }
            }
        }

        private fun scrollToView(view: View) {
            binding.root.post {
                val scrollViewHeight = binding.root.height
                val viewTop = view.top
                val viewHeight = view.height
                val scrollTo = viewTop - (scrollViewHeight / 2) + (viewHeight / 2)
                binding.root.smoothScrollTo(0, scrollTo.coerceAtLeast(0))
            }
        }

        private fun setupClickListeners() {
            binding.apply {
                ivBack.setOnClickListener {
                    finish()
                }

                btnCreateAccount.setOnClickListener {
                    val fullName = etFullName.text.toString().trim()
                    val email = etEmail.text.toString().trim()
                    val phone = etPhone.text.toString().trim()
                    val password = etPassword.text.toString().trim()
                    val city = actvCity.text.toString().trim()

                    if (validateInput(fullName, email, phone, password,city)) {
                        sendVerificationCode(fullName, email, phone, password,city)
                    }
                }

                tvLogin.setOnClickListener {
                    finish()
                }
            }
        }

        private fun validateInput(
            fullName: String,
            email: String,
            phone: String,
            password: String,
            city:String
        ): Boolean {
            binding.apply {
                if (fullName.isEmpty()) {
                    tilFullName.error = "Full name is required"
                    etFullName.requestFocus()
                    scrollToView(tilFullName)
                    return false
                }
                if (fullName.length < 3) {
                    tilFullName.error = "Name must be at least 3 characters"
                    etFullName.requestFocus()
                    scrollToView(tilFullName)
                    return false
                }
                tilFullName.error = null

                if (email.isEmpty()) {
                    tilEmail.error = "Email is required"
                    etEmail.requestFocus()
                    scrollToView(tilEmail)
                    return false
                }
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    tilEmail.error = "Please enter a valid email"
                    etEmail.requestFocus()
                    scrollToView(tilEmail)
                    return false
                }
                tilEmail.error = null

                if (phone.isEmpty()) {
                    tilPhone.error = "Phone number is required"
                    etPhone.requestFocus()
                    scrollToView(tilPhone)
                    return false
                }
                if (phone.length < 10) {
                    tilPhone.error = "Please enter a valid phone number"
                    etPhone.requestFocus()
                    scrollToView(tilPhone)
                    return false
                }
                tilPhone.error = null

                if (password.isEmpty()) {
                    tilPassword.error = "Password is required"
                    etPassword.requestFocus()
                    scrollToView(tilPassword)
                    return false
                }
                if (password.length < 6) {
                    tilPassword.error = "Password must be at least 6 characters"
                    etPassword.requestFocus()
                    scrollToView(tilPassword)
                    return false
                }
                tilPassword.error = null
                if (city.isEmpty()){
                    tilCity.error = "Please select a city"
                    return false
                }
            }

            return true
        }

        private fun sendVerificationCode(
            fullName: String,
            email: String,
            phone: String,
            password: String,
            city:String
        ) {
            showLoading(true)

            val verificationCode = emailService.generateVerificationCode()


            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val emailSent =
                        emailService.sendVerificationCode(email, fullName, verificationCode)

                    withContext(Dispatchers.Main) {
                        showLoading(false)

                        if (emailSent) {
                            showToast("Verification code sent to $email")
                            navigateToVerification(
                                fullName,
                                email,
                                phone,
                                password,
                                city,
                                verificationCode
                            )
                        } else {
                            showToast("Failed to send verification email. Please try again.")
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        showToast("Error: ${e.message}")
                    }
                }
            }
        }

    private fun navigateToVerification(
            fullName: String,
            email: String,
            phone: String,
            password: String,
            city: String,
            verificationCode: String
    ) {
            val intent = Intent(this, VerificationActivity::class.java).apply {
                putExtra("FULL_NAME", fullName)
                putExtra("EMAIL", email)
                putExtra("PHONE", phone)
                putExtra("PASSWORD", password)
                putExtra("CITY",city)
                putExtra("VERIFICATION_CODE", verificationCode)
                putExtra("CODE_TIMESTAMP", System.currentTimeMillis())
            }
            startActivity(intent)
            finish()
    }

    private fun showLoading(show: Boolean) {
        binding.apply {
            if (show) {
                progressBar.visibility = View.VISIBLE
                btnCreateAccount.text = ""
                btnCreateAccount.isEnabled = false
            } else {
                progressBar.visibility = View.GONE
                btnCreateAccount.text = "Create Account"
                btnCreateAccount.isEnabled = true
            }
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