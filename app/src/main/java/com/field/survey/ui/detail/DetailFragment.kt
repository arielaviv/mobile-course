package com.field.survey.ui.detail

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.field.survey.R
import com.field.survey.databinding.FragmentDetailBinding
import com.field.survey.ui.adapter.CommentAdapter
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class DetailFragment : Fragment() {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!
    private val args: DetailFragmentArgs by navArgs()
    private val viewModel: DetailViewModel by viewModels()
    private lateinit var commentAdapter: CommentAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        commentAdapter = CommentAdapter()
        binding.rvComments.layoutManager = LinearLayoutManager(requireContext())
        binding.rvComments.adapter = commentAdapter

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnSend.setOnClickListener {
            val text = binding.etComment.text.toString()
            if (text.isNotBlank()) {
                viewModel.addComment(text)
                binding.etComment.text?.clear()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.isVisible = loading
            binding.scrollView.isVisible = !loading
        }

        viewModel.point.observe(viewLifecycleOwner) { point ->
            if (point == null) return@observe

            binding.tvLabel.text = point.label
            binding.tvType.text = getString(point.type.labelRes)
            binding.tvNotes.text = point.notes.ifBlank { getString(R.string.point_no_description) }
            val author = point.createdByName.ifBlank { getString(R.string.point_author_unknown) }
            binding.tvAuthor.text = getString(R.string.detail_author, author)

            val dateStr =
                SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    .format(Date(point.createdAt))
            binding.tvLocation.text =
                "%.4f, %.4f · %s".format(
                    point.latitude, point.longitude, dateStr,
                )

            if (!point.imageBase64.isNullOrBlank()) {
                try {
                    val bytes = Base64.decode(point.imageBase64, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    binding.ivPhoto.setImageBitmap(bmp)
                } catch (_: Exception) {
                    binding.ivPhoto.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            }
        }

        viewModel.comments.observe(viewLifecycleOwner) { comments ->
            commentAdapter.submitList(comments)
            binding.tvNoComments.isVisible = comments.isEmpty()
            binding.tvCommentsHeader.text =
                if (comments.isEmpty()) {
                    getString(R.string.comments_header)
                } else {
                    getString(R.string.comments_header_count, comments.size)
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
