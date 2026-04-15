package com.field.survey.ui.stats

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.field.survey.R
import com.field.survey.databinding.FragmentStatsBinding
import com.field.survey.databinding.RowStatsContributorBinding
import com.field.survey.databinding.RowStatsLegendBinding
import com.field.survey.domain.model.DpType
import com.field.survey.ui.util.colorRes
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StatsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
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

        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_refresh_stats) {
                viewModel.refresh()
                true
            } else {
                false
            }
        }

        binding.btnExportCsv.setOnClickListener {
            viewModel.requestExport()
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            render(state)
        }

        viewModel.exportFile.observe(viewLifecycleOwner) { file ->
            if (file == null) return@observe
            shareCsv(file)
            viewModel.consumeExport()
        }

        viewModel.exportError.observe(viewLifecycleOwner) { err ->
            if (err) {
                Toast.makeText(requireContext(), R.string.stats_export_error, Toast.LENGTH_SHORT).show()
                viewModel.consumeExportError()
            }
        }
    }

    private fun render(state: StatsUiState) {
        val empty = state.total == 0
        binding.emptyState.isVisible = empty
        binding.heroCard.isVisible = !empty
        binding.byTypeCard.isVisible = !empty
        binding.contributorsCard.isVisible = !empty
        binding.weeklyCard.isVisible = !empty
        binding.btnExportCsv.isEnabled = !empty

        if (empty) {
            binding.pieChart.setData(emptyList(), animate = false)
            binding.barChart.setData(emptyList())
            return
        }

        binding.tvHeroValue.text = state.total.toString()
        binding.tvHeroCaption.text =
            getString(R.string.stats_total_caption, state.byType.size, state.topContributors.size)

        val slices =
            state.byType
                .entries
                .sortedByDescending { it.value }
                .map { (type, count) ->
                    PieChartView.Slice(
                        color = ContextCompat.getColor(requireContext(), type.colorRes()),
                        fraction = count.toFloat() / state.total,
                    )
                }
        binding.pieChart.setData(slices)

        renderLegend(state)
        renderContributors(state)
        renderWeekly(state)
    }

    private fun renderLegend(state: StatsUiState) {
        binding.legendContainer.removeAllViews()
        val ordered = DpType.entries.filter { state.byType.getOrDefault(it, 0) > 0 }
            .sortedByDescending { state.byType.getOrDefault(it, 0) }

        ordered.forEach { type ->
            val count = state.byType.getOrDefault(type, 0)
            val pct = (count.toFloat() / state.total * 100).toInt()
            val row = RowStatsLegendBinding.inflate(layoutInflater, binding.legendContainer, false)
            row.dot.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), type.colorRes())
            row.tvLabel.text = getString(type.labelRes)
            row.tvCount.text = "$count · $pct%"
            binding.legendContainer.addView(row.root)
        }
    }

    private fun renderContributors(state: StatsUiState) {
        binding.contributorsContainer.removeAllViews()
        val maxCount = state.topContributors.maxOfOrNull { it.count } ?: 1

        state.topContributors.forEach { c ->
            val row = RowStatsContributorBinding.inflate(layoutInflater, binding.contributorsContainer, false)
            row.tvInitial.text = c.initial
            row.tvName.text = c.name
            row.tvCount.text = c.count.toString()
            row.progress.max = 100
            row.progress.setProgress((c.count.toFloat() / maxCount * 100).toInt(), true)
            binding.contributorsContainer.addView(row.root)

            val params = row.root.layoutParams as? LinearLayout.LayoutParams
            params?.topMargin = (8 * resources.displayMetrics.density).toInt()
            row.root.layoutParams = params
        }
    }

    private fun renderWeekly(state: StatsUiState) {
        val letters = listOf("M", "T", "W", "T", "F", "S", "S")
        val today = java.util.Calendar.getInstance()
        val bars =
            state.weeklyBuckets.mapIndexed { i, count ->
                val cal = (today.clone() as java.util.Calendar).apply {
                    add(java.util.Calendar.DAY_OF_YEAR, -(6 - i))
                }
                val idx = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
                BarChartView.Bar(value = count, label = letters[idx])
            }
        binding.barChart.setData(bars)
    }

    private fun shareCsv(file: File) {
        val uri =
            FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file,
            )
        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        val chooser = Intent.createChooser(intent, getString(R.string.stats_export_chooser))
        try {
            startActivity(chooser)
        } catch (_: Exception) {
            Toast.makeText(requireContext(), R.string.stats_export_error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
