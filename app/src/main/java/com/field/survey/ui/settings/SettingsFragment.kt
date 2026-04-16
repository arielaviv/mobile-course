package com.field.survey.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.field.survey.BuildConfig
import com.field.survey.R
import com.field.survey.data.repository.MapSettingsRepository
import com.field.survey.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.tvVersion.text = BuildConfig.VERSION_NAME

        bindInitialState()
        wireLightingChips()
        wireDisplaySwitches()

        binding.btnViewStats.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_stats)
        }

        binding.btnLogout.setOnClickListener {
            viewModel.logout()
            findNavController().navigate(R.id.action_settings_to_login)
        }
    }

    private fun bindInitialState() {
        val s = viewModel.state.value ?: return
        binding.chipGroupLighting.check(
            when (s.lightPreset) {
                MapSettingsRepository.PRESET_DAWN -> R.id.chipPresetDawn
                MapSettingsRepository.PRESET_DUSK -> R.id.chipPresetDusk
                MapSettingsRepository.PRESET_NIGHT -> R.id.chipPresetNight
                else -> R.id.chipPresetDay
            },
        )
        binding.switchRoadLabels.isChecked = s.roadLabels
        binding.switchPoiLabels.isChecked = s.poiLabels
        binding.switchPlaceLabels.isChecked = s.placeLabels
        binding.switch3dObjects.isChecked = s.objects3d
    }

    private fun wireLightingChips() {
        binding.chipGroupLighting.setOnCheckedStateChangeListener { _, checkedIds ->
            val preset =
                when (checkedIds.firstOrNull()) {
                    R.id.chipPresetDawn -> MapSettingsRepository.PRESET_DAWN
                    R.id.chipPresetDusk -> MapSettingsRepository.PRESET_DUSK
                    R.id.chipPresetNight -> MapSettingsRepository.PRESET_NIGHT
                    R.id.chipPresetDay -> MapSettingsRepository.PRESET_DAY
                    else -> return@setOnCheckedStateChangeListener
                }
            viewModel.setLightPreset(preset)
        }
    }

    private fun wireDisplaySwitches() {
        binding.switchRoadLabels.setOnCheckedChangeListener(onToggle(viewModel::setRoadLabels))
        binding.switchPoiLabels.setOnCheckedChangeListener(onToggle(viewModel::setPoiLabels))
        binding.switchPlaceLabels.setOnCheckedChangeListener(onToggle(viewModel::setPlaceLabels))
        binding.switch3dObjects.setOnCheckedChangeListener(onToggle(viewModel::setObjects3d))
    }

    private fun onToggle(sink: (Boolean) -> Unit): CompoundButton.OnCheckedChangeListener =
        CompoundButton.OnCheckedChangeListener { _, checked -> sink(checked) }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
