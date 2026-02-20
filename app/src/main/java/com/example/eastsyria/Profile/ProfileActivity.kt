package com.example.eastsyria.Profile
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.bumptech.glide.Glide
import com.example.eastsyria.Login.LoginActivity
import com.example.eastsyria.MainPage.MainPageActivity
import com.example.eastsyria.Profile.PersonalDetails.PersonalDetailsActivity
import com.example.eastsyria.R
import com.example.eastsyria.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private var currentUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        currentUserId = auth.currentUser?.uid

        setupUI()
        loadUserData()
        hideSystemBars()

    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            val intent = Intent(this, MainPageActivity::class.java).apply {
                putExtra(MainPageActivity.EXTRA_SELECT_EXPLORE, true)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent)
        }



        binding.btnSettings.setOnClickListener {

        }


        binding.btnEditProfile.setOnClickListener {

        }

        binding.layoutPersonalDetails.setOnClickListener {
            val intent = Intent(this, PersonalDetailsActivity::class.java)
            startActivity(intent)
        }

        binding.layoutSettings.setOnClickListener {
            //val intent = Intent(this, SettingsActivity::class.java)
            //startActivity(intent)
        }

        binding.layoutAboutUs.setOnClickListener {
           // val intent = Intent(this, AboutUsActivity::class.java)
           // startActivity(intent)
        }

        binding.layoutPrivacyAgreement.setOnClickListener {
            //val intent = Intent(this, PrivacyAgreementActivity::class.java)
            //startActivity(intent)
        }


        binding.btnSignOut.setOnClickListener {
            signOut()
        }
    }

    private fun loadUserData() {
        currentUserId?.let { uid ->
            binding.progressBar.visibility = View.VISIBLE

            database.child("users").child(uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        binding.progressBar.visibility = View.GONE

                        if (snapshot.exists()) {
                            val fullName = snapshot.child("fullName").getValue(String::class.java) ?: ""
                            val email = snapshot.child("email").getValue(String::class.java) ?: ""
                            val phone = snapshot.child("phone").getValue(String::class.java) ?: ""
                            val createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: 0L
                            val city = snapshot.child("city").getValue(String::class.java) ?: ""
                            val imgUrl = snapshot.child("imgUrl").getValue(String::class.java) ?: ""

                            binding.tvUserName.text = fullName
                            binding.tvLocation.text = "Explorer from ${city}"
                            binding.tvMemberSince.text = formatMemberSince(createdAt)

                            Glide.with(this@ProfileActivity)
                                .load(imgUrl)
                                .circleCrop()
                                .into(binding.imgProfile)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        binding.progressBar.visibility = View.GONE
                    }
                })
        }
    }

    private fun formatMemberSince(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
        return "Member since ${format.format(date)}"
    }

    private fun signOut() {
        val uid = auth.currentUser?.uid
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
    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            hide(WindowInsetsCompat.Type.navigationBars())

            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
    override fun onResume() {
        super.onResume()
        loadUserData()
    }
}