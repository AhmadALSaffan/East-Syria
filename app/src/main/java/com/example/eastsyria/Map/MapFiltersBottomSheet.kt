package com.example.eastsyria.Map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.eastsyria.databinding.BottomSheetMapFiltersBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class MapFiltersBottomSheet(
    private val onFiltersApplied: (FilterOptions) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetMapFiltersBinding? = null
    private val binding get() = _binding!!

    private var currentFilters = FilterOptions()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetMapFiltersBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hideSystemBars()
        setupDistanceSlider()
        setupClickListeners()
    }

    private fun setupDistanceSlider() {
        binding.sliderDistance.addOnChangeListener { slider, value, fromUser ->
            if (value >= 100f) {
                binding.tvDistanceValue.text = "Any distance"
            } else {
                binding.tvDistanceValue.text = "${value.toInt()} km"
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        binding.btnClearFilters.setOnClickListener {
            clearAllFilters()
        }

        binding.btnApplyFilters.setOnClickListener {
            applyFilters()
        }
    }

    private fun clearAllFilters() {
        binding.chipGroupCategory.clearCheck()

        binding.sliderDistance.value = 100f
        binding.tvDistanceValue.text = "Any distance"


        binding.chipRatingAny.isChecked = true


        binding.cbFeaturedOnly.isChecked = false
        binding.cbTrendingOnly.isChecked = false
    }

    private fun applyFilters() {
        val categories = mutableListOf<String>()


        if (binding.chipHistorical.isChecked) categories.add("Historical")
        if (binding.chipArchaeological.isChecked) categories.add("Archaeological")
        if (binding.chipNature.isChecked) categories.add("Nature")
        if (binding.chipCulture.isChecked) categories.add("Culture")


        val distance = if (binding.sliderDistance.value >= 100f) {
            null
        } else {
            binding.sliderDistance.value
        }


        val minRating = when {
            binding.chipRating45.isChecked -> 4.5
            binding.chipRating4.isChecked -> 4.0
            binding.chipRating3.isChecked -> 3.0
            else -> 0.0
        }


        val featuredOnly = binding.cbFeaturedOnly.isChecked
        val trendingOnly = binding.cbTrendingOnly.isChecked


        val filterOptions = FilterOptions(
            categories = categories,
            maxDistance = distance,
            minRating = minRating,
            featuredOnly = featuredOnly,
            trendingOnly = trendingOnly
        )

        onFiltersApplied(filterOptions)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private fun hideSystemBars() {
        dialog?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.apply {
                hide(WindowInsetsCompat.Type.navigationBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    companion object {
        const val TAG = "MapFiltersBottomSheet"
    }
}

data class FilterOptions(
    val categories: List<String> = emptyList(),
    val maxDistance: Float? = null,
    val minRating: Double = 0.0,
    val featuredOnly: Boolean = false,
    val trendingOnly: Boolean = false
)
