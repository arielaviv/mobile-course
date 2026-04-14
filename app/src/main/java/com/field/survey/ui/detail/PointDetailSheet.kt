package com.field.survey.ui.detail

import android.app.Dialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.field.survey.R
import com.field.survey.databinding.FragmentPointDetailSheetBinding
import com.field.survey.domain.model.DpType
import com.field.survey.domain.model.PathCoordinates
import com.field.survey.ui.adapter.CommentAdapter
import com.field.survey.ui.util.GeoMath
import com.field.survey.ui.util.NavigateIntent
import com.field.survey.ui.util.colorRes
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class PointDetailSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentPointDetailSheetBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DetailViewModel by viewModels()
    private lateinit var commentAdapter: CommentAdapter

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPointDetailSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        val sheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) ?: return
        val behavior = BottomSheetBehavior.from(sheet)
        val density = resources.displayMetrics.density
        behavior.peekHeight = (360 * density).toInt()
        behavior.isFitToContents = true
        behavior.skipCollapsed = false
        behavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        commentAdapter = CommentAdapter()
        binding.rvComments.layoutManager = LinearLayoutManager(requireContext())
        binding.rvComments.adapter = commentAdapter
        binding.rvComments.isNestedScrollingEnabled = false

        val pointId =
            arguments?.getString(ARG_POINT_ID)
                ?: run {
                    dismiss()
                    return
                }

        binding.btnViewAll.setOnClickListener {
            dismiss()
            findNavController().navigate(
                R.id.detailFragment,
                bundleOf("pointId" to pointId),
            )
        }

        viewModel.point.observe(viewLifecycleOwner) { point ->
            if (point == null) return@observe

            binding.tvLabel.text = point.label.ifBlank { "Untitled" }
            binding.chipType.text = getString(point.type.labelRes)

            applyTypeTint(point.type)

            val dateStr =
                SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    .format(Date(point.createdAt))
            val author = point.createdByName.ifBlank { "Unknown" }

            binding.tvMeta.text =
                if (point.type.isLine) {
                    val verts = PathCoordinates.decode(point.pathCoordinates)
                    val lengthM = GeoMath.polylineMeters(verts)
                    "$author · ${verts.size} vertices · ${GeoMath.formatDistance(lengthM)} · $dateStr"
                } else {
                    "$author · %.5f, %.5f · $dateStr".format(point.latitude, point.longitude)
                }

            binding.tvNotes.text = point.notes.ifBlank { "No description" }
            binding.tvNotes.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (point.notes.isBlank()) R.color.zinc_400 else R.color.zinc_100,
                ),
            )

            val bitmap =
                decodeBitmap(point.imageBase64) ?: decodeFile(point.photoPath)
            if (bitmap != null) {
                binding.ivPhoto.setImageBitmap(bitmap)
                binding.photoCard.isVisible = true
            } else {
                binding.photoCard.isVisible = false
            }

            binding.btnNavigate.setOnClickListener {
                NavigateIntent.launch(
                    context = requireContext(),
                    latitude = point.latitude,
                    longitude = point.longitude,
                    label = point.label,
                )
            }
            binding.btnShare.setOnClickListener {
                NavigateIntent.share(
                    context = requireContext(),
                    latitude = point.latitude,
                    longitude = point.longitude,
                    label = point.label,
                )
            }
            binding.btnEdit.setOnClickListener {
                dismiss()
                findNavController().navigate(
                    R.id.editPointFragment,
                    bundleOf("pointId" to pointId),
                )
            }
        }

        viewModel.comments.observe(viewLifecycleOwner) { comments ->
            val preview = comments.take(COMMENTS_PREVIEW_LIMIT)
            commentAdapter.submitList(preview)
            binding.tvNoComments.isVisible = comments.isEmpty()
            binding.tvCommentsHeader.text =
                if (comments.isEmpty()) "Comments" else "Comments (${comments.size})"
            binding.btnViewAll.isVisible = comments.size > COMMENTS_PREVIEW_LIMIT || comments.isNotEmpty()
        }
    }

    private fun applyTypeTint(type: DpType) {
        val colorRes = type.colorRes()
        val tint = ContextCompat.getColorStateList(requireContext(), colorRes)
        binding.chipType.chipStrokeColor = tint
        binding.chipType.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
        binding.chipType.chipStrokeWidth = resources.displayMetrics.density * 1.5f
    }

    private fun decodeBitmap(base64: String?): android.graphics.Bitmap? {
        if (base64.isNullOrBlank()) return null
        return runCatching {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    private fun decodeFile(path: String?): android.graphics.Bitmap? {
        if (path.isNullOrBlank()) return null
        return runCatching { BitmapFactory.decodeFile(File(path).absolutePath) }.getOrNull()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "PointDetailSheet"
        private const val ARG_POINT_ID = "pointId"
        private const val COMMENTS_PREVIEW_LIMIT = 3

        fun newInstance(pointId: String): PointDetailSheet =
            PointDetailSheet().apply {
                arguments = bundleOf(ARG_POINT_ID to pointId)
            }
    }
}
