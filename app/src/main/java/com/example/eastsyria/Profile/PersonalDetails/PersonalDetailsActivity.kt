package com.example.eastsyria.Profile.PersonalDetails

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.eastsyria.Profile.ProfileActivity
import com.example.eastsyria.R
import com.example.eastsyria.databinding.ActivityPersonalDetailsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

class PersonalDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPersonalDetailsBinding

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val dbRef by lazy { FirebaseDatabase.getInstance().reference }
    private val storageRef by lazy { FirebaseStorage.getInstance().reference }

    private var selectedImageUri: Uri? = null
    private val MAX_IMAGE_SIZE_BYTES = 1 * 1024 * 1024

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri ?: return@registerForActivityResult
            val compressed = compressToUnder1MB(uri)
            if (compressed == null) {
                Toast.makeText(
                    this,
                    "Image is larger than 1 MB and couldn't be compressed. Choose a smaller image.",
                    Toast.LENGTH_LONG
                ).show()
                return@registerForActivityResult
            }
            selectedImageUri = uri
            val bmp = BitmapFactory.decodeByteArray(compressed, 0, compressed.size)
            binding.imgProfile.setImageBitmap(bmp)
        }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) openImagePicker()
            else Toast.makeText(this, "Permission denied. Cannot access gallery.", Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPersonalDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadUserData()
        setupListeners()
        setupCityDropdown()
    }

    private fun setupCityDropdown() {
        val cities = resources.getStringArray(R.array.syria_cities)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, cities)
        binding.etCity.setAdapter(adapter)
        binding.etCity.setOnClickListener {
            binding.etCity.showDropDown()
        }
    }


    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return
        setLoading(true)

        dbRef.child("users").child(uid).get()
            .addOnSuccessListener { snap ->
                setLoading(false)
                val imgUri = snap.child("imgUrl").value?.toString().orEmpty()
                binding.etFullName.setText(snap.child("fullName").value?.toString().orEmpty())
                binding.etEmail.setText(snap.child("email").value?.toString().orEmpty())
                binding.etPhone.setText(snap.child("phone").value?.toString().orEmpty())
                binding.etCity.setText(snap.child("city").value?.toString().orEmpty(), false)
                if (!imgUri.isNullOrEmpty()){
                    Glide.with(this@PersonalDetailsActivity)
                        .load(imgUri)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .into(binding.imgProfile)
                }
                else{
                    binding.imgProfile.setImageResource(R.drawable.ic_profile_placeholder)
                }

                val imageUrl = snap.child("profileImageUrl").value?.toString()
                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .into(binding.imgProfile)
                }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(this, "Failed to load data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.imgProfile.setOnClickListener { requestImagePermission() }
        binding.btnEditPhoto.setOnClickListener { requestImagePermission() }
        binding.tvChangePhoto.setOnClickListener { requestImagePermission() }

        binding.tvDiscardChanges.setOnClickListener {
            selectedImageUri = null
            loadUserData()
        }

        binding.btnSaveChanges.setOnClickListener { onSaveClicked() }
    }

    private fun requestImagePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openImagePicker()
        } else {
            permissionLauncher.launch(permission)
        }
    }

    private fun openImagePicker() {
        imagePickerLauncher.launch("image/*")
    }

    private fun compressToUnder1MB(uri: Uri): ByteArray? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val out = ByteArrayOutputStream()
            var quality = 95

            do {
                out.reset()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                quality -= 5
            } while (out.size() > MAX_IMAGE_SIZE_BYTES && quality > 5)

            if (out.size() > MAX_IMAGE_SIZE_BYTES) null else out.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun onSaveClicked() {
        val uid = auth.currentUser?.uid ?: return

        val fullName = binding.etFullName.text.toString().trim()
        val email    = binding.etEmail.text.toString().trim()
        val phone    = binding.etPhone.text.toString().trim()
        val city     = binding.etCity.text.toString().trim()

        if (fullName.isEmpty()) { binding.etFullName.error = "Required"; return }
        if (email.isEmpty())    { binding.etEmail.error    = "Required"; return }
        if (phone.isEmpty())    { binding.etPhone.error    = "Required"; return }

        setLoading(true)

        if (selectedImageUri != null) {
            uploadImageThenSave(uid, fullName, email, phone, city)
        } else {
            saveToDatabase(uid, fullName, email, phone, city, imageUrl = null)
        }
    }

    private fun uploadImageThenSave(
        uid: String, fullName: String, email: String, phone: String, city: String
    ) {
        val compressed = compressToUnder1MB(selectedImageUri!!)
        if (compressed == null) {
            setLoading(false)
            Toast.makeText(this, "Image exceeds 1 MB. Choose a smaller image.", Toast.LENGTH_SHORT).show()
            return
        }

        val imageRef = storageRef.child("profile_images/$uid.jpg")

        imageRef.putBytes(compressed)
            .addOnSuccessListener {
                imageRef.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        saveToDatabase(uid, fullName, email, phone, city, downloadUri.toString())
                    }
                    .addOnFailureListener { e ->
                        setLoading(false)
                        Toast.makeText(this, "Could not get image URL: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(this, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveToDatabase(
        uid: String, fullName: String, email: String,
        phone: String, city: String, imageUrl: String?
    ) {
        val updates = mutableMapOf<String, Any>(
            "fullName" to fullName,
            "email"    to email,
            "phone"    to phone,
            "city"     to city
        )
        if (imageUrl != null) updates["imgUrl"] = imageUrl

        dbRef.child("users").child(uid).updateChildren(updates)
            .addOnSuccessListener {
                setLoading(false)
                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                goToProfile()
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun goToProfile() {
        val intent = Intent(this, ProfileActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    private fun setLoading(show: Boolean) {
        val visibility = if (show) View.VISIBLE else View.GONE
        binding.loadingOverlay.visibility = visibility
        binding.progressBar.visibility    = visibility
        binding.btnSaveChanges.isEnabled  = !show
        binding.btnEditPhoto.isEnabled    = !show
        binding.tvChangePhoto.isEnabled   = !show
    }
}
