package com.field.survey.ui.stats

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.field.survey.data.repository.DistributionPointRepository
import com.field.survey.domain.model.DistributionPoint
import com.field.survey.ui.util.CsvExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class StatsViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val repository: DistributionPointRepository,
    ) : ViewModel() {

        val state: LiveData<StatsUiState> =
            repository.observeAll()
                .map { compute(it) }
                .flowOn(Dispatchers.Default)
                .asLiveData()

        private val _exportFile = MutableLiveData<File?>(null)
        val exportFile: LiveData<File?> = _exportFile

        private val _exportError = MutableLiveData<Boolean>(false)
        val exportError: LiveData<Boolean> = _exportError

        fun requestExport() {
            val points = state.value?.points.orEmpty()
            if (points.isEmpty()) {
                _exportError.value = true
                return
            }
            viewModelScope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        val csv = CsvExporter.buildCsv(points)
                        CsvExporter.writeToCache(context, csv)
                    }
                }
                    .onSuccess { _exportFile.value = it }
                    .onFailure { _exportError.value = true }
            }
        }

        fun consumeExport() {
            _exportFile.value = null
        }

        fun consumeExportError() {
            _exportError.value = false
        }

        fun refresh() {
            viewModelScope.launch {
                repository.syncFromFirestore()
            }
        }

        private fun compute(points: List<DistributionPoint>): StatsUiState {
            val byType = points.groupingBy { it.type }.eachCount()

            val contributors =
                points
                    .groupBy { it.createdBy.ifBlank { "unknown" } }
                    .map { (uid, list) ->
                        val name = list.firstOrNull()?.createdByName.orEmpty().ifBlank { "Unknown" }
                        ContributorRow(
                            userId = uid,
                            name = name,
                            count = list.size,
                            initial = name.firstOrNull()?.uppercase().orEmpty().take(1).ifBlank { "?" },
                        )
                    }
                    .sortedByDescending { it.count }
                    .take(5)

            val todayStart = startOfToday()
            val oneDayMs = 24L * 60 * 60 * 1000
            val weekly = IntArray(7)
            points.forEach { p ->
                val dayStart = startOfDay(p.createdAt)
                val diff = ((todayStart - dayStart) / oneDayMs).toInt()
                if (diff in 0..6) weekly[6 - diff]++
            }

            return StatsUiState(
                total = points.size,
                byType = byType,
                topContributors = contributors,
                weeklyBuckets = weekly.toList(),
                points = points,
            )
        }

        private fun startOfToday(): Long = startOfDay(System.currentTimeMillis())

        private fun startOfDay(epoch: Long): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = epoch
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
    }
