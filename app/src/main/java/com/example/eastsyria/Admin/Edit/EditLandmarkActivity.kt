package com.example.eastsyria.Admin.Edit

import com.example.eastsyria.Admin.MainPageAdminActivity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.eastsyria.databinding.ActivityEditLandmarkBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class EditLandmarkActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditLandmarkBinding
    private val database = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance().reference
    private var landmarkId = ""
    private var currentImageUrl = ""
    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            binding.ivPreview.visibility = View.VISIBLE
            binding.ivPreview.setImageURI(it)
            binding.tvImageHint.text = "New image selected — will replace current"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditLandmarkBinding.inflate(layoutInflater)
        setContentView(binding.root)

        landmarkId = intent.getStringExtra("landmarkId") ?: ""

        if (landmarkId.isEmpty()) {
            Toast.makeText(this, "Invalid landmark ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.btnBack.setOnClickListener { finish() }
        binding.btnPickImage.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.btnSaveLandmark.setOnClickListener { handleSave() }

        loadLandmarkData()
        setupFocusScrolling()
    }

    private fun setupFocusScrolling() {
        val scrollView = binding.scrollView

        val allFields = listOf(
            binding.etNameEn,
            binding.etNameAr,
            binding.etCategoryEn,
            binding.etCategoryAr,
            binding.etCityEn,
            binding.etCityAr,
            binding.etGovernorateEn,
            binding.etGovernorateAr,
            binding.etRegionEn,
            binding.etLatitude,
            binding.etLongitude,
            binding.etYearBuilt,
            binding.etBuiltBy,
            binding.etEra,
            binding.etMaterial,
            binding.etElevation,
            binding.etVisitingHours,
            binding.etEntryFee,
            binding.etBestTimeToVisit,
            binding.etStatus,
            binding.etStatusArabic
        )

        allFields.forEach { field ->
            field.setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    scrollView.post {
                        val location = IntArray(2)
                        view.getLocationOnScreen(location)
                        val scrollLocation = IntArray(2)
                        scrollView.getLocationOnScreen(scrollLocation)
                        val screenHeight = resources.displayMetrics.heightPixels
                        val offset = (screenHeight * 0.25).toInt()
                        val relativeY = location[1] - scrollLocation[1] + scrollView.scrollY - offset
                        scrollView.smoothScrollTo(0, relativeY)
                    }
                }
            }
        }
    }



    private fun loadLandmarkData() {
        showProgress(true)
        database.child("landmarks").child(landmarkId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    showProgress(false)
                    if (!snapshot.exists()) {
                        Toast.makeText(this@EditLandmarkActivity, "Landmark not found", Toast.LENGTH_SHORT).show()
                        finish()
                        return
                    }
                    populateFields(snapshot)
                }

                override fun onCancelled(error: DatabaseError) {
                    showProgress(false)
                    Toast.makeText(this@EditLandmarkActivity, error.message, Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun populateFields(snapshot: DataSnapshot) {
        binding.etNameEn.setText(snapshot.child("name").getValue(String::class.java).orEmpty())
        binding.etNameAr.setText(snapshot.child("nameArabic").getValue(String::class.java).orEmpty())
        binding.etCategoryEn.setText(snapshot.child("category").getValue(String::class.java).orEmpty())
        binding.etCategoryAr.setText(snapshot.child("categoryArabic").getValue(String::class.java).orEmpty())

        binding.etDescriptionEn.setText(snapshot.child("description").getValue(String::class.java).orEmpty())
        binding.etDescriptionAr.setText(snapshot.child("descriptionArabic").getValue(String::class.java).orEmpty())
        binding.etLongDescriptionEn.setText(snapshot.child("longDescription").getValue(String::class.java).orEmpty())
        binding.etLongDescriptionAr.setText(snapshot.child("longDescriptionArabic").getValue(String::class.java).orEmpty())

        currentImageUrl = snapshot.child("imageUrl").getValue(String::class.java).orEmpty()
        if (currentImageUrl.isNotEmpty()) {
            binding.ivPreview.visibility = View.VISIBLE
            binding.tvImageHint.text = "Current image loaded"
            Glide.with(this)
                .load(currentImageUrl)
                .centerCrop()
                .into(binding.ivPreview)
        }

        val loc = snapshot.child("location")
        binding.etCityEn.setText(loc.child("city").getValue(String::class.java).orEmpty())
        binding.etCityAr.setText(loc.child("cityArabic").getValue(String::class.java).orEmpty())
        binding.etGovernorateEn.setText(loc.child("governorate").getValue(String::class.java).orEmpty())
        binding.etGovernorateAr.setText(loc.child("governorateArabic").getValue(String::class.java).orEmpty())
        binding.etRegionEn.setText(loc.child("region").getValue(String::class.java).orEmpty())
        binding.etLatitude.setText(loc.child("latitude").getValue(Double::class.java)?.toString().orEmpty())
        binding.etLongitude.setText(loc.child("longitude").getValue(Double::class.java)?.toString().orEmpty())

        val details = snapshot.child("details")
        val yearBuilt = details.child("yearBuilt").getValue(String::class.java)
            ?: details.child("yearBuilt").getValue(Long::class.java)?.toString().orEmpty()
        binding.etYearBuilt.setText(yearBuilt)
        binding.etBuiltBy.setText(details.child("builtBy").getValue(String::class.java).orEmpty())
        binding.etEra.setText(details.child("era").getValue(String::class.java).orEmpty())
        binding.etMaterial.setText(details.child("material").getValue(String::class.java).orEmpty())
        binding.etElevation.setText(details.child("elevation").getValue(String::class.java).orEmpty())

        binding.etVisitingHours.setText(snapshot.child("visitingHours").getValue(String::class.java).orEmpty())
        binding.etEntryFee.setText(snapshot.child("entryFee").getValue(String::class.java).orEmpty())
        binding.etBestTimeToVisit.setText(snapshot.child("bestTimeToVisit").getValue(String::class.java).orEmpty())

        binding.etStatus.setText(snapshot.child("status").getValue(String::class.java).orEmpty())
        binding.etStatusArabic.setText(snapshot.child("statusArabic").getValue(String::class.java).orEmpty())

        binding.switchFeatured.isChecked = snapshot.child("isFeatured").getValue(Boolean::class.java) ?: false
        binding.switchTrending.isChecked = snapshot.child("isTrending").getValue(Boolean::class.java) ?: false
        binding.switchHistorical.isChecked = snapshot.child("isHistorical").getValue(Boolean::class.java) ?: false
    }

    private fun handleSave() {
        if (!validateInputs()) return
        if (selectedImageUri != null) {
            uploadImageThenSave()
        } else {
            saveToFirebase(currentImageUrl)
        }
    }

    private fun uploadImageThenSave() {
        showProgress(true)
        val filename = "landmarks/${UUID.randomUUID()}.jpg"
        val ref = storage.child(filename)
        ref.putFile(selectedImageUri!!)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    saveToFirebase(uri.toString())
                }
            }
            .addOnFailureListener {
                showProgress(false)
                Toast.makeText(this, "Image upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveToFirebase(imageUrl: String) {
        showProgress(true)

        val details = mapOf(
            "yearBuilt" to binding.etYearBuilt.text.toString().trim(),
            "builtBy" to binding.etBuiltBy.text.toString().trim(),
            "era" to binding.etEra.text.toString().trim(),
            "material" to binding.etMaterial.text.toString().trim(),
            "elevation" to binding.etElevation.text.toString().trim()
        )

        val location = mapOf(
            "city" to binding.etCityEn.text.toString().trim(),
            "cityArabic" to binding.etCityAr.text.toString().trim(),
            "governorate" to binding.etGovernorateEn.text.toString().trim(),
            "governorateArabic" to binding.etGovernorateAr.text.toString().trim(),
            "region" to binding.etRegionEn.text.toString().trim(),
            "latitude" to (binding.etLatitude.text.toString().toDoubleOrNull() ?: 0.0),
            "longitude" to (binding.etLongitude.text.toString().toDoubleOrNull() ?: 0.0)
        )

        val updates: Map<String, Any> = mapOf(
            "name" to binding.etNameEn.text.toString().trim(),
            "nameArabic" to binding.etNameAr.text.toString().trim(),
            "category" to binding.etCategoryEn.text.toString().trim(),
            "categoryArabic" to binding.etCategoryAr.text.toString().trim(),
            "description" to binding.etDescriptionEn.text.toString().trim(),
            "descriptionArabic" to binding.etDescriptionAr.text.toString().trim(),
            "longDescription" to binding.etLongDescriptionEn.text.toString().trim(),
            "longDescriptionArabic" to binding.etLongDescriptionAr.text.toString().trim(),
            "imageUrl" to imageUrl,
            "location" to location,
            "details" to details,
            "visitingHours" to binding.etVisitingHours.text.toString().trim(),
            "entryFee" to binding.etEntryFee.text.toString().trim(),
            "bestTimeToVisit" to binding.etBestTimeToVisit.text.toString().trim(),
            "status" to binding.etStatus.text.toString().trim(),
            "statusArabic" to binding.etStatusArabic.text.toString().trim(),
            "isFeatured" to binding.switchFeatured.isChecked,
            "isTrending" to binding.switchTrending.isChecked,
            "isHistorical" to binding.switchHistorical.isChecked,
            "updatedAt" to System.currentTimeMillis()
        )

        database.child("landmarks").child(landmarkId).updateChildren(updates)
            .addOnSuccessListener {
                showProgress(false)
                Toast.makeText(this, "Landmark updated successfully", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainPageAdminActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                showProgress(false)
                Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            }
    }

    private fun validateInputs(): Boolean {
        if (binding.etNameEn.text.isNullOrBlank()) {
            binding.etNameEn.error = "Required"
            binding.etNameEn.requestFocus()
            return false
        }
        if (binding.etNameAr.text.isNullOrBlank()) {
            binding.etNameAr.error = "Required"
            binding.etNameAr.requestFocus()
            return false
        }
        if (binding.etDescriptionEn.text.isNullOrBlank()) {
            binding.etDescriptionEn.error = "Required"
            binding.etDescriptionEn.requestFocus()
            return false
        }
        if (binding.etCategoryEn.text.isNullOrBlank()) {
            binding.etCategoryEn.error = "Required"
            binding.etCategoryEn.requestFocus()
            return false
        }
        return true
    }

    private fun showProgress(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnSaveLandmark.isEnabled = !show
    }
}