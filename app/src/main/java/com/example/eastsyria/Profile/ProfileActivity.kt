package com.example.eastsyria.Profile
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.eastsyria.Login.LoginActivity
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
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }


        binding.btnSettings.setOnClickListener {

        }


        binding.btnEditProfile.setOnClickListener {

        }

        binding.layoutPersonalDetails.setOnClickListener {
            //val intent = Intent(this, PersonalDetailsActivity::class.java)
            //startActivity(intent)
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
        auth.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}