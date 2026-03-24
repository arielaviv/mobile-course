package com.field.survey.ui.editpoint

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.field.survey.databinding.FragmentEditPointBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditPointFragment : Fragment() {
    private var _binding: FragmentEditPointBinding? = null
    private val binding get() = _binding!!
    private val args: EditPointFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditPointBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val pointId = args.pointId
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
