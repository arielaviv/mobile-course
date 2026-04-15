package com.field.survey.ui.util

import android.content.Context
import com.field.survey.domain.model.DistributionPoint
import java.io.File
import java.time.Instant

object CsvExporter {

    private const val HEADER =
        "id,label,type,latitude,longitude,notes,createdBy,createdByName,createdAtIso,updatedAtIso"

    fun buildCsv(points: List<DistributionPoint>): String =
        buildString {
            appendLine(HEADER)
            points.forEach { p ->
                val row =
                    listOf(
                        p.id,
                        p.label,
                        p.type.name,
                        p.latitude.toString(),
                        p.longitude.toString(),
                        p.notes,
                        p.createdBy,
                        p.createdByName,
                        formatIso(p.createdAt),
                        p.updatedAt?.let(::formatIso).orEmpty(),
                    ).joinToString(",") { escape(it) }
                appendLine(row)
            }
        }

    fun writeToCache(
        context: Context,
        csv: String,
    ): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "field_survey_points_${System.currentTimeMillis()}.csv")
        file.writeText(csv)
        return file
    }

    private fun escape(value: String): String {
        val needsQuoting =
            value.contains(',') ||
                value.contains('"') ||
                value.contains('\n') ||
                value.contains('\r')
        return if (needsQuoting) "\"${value.replace("\"", "\"\"")}\"" else value
    }

    private fun formatIso(epochMs: Long): String = Instant.ofEpochMilli(epochMs).toString()
}
