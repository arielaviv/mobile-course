package com.field.survey.ui.util

import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData

fun Fragment.bindErrorText(
    source: LiveData<Int?>,
    target: TextView,
) {
    source.observe(viewLifecycleOwner) { res ->
        target.isVisible = res != null
        target.text = res?.let { getString(it) }
    }
}
