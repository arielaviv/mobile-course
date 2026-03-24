package com.field.survey.ui.mypoints

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.field.survey.R
import com.field.survey.databinding.FragmentMyPointsBinding
import com.field.survey.domain.model.DistributionPoint
import com.field.survey.ui.adapter.PostAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MyPointsFragment : Fragment() {

    private var _binding: FragmentMyPointsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MyPointsViewModel by viewModels()
    private lateinit var adapter: PostAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMyPointsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        adapter = PostAdapter(
            onClick = { point ->
                val action = MyPointsFragmentDirections.actionMyPointsToDetail(point.id)
                findNavController().navigate(action)
            },
            onEdit = { point ->
                val action = MyPointsFragmentDirections.actionMyPointsToEdit(point.id)
                findNavController().navigate(action)
            },
            onDelete = { point ->
                showDeleteDialog(point)
            },
        )

        binding.rvPoints.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPoints.adapter = adapter

        viewModel.posts.observe(viewLifecycleOwner) { points ->
            adapter.submitList(points)
            binding.emptyState.isVisible = points.isEmpty()
            binding.rvPoints.isVisible = points.isNotEmpty()
        }
    }

    private fun showDeleteDialog(point: DistributionPoint) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.my_dps_delete_title)
            .setMessage(getString(R.string.my_dps_delete_message, point.label))
            .setPositiveButton(R.string.my_dps_delete_confirm) { _, _ ->
                viewModel.deletePoint(point.id)
            }
            .setNegativeButton(R.string.my_dps_delete_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
