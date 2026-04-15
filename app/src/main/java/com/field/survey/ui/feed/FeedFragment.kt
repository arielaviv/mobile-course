package com.field.survey.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.field.survey.R
import com.field.survey.databinding.FragmentFeedBinding
import com.field.survey.ui.adapter.PostPagingAdapter
import com.field.survey.ui.addpoint.AddPointSheet
import com.field.survey.ui.detail.PointDetailSheet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FeedViewModel by viewModels()
    private lateinit var adapter: PostPagingAdapter
    private lateinit var recentAdapter: RecentActivityAdapter
    private var shimmerRunning = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        adapter =
            PostPagingAdapter(
                onClick = { point ->
                    val action = FeedFragmentDirections.actionFeedToDetail(point.id)
                    findNavController().navigate(action)
                },
            )

        binding.rvPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPosts.adapter = adapter

        recentAdapter =
            RecentActivityAdapter { point ->
                PointDetailSheet.newInstance(point.id).show(parentFragmentManager, PointDetailSheet.TAG)
            }
        binding.rvRecent.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        binding.rvRecent.adapter = recentAdapter

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
            adapter.refresh()
        }

        binding.fabAdd.setOnClickListener {
            AddPointSheet.newInstance().show(parentFragmentManager, AddPointSheet.TAG)
        }

        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_stats -> {
                    findNavController().navigate(R.id.action_feed_to_stats)
                    true
                }
                R.id.action_profile -> {
                    findNavController().navigate(R.id.action_feed_to_profile)
                    true
                }
                R.id.action_settings -> {
                    findNavController().navigate(R.id.action_feed_to_settings)
                    true
                }
                else -> false
            }
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_feed -> true
                R.id.nav_map -> {
                    findNavController().navigate(R.id.action_feed_to_map)
                    true
                }
                R.id.nav_my_points -> {
                    findNavController().navigate(R.id.action_feed_to_myPoints)
                    true
                }
                else -> false
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pagedPosts.collect { adapter.submitData(it) }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                adapter.loadStateFlow.collect { loadStates ->
                    val isInitialLoading =
                        loadStates.refresh is LoadState.Loading && adapter.itemCount == 0
                    binding.shimmerFeed.isVisible = isInitialLoading
                    if (isInitialLoading && !shimmerRunning) {
                        binding.shimmerFeed.startShimmer()
                        shimmerRunning = true
                    } else if (!isInitialLoading && shimmerRunning) {
                        binding.shimmerFeed.stopShimmer()
                        shimmerRunning = false
                    }

                    binding.tvEmpty.isVisible =
                        loadStates.refresh is LoadState.NotLoading && adapter.itemCount == 0
                }
            }
        }

        viewModel.recentActivity.observe(viewLifecycleOwner) { recent ->
            recentAdapter.submitList(recent)
            val show = recent.isNotEmpty()
            binding.tvRecentTitle.isVisible = show
            binding.rvRecent.isVisible = show
        }

        viewModel.isRefreshing.observe(viewLifecycleOwner) { refreshing ->
            binding.swipeRefresh.isRefreshing = refreshing
        }
    }

    override fun onPause() {
        super.onPause()
        if (shimmerRunning) {
            _binding?.shimmerFeed?.stopShimmer()
            shimmerRunning = false
        }
    }

    override fun onDestroyView() {
        _binding?.shimmerFeed?.stopShimmer()
        shimmerRunning = false
        super.onDestroyView()
        _binding = null
    }
}
