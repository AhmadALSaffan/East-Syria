package com.example.eastsyria.Settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.eastsyria.MainActivity
import com.example.eastsyria.Profile.PersonalDetails.PersonalDetailsActivity
import com.example.eastsyria.R
import com.example.eastsyria.Ui.AppLanguage
import com.example.eastsyria.Ui.padBottomForInsets
import com.example.eastsyria.Ui.showSnack
import com.example.eastsyria.Ui.warmEdgeToEdge
import com.example.eastsyria.databinding.ActivitySettingsBinding
import java.util.concurrent.Executors

/** Language, the app's housekeeping (image cache, replay the intro), and account shortcuts. */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var reauth: com.example.eastsyria.Login.ReauthFlow

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        warmEdgeToEdge()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.scroll.padBottomForInsets(24)
        binding.header.onBack { finish() }
        reauth = com.example.eastsyria.Login.ReauthFlow(this) { message -> showSnack(binding.root, message, long = true) }

        showLanguage()
        binding.rowEnglish.setOnClickListener { AppLanguage.set(AppLanguage.ENGLISH) }
        binding.rowArabic.setOnClickListener { AppLanguage.set(AppLanguage.ARABIC) }

        binding.rowClearCache.setOnClickListener { clearImageCache() }
        binding.rowReplayIntro.setOnClickListener {
            getSharedPreferences("user_prefs", MODE_PRIVATE).edit().remove("onboarding_seen").apply()
            startActivity(
                Intent(this, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_REPLAY, true)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        }
        binding.rowSecurity.setOnClickListener {
            reauth.confirm { startActivity(Intent(this, com.example.eastsyria.Security.SecurityActivity::class.java)) }
        }
        binding.rowPersonal.setOnClickListener {
            reauth.confirm { startActivity(Intent(this, PersonalDetailsActivity::class.java)) }
        }
        binding.rowPrivacy.setOnClickListener {
            startActivity(Intent(this, PrivacyActivity::class.java))
        }

        binding.tvVersion.text = versionLabel()
    }

    private fun showLanguage() {
        val arabic = AppLanguage.current() == AppLanguage.ARABIC
        binding.checkEnglish.visibility = if (arabic) View.INVISIBLE else View.VISIBLE
        binding.checkArabic.visibility = if (arabic) View.VISIBLE else View.INVISIBLE
        binding.rowEnglish.isSelected = !arabic
        binding.rowArabic.isSelected = arabic
    }

    /** Photos come back from the network on the next view; the catalog itself is untouched. */
    private fun clearImageCache() {
        Glide.get(this).clearMemory()
        Executors.newSingleThreadExecutor().execute {
            Glide.get(applicationContext).clearDiskCache()
            runOnUiThread { showSnack(binding.root, getString(R.string.settings_cache_cleared)) }
        }
    }
}

/** "Version 1.0", or blank when the package can't be read. */
internal fun AppCompatActivity.versionLabel(): String {
    val version = runCatching { packageManager.getPackageInfo(packageName, 0).versionName }
        .getOrNull().orEmpty()
    return if (version.isBlank()) "" else getString(R.string.profile_version, version)
}
