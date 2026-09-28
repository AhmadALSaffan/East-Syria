package com.example.eastsyria.Map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.eastsyria.R
import com.example.eastsyria.databinding.BottomSheetMapFiltersBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip

/**
 * The map's filters. The category chips are built from the categories that
 * exist in the catalog (they used to be four fixed labels), and the distance
 * slider is only offered when the phone has a location to measure from.
 */
class MapFiltersBottomSheet(
    private val categories: List<String>,
    private val current: FilterOptions,
    private val hasLocation: Boolean,
    private val onFiltersApplied: (FilterOptions) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetMapFiltersBinding? = null
    private val binding get() = _binding!!

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
        buildCategoryChips()
        setupDistanceSlider()
        restoreState()
        setupClickListeners()
    }

    private fun buildCategoryChips() {
        val inflater = LayoutInflater.from(requireContext())
        categories.forEach { category ->
            val chip = inflater.inflate(R.layout.item_filter_chip, binding.chipGroupCategory, false) as Chip
            chip.text = category
            chip.tag = category
            chip.isChecked = category in current.categories
            binding.chipGroupCategory.addView(chip)
        }
    }

    private fun setupDistanceSlider() {
        binding.sliderDistance.isEnabled = hasLocation
        binding.tvDistanceHint.visibility = if (hasLocation) View.GONE else View.VISIBLE
        binding.sliderDistance.addOnChangeListener { _, value, _ -> showDistance(value) }
    }

    private fun showDistance(value: Float) {
        binding.tvDistanceValue.text = if (value >= 100f) {
            getString(R.string.map_filter_any_distance)
        } else {
            getString(R.string.map_filter_distance_km, value.toInt())
        }
    }

    private fun restoreState() {
        binding.sliderDistance.value = current.maxDistance?.coerceIn(5f, 100f) ?: 100f
        showDistance(binding.sliderDistance.value)

        when {
            current.minRating >= 4.5 -> binding.chipRating45.isChecked = true
            current.minRating >= 4.0 -> binding.chipRating4.isChecked = true
            current.minRating >= 3.0 -> binding.chipRating3.isChecked = true
            else -> binding.chipRatingAny.isChecked = true
        }
        binding.cbFeaturedOnly.isChecked = current.featuredOnly
        binding.cbTrendingOnly.isChecked = current.trendingOnly
    }

    private fun setupClickListeners() {
        binding.btnClose.setOnClickListener { dismiss() }
        binding.btnClearFilters.setOnClickListener { clearAllFilters() }
        binding.btnApplyFilters.setOnClickListener { applyFilters() }
    }

    private fun clearAllFilters() {
        binding.chipGroupCategory.clearCheck()
        binding.sliderDistance.value = 100f
        showDistance(100f)
        binding.chipRatingAny.isChecked = true
        binding.cbFeaturedOnly.isChecked = false
        binding.cbTrendingOnly.isChecked = false
    }

    private fun applyFilters() {
        val selected = (0 until binding.chipGroupCategory.childCount)
            .map { binding.chipGroupCategory.getChildAt(it) as Chip }
            .filter { it.isChecked }
            .map { it.tag as String }

        val distance = if (!hasLocation || binding.sliderDistance.value >= 100f) {
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

        onFiltersApplied(
            FilterOptions(
                categories = selected,
                maxDistance = distance,
                minRating = minRating,
                featuredOnly = binding.cbFeaturedOnly.isChecked,
                trendingOnly = binding.cbTrendingOnly.isChecked
            )
        )
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

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
) {
    fun isActive(): Boolean = categories.isNotEmpty() || maxDistance != null ||
            minRating > 0.0 || featuredOnly || trendingOnly
}
