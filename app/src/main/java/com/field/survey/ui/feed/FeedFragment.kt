package com.field.survey.ui.feed

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
import com.field.survey.databinding.FragmentFeedBinding
import com.field.survey.ui.adapter.PostAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FeedViewModel by viewModels()
    private lateinit var adapter: PostAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PostAdapter { point ->
            val action = FeedFragmentDirections.actionFeedToDetail(point.id)
            findNavController().navigate(action)
        }

        binding.rvPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPosts.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }

        binding.fabAdd.setOnClickListener {
            findNavController().navigate(R.id.action_feed_to_addPoint)
        }

        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
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

        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            adapter.submitList(posts)
            binding.tvEmpty.isVisible = posts.isEmpty()
        }

        viewModel.isRefreshing.observe(viewLifecycleOwner) { refreshing ->
            binding.swipeRefresh.isRefreshing = refreshing
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
