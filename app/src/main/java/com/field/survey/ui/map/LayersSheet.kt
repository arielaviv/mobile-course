package com.field.survey.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.field.survey.databinding.FragmentLayersSheetBinding
import com.field.survey.databinding.ItemLayerRowBinding
import com.field.survey.domain.model.DpType
import com.field.survey.ui.util.colorRes
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LayersSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentLayersSheetBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var filtersBus: MapFiltersBus

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLayersSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        val visible = filtersBus.visibleTypes.value
        DpType.entries.forEach { type ->
            val rowBinding = ItemLayerRowBinding.inflate(layoutInflater, binding.rowsContainer, false)
            rowBinding.tvLabel.text = getString(type.labelRes)
            rowBinding.dot.setBackgroundColor(ContextCompat.getColor(requireContext(), type.colorRes()))
            rowBinding.switchVisible.isChecked = type in visible
            rowBinding.switchVisible.setOnCheckedChangeListener { _, isChecked ->
                filtersBus.setVisible(type, isChecked)
            }
            rowBinding.root.setOnClickListener {
                rowBinding.switchVisible.isChecked = !rowBinding.switchVisible.isChecked
            }
            binding.rowsContainer.addView(rowBinding.root)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "LayersSheet"

        fun newInstance(): LayersSheet = LayersSheet()
    }
}
