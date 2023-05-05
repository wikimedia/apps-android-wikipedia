package org.wikipedia.nearby

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.WellKnownTileServer
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.module.http.HttpRequestImpl
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import org.wikipedia.R
import org.wikipedia.databinding.FragmentNearbyBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.linkpreview.LinkPreviewDialog
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.log.L
import kotlin.math.abs

class NearbyFragment : Fragment() {

    private var _binding: FragmentNearbyBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NearbyFragmentViewModel by viewModels { NearbyFragmentViewModel.Factory(requireArguments()) }

    private var mapboxMap: MapboxMap? = null
    private var symbolManager: SymbolManager? = null

    private val annotationCache = ArrayDeque<NearbyFragmentViewModel.NearbyPage>()
    private var lastLocationUpdated: LatLng? = null

    private val locationPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                startLocationTracking()
                goToLastKnownLocation(1000)
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Only approximate location access granted.
            }
            else -> {
                FeedbackUtil.showMessage(requireActivity(), R.string.nearby_permissions_denied)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Mapbox.getInstance(requireActivity().applicationContext, "", WellKnownTileServer.MapLibre)

        HttpRequestImpl.setOkHttpClient(OkHttpConnectionFactory.client)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentNearbyBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)

        binding.myLocationButton.setOnClickListener {
            if (haveLocationPermissions()) {
                goToLastKnownLocation(0)
            } else {
                locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Style JSON taken from:
        // https://gerrit.wikimedia.org/r/c/mediawiki/extensions/Kartographer/+/663867 (mvt-style.json)
        // https://tegola-wikimedia.s3.amazonaws.com/wikimedia-tilejson.json (for some reason)

        binding.mapView.onCreate(savedInstanceState)

        binding.mapView.getMapAsync { map ->
            map.setStyle(Style.Builder().fromUri("asset://mapstyle.json")) { style ->
                mapboxMap = map

                style.addImage(MARKER_DRAWABLE, AppCompatResources.getDrawable(requireActivity(), R.drawable.location_marker_24)!!)

                // TODO: Currently the style seems to break when zooming beyond 16.0. See if we can fix this.
                map.setMaxZoomPreference(15.999)

                map.uiSettings.isLogoEnabled = false
                val attribMargin = DimenUtil.roundedDpToPx(16f)
                map.uiSettings.setAttributionMargins(attribMargin, 0, attribMargin, attribMargin)

                map.addOnCameraIdleListener {
                    onUpdateCameraPosition(mapboxMap?.cameraPosition?.target)
                }

                symbolManager = SymbolManager(binding.mapView, map, style)

                symbolManager?.iconAllowOverlap = true
                symbolManager?.textAllowOverlap = true
                symbolManager?.addClickListener { symbol ->
                    L.d(">>>> clicked: " + symbol.latLng.latitude + ", " + symbol.latLng.longitude)
                    annotationCache.find { it.annotation == symbol }?.let {
                        val entry = HistoryEntry(it.pageTitle, HistoryEntry.SOURCE_NEARBY)
                        ExclusiveBottomSheetPresenter.show(childFragmentManager, LinkPreviewDialog.newInstance(entry, null))
                    }
                    true
                }

                if (haveLocationPermissions()) {
                    startLocationTracking()
                    if (savedInstanceState == null) {
                        goToLastKnownLocation(1000)
                    }
                }
            }
        }

        viewModel.nearbyPages.observe(viewLifecycleOwner) {
            if (it is Resource.Success) {
                updateMapMarkers(it.data)
            } else if (it is Resource.Error) {
                FeedbackUtil.showError(requireActivity(), it.throwable)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        binding.mapView.onDestroy()
        _binding = null
        super.onDestroyView()
    }

    private fun onUpdateCameraPosition(latLng: LatLng?) {
        if (latLng == null) {
            return
        }
        // Fetch new pages within the current viewport, but only if the map has moved a significant distance.

        val latEpsilon = (mapboxMap?.projection?.visibleRegion?.latLngBounds?.latitudeSpan ?: 0.0) * 0.1
        val lngEpsilon = (mapboxMap?.projection?.visibleRegion?.latLngBounds?.longitudeSpan ?: 0.0) * 0.1

        if (lastLocationUpdated != null &&
            abs(latLng.latitude - (lastLocationUpdated?.latitude ?: 0.0)) < latEpsilon &&
            abs(latLng.longitude - (lastLocationUpdated?.longitude ?: 0.0)) < lngEpsilon) {
            return
        }

        lastLocationUpdated = LatLng(latLng.latitude, latLng.longitude)

        L.d(">>> requesting update: " + latLng.latitude + ", " + latLng.longitude + ", " + mapboxMap?.cameraPosition?.zoom)
        viewModel.fetchNearbyPages(latLng.latitude, latLng.longitude)
    }

    private fun updateMapMarkers(pages: List<NearbyFragmentViewModel.NearbyPage>) {
        symbolManager?.let { manager ->

            pages.filter {
                annotationCache.find { item -> item.pageId == it.pageId } == null
            }.forEach {
                it.annotation = manager.create(SymbolOptions()
                    .withLatLng(LatLng(it.latitude, it.longitude))
                    .withTextField(it.pageTitle.displayText)
                    .withTextSize(10f)
                    .withIconImage(MARKER_DRAWABLE)
                    .withIconSize(2.0f)
                    .withIconOffset(arrayOf(0f, -16f)))

                annotationCache.addFirst(it)
                manager.update(it.annotation)

                if (annotationCache.size > MAX_ANNOTATIONS) {
                    val removed = annotationCache.removeLast()
                    manager.delete(removed.annotation)
                }
            }
        }
    }

    private fun haveLocationPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun startLocationTracking() {
        mapboxMap?.let {
            it.locationComponent.activateLocationComponent(LocationComponentActivationOptions.builder(requireContext(), it.style!!).build())
            it.locationComponent.isLocationComponentEnabled = true
            it.locationComponent.cameraMode = CameraMode.NONE
            it.locationComponent.renderMode = RenderMode.COMPASS
        }
    }

    private fun goToLastKnownLocation(delayMillis: Long) {
        binding.mapView.postDelayed({
            if (isAdded) {
                mapboxMap?.let {
                    val location = it.locationComponent.lastKnownLocation
                    if (location != null) {
                        val latLng = LatLng(location.latitude, location.longitude)
                        it.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.0))
                    }
                }
            }
        }, delayMillis)
    }

    companion object {
        const val MARKER_DRAWABLE = "markerDrawable"
        const val MAX_ANNOTATIONS = 256

        fun newInstance(wiki: WikiSite): NearbyFragment {
            return NearbyFragment().apply {
                arguments = bundleOf(NearbyActivity.EXTRA_WIKI to wiki)
            }
        }
    }
}
