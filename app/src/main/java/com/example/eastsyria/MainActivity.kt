package com.example.eastsyria

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.eastsyria.Login.LoginActivity
import com.example.eastsyria.MainPage.MainPageActivity
import com.example.eastsyria.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.Auth
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        setupClickListeners()
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            navigateToLoginPage()
        }
    }

    private fun navigateToLoginPage() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun setupClickListeners() {
        binding.apply {
            btnStartJourney.setOnClickListener {
                navigateToLoginPage()
            }

            btnExploreNearby.setOnClickListener {
                startMainActivityWithMap()
            }

            fabNightMode.setOnClickListener {
                toggleNightMode()
            }
        }
    }


    private fun startMainActivityWithMap() {
        if (auth.currentUser?.uid!=null) {
            val intent = Intent(this, MainPageActivity::class.java)
            intent.putExtra("OPEN_MAP", true)
            startActivity(intent)
            finish()
        }
        if (auth.currentUser?.uid == null){
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra("OPEN_MAP", true)
            startActivity(intent)
            finish()
        }
    }

    private fun toggleNightMode() {

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