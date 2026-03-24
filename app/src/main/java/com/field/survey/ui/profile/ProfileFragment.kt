package com.field.survey.ui.profile

import android.graphics.BitmapFactory
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
import com.field.survey.databinding.FragmentProfileBinding
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()
    private var tempPhotoFile: File? = null

    private val pickImage = registerForActivityResult(
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

    private val takePhoto = registerForActivityResult(
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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
            if (profile.photoBase64.isNotBlank()) {
                try {
                    val bytes = Base64.decode(profile.photoBase64, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    binding.ivProfilePhoto.setImageBitmap(bmp)
                } catch (_: Exception) {
                    // keep default
                }
            }
        }

        viewModel.email.observe(viewLifecycleOwner) { email ->
            binding.tvEmail.text = email
        }

        viewModel.isSaving.observe(viewLifecycleOwner) { saving ->
            binding.progressBar.isVisible = saving
            binding.btnSave.isEnabled = !saving
        }

        viewModel.saveResult.observe(viewLifecycleOwner) { msg ->
            if (msg != null) {
                binding.tvStatus.text = msg
                binding.tvStatus.isVisible = true
                binding.tvStatus.setTextColor(
                    if (msg.contains("updated")) resources.getColor(com.field.survey.R.color.zinc_400, null)
                    else resources.getColor(com.field.survey.R.color.error, null),
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
