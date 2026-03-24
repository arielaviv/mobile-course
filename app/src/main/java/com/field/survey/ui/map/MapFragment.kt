package com.field.survey.ui.map

import android.graphics.Color
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.field.survey.databinding.FragmentMapBinding
import com.field.survey.domain.model.DistributionPoint
import com.field.survey.domain.model.DpType
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

@AndroidEntryPoint
class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MapViewModel by viewModels()

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

        Configuration.getInstance().userAgentValue = requireContext().packageName

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        val mapView = binding.mapView
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(13.0)
        mapView.controller.setCenter(GeoPoint(32.0750, 34.7725))

        val tapOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?) = false
            override fun longPressHelper(p: GeoPoint?): Boolean {
                if (p != null) {
                    val action = MapFragmentDirections.actionMapToAddPoint(
                        latitude = p.latitude.toFloat(),
                        longitude = p.longitude.toFloat(),
                    )
                    findNavController().navigate(action)
                }
                return true
            }
        })
        mapView.overlays.add(tapOverlay)

        binding.progressBar.isVisible = true

        viewModel.posts.observe(viewLifecycleOwner) { points ->
            binding.progressBar.isVisible = false
            updateMarkers(mapView, points)
        }
    }

    private fun updateMarkers(mapView: MapView, points: List<DistributionPoint>) {
        mapView.overlays.clear()

        for (dp in points) {
            val marker = Marker(mapView)
            marker.position = GeoPoint(dp.latitude, dp.longitude)
            marker.title = dp.label
            marker.snippet = dp.notes
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            marker.icon = createSquareMarker(colorForType(dp.type))
            marker.relatedObject = dp.id

            marker.setOnMarkerClickListener { clickedMarker, _ ->
                val pointId = clickedMarker.relatedObject as? String
                if (pointId != null) {
                    val action = MapFragmentDirections.actionMapToDetail(pointId)
                    findNavController().navigate(action)
                }
                true
            }

            mapView.overlays.add(marker)
        }

        mapView.invalidate()
    }

    private fun createSquareMarker(color: Int): ShapeDrawable {
        val size = 32
        return ShapeDrawable(RectShape()).apply {
            intrinsicWidth = size
            intrinsicHeight = size
            paint.color = color
        }
    }

    private fun colorForType(type: DpType): Int = when (type) {
        DpType.MANHOLE -> Color.parseColor("#6366F1")
        DpType.JUNCTION_BOX -> Color.parseColor("#3B82F6")
        DpType.CABINET -> Color.parseColor("#10B981")
        DpType.POLE -> Color.parseColor("#F59E0B")
        DpType.DUCT -> Color.parseColor("#8B5CF6")
        DpType.HANDHOLE -> Color.parseColor("#06B6D4")
        DpType.PEDESTAL -> Color.parseColor("#F97316")
        DpType.OTHER -> Color.parseColor("#71717A")
    }

    override fun onResume() {
        super.onResume()
        _binding?.mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        _binding?.mapView?.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
