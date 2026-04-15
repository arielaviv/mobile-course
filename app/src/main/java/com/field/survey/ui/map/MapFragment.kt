package com.field.survey.ui.map

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.field.survey.R
import com.field.survey.data.repository.MapSettingsRepository
import com.field.survey.databinding.FragmentMapBinding
import com.field.survey.domain.model.DistributionPoint
import com.field.survey.domain.model.DpType
import com.field.survey.domain.model.PathCoordinates
import com.field.survey.ui.addpoint.AddPointSheet
import com.field.survey.ui.detail.PointDetailSheet
import com.field.survey.ui.util.GeoMath
import com.mapbox.bindgen.Value
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.ImageHolder
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.CircleLayer
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.style.layers.getLayerAs
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.extension.style.terrain.generated.Terrain
import com.mapbox.maps.extension.style.terrain.generated.removeTerrain
import com.mapbox.maps.extension.style.terrain.generated.setTerrain
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.addOnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.scalebar.scalebar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MapViewModel by viewModels()

    @Inject lateinit var mapEvents: MapEventsBus

    @Inject lateinit var filtersBus: MapFiltersBus

    @Inject lateinit var mapSettings: MapSettingsRepository

    private var isSatellite = false
    private var terrainEnabled = false
    private var pitch3D = true

    private var pendingType: DpType? = null
    private val drawnPoints = mutableListOf<Point>()

    private var measureMode = false
    private val measurePoints = mutableListOf<Point>()

    private var markers: MarkerBitmaps.MarkerSet? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
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

        markers = MarkerBitmaps.build(requireContext())

        val mapView = binding.mapView

        mapView.scalebar.enabled = false
        mapView.compass.enabled = false
        mapView.attribution.enabled = false
        mapView.logo.enabled = false
        mapView.gestures.pitchEnabled = true

        mapView.mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(34.7725, 32.0750))
                .zoom(15.0)
                .pitch(INITIAL_PITCH)
                .build(),
        )

        mapView.mapboxMap.loadStyle(Style.STANDARD) { style ->
            applyLightPreset(style)
            setupImages(style)
            setupSources(style)
            setupLayers(style)
            enableLocationPuck()
            observePoints()
        }

        mapEvents.updateMapCenter(mapView.mapboxMap.cameraState.center)
        mapView.mapboxMap.subscribeMapIdle {
            mapEvents.updateMapCenter(mapView.mapboxMap.cameraState.center)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mapEvents.events.collect { event ->
                    when (event) {
                        is MapEvent.FlyTo -> {
                            mapView.mapboxMap.flyTo(
                                CameraOptions.Builder()
                                    .center(event.point)
                                    .zoom(event.zoom)
                                    .pitch(if (pitch3D) INITIAL_PITCH else 0.0)
                                    .build(),
                                MapAnimationOptions.mapAnimationOptions { duration(620L) },
                            )
                        }
                        is MapEvent.ShowPoint -> {
                            PointDetailSheet.newInstance(event.pointId)
                                .show(parentFragmentManager, PointDetailSheet.TAG)
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                filtersBus.visibleTypes.collect {
                    viewModel.posts.value?.let { points -> updateGeoJsonSource(points) }
                }
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.isVisible = loading
        }

        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_layers -> {
                    LayersSheet.newInstance().show(parentFragmentManager, LayersSheet.TAG)
                    true
                }
                R.id.action_measure -> {
                    toggleMeasureMode()
                    true
                }
                R.id.action_satellite -> {
                    isSatellite = !isSatellite
                    item.title = getString(if (isSatellite) R.string.map_street else R.string.map_satellite)
                    val newStyle = if (isSatellite) Style.SATELLITE_STREETS else Style.STANDARD
                    mapView.mapboxMap.loadStyle(newStyle) { style ->
                        if (!isSatellite) applyLightPreset(style)
                        setupImages(style)
                        setupSources(style)
                        setupLayers(style)
                        if (terrainEnabled) enableTerrain(style)
                        viewModel.posts.value?.let { updateGeoJsonSource(it) }
                    }
                    true
                }
                R.id.action_pitch -> {
                    pitch3D = !pitch3D
                    item.title = getString(if (pitch3D) R.string.map_flat_view else R.string.map_3d_view)
                    mapView.mapboxMap.flyTo(
                        CameraOptions.Builder().pitch(if (pitch3D) INITIAL_PITCH else 0.0).build(),
                        MapAnimationOptions.mapAnimationOptions { duration(520L) },
                    )
                    true
                }
                R.id.action_terrain -> {
                    terrainEnabled = !terrainEnabled
                    item.title = getString(if (terrainEnabled) R.string.map_flat else R.string.map_3d_terrain)
                    mapView.mapboxMap.style?.let { style ->
                        if (terrainEnabled) enableTerrain(style) else disableTerrain(style)
                    }
                    true
                }
                else -> false
            }
        }

        mapView.mapboxMap.addOnMapLongClickListener {
            if (measureMode) clearMeasure()
            true
        }

        mapView.mapboxMap.addOnMapClickListener { point ->
            if (measureMode) {
                measurePoints.add(point)
                updateMeasureLine()
                updateMeasurePill()
                return@addOnMapClickListener true
            }
            val type = pendingType
            if (type != null) {
                if (type.isLine) {
                    drawnPoints.add(point)
                    updateDrawLine()
                    updateDrawHint()
                    binding.fabDone.isVisible = drawnPoints.size >= 2
                } else {
                    navigateToAddPoint(type, listOf(point))
                    exitAddMode()
                }
                return@addOnMapClickListener true
            }

            val pixel = mapView.mapboxMap.pixelForCoordinate(point)
            mapView.mapboxMap.queryRenderedFeatures(
                com.mapbox.maps.RenderedQueryGeometry(pixel),
                com.mapbox.maps.RenderedQueryOptions(
                    listOf(
                        UNCLUSTERED_LAYER_ID,
                        UNDERGROUND_HIT_LAYER_ID,
                        AERIAL_HIT_LAYER_ID,
                    ),
                    null,
                ),
            ) { result ->
                result.value?.firstOrNull()?.let { queriedFeature ->
                    val pointId = queriedFeature.queriedFeature.feature.getStringProperty("id")
                    if (pointId != null) {
                        PointDetailSheet.newInstance(pointId).show(parentFragmentManager, PointDetailSheet.TAG)
                    }
                }
            }
            true
        }

        binding.fabAdd.setOnClickListener { showAddMenu() }

        binding.fabDone.setOnClickListener {
            val type = pendingType ?: return@setOnClickListener
            if (drawnPoints.size < 2) return@setOnClickListener
            navigateToAddPoint(type, drawnPoints.toList())
            exitAddMode()
        }
    }

    private fun showAddMenu() {
        if (measureMode) {
            Toast.makeText(requireContext(), R.string.map_stop_measuring_first, Toast.LENGTH_SHORT).show()
            return
        }
        if (pendingType != null) {
            exitAddMode()
            return
        }
        val popup = PopupMenu(requireContext(), binding.fabAdd)
        DpType.entries.forEachIndexed { index, type ->
            popup.menu.add(0, index, index, getString(type.labelRes))
        }
        popup.setOnMenuItemClickListener { item ->
            val type = DpType.entries[item.itemId]
            enterAddMode(type)
            true
        }
        popup.show()
    }

    private fun enterAddMode(type: DpType) {
        pendingType = type
        drawnPoints.clear()
        clearDrawLine()
        if (type.isLine) {
            val color = if (type == DpType.AERIAL_SPAN) COLOR_AERIAL else COLOR_UNDERGROUND
            binding.mapView.mapboxMap.style
                ?.getLayerAs<LineLayer>(DRAW_LINE_LAYER_ID)
                ?.lineColor(Color.parseColor(color))
        }
        binding.fabAdd.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
        binding.fabDone.isVisible = false
        updateDrawHint()
    }

    private fun exitAddMode() {
        pendingType = null
        drawnPoints.clear()
        clearDrawLine()
        binding.fabAdd.setImageResource(android.R.drawable.ic_input_add)
        binding.fabDone.isVisible = false
        binding.tvDrawHint.isVisible = false
    }

    private fun updateDrawHint() {
        val type =
            pendingType ?: run {
                binding.tvDrawHint.isVisible = false
                return
            }
        val label = getString(type.labelRes)
        binding.tvDrawHint.text =
            if (type.isLine) {
                getString(R.string.add_dp_line_vertices, label, drawnPoints.size)
            } else {
                getString(R.string.map_tap_to_place, label)
            }
        binding.tvDrawHint.isVisible = true
    }

    private fun navigateToAddPoint(
        type: DpType,
        points: List<Point>,
    ) {
        val first = points.first()
        val pathJson =
            if (type.isLine) {
                PathCoordinates.encode(points.map { it.longitude() to it.latitude() })
            } else {
                ""
            }
        AddPointSheet.newInstance(
            latitude = first.latitude().toFloat(),
            longitude = first.longitude().toFloat(),
            dpType = type.name,
            pathCoordinates = pathJson,
        ).show(parentFragmentManager, AddPointSheet.TAG)
    }

    private fun setupImages(style: Style) {
        val m = markers ?: return
        style.addImage(MARKER_POLE_ID, m.pole)
        style.addImage(MARKER_OFFICE_ID, m.centralOffice)
    }

    private fun setupSources(style: Style) {
        if (style.getSourceAs<GeoJsonSource>(POINTS_SOURCE_ID) == null) {
            val source =
                GeoJsonSource.Builder(POINTS_SOURCE_ID)
                    .cluster(true)
                    .clusterRadius(50)
                    .clusterMaxZoom(14)
                    .build()
            style.addSource(source)
        }

        if (style.getSourceAs<GeoJsonSource>(LINES_SOURCE_ID) == null) {
            style.addSource(GeoJsonSource.Builder(LINES_SOURCE_ID).build())
        }

        if (style.getSourceAs<GeoJsonSource>(LINE_LABELS_SOURCE_ID) == null) {
            style.addSource(GeoJsonSource.Builder(LINE_LABELS_SOURCE_ID).build())
        }

        if (style.getSourceAs<GeoJsonSource>(DRAW_SOURCE_ID) == null) {
            style.addSource(GeoJsonSource.Builder(DRAW_SOURCE_ID).build())
        }
    }

    private fun setupLayers(style: Style) {
        // Transparent hit layers for lines (fat tap target) — underneath visible lines
        if (style.getLayerAs<LineLayer>(UNDERGROUND_HIT_LAYER_ID) == null) {
            val hit = LineLayer(UNDERGROUND_HIT_LAYER_ID, LINES_SOURCE_ID)
            hit.filter(Expression.eq(Expression.get("type"), Expression.literal("UNDERGROUND_PATH")))
            hit.lineColor(Color.TRANSPARENT)
            hit.lineWidth(20.0)
            style.addLayer(hit)
        }
        if (style.getLayerAs<LineLayer>(AERIAL_HIT_LAYER_ID) == null) {
            val hit = LineLayer(AERIAL_HIT_LAYER_ID, LINES_SOURCE_ID)
            hit.filter(Expression.eq(Expression.get("type"), Expression.literal("AERIAL_SPAN")))
            hit.lineColor(Color.TRANSPARENT)
            hit.lineWidth(20.0)
            style.addLayer(hit)
        }

        if (style.getLayerAs<LineLayer>(UNDERGROUND_LAYER_ID) == null) {
            val layer = LineLayer(UNDERGROUND_LAYER_ID, LINES_SOURCE_ID)
            layer.filter(Expression.eq(Expression.get("type"), Expression.literal("UNDERGROUND_PATH")))
            layer.lineColor(Color.parseColor(COLOR_UNDERGROUND))
            layer.lineWidth(4.0)
            style.addLayer(layer)
        }

        if (style.getLayerAs<LineLayer>(AERIAL_LAYER_ID) == null) {
            val layer = LineLayer(AERIAL_LAYER_ID, LINES_SOURCE_ID)
            layer.filter(Expression.eq(Expression.get("type"), Expression.literal("AERIAL_SPAN")))
            layer.lineColor(Color.parseColor(COLOR_AERIAL))
            layer.lineWidth(4.0)
            style.addLayer(layer)
        }

        if (style.getLayerAs<CircleLayer>(CLUSTER_LAYER_ID) == null) {
            val clusterLayer = CircleLayer(CLUSTER_LAYER_ID, POINTS_SOURCE_ID)
            clusterLayer.filter(Expression.has("point_count"))
            clusterLayer.circleColor(Color.parseColor(COLOR_AERIAL))
            clusterLayer.circleRadius(
                Expression.interpolate(
                    Expression.linear(),
                    Expression.get("point_count"),
                    Expression.literal(2),
                    Expression.literal(22),
                    Expression.literal(50),
                    Expression.literal(34),
                    Expression.literal(200),
                    Expression.literal(44),
                ),
            )
            clusterLayer.circleStrokeWidth(3.0)
            clusterLayer.circleStrokeColor(Color.WHITE)
            style.addLayer(clusterLayer)
        }

        if (style.getLayerAs<SymbolLayer>(CLUSTER_COUNT_LAYER_ID) == null) {
            val countLayer = SymbolLayer(CLUSTER_COUNT_LAYER_ID, POINTS_SOURCE_ID)
            countLayer.filter(Expression.has("point_count"))
            countLayer.textField(Expression.get("point_count_abbreviated"))
            countLayer.textSize(13.0)
            countLayer.textColor(Color.WHITE)
            style.addLayer(countLayer)
        }

        if (style.getLayerAs<SymbolLayer>(UNCLUSTERED_LAYER_ID) == null) {
            val symbolLayer = SymbolLayer(UNCLUSTERED_LAYER_ID, POINTS_SOURCE_ID)
            symbolLayer.filter(Expression.not(Expression.has("point_count")))
            symbolLayer.iconImage(
                Expression.match(
                    Expression.get("type"),
                    Expression.literal("POLES"),
                    Expression.literal(MARKER_POLE_ID),
                    Expression.literal("CENTRAL_OFFICES"),
                    Expression.literal(MARKER_OFFICE_ID),
                    Expression.literal(MARKER_POLE_ID),
                ),
            )
            symbolLayer.iconAnchor(IconAnchor.BOTTOM)
            symbolLayer.iconAllowOverlap(true)
            symbolLayer.iconIgnorePlacement(true)
            style.addLayer(symbolLayer)
        }

        if (style.getLayerAs<SymbolLayer>(LINE_LABELS_LAYER_ID) == null) {
            val labelLayer = SymbolLayer(LINE_LABELS_LAYER_ID, LINE_LABELS_SOURCE_ID)
            labelLayer.textField(Expression.get("length"))
            labelLayer.textSize(12.0)
            labelLayer.textColor(Color.WHITE)
            labelLayer.textHaloColor(Color.BLACK)
            labelLayer.textHaloWidth(1.2)
            labelLayer.textAllowOverlap(false)
            labelLayer.textIgnorePlacement(false)
            style.addLayer(labelLayer)
        }

        if (style.getLayerAs<LineLayer>(DRAW_LINE_LAYER_ID) == null) {
            val lineLayer = LineLayer(DRAW_LINE_LAYER_ID, DRAW_SOURCE_ID)
            lineLayer.lineColor(Color.parseColor(COLOR_UNDERGROUND))
            lineLayer.lineWidth(3.0)
            lineLayer.lineDasharray(listOf(2.0, 2.0))
            style.addLayer(lineLayer)
        }
    }

    private fun enableLocationPuck() {
        binding.mapView.location.updateSettings {
            enabled = true
            pulsingEnabled = true
            locationPuck =
                LocationPuck2D(
                    topImage = ImageHolder.from(R.drawable.puck_top),
                    bearingImage = ImageHolder.from(R.drawable.puck_bearing),
                    shadowImage = ImageHolder.from(R.drawable.puck_shadow),
                )
            puckBearing = PuckBearing.HEADING
            puckBearingEnabled = true
        }
    }

    private fun enableTerrain(style: Style) {
        try {
            if (style.getSourceAs<com.mapbox.maps.extension.style.sources.generated.RasterDemSource>("terrain-source") == null) {
                val demSource =
                    com.mapbox.maps.extension.style.sources.generated.RasterDemSource.Builder("terrain-source")
                        .url("mapbox://mapbox.mapbox-terrain-dem-v1")
                        .build()
                style.addSource(demSource)
            }
            style.setTerrain(Terrain("terrain-source"))
        } catch (_: Exception) {
        }
    }

    private fun disableTerrain(style: Style) {
        try {
            style.removeTerrain()
        } catch (_: Exception) {
        }
    }

    private fun observePoints() {
        viewModel.posts.observe(viewLifecycleOwner) { points ->
            updateGeoJsonSource(points)
        }
    }

    private fun updateGeoJsonSource(points: List<DistributionPoint>) {
        val visible = filtersBus.visibleTypes.value
        val (linePoints, pointPoints) =
            points.asSequence()
                .filter { it.type in visible }
                .partition { it.type.isLine }

        val pointFeatures =
            pointPoints.map { dp ->
                Feature.fromGeometry(
                    Point.fromLngLat(dp.longitude, dp.latitude),
                ).apply {
                    addStringProperty("id", dp.id)
                    addStringProperty("label", dp.label)
                    addStringProperty("type", dp.type.name)
                    addStringProperty("notes", dp.notes)
                }
            }

        val labelFeatures = mutableListOf<Feature>()
        val lineFeatures =
            linePoints.mapNotNull { dp ->
                val verts = PathCoordinates.decode(dp.pathCoordinates)
                if (verts.size < 2) return@mapNotNull null
                val linePts = verts.map { (lng, lat) -> Point.fromLngLat(lng, lat) }
                val lineFeature =
                    Feature.fromGeometry(LineString.fromLngLats(linePts)).apply {
                        addStringProperty("id", dp.id)
                        addStringProperty("label", dp.label)
                        addStringProperty("type", dp.type.name)
                    }
                val (midLng, midLat, lengthM) = midpointAndLength(verts)
                labelFeatures +=
                    Feature.fromGeometry(Point.fromLngLat(midLng, midLat)).apply {
                        addStringProperty("id", dp.id)
                        addStringProperty("type", dp.type.name)
                        addStringProperty("length", GeoMath.formatDistance(lengthM))
                    }
                lineFeature
            }

        binding.mapView.mapboxMap.style?.let { style ->
            style.getSourceAs<GeoJsonSource>(POINTS_SOURCE_ID)
                ?.featureCollection(FeatureCollection.fromFeatures(pointFeatures))
            style.getSourceAs<GeoJsonSource>(LINES_SOURCE_ID)
                ?.featureCollection(FeatureCollection.fromFeatures(lineFeatures))
            style.getSourceAs<GeoJsonSource>(LINE_LABELS_SOURCE_ID)
                ?.featureCollection(FeatureCollection.fromFeatures(labelFeatures))
        }
    }

    /**
     * Returns (midLng, midLat, totalMeters). The midpoint is the position at half of the
     * accumulated distance along the polyline (length-weighted), interpolated linearly
     * between the two adjacent vertices.
     */
    private fun midpointAndLength(verts: List<Pair<Double, Double>>): Triple<Double, Double, Double> {
        if (verts.size < 2) return Triple(verts.first().first, verts.first().second, 0.0)
        val segMeters =
            (1 until verts.size).map { i ->
                val (lng1, lat1) = verts[i - 1]
                val (lng2, lat2) = verts[i]
                GeoMath.haversineMeters(lat1, lng1, lat2, lng2)
            }
        val total = segMeters.sum()
        if (total == 0.0) return Triple(verts.first().first, verts.first().second, 0.0)
        val half = total / 2.0
        var acc = 0.0
        for (i in segMeters.indices) {
            val nextAcc = acc + segMeters[i]
            if (nextAcc >= half) {
                val into = (half - acc) / segMeters[i]
                val (lng1, lat1) = verts[i]
                val (lng2, lat2) = verts[i + 1]
                val midLng = lng1 + (lng2 - lng1) * into
                val midLat = lat1 + (lat2 - lat1) * into
                return Triple(midLng, midLat, total)
            }
            acc = nextAcc
        }
        val last = verts.last()
        return Triple(last.first, last.second, total)
    }

    private fun updateDrawLine() {
        if (drawnPoints.size < 2) {
            clearDrawLine()
            return
        }
        val lineString = LineString.fromLngLats(drawnPoints)
        binding.mapView.mapboxMap.style?.let { style ->
            style.getSourceAs<GeoJsonSource>(DRAW_SOURCE_ID)
                ?.geometry(lineString)
        }
    }

    private fun clearDrawLine() {
        binding.mapView.mapboxMap.style?.let { style ->
            style.getSourceAs<GeoJsonSource>(DRAW_SOURCE_ID)
                ?.featureCollection(FeatureCollection.fromFeatures(emptyList()))
        }
    }

    private fun toggleMeasureMode() {
        if (pendingType != null) {
            Toast.makeText(requireContext(), R.string.map_finish_feature_first, Toast.LENGTH_SHORT).show()
            return
        }
        if (measureMode) {
            exitMeasureMode()
        } else {
            enterMeasureMode()
        }
    }

    private fun enterMeasureMode() {
        measureMode = true
        measurePoints.clear()
        binding.mapView.mapboxMap.style
            ?.getLayerAs<LineLayer>(DRAW_LINE_LAYER_ID)
            ?.lineColor(Color.WHITE)
        clearDrawLine()
        binding.tvMeasurePill.text = getString(R.string.map_tap_to_measure)
        binding.tvMeasurePill.isVisible = true
    }

    private fun exitMeasureMode() {
        measureMode = false
        measurePoints.clear()
        clearDrawLine()
        binding.tvMeasurePill.isVisible = false
    }

    private fun clearMeasure() {
        measurePoints.clear()
        clearDrawLine()
        binding.tvMeasurePill.text = getString(R.string.map_tap_to_measure)
    }

    private fun updateMeasureLine() {
        if (measurePoints.size < 2) {
            clearDrawLine()
            return
        }
        val lineString = LineString.fromLngLats(measurePoints)
        binding.mapView.mapboxMap.style?.let { style ->
            style.getSourceAs<GeoJsonSource>(DRAW_SOURCE_ID)
                ?.geometry(lineString)
        }
    }

    private fun updateMeasurePill() {
        if (measurePoints.size < 2) {
            binding.tvMeasurePill.text = getString(R.string.map_tap_second_point)
            return
        }
        val total = GeoMath.polylineMeters(measurePoints.map { it.longitude() to it.latitude() })
        binding.tvMeasurePill.text = "Total: ${GeoMath.formatDistance(total)}"
    }

    override fun onStart() {
        super.onStart()
        _binding?.mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        _binding?.mapView?.mapboxMap?.getStyle { style ->
            if (!isSatellite) applyLightPreset(style)
        }
    }

    private fun applyLightPreset(style: Style) {
        style.setStyleImportConfigProperty("basemap", "lightPreset", Value(mapSettings.getLightPreset()))
        applyMapDisplayToggles(style)
        applyMapLanguage(style)
    }

    private fun applyMapDisplayToggles(style: Style) {
        val toggles =
            listOf(
                "showRoadLabels" to (MapSettingsRepository.KEY_ROAD_LABELS to MapSettingsRepository.DEFAULT_ROAD_LABELS),
                "showPointOfInterestLabels" to (MapSettingsRepository.KEY_POI_LABELS to MapSettingsRepository.DEFAULT_POI_LABELS),
                "showPlaceLabels" to (MapSettingsRepository.KEY_PLACE_LABELS to MapSettingsRepository.DEFAULT_PLACE_LABELS),
                "show3dObjects" to (MapSettingsRepository.KEY_3D_OBJECTS to MapSettingsRepository.DEFAULT_3D_OBJECTS),
            )
        toggles.forEach { (name, kv) ->
            style.setStyleImportConfigProperty("basemap", name, Value(mapSettings.getBool(kv.first, kv.second)))
        }
    }

    private fun applyMapLanguage(style: Style) {
        style.setStyleImportConfigProperty("basemap", "language", Value("en"))
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
        private const val LINES_SOURCE_ID = "lines-source"
        private const val DRAW_SOURCE_ID = "draw-source"
        private const val CLUSTER_LAYER_ID = "cluster-layer"
        private const val CLUSTER_COUNT_LAYER_ID = "cluster-count-layer"
        private const val UNCLUSTERED_LAYER_ID = "unclustered-layer"
        private const val UNDERGROUND_LAYER_ID = "underground-layer"
        private const val AERIAL_LAYER_ID = "aerial-layer"
        private const val UNDERGROUND_HIT_LAYER_ID = "underground-hit-layer"
        private const val AERIAL_HIT_LAYER_ID = "aerial-hit-layer"
        private const val DRAW_LINE_LAYER_ID = "draw-line-layer"
        private const val LINE_LABELS_SOURCE_ID = "line-labels-source"
        private const val LINE_LABELS_LAYER_ID = "line-labels-layer"
        private const val MARKER_POLE_ID = "marker-pole"
        private const val MARKER_OFFICE_ID = "marker-central-office"
        private const val COLOR_UNDERGROUND = "#F59E0B"
        private const val COLOR_AERIAL = "#3B82F6"
        private const val INITIAL_PITCH = 45.0
    }
}
