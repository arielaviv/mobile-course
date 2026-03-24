package com.field.survey.ui.addpoint

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.field.survey.R
import com.field.survey.databinding.FragmentAddPointBinding
import com.field.survey.domain.model.DpType
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class AddPointFragment : Fragment() {

    private var _binding: FragmentAddPointBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddPointViewModel by viewModels()

    private var photoUri: Uri? = null

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success && photoUri != null) {
            binding.ivPhoto.setImageURI(photoUri)
            viewModel.setPhotoPath(photoUri?.path)
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            binding.ivPhoto.setImageURI(uri)
            val path = copyUriToLocal(uri)
            viewModel.setPhotoPath(path)
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.fetchLocation(requireContext())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAddPointBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        setupTypeChips()
        requestLocationAndFetch()

        binding.btnTakePhoto.setOnClickListener {
            val file = File(requireContext().cacheDir, "photo_${System.currentTimeMillis()}.jpg")
            photoUri = FileProvider.getUriForFile(
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
            viewModel.save(label, notes)
        }

        viewModel.latitude.observe(viewLifecycleOwner) { updateLocationDisplay() }
        viewModel.longitude.observe(viewLifecycleOwner) { updateLocationDisplay() }

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
            val chip = Chip(requireContext()).apply {
                text = getString(type.labelRes)
                isCheckable = true
                isChecked = type == DpType.MANHOLE
                tag = type
            }
            binding.chipGroupType.addView(chip)
        }

        binding.chipGroupType.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chip = group.findViewById<Chip>(checkedIds.first())
                val type = chip?.tag as? DpType ?: return@setOnCheckedStateChangeListener
                viewModel.setSelectedType(type)
            }
        }
    }

    private fun requestLocationAndFetch() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
    }

    private fun updateLocationDisplay() {
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
}
