package com.example.eastsyria

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import com.example.eastsyria.Ui.lightBarIcons
import com.example.eastsyria.Ui.showSnack
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.viewpager2.widget.ViewPager2
import com.example.eastsyria.Admin.MainPageAdminActivity
import com.example.eastsyria.Login.LoginActivity
import com.example.eastsyria.MainPage.MainPageActivity
import com.example.eastsyria.Onboarding.OnboardingAdapter
import com.example.eastsyria.Onboarding.approachStops
import com.example.eastsyria.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlin.math.abs

/**
 * Hosts The Approach: the four-stop onboarding flow.
 *
 * Routing is unchanged from before — a signed-in user never sees this screen,
 * and role still decides which home they land on. What is new is that the
 * onboarding is first-run only; once walked, this activity forwards to login
 * and finishes itself so system Back exits rather than looping.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    private val prefs by lazy { getSharedPreferences(PREFS, MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lightBarIcons()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Insets land on the chrome, not the root, so the artwork keeps
        // bleeding under the status bar.
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.topBar.updatePadding(top = bars.top)
            binding.bottomChrome.updatePadding(bottom = bars.bottom + dp(20))
            insets
        }

        auth = FirebaseAuth.getInstance()

        setupPager()
        setupClickListeners()
        setupBackNavigation()
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) {
            navigateToMain()
        } else if (prefs.getBoolean(KEY_ONBOARDED, false)) {
            // Already walked the approach. Straight to the door.
            goToLogin(finishSelf = true)
        }
    }

    // ---------------------------------------------------------------- pager

    private fun setupPager() = with(binding) {
        pager.adapter = OnboardingAdapter()
        pager.offscreenPageLimit = 1

        // Depth: the photograph trails the swipe and the drawn art runs ahead of
        // it, so a stop feels like a place you pass rather than a slide you flip.
        pager.setPageTransformer { page, position ->
            val width = page.width.toFloat()
            page.findViewById<View>(R.id.layerPhoto)?.translationX = position * width * -0.15f
            page.findViewById<View>(R.id.layerArt)?.translationX = position * width * 0.12f
            page.findViewById<View>(R.id.plate)?.translationX = position * width * 0.12f

            val fade = (1f - abs(position)).coerceIn(0f, 1f)
            page.findViewById<View>(R.id.ornament)?.alpha = fade
            page.findViewById<View>(R.id.tvTitle)?.alpha = fade
            page.findViewById<View>(R.id.tvBody)?.alpha = fade
            page.findViewById<View>(R.id.layerArt)?.alpha = fade
            page.findViewById<View>(R.id.plate)?.alpha = fade
        }

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                onStopReached(position)
            }
        })

        onStopReached(0)
    }

    private fun onStopReached(position: Int) = with(binding) {
        val last = position == approachStops.lastIndex

        listOf(ruleSeg1, ruleSeg2, ruleSeg3, ruleSeg4).forEachIndexed { index, segment ->
            segment.setBackgroundColor(
                getColor(if (index <= position) R.color.terracotta else R.color.line)
            )
            segment.alpha = if (index == position) 1f else if (index < position) 0.45f else 1f
        }

        distanceRule.contentDescription =
            getString(R.string.onboarding_page_of, position + 1, approachStops.size)

        btnStartJourney.setText(
            if (last) R.string.onboarding_start else R.string.onboarding_next
        )
        btnSkip.visibility = if (last) View.INVISIBLE else View.VISIBLE
    }

    // -------------------------------------------------------------- actions

    private fun setupClickListeners() = with(binding) {
        btnStartJourney.setOnClickListener {
            val current = pager.currentItem
            if (current < approachStops.lastIndex) {
                pager.setCurrentItem(current + 1, true)
            } else {
                markOnboarded()
                goToLogin(finishSelf = true)
            }
        }

        btnSkip.setOnClickListener {
            markOnboarded()
            goToLogin(finishSelf = true)
        }

        btnExploreNearby.setOnClickListener {
            markOnboarded()
            startActivityWithMap()
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.pager.currentItem > 0) {
                    binding.pager.setCurrentItem(binding.pager.currentItem - 1, true)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun markOnboarded() {
        prefs.edit().putBoolean(KEY_ONBOARDED, true).apply()
    }

    // -------------------------------------------------------------- routing

    private fun navigateToMain() {
        val uid = auth.currentUser?.uid ?: run {
            goToLogin(finishSelf = true)
            return
        }

        val cachedRole = prefs.getString("role_$uid", null)
        if (cachedRole != null) {
            navigateByRole(cachedRole)
            refreshRoleInBackground(uid)
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        FirebaseDatabase.getInstance().getReference("users")
            .child(uid)
            .child("role")
            .get()
            .addOnSuccessListener { snapshot ->
                binding.progressBar.visibility = View.GONE
                val role = snapshot.getValue(String::class.java) ?: run {
                    goToLogin(finishSelf = true)
                    return@addOnSuccessListener
                }
                prefs.edit().putString("role_$uid", role).apply()
                navigateByRole(role)
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                showSnack(binding.root, "Couldn't load your account. Try again.")
                goToLogin(finishSelf = true)
            }
    }

    private fun navigateByRole(role: String) {
        when (role) {
            "admin" -> {
                startActivity(Intent(this, MainPageAdminActivity::class.java))
                finish()
            }

            "user" -> {
                startActivity(Intent(this, MainPageActivity::class.java))
                finish()
            }

            else -> goToLogin(finishSelf = true)
        }
    }

    private fun refreshRoleInBackground(uid: String) {
        FirebaseDatabase.getInstance().getReference("users")
            .child(uid)
            .child("role")
            .get()
            .addOnSuccessListener { snapshot ->
                val role = snapshot.getValue(String::class.java) ?: return@addOnSuccessListener
                prefs.edit().putString("role_$uid", role).apply()
            }
    }

    private fun goToLogin(finishSelf: Boolean) {
        startActivity(Intent(this, LoginActivity::class.java))
        if (finishSelf) finish()
    }

    private fun startActivityWithMap() {
        val destination = if (auth.currentUser?.uid != null) {
            Intent(this, MainPageActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }
        destination.putExtra("OPEN_MAP", true)
        startActivity(destination)
        finish()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val PREFS = "user_prefs"
        private const val KEY_ONBOARDED = "onboarding_seen"
    }
}
