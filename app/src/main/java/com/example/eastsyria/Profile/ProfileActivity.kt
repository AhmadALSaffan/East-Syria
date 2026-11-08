package com.example.eastsyria.Profile

import com.example.eastsyria.Ui.showSnack
import com.example.eastsyria.Ui.isArabic
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.eastsyria.Details.LandmarkDetailActivity
import com.example.eastsyria.Login.LoginActivity
import com.example.eastsyria.MainPage.Data.Landmark
import com.example.eastsyria.MainPage.MainPageActivity
import com.example.eastsyria.Profile.PersonalDetails.PersonalDetailsActivity
import com.example.eastsyria.Nav.NavDock
import com.example.eastsyria.R
import com.example.eastsyria.Saved.SavedLandmarksActivity
import com.example.eastsyria.Ui.SkeletonPulse
import com.example.eastsyria.Ui.warmEdgeToEdge
import com.example.eastsyria.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var reauth: com.example.eastsyria.Login.ReauthFlow
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var pulse: SkeletonPulse

    private val stamps = mutableListOf<Landmark>()
    private lateinit var stampAdapter: SavedStampAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        warmEdgeToEdge()
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        pulse = SkeletonPulse(binding.identitySkeleton, binding.stampsSkeleton)
        // Personal details opens only after the owner signs in again.
        reauth = com.example.eastsyria.Login.ReauthFlow(this) { message ->
            showSnack(binding.root, message, long = true)
        }

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        setupInsets()
        binding.navDock.attach(this, NavDock.TAB_PROFILE)
        setupUI()
        setupStamps()
    }

    /** Loaded on every return, so a change made in Personal details shows straight away. */
    override fun onResume() {
        super.onResume()
        if (auth.currentUser == null) {
            showSignedOut()
            return
        }
        binding.statePanel.hide()
        loadUserData()
        loadSavedStamps()
    }

    override fun onDestroy() {
        pulse.stop()
        super.onDestroy()
    }

    private fun setupInsets() {
        // The dock pads itself for the gesture bar; the content only has to clear the dock.
        val base = binding.profileContent.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.profileContent) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.updatePadding(bottom = base + bars.bottom)
            insets
        }
    }

    private fun setupUI() {
        binding.header.onBack {
            startActivity(
                Intent(this, MainPageActivity::class.java).apply {
                    putExtra(MainPageActivity.EXTRA_SELECT_EXPLORE, true)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            )
        }

        binding.layoutSecurity.setOnClickListener {
            reauth.confirm { startActivity(Intent(this, com.example.eastsyria.Security.SecurityActivity::class.java)) }
        }
        binding.layoutPersonalDetails.setOnClickListener {
            reauth.confirm { startActivity(Intent(this, PersonalDetailsActivity::class.java)) }
        }
        binding.tvSavedAll.setOnClickListener {
            startActivity(Intent(this, SavedLandmarksActivity::class.java))
        }
        binding.btnSignOut.setOnClickListener { signOut() }
        binding.btnIdentityRetry.setOnClickListener { loadUserData() }

        val settings = Intent(this, com.example.eastsyria.Settings.SettingsActivity::class.java)
        binding.layoutSettings.setOnClickListener { startActivity(settings) }
        binding.layoutLanguage.setOnClickListener { startActivity(settings) }
        binding.layoutAbout.setOnClickListener {
            startActivity(Intent(this, com.example.eastsyria.Settings.AboutActivity::class.java))
        }
        binding.layoutPrivacy.setOnClickListener {
            startActivity(Intent(this, com.example.eastsyria.Settings.PrivacyActivity::class.java))
        }
        binding.tvLanguageValue.text = getString(
            if (isArabic()) R.string.language_arabic else R.string.language_english
        )

        // The real installed version, not a hardcoded "1.0.0 (Stable)".
        // Read from PackageManager because BuildConfig generation is off.
        val version = runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName
        }.getOrNull().orEmpty()

        if (version.isBlank()) {
            binding.tvAppVersion.visibility = View.GONE
        } else {
            binding.tvAppVersion.text = getString(R.string.profile_version, version)
        }
    }

    private fun setupStamps() {
        stampAdapter = SavedStampAdapter(stamps) { landmark ->
            startActivity(
                Intent(this, LandmarkDetailActivity::class.java)
                    .putExtra("LANDMARK_ID", landmark.id)
            )
        }
        binding.rvSavedStamps.apply {
            adapter = stampAdapter
            layoutManager = LinearLayoutManager(
                this@ProfileActivity, LinearLayoutManager.HORIZONTAL, false
            )
        }
    }

    // ------------------------------------------------------------ identity

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return
        showIdentity(loading = true)

        database.child("users").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        showIdentity(loading = false)
                        return
                    }

                    val fullName = snapshot.child("fullName").getValue(String::class.java) ?: ""
                    val createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: 0L
                    val city = snapshot.child("city").getValue(String::class.java) ?: ""
                    val imgUrl = snapshot.child("imgUrl").getValue(String::class.java) ?: ""

                    binding.tvUserName.text = fullName

                    // "Explorer from " with nothing after it is worse than just "Explorer".
                    binding.tvLocation.text = if (city.isBlank()) {
                        getString(R.string.profile_explorer)
                    } else {
                        getString(R.string.profile_explorer_from, city)
                    }

                    binding.tvFactSince.text =
                        if (createdAt > 0L) formatMonth(createdAt) else getString(R.string.admin_figure_unknown)

                    if (imgUrl.isNotBlank()) {
                        Glide.with(this@ProfileActivity)
                            .load(imgUrl)
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .error(R.drawable.ic_profile_placeholder)
                            .circleCrop()
                            .into(binding.imgProfile)
                    }
                    showIdentity(loading = false)
                }

                override fun onCancelled(error: DatabaseError) {
                    showIdentity(loading = false, failed = true)
                }
            })
    }

    private fun showIdentity(loading: Boolean, failed: Boolean = false) {
        binding.identitySkeleton.visibility = if (loading) View.VISIBLE else View.GONE
        binding.layoutIdentity.visibility = if (loading || failed) View.GONE else View.VISIBLE
        binding.identityError.visibility = if (failed) View.VISIBLE else View.GONE
        syncPulse()
    }

    // ------------------------------------------------------------ stamps

    /**
     * The saved places. Two reads rather than one per saved id: collect the
     * saved keys, then match them against the published catalog in a single pass.
     */
    private fun loadSavedStamps() {
        val uid = auth.currentUser?.uid ?: run {
            showStamps(emptyList())
            return
        }
        showStampsLoading()

        database.child("users").child(uid).child("savedLandmarks")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val savedIds = snapshot.children.mapNotNull { it.key }.toSet()
                    if (savedIds.isEmpty()) {
                        showStamps(emptyList())
                        return
                    }
                    fetchLandmarks(savedIds)
                }

                override fun onCancelled(error: DatabaseError) {
                    showStamps(emptyList())
                }
            })
    }

    private fun fetchLandmarks(savedIds: Set<String>) {
        database.child("landmarks")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val found = snapshot.children.mapNotNull { child ->
                        val key = child.key ?: return@mapNotNull null
                        if (key !in savedIds) return@mapNotNull null
                        child.getValue(Landmark::class.java)?.also { it.id = key }
                    }.filter { it.status == "published" }

                    showStamps(found)
                }

                override fun onCancelled(error: DatabaseError) {
                    showStamps(emptyList())
                }
            })
    }

    private fun showStampsLoading() {
        binding.stampsSkeleton.visibility = View.VISIBLE
        binding.rvSavedStamps.visibility = View.GONE
        binding.tvSavedEmpty.visibility = View.GONE
        syncPulse()
    }

    private fun showStamps(found: List<Landmark>) {
        stamps.clear()
        stamps.addAll(found)
        stampAdapter.notifyDataSetChanged()

        val empty = found.isEmpty()
        // Two counted facts on the passport: how many, and how many kinds of place.
        binding.tvFactSaved.text = found.size.toString()
        binding.tvFactKinds.text = found.map { it.category.trim().lowercase() }
            .filter { it.isNotEmpty() }.distinct().size.toString()
        binding.stampsSkeleton.visibility = View.GONE
        binding.rvSavedStamps.visibility = if (empty) View.GONE else View.VISIBLE
        binding.tvSavedEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        binding.tvSavedCount.visibility = if (empty) View.GONE else View.VISIBLE
        binding.tvSavedAll.visibility = if (empty) View.GONE else View.VISIBLE
        if (!empty) {
            binding.tvSavedCount.text = getString(R.string.profile_saved_count, found.size)
        }
        syncPulse()
    }

    /** One pulse for both skeletons: it runs while either is showing. */
    private fun syncPulse() {
        val loading = binding.identitySkeleton.visibility == View.VISIBLE ||
                binding.stampsSkeleton.visibility == View.VISIBLE
        if (loading) pulse.start() else pulse.stop()
    }

    // ------------------------------------------------------------ session

    private fun showSignedOut() {
        binding.statePanel.show(
            getString(R.string.profile_signed_out_title),
            getString(R.string.profile_signed_out_body),
            getString(R.string.saved_signed_out_action)
        ) { startActivity(Intent(this, LoginActivity::class.java)) }
    }

    private fun formatMonth(timestamp: Long): String =
        SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(timestamp))

    private fun signOut() {
        val uid = auth.currentUser?.uid
        uid?.let { com.example.eastsyria.Security.TwoFactorTrust.forget(this, it) }
        com.example.eastsyria.Social.Presence.withdraw()
        com.example.eastsyria.Social.Profile.forget()
        auth.signOut()
        if (uid != null) {
            getSharedPreferences("user_prefs", MODE_PRIVATE)
                .edit()
                .remove("role_$uid")
                .apply()
        }
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
