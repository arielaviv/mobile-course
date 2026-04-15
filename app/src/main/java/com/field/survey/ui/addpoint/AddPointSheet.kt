package com.field.survey.ui.addpoint

import android.Manifest
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.field.survey.R
import com.field.survey.databinding.FragmentAddPointBinding
import com.field.survey.domain.model.DpType
import com.field.survey.ui.util.bindErrorText
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class AddPointSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentAddPointBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddPointViewModel by viewModels()

    private var photoUri: Uri? = null

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && photoUri != null) {
                binding.ivPhoto.setImageURI(photoUri)
                viewModel.setPhotoPath(photoUri?.path)
            }
        }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                binding.ivPhoto.setImageURI(uri)
                val path = copyUriToLocal(uri)
                viewModel.setPhotoPath(path)
            }
        }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) {
                viewModel.fetchLocation(requireContext())
            }
        }

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
        _binding = FragmentAddPointBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) ?: return
        val behavior = BottomSheetBehavior.from(bottomSheet)
        val density = resources.displayMetrics.density
        behavior.peekHeight = (360 * density).toInt()
        behavior.isFitToContents = false
        behavior.halfExpandedRatio = 0.75f
        behavior.isHideable = true
        behavior.skipCollapsed = true
        behavior.state = BottomSheetBehavior.STATE_EXPANDED

        bottomSheet.layoutParams =
            bottomSheet.layoutParams.apply {
                height = android.view.ViewGroup.LayoutParams.MATCH_PARENT
            }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupTypeChips()

        if (!viewModel.typeLocked) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }

        binding.btnTakePhoto.setOnClickListener {
            val file = File(requireContext().cacheDir, "photo_${System.currentTimeMillis()}.jpg")
            photoUri =
                FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    file,
                )
            viewModel.setPhotoPath(file.absolutePath)
            try {
                takePictureLauncher.launch(photoUri!!)
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(requireContext(), R.string.camera_unavailable, Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnGallery.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnSave.setOnClickListener {
            val label = binding.etLabel.text.toString()
            val notes = binding.etNotes.text.toString()
            viewModel.save(label, notes)
        }

        binding.btnAnalyzeAi.setOnClickListener {
            viewModel.analyzePhoto()
        }

        viewModel.photoPath.observe(viewLifecycleOwner) { path ->
            binding.btnAnalyzeAi.isEnabled = !path.isNullOrBlank() && viewModel.isAnalyzing.value != true
        }

        viewModel.isAnalyzing.observe(viewLifecycleOwner) { analyzing ->
            binding.btnAnalyzeAi.isEnabled = analyzing == false && !viewModel.photoPath.value.isNullOrBlank()
            binding.btnAnalyzeAi.text =
                if (analyzing == true) getString(R.string.ai_analyze_loading) else getString(R.string.ai_analyze_button)
        }

        viewModel.aiNotes.observe(viewLifecycleOwner) { notes ->
            if (notes.isNullOrBlank()) return@observe
            binding.etNotes.setText(notes)
            Snackbar.make(binding.root, R.string.ai_analyze_success, Snackbar.LENGTH_SHORT).show()
            viewModel.consumeAiNotes()
        }

        viewModel.aiError.observe(viewLifecycleOwner) { err ->
            if (err == null) return@observe
            Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
            viewModel.consumeAiError()
        }

        expandSheetOnFocus(binding.etLabel)
        expandSheetOnFocus(binding.etNotes)

        viewModel.latitude.observe(viewLifecycleOwner) { updateLocationDisplay() }
        viewModel.longitude.observe(viewLifecycleOwner) { updateLocationDisplay() }
        viewModel.accuracy.observe(viewLifecycleOwner) { updateAccuracyPill(it) }

        viewModel.isSaving.observe(viewLifecycleOwner) { saving ->
            binding.btnSave.isVisible = !saving
            binding.progressBar.isVisible = saving
        }

        bindErrorText(viewModel.error, binding.tvError)

        viewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            if (success) dismiss()
        }
    }

    private fun expandSheetOnFocus(view: View) {
        view.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) return@setOnFocusChangeListener
            val sheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) ?: return@setOnFocusChangeListener
            BottomSheetBehavior.from(sheet).state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun setupTypeChips() {
        val locked = viewModel.typeLocked
        val presetType = viewModel.selectedType.value ?: DpType.POLES
        val types = if (locked) listOf(presetType) else DpType.entries

        types.forEach { type ->
            val chip =
                Chip(requireContext()).apply {
                    text = getString(type.labelRes)
                    isCheckable = !locked
                    isChecked = type == presetType
                    tag = type
                    isClickable = !locked
                }
            binding.chipGroupType.addView(chip)
        }

        if (locked) return

        binding.chipGroupType.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chip = group.findViewById<Chip>(checkedIds.first())
                val type = chip?.tag as? DpType ?: return@setOnCheckedStateChangeListener
                viewModel.setSelectedType(type)
                updateLocationDisplay()
                updateAccuracyPill(viewModel.accuracy.value)
            }
        }
    }

    private fun updateAccuracyPill(accuracy: Float?) {
        val type = viewModel.selectedType.value ?: DpType.POLES
        if (accuracy == null || type.isLine) {
            binding.chipAccuracy.isVisible = false
            return
        }
        val bandColor =
            when {
                accuracy <= 3f -> android.graphics.Color.parseColor("#10B981")
                accuracy <= 8f -> android.graphics.Color.parseColor("#F59E0B")
                else -> android.graphics.Color.parseColor("#EF4444")
            }
        binding.chipAccuracy.text = "±%.0fm".format(accuracy)
        binding.chipAccuracy.setTextColor(bandColor)
        binding.chipAccuracy.chipStrokeColor = android.content.res.ColorStateList.valueOf(bandColor)
        binding.chipAccuracy.isVisible = true
    }

    private fun updateLocationDisplay() {
        val type = viewModel.selectedType.value ?: DpType.POLES
        if (type.isLine) {
            val label = getString(type.labelRes)
            binding.tvLocation.text = getString(R.string.add_dp_line_vertices, label, viewModel.vertexCount)
            return
        }
        val lat = viewModel.latitude.value ?: 0.0
        val lng = viewModel.longitude.value ?: 0.0
        if (lat != 0.0 || lng != 0.0) {
            binding.tvLocation.text = "%.6f, %.6f".format(lat, lng)
        } else {
            binding.tvLocation.text = getString(R.string.add_dp_fetching_location)
        }
    }

    private fun copyUriToLocal(uri: Uri): String? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val file = File(requireContext().cacheDir, "gallery_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddPointSheet"

        fun newInstance(
            latitude: Float = 0f,
            longitude: Float = 0f,
            dpType: String = "",
            pathCoordinates: String = "",
        ): AddPointSheet =
            AddPointSheet().apply {
                arguments =
                    bundleOf(
                        "latitude" to latitude,
                        "longitude" to longitude,
                        "dpType" to dpType,
                        "pathCoordinates" to pathCoordinates,
                    )
            }
    }
}
