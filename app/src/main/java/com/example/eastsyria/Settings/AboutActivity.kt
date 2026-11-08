package com.example.eastsyria.Settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.eastsyria.Ui.padBottomForInsets
import com.example.eastsyria.Ui.warmEdgeToEdge
import com.example.eastsyria.databinding.ActivityAboutBinding

/** What East Syria is, what is in it, and who made it. */
class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        warmEdgeToEdge()
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.scroll.padBottomForInsets(24)
        binding.header.onBack { finish() }

        binding.rowGithub.setOnClickListener {
            runCatching {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/AhmadALSaffan/East-Syria")))
            }
        }
        binding.tvVersion.text = versionLabel()
    }
}
