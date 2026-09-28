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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.bumptech.glide.Glide
import com.example.eastsyria.Profile.ProfileActivity
import com.example.eastsyria.R
import com.example.eastsyria.Ui.warmEdgeToEdge
import com.example.eastsyria.databinding.ActivityPersonalDetailsBinding
import com.google.android.material.snackbar.Snackbar
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
                showMessage(R.string.personal_image_too_large)
                return@registerForActivityResult
            }
            selectedImageUri = uri
            val bmp = BitmapFactory.decodeByteArray(compressed, 0, compressed.size)
            binding.imgProfile.setImageBitmap(bmp)
        }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) openImagePicker() else showMessage(R.string.personal_photo_permission)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        warmEdgeToEdge()
        binding = ActivityPersonalDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInsets()
        loadUserData()
        setupListeners()
        setupCityDropdown()
    }

    /** The form scrolls clear of the gesture bar and the keyboard. */
    private fun setupInsets() {
        val base = binding.formContent.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.formContent) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.updatePadding(bottom = base + maxOf(bars.bottom, ime.bottom))
            insets
        }
    }

    private fun setupCityDropdown() {
        val cities = resources.getStringArray(R.array.syria_cities)
        val adapter = ArrayAdapter(this, R.layout.dropdown_item_city, cities)
        binding.etCity.setAdapter(adapter)
        binding.etCity.setOnClickListener {
            binding.etCity.showDropDown()
        }
    }


    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return
        setLoading(true)
        binding.loadError.visibility = View.GONE

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
            .addOnFailureListener {
                setLoading(false)
                binding.loadError.visibility = View.VISIBLE
            }
    }

    private fun setupListeners() {
        binding.header.onBack { finish() }
        binding.btnLoadRetry.setOnClickListener { loadUserData() }

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
            val bitmap = fixImageRotation(uri) ?: return null
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

    private fun fixImageRotation(uri: Uri): Bitmap? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val exifStream = contentResolver.openInputStream(uri) ?: return bitmap
            val exif = androidx.exifinterface.media.ExifInterface(exifStream)
            exifStream.close()

            val orientation = exif.getAttributeInt(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
            )

            val rotation = when (orientation) {
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90  -> 90f
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }

            if (rotation != 0f) {
                val matrix = android.graphics.Matrix()
                matrix.postRotate(rotation)
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
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

        binding.tilFullName.error = null
        binding.tilPhone.error = null
        if (fullName.isEmpty()) { binding.tilFullName.error = getString(R.string.personal_required); return }
        if (phone.isEmpty())    { binding.tilPhone.error    = getString(R.string.personal_required); return }

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
            showMessage(R.string.personal_image_too_large)
            return
        }

        val imageRef = storageRef.child("profile_images/$uid.jpg")

        imageRef.putBytes(compressed)
            .addOnSuccessListener {
                imageRef.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        saveToDatabase(uid, fullName, email, phone, city, downloadUri.toString())
                    }
                    .addOnFailureListener {
                        setLoading(false)
                        showMessage(R.string.personal_photo_upload_failed)
                    }
            }
            .addOnFailureListener {
                setLoading(false)
                showMessage(R.string.personal_photo_upload_failed)
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
                goToProfile()
            }
            .addOnFailureListener {
                setLoading(false)
                showMessage(R.string.personal_save_failed)
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

    private fun showMessage(resId: Int) {
        Snackbar.make(binding.root, resId, Snackbar.LENGTH_LONG)
            .setBackgroundTint(ContextCompat.getColor(this, R.color.surface_high))
            .setTextColor(ContextCompat.getColor(this, R.color.ivory))
            .show()
    }
}
