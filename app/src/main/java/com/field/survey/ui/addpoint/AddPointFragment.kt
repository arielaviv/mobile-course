package com.field.survey.ui.addpoint

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.field.survey.databinding.FragmentAddPointBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddPointFragment : Fragment() {
    private var _binding: FragmentAddPointBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddPointBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
