package com.field.survey.ui.map

import com.field.survey.domain.model.DpType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapFiltersBus
    @Inject
    constructor() {
        private val _visibleTypes = MutableStateFlow(DpType.entries.toSet())
        val visibleTypes: StateFlow<Set<DpType>> = _visibleTypes.asStateFlow()

        fun setVisible(
            type: DpType,
            visible: Boolean,
        ) {
            _visibleTypes.value =
                if (visible) _visibleTypes.value + type else _visibleTypes.value - type
        }
    }
