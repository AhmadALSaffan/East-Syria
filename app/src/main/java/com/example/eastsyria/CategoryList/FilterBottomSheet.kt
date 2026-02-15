package com.example.eastsyria.CategoryList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.eastsyria.R
import com.example.eastsyria.databinding.DialogFilterBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FilterBottomSheet(
    private val currentFilter: FilterOptions,
    private val onApply: (FilterOptions) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var binding: DialogFilterBinding
    private val tempFilter = FilterOptions()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tempFilter.minRating = currentFilter.minRating
        tempFilter.statusFilters.addAll(currentFilter.statusFilters)
        tempFilter.isFeatured = currentFilter.isFeatured
        tempFilter.isTrending = currentFilter.isTrending

        setupRatingFilter()
        setupStatusFilter()
        setupSpecialFilter()
        setupButtons()
    }

    private fun setupRatingFilter() {
        binding.apply {
            // Set initial selection
            when (tempFilter.minRating) {
                0.0 -> chipAll.isChecked = true
                4.0 -> chip4Plus1.isChecked = true
                4.5 -> chip45Plus1.isChecked = true
            }

            chipGroupRating.setOnCheckedChangeListener { _, checkedId ->
                tempFilter.minRating = when (checkedId) {
                    R.id.chip4Plus1 -> 4.0
                    R.id.chip45Plus1 -> 4.5
                    else -> 0.0
                }
            }
        }
    }

    private fun setupStatusFilter() {
        binding.apply {
            chipIntact1.isChecked = tempFilter.statusFilters.contains("Intact")
            chipRuins1.isChecked = tempFilter.statusFilters.contains("Ruins")
            chipDestroyed1.isChecked = tempFilter.statusFilters.contains("Destroyed")

            chipIntact1.setOnCheckedChangeListener { _, isChecked ->
                updateStatusFilter("Intact", isChecked)
            }

            chipRuins1.setOnCheckedChangeListener { _, isChecked ->
                updateStatusFilter("Ruins", isChecked)
            }

            chipDestroyed1.setOnCheckedChangeListener { _, isChecked ->
                updateStatusFilter("Destroyed", isChecked)
            }
        }
    }

    private fun updateStatusFilter(status: String, isChecked: Boolean) {
        if (isChecked) {
            if (!tempFilter.statusFilters.contains(status)) {
                tempFilter.statusFilters.add(status)
            }
        } else {
            tempFilter.statusFilters.remove(status)
        }
    }

    private fun setupSpecialFilter() {
        binding.apply {
            chipFeatured1.isChecked = tempFilter.isFeatured == true
            chipTrending1.isChecked = tempFilter.isTrending == true

            chipFeatured1.setOnCheckedChangeListener { _, isChecked ->
                tempFilter.isFeatured = if (isChecked) true else null
            }

            chipTrending1.setOnCheckedChangeListener { _, isChecked ->
                tempFilter.isTrending = if (isChecked) true else null
            }
        }
    }

    private fun setupButtons() {
        binding.apply {
            btnCloseFilter1.setOnClickListener {
                dismiss()
            }

            btnResetFilter1.setOnClickListener {
                tempFilter.reset()
                setupRatingFilter()
                setupStatusFilter()
                setupSpecialFilter()
            }

            btnApplyFilter1.setOnClickListener {
                onApply(tempFilter)
                dismiss()
            }
        }
    }

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme
}