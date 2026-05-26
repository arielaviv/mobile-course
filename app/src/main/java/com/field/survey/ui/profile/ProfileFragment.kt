package com.field.survey.ui.profile

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
import com.field.survey.databinding.FragmentProfileBinding
import com.field.survey.ui.util.loadProfileImage
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()
    private var tempPhotoFile: File? = null

    private val pickImage =
        registerForActivityResult(
            ActivityResultContracts.GetContent(),
        ) { uri ->
            uri?.let {
                binding.ivProfilePhoto.setImageURI(it)
                val input = requireContext().contentResolver.openInputStream(it)
                val bytes = input?.readBytes()
                input?.close()
                if (bytes != null) {
                    val tempFile = File(requireContext().cacheDir, "profile_temp.jpg")
                    tempFile.writeBytes(bytes)
                    viewModel.setPhotoFromPath(tempFile.absolutePath)
                }
            }
        }

    private val takePhoto =
        registerForActivityResult(
            ActivityResultContracts.TakePicture(),
        ) { success ->
            if (success && tempPhotoFile != null) {
                binding.ivProfilePhoto.setImageURI(
                    FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.fileprovider",
                        tempPhotoFile!!,
                    ),
                )
                viewModel.setPhotoFromPath(tempPhotoFile!!.absolutePath)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
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

        binding.btnChangePhoto.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString()
            viewModel.save(name)
        }

        viewModel.profile.observe(viewLifecycleOwner) { profile ->
            if (profile == null) return@observe
            binding.etName.setText(profile.name)
            binding.ivProfilePhoto.loadProfileImage(profile.photoUrl)
        }

        viewModel.email.observe(viewLifecycleOwner) { email ->
            binding.tvEmail.text = email
        }

        viewModel.isSaving.observe(viewLifecycleOwner) { saving ->
            binding.progressBar.isVisible = saving
            binding.btnSave.isEnabled = !saving
        }

        viewModel.saveResult.observe(viewLifecycleOwner) { result ->
            if (result == null) return@observe
            val (stringRes, isSuccess) =
                when (result) {
                    is ProfileViewModel.SaveResult.Success -> result.stringRes to true
                    is ProfileViewModel.SaveResult.Error -> result.stringRes to false
                }
            binding.tvStatus.text = getString(stringRes)
            binding.tvStatus.isVisible = true
            binding.tvStatus.setTextColor(
                resources.getColor(
                    if (isSuccess) com.field.survey.R.color.zinc_400 else com.field.survey.R.color.error,
                    null,
                ),
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
