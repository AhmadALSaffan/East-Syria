package com.example.eastsyria.Settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.eastsyria.Ui.padBottomForInsets
import com.example.eastsyria.Ui.warmEdgeToEdge
import com.example.eastsyria.databinding.ActivityPrivacyBinding

/** The privacy agreement, in plain words. */
class PrivacyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        warmEdgeToEdge()
        val binding = ActivityPrivacyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.scroll.padBottomForInsets(24)
        binding.header.onBack { finish() }
    }
}
