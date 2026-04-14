package com.field.survey.ui.editpoint

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.field.survey.databinding.FragmentEditPointBinding
import com.field.survey.domain.model.DpType
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class EditPointFragment : Fragment() {

    private var _binding: FragmentEditPointBinding? = null
    private val binding get() = _binding!!
    private val args: EditPointFragmentArgs by navArgs()
    private val viewModel: EditPointViewModel by viewModels()

    private var photoUri: Uri? = null
    private var selectedType: DpType = DpType.POLES

    private val takePictureLauncher =
        registerForActivityResult(
            ActivityResultContracts.TakePicture(),
        ) { success ->
            if (success && photoUri != null) {
                binding.ivPhoto.setImageURI(photoUri)
                viewModel.setPhotoPath(photoUri?.path)
            }
        }

    private val pickImageLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetContent(),
        ) { uri ->
            if (uri != null) {
                binding.ivPhoto.setImageURI(uri)
                val path = copyUriToLocal(uri)
                viewModel.setPhotoPath(path)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentEditPointBinding.inflate(inflater, container, false)
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

        setupTypeChips()

        binding.btnTakePhoto.setOnClickListener {
            val file = File(requireContext().cacheDir, "photo_${System.currentTimeMillis()}.jpg")
            photoUri =
                FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    file,
                )
            viewModel.setPhotoPath(file.absolutePath)
            takePictureLauncher.launch(photoUri!!)
        }

        binding.btnGallery.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnSave.setOnClickListener {
            val label = binding.etLabel.text.toString()
            val notes = binding.etNotes.text.toString()
            viewModel.save(label, notes, selectedType)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressLoading.isVisible = loading
            binding.scrollView.isVisible = !loading
        }

        viewModel.point.observe(viewLifecycleOwner) { point ->
            if (point == null) return@observe
            binding.etLabel.setText(point.label)
            binding.etNotes.setText(point.notes)
            selectedType = point.type
            selectTypeChip(point.type)

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

        viewModel.isSaving.observe(viewLifecycleOwner) { saving ->
            binding.btnSave.isVisible = !saving
            binding.progressBar.isVisible = saving
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            binding.tvError.isVisible = error != null
            binding.tvError.text = error
        }

        viewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                findNavController().popBackStack()
            }
        }
    }

    private fun setupTypeChips() {
        DpType.entries.forEach { type ->
            val chip =
                Chip(requireContext()).apply {
                    text = getString(type.labelRes)
                    isCheckable = true
                    tag = type
                }
            binding.chipGroupType.addView(chip)
        }

        binding.chipGroupType.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chip = group.findViewById<Chip>(checkedIds.first())
                val type = chip?.tag as? DpType ?: return@setOnCheckedStateChangeListener
                selectedType = type
            }
        }
    }

    private fun selectTypeChip(type: DpType) {
        for (i in 0 until binding.chipGroupType.childCount) {
            val chip = binding.chipGroupType.getChildAt(i) as? Chip ?: continue
            chip.isChecked = chip.tag == type
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
}
