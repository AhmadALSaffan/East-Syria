package com.example.eastsyria.Admin.Add

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.eastsyria.Admin.MainPageAdminActivity
import com.example.eastsyria.databinding.ActivityAddLandmarkBinding
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class AddLandmarkActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddLandmarkBinding
    private var selectedImageUri: Uri? = null
    private val storageRef = FirebaseStorage.getInstance().reference
    private val databaseRef = FirebaseDatabase.getInstance().getReference("landmarks")

    companion object {
        private const val PICK_IMAGE_REQUEST = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddLandmarkBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBarsInsets.left,
                systemBarsInsets.top,
                systemBarsInsets.right,
                imeInsets.bottom
            )
            insets
        }
        hideSystemBars()
        setupFocusScrolling()

        binding.btnBack.setOnClickListener { finish() }

        binding.btnPickImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        binding.switchFeatured.setOnCheckedChangeListener { _, isChecked ->
            binding.switchFeatured.text = if (isChecked) "Featured: ON" else "Featured: OFF"
        }

        binding.switchTrending.setOnCheckedChangeListener { _, isChecked ->
            binding.switchTrending.text = if (isChecked) "Trending: ON" else "Trending: OFF"
        }

        binding.switchHistorical.setOnCheckedChangeListener { _, isChecked ->
            binding.switchHistorical.text = if (isChecked) "Historical: ON" else "Historical: OFF"
        }

        binding.btnSaveLandmark.setOnClickListener {
            if (validateForm()) {
                saveLandmark()
            }
        }
        binding.etBestTimeToVisit.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                val imm =
                    getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(binding.etBestTimeToVisit.windowToken, 0)
                if (validateForm()) saveLandmark()
                true
            } else {
                false
            }
        }
    }

    private fun setupFocusScrolling() {
        val scrollView = binding.scrollView

        val groupOne = listOf(
            binding.etLandmarkId,
            binding.etNameEn,
            binding.etNameAr,
            binding.etCategoryEn,
            binding.etCategoryAr
        )

        val groupTwo = listOf(
            binding.etCityEn,
            binding.etCityAr,
            binding.etGovernorateEn,
            binding.etGovernorateAr,
            binding.etRegionEn,
            binding.etLatitude,
            binding.etLongitude,
            binding.etVisitingHours,
            binding.etEntryFee,
            binding.etBestTimeToVisit
        )

        (groupOne + groupTwo).forEach { field ->
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

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            hide(WindowInsetsCompat.Type.navigationBars())

            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            binding.ivPreview.setImageURI(selectedImageUri)
            binding.ivPreview.visibility = View.VISIBLE
            binding.tvImageHint.text = "Image selected ✓"
        }
    }

    private fun validateForm(): Boolean {
        binding.apply {
            if (etLandmarkId.text.isNullOrEmpty()) {
                etLandmarkId.error = "Required"
                etLandmarkId.requestFocus()
                return false
            }
            if (etNameEn.text.isNullOrEmpty()) {
                etNameEn.error = "Required"
                etNameEn.requestFocus()
                return false
            }
            if (etNameAr.text.isNullOrEmpty()) {
                etNameAr.error = "Required"
                etNameAr.requestFocus()
                return false
            }
            if (etCategoryEn.text.isNullOrEmpty()) {
                etCategoryEn.error = "Required"
                etCategoryEn.requestFocus()
                return false
            }
            if (etCategoryAr.text.isNullOrEmpty()) {
                etCategoryAr.error = "Required"
                etCategoryAr.requestFocus()
                return false
            }
            if (etDescriptionEn.text.isNullOrEmpty()) {
                etDescriptionEn.error = "Required"
                etDescriptionEn.requestFocus()
                return false
            }
            if (etDescriptionAr.text.isNullOrEmpty()) {
                etDescriptionAr.error = "Required"
                etDescriptionAr.requestFocus()
                return false
            }
            if (etLongDescriptionEn.text.isNullOrEmpty()) {
                etLongDescriptionEn.error = "Required"
                etLongDescriptionEn.requestFocus()
                return false
            }
            if (etLongDescriptionAr.text.isNullOrEmpty()) {
                etLongDescriptionAr.error = "Required"
                etLongDescriptionAr.requestFocus()
                return false
            }
            if (etCityEn.text.isNullOrEmpty()) {
                etCityEn.error = "Required"
                etCityEn.requestFocus()
                return false
            }
            if (etCityAr.text.isNullOrEmpty()) {
                etCityAr.error = "Required"
                etCityAr.requestFocus()
                return false
            }
            if (etGovernorateEn.text.isNullOrEmpty()) {
                etGovernorateEn.error = "Required"
                etGovernorateEn.requestFocus()
                return false
            }
            if (etGovernorateAr.text.isNullOrEmpty()) {
                etGovernorateAr.error = "Required"
                etGovernorateAr.requestFocus()
                return false
            }
            if (etRegionEn.text.isNullOrEmpty()) {
                etRegionEn.error = "Required"
                etRegionEn.requestFocus()
                return false
            }
            if (etLatitude.text.isNullOrEmpty()) {
                etLatitude.error = "Required"
                etLatitude.requestFocus()
                return false
            }
            if (etLongitude.text.isNullOrEmpty()) {
                etLongitude.error = "Required"
                etLongitude.requestFocus()
                return false
            }
            if (etVisitingHours.text.isNullOrEmpty()) {
                etVisitingHours.error = "Required"
                etVisitingHours.requestFocus()
                return false
            }
            if (etEntryFee.text.isNullOrEmpty()) {
                etEntryFee.error = "Required"
                etEntryFee.requestFocus()
                return false
            }
            if (etBestTimeToVisit.text.isNullOrEmpty()) {
                etBestTimeToVisit.error = "Required"
                etBestTimeToVisit.requestFocus()
                return false
            }
            if (selectedImageUri == null) {
                Toast.makeText(this@AddLandmarkActivity, "Please select an image", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }

    private fun saveLandmark() {
        setLoading(true)

        val imageFileName = "landmarks/${UUID.randomUUID()}.jpg"
        val imageRef = storageRef.child(imageFileName)

        imageRef.putFile(selectedImageUri!!)
            .addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                binding.progressBar.progress = progress
            }
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    uploadToDatabase(downloadUri.toString())
                }
            }
            .addOnFailureListener {
                setLoading(false)
                Toast.makeText(this, "Image upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadToDatabase(imageUrl: String) {
        binding.apply {
            val now = System.currentTimeMillis()

            databaseRef.get().addOnSuccessListener { snapshot ->
                var maxId = 0
                for (child in snapshot.children) {
                    val key = child.key?.toIntOrNull()
                    if (key != null && key > maxId && child.getValue(Any::class.java) != null) {
                        maxId = key
                    }
                }
                val newId = (maxId + 1).toString()
                sendLandmarkNotification(newId, etNameEn.text.toString().trim(), imageUrl)
                val landmark = mapOf(
                    "id" to newId,
                    "name" to etNameEn.text.toString().trim(),
                    "nameArabic" to etNameAr.text.toString().trim(),
                    "category" to etCategoryEn.text.toString().trim(),
                    "categoryArabic" to etCategoryAr.text.toString().trim(),
                    "description" to etDescriptionEn.text.toString().trim(),
                    "descriptionArabic" to etDescriptionAr.text.toString().trim(),
                    "longDescription" to etLongDescriptionEn.text.toString().trim(),
                    "longDescriptionArabic" to etLongDescriptionAr.text.toString().trim(),
                    "imageUrl" to imageUrl,
                    "visitingHours" to etVisitingHours.text.toString().trim(),
                    "entryFee" to etEntryFee.text.toString().trim(),
                    "bestTimeToVisit" to etBestTimeToVisit.text.toString().trim(),
                    "status" to "pending",
                    "statusArabic" to "قيد المراجعة",
                    "isFeatured" to switchFeatured.isChecked,
                    "isTrending" to switchTrending.isChecked,
                    "isHistorical" to switchHistorical.isChecked,
                    "rating" to 0.0,
                    "reviewCount" to 0,
                    "createdAt" to now,
                    "updatedAt" to now,
                    "location" to mapOf(
                        "city" to etCityEn.text.toString().trim(),
                        "cityArabic" to etCityAr.text.toString().trim(),
                        "governorate" to etGovernorateEn.text.toString().trim(),
                        "governorateArabic" to etGovernorateAr.text.toString().trim(),
                        "region" to etRegionEn.text.toString().trim(),
                        "latitude" to etLatitude.text.toString().trim().toDoubleOrNull(),
                        "longitude" to etLongitude.text.toString().trim().toDoubleOrNull()
                    ),
                    "details" to mapOf(
                        "yearBuilt" to binding.etYearBuilt.text.toString().trim(),
                        "builtBy"   to binding.etBuiltBy.text.toString().trim(),
                        "era"       to binding.etEra.text.toString().trim(),
                        "material"  to binding.etMaterial.text.toString().trim(),
                        "elevation" to binding.etElevation.text.toString().trim()
                    )
                )

                databaseRef.child(newId).setValue(landmark)
                    .addOnSuccessListener {
                        setLoading(false)
                        Toast.makeText(
                            this@AddLandmarkActivity,
                            "Landmark saved as #$newId",
                            Toast.LENGTH_SHORT
                        ).show()
                        startActivity(Intent(this@AddLandmarkActivity, MainPageAdminActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener {
                        setLoading(false)
                        Toast.makeText(
                            this@AddLandmarkActivity,
                            "Failed to save: ${it.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }.addOnFailureListener {
                setLoading(false)
                Toast.makeText(this@AddLandmarkActivity, "Failed to read DB: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSaveLandmark.isEnabled = !loading
        binding.btnSaveLandmark.text = if (loading) "Saving..." else "SAVE LANDMARK"
    }
    private fun sendLandmarkNotification(landmarkId: String, landmarkName: String, imageUrl: String) {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        val notificationsRef = FirebaseDatabase.getInstance().getReference("notifications")

        usersRef.get().addOnSuccessListener { snapshot ->
            for (userSnapshot in snapshot.children) {
                val userId = userSnapshot.key ?: continue
                val notifRef = notificationsRef.child(userId).push()
                val notification = mapOf(
                    "type" to "LANDMARK",
                    "title" to "New Landmark Added: $landmarkName",
                    "description" to "A new historical site is now available for exploration in the eastern region.",
                    "imageUrl" to imageUrl,
                    "isFeatured" to true,
                    "isRead" to false,
                    "relatedId" to landmarkId,
                    "timestamp" to System.currentTimeMillis()
                )
                notifRef.setValue(notification)
            }
        }
    }
}
