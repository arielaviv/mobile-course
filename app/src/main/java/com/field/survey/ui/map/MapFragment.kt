package com.field.survey.ui.map

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.field.survey.R
import com.field.survey.databinding.FragmentMapBinding
import com.field.survey.domain.model.DistributionPoint
import com.field.survey.domain.model.DpType
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.CircleLayer
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.style.layers.getLayerAs
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.extension.style.terrain.generated.Terrain
import com.mapbox.maps.extension.style.terrain.generated.removeTerrain
import com.mapbox.maps.extension.style.terrain.generated.setTerrain
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.addOnMapLongClickListener
import com.mapbox.maps.plugin.locationcomponent.location
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MapViewModel by viewModels()

    private var isSatellite = false
    private var terrainEnabled = false
    private var drawMode = false
    private val drawnPoints = mutableListOf<Point>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        val mapView = binding.mapView

        mapView.mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(34.7725, 32.0750))
                .zoom(13.0)
                .build(),
        )

        mapView.mapboxMap.loadStyle(Style.STANDARD) { style ->
            setupSources(style)
            setupLayers(style)
            enableLocationPuck()
            observePoints()
        }

        // satellite toggle
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_satellite -> {
                    isSatellite = !isSatellite
                    item.title = if (isSatellite) "Street" else "Satellite"
                    val newStyle = if (isSatellite) Style.SATELLITE_STREETS else Style.STANDARD
                    mapView.mapboxMap.loadStyle(newStyle) { style ->
                        setupSources(style)
                        setupLayers(style)
                        if (terrainEnabled) enableTerrain(style)
                        viewModel.posts.value?.let { updateGeoJsonSource(it) }
                    }
                    true
                }
                R.id.action_terrain -> {
                    terrainEnabled = !terrainEnabled
                    item.title = if (terrainEnabled) "Flat" else "3D Terrain"
                    mapView.mapboxMap.style?.let { style ->
                        if (terrainEnabled) enableTerrain(style) else disableTerrain(style)
                    }
                    true
                }
                else -> false
            }
        }

        // long press -> add point
        mapView.mapboxMap.addOnMapLongClickListener { point ->
            if (!drawMode) {
                val action = MapFragmentDirections.actionMapToAddPoint(
                    latitude = point.latitude().toFloat(),
                    longitude = point.longitude().toFloat(),
                )
                findNavController().navigate(action)
            }
            true
        }

        // tap -> detail or draw vertex
        mapView.mapboxMap.addOnMapClickListener { point ->
            if (drawMode) {
                drawnPoints.add(point)
                updateDrawLine()
                return@addOnMapClickListener true
            }

            // check if tapped on a point marker
            val pixel = mapView.mapboxMap.pixelForCoordinate(point)
            mapView.mapboxMap.queryRenderedFeatures(
                com.mapbox.maps.RenderedQueryGeometry(pixel),
                com.mapbox.maps.RenderedQueryOptions(listOf(UNCLUSTERED_LAYER_ID), null),
            ) { result ->
                result.value?.firstOrNull()?.let { queriedFeature ->
                    val pointId = queriedFeature.queriedFeature.feature.getStringProperty("id")
                    if (pointId != null) {
                        val action = MapFragmentDirections.actionMapToDetail(pointId)
                        findNavController().navigate(action)
                    }
                }
            }
            true
        }

        // draw mode fab
        binding.fabDraw.setOnClickListener {
            drawMode = !drawMode
            if (drawMode) {
                drawnPoints.clear()
                Toast.makeText(requireContext(), "Tap map to draw. Tap again to finish.", Toast.LENGTH_SHORT).show()
                binding.fabDraw.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            } else {
                binding.fabDraw.setImageResource(android.R.drawable.ic_menu_edit)
                // keep drawn line visible
            }
        }
    }

    private fun setupSources(style: Style) {
        // points source with clustering
        if (style.getSourceAs<GeoJsonSource>(POINTS_SOURCE_ID) == null) {
            val source = GeoJsonSource.Builder(POINTS_SOURCE_ID)
                .cluster(true)
                .clusterRadius(50)
                .clusterMaxZoom(14)
                .build()
            style.addSource(source)
        }

        // draw line source
        if (style.getSourceAs<GeoJsonSource>(DRAW_SOURCE_ID) == null) {
            val drawSource = GeoJsonSource.Builder(DRAW_SOURCE_ID).build()
            style.addSource(drawSource)
        }
    }

    private fun setupLayers(style: Style) {
        // cluster circles
        if (style.getLayerAs<CircleLayer>(CLUSTER_LAYER_ID) == null) {
            val clusterLayer = CircleLayer(CLUSTER_LAYER_ID, POINTS_SOURCE_ID)
            clusterLayer.filter(Expression.has("point_count"))
            clusterLayer.circleColor(Color.parseColor("#3B82F6"))
            clusterLayer.circleRadius(20.0)
            clusterLayer.circleStrokeWidth(2.0)
            clusterLayer.circleStrokeColor(Color.WHITE)
            style.addLayer(clusterLayer)
        }

        // cluster count text
        if (style.getLayerAs<SymbolLayer>(CLUSTER_COUNT_LAYER_ID) == null) {
            val countLayer = SymbolLayer(CLUSTER_COUNT_LAYER_ID, POINTS_SOURCE_ID)
            countLayer.filter(Expression.has("point_count"))
            countLayer.textSize(12.0)
            countLayer.textColor(Color.WHITE)
            style.addLayer(countLayer)
        }

        // unclustered points
        if (style.getLayerAs<CircleLayer>(UNCLUSTERED_LAYER_ID) == null) {
            val pointLayer = CircleLayer(UNCLUSTERED_LAYER_ID, POINTS_SOURCE_ID)
            pointLayer.filter(Expression.not(Expression.has("point_count")))
            pointLayer.circleRadius(8.0)
            pointLayer.circleColor(Color.parseColor("#3B82F6"))
            pointLayer.circleStrokeWidth(2.0)
            pointLayer.circleStrokeColor(Color.WHITE)
            style.addLayer(pointLayer)
        }

        // draw line layer
        if (style.getLayerAs<LineLayer>(DRAW_LINE_LAYER_ID) == null) {
            val lineLayer = LineLayer(DRAW_LINE_LAYER_ID, DRAW_SOURCE_ID)
            lineLayer.lineColor(Color.parseColor("#F59E0B"))
            lineLayer.lineWidth(3.0)
            style.addLayer(lineLayer)
        }
    }

    private fun enableLocationPuck() {
        binding.mapView.location.enabled = true
        binding.mapView.location.pulsingEnabled = true
    }

    private fun enableTerrain(style: Style) {
        try {
            if (style.getSourceAs<com.mapbox.maps.extension.style.sources.generated.RasterDemSource>("terrain-source") == null) {
                val demSource = com.mapbox.maps.extension.style.sources.generated.RasterDemSource.Builder("terrain-source")
                    .url("mapbox://mapbox.mapbox-terrain-dem-v1")
                    .build()
                style.addSource(demSource)
            }
            style.setTerrain(Terrain("terrain-source"))
        } catch (_: Exception) {
            // terrain not supported on this device
        }
    }

    private fun disableTerrain(style: Style) {
        try {
            style.removeTerrain()
        } catch (_: Exception) {}
    }

    private fun observePoints() {
        viewModel.posts.observe(viewLifecycleOwner) { points ->
            updateGeoJsonSource(points)
        }
    }

    private fun updateGeoJsonSource(points: List<DistributionPoint>) {
        val features = points.map { dp ->
            Feature.fromGeometry(
                Point.fromLngLat(dp.longitude, dp.latitude),
            ).apply {
                addStringProperty("id", dp.id)
                addStringProperty("label", dp.label)
                addStringProperty("type", dp.type.name)
                addStringProperty("notes", dp.notes)
            }
        }
        binding.mapView.mapboxMap.style?.let { style ->
            style.getSourceAs<GeoJsonSource>(POINTS_SOURCE_ID)
                ?.featureCollection(FeatureCollection.fromFeatures(features))
        }
    }

    private fun updateDrawLine() {
        if (drawnPoints.size < 2) return
        val lineString = LineString.fromLngLats(drawnPoints)
        binding.mapView.mapboxMap.style?.let { style ->
            style.getSourceAs<GeoJsonSource>(DRAW_SOURCE_ID)
                ?.geometry(lineString)
        }
    }

    override fun onStart() {
        super.onStart()
        _binding?.mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        _binding?.mapView?.onStop()
    }

    override fun onDestroyView() {
        _binding?.mapView?.onDestroy()
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val POINTS_SOURCE_ID = "points-source"
        private const val DRAW_SOURCE_ID = "draw-source"
        private const val CLUSTER_LAYER_ID = "cluster-layer"
        private const val CLUSTER_COUNT_LAYER_ID = "cluster-count-layer"
        private const val UNCLUSTERED_LAYER_ID = "unclustered-layer"
        private const val DRAW_LINE_LAYER_ID = "draw-line-layer"
    }
}
