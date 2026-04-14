package com.field.survey.domain.model

import org.json.JSONArray

object PathCoordinates {

    fun encode(points: List<Pair<Double, Double>>): String {
        val array = JSONArray()
        points.forEach { (lng, lat) ->
            val pair = JSONArray()
            pair.put(lng)
            pair.put(lat)
            array.put(pair)
        }
        return array.toString()
    }

    fun decode(json: String?): List<Pair<Double, Double>> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val pair = array.getJSONArray(i)
                pair.getDouble(0) to pair.getDouble(1)
            }
        }.getOrDefault(emptyList())
    }
}
