package org.wikipedia.nearby

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff.Mode
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.applyCanvas
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.mapbox.geojson.Feature
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.module.http.HttpRequestImpl
import com.mapbox.mapboxsdk.plugins.annotation.ClusterOptions
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.expressions.Expression.get
import com.mapbox.mapboxsdk.style.expressions.Expression.literal
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleStrokeColor
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleStrokeWidth
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.textAllowOverlap
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.textFont
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.textIgnorePlacement
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.databinding.FragmentNearbyBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.page.linkpreview.LinkPreviewDialog
import org.wikipedia.util.ClipboardUtil
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.ShareUtil
import org.wikipedia.util.log.L
import kotlin.math.abs

class NearbyFragment : Fragment(), LinkPreviewDialog.Callback, MapboxMap.OnMapClickListener {

    private var _binding: FragmentNearbyBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NearbyFragmentViewModel by viewModels { NearbyFragmentViewModel.Factory(requireArguments()) }

    private var mapboxMap: MapboxMap? = null
    private var symbolManager: SymbolManager? = null

    private val annotationCache = ArrayDeque<NearbyFragmentViewModel.NearbyPage>()
    private var lastLocationUpdated: LatLng? = null

    private lateinit var markerBitmapBase: Bitmap
    private lateinit var markerBitmapBaseRect: Rect
    private val markerRect = Rect(0, 0, MARKER_WIDTH, MARKER_HEIGHT)
    private val markerPaintSrc = Paint().apply { isAntiAlias = true; xfermode = PorterDuffXfermode(Mode.SRC) }
    private val markerPaintSrcIn = Paint().apply { isAntiAlias = true; xfermode = PorterDuffXfermode(Mode.SRC_IN) }

    private val locationPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                startLocationTracking()
                goToLastKnownLocation(1000)
            }
            else -> {
                FeedbackUtil.showMessage(requireActivity(), R.string.nearby_permissions_denied)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        markerBitmapBase = ResourceUtil.bitmapFromVectorDrawable(requireContext(), R.drawable.map_marker_outline, null)
        markerBitmapBaseRect = Rect(0, 0, markerBitmapBase.width, markerBitmapBase.height)

        Mapbox.getInstance(requireActivity().applicationContext)

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

                style.addImage(MARKER_DRAWABLE, AppCompatResources.getDrawable(requireActivity(), R.drawable.map_marker)!!)

                // TODO: Currently the style seems to break when zooming beyond 16.0. See if we can fix this.
                map.setMaxZoomPreference(15.999)

                map.uiSettings.isLogoEnabled = false
                val attribMargin = DimenUtil.roundedDpToPx(16f)
                map.uiSettings.setAttributionMargins(attribMargin, 0, attribMargin, attribMargin)

                map.addOnCameraIdleListener {
                    onUpdateCameraPosition(mapboxMap?.cameraPosition?.target)
                }

                map.addOnMapClickListener(this)

                setUpSymbolManagerWithClustering(map, style)

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

    private fun setUpSymbolManagerWithClustering(mapboxMap: MapboxMap, style: Style) {
        val clusterColorLayers = arrayOf(
            androidx.core.util.Pair(
                0, ContextCompat.getColor(requireActivity(), ResourceUtil.getThemedAttributeId(requireContext(), R.attr.success_color))
            )
        )
        val clusterOptions = ClusterOptions()
            .withClusterRadius(60)
            .withColorLevels(clusterColorLayers)
            .withTextSize(literal(12f))
            .withTextField(Expression.toString(get(POINT_COUNT)))
            .withTextColor(Expression.color(fetchAttributeId(R.attr.paper_color)))
        symbolManager = SymbolManager(binding.mapView, mapboxMap, style, null, null, clusterOptions)

        // Clustering with SymbolManager doesn't expose a few style specifications we need.
        // Accessing the styles in a fail-safe manner
        try {
            style.getLayer(CLUSTER_TEXT_LAYER_ID)?.apply {
                this.setProperties(
                    textFont(MARKER_FONT_STACK),
                    textIgnorePlacement(true),
                    textAllowOverlap(true)
                )
            }
            style.getLayer(CLUSTER_CIRCLE_LAYER_ID)?.apply {
                this.setProperties(
                    circleStrokeColor(fetchAttributeId(R.attr.paper_color)),
                    circleStrokeWidth(2.0f),
                )
            }
        } catch (e: Exception) {
            L.e(e)
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

        annotationCache.forEach {
            if (it.bitmap != null) {
                Glide.get(requireContext()).bitmapPool.put(it.bitmap!!)
            }
        }
        markerBitmapBase.recycle()

        super.onDestroyView()
    }

    private fun onUpdateCameraPosition(latLng: LatLng?) {
        if (latLng == null) {
            return
        }

        if ((mapboxMap?.cameraPosition?.zoom ?: 0.0) < 3.0) {
            // Don't fetch pages if the map is zoomed out too far.
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

        val searchRadius = latitudeDiffToMeters(mapboxMap?.projection?.visibleRegion?.latLngBounds?.latitudeSpan ?: 0.0) / 2

        L.d(">>> requesting update: " + latLng.latitude + ", " + latLng.longitude + ", " + mapboxMap?.cameraPosition?.zoom)
        viewModel.fetchNearbyPages(latLng.latitude, latLng.longitude, searchRadius, ITEMS_PER_REQUEST)
    }

    private fun updateMapMarkers(pages: List<NearbyFragmentViewModel.NearbyPage>) {
        symbolManager?.let { manager ->

            pages.filter {
                annotationCache.find { item -> item.pageId == it.pageId } == null
            }.forEach {
                it.annotation = manager.create(
                    SymbolOptions()
                    .withLatLng(LatLng(it.latitude, it.longitude))
                    .withTextFont(MARKER_FONT_STACK)
                    .withIconImage(MARKER_DRAWABLE)
                    .withIconOffset(arrayOf(0f, -32f)))

                annotationCache.addFirst(it)
                manager.update(it.annotation)

                queueImageForAnnotation(it)

                if (annotationCache.size > MAX_ANNOTATIONS) {
                    val removed = annotationCache.removeLast()
                    manager.delete(removed.annotation)
                    if (!removed.pageTitle.thumbUrl.isNullOrEmpty()) {
                        mapboxMap?.style?.removeImage(removed.pageTitle.thumbUrl!!)
                    }
                    if (removed.bitmap != null) {
                        Glide.get(requireContext()).bitmapPool.put(removed.bitmap!!)
                    }
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

    private fun queueImageForAnnotation(page: NearbyFragmentViewModel.NearbyPage) {
        val url = page.pageTitle.thumbUrl
        if (url.isNullOrEmpty()) {
            return
        }

        Glide.with(requireContext())
            .asBitmap()
            .load(url)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    if (!isAdded) {
                        return
                    }
                    annotationCache.find { it.pageId == page.pageId }?.let {
                        val bmp = getMarkerBitmap(resource)
                        it.bitmap = bmp

                        mapboxMap?.style?.addImage(url, BitmapDrawable(resources, bmp))

                        it.annotation?.let { annotation ->
                            annotation.iconImage = url
                            symbolManager?.update(annotation)
                        }
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    private fun getMarkerBitmap(thumbnailBitmap: Bitmap): Bitmap {

        // Retrieve an unused bitmap from the pool
        val result = Glide.get(requireContext()).bitmapPool
            .getDirty(MARKER_WIDTH, MARKER_HEIGHT, Bitmap.Config.ARGB_8888)

        // Make the background fully transparent.
        result.eraseColor(Color.TRANSPARENT)

        result.applyCanvas {
            val srcRect = Rect(0, 0, thumbnailBitmap.width, thumbnailBitmap.height)

            // Draw a filled circle that will serve as a mask for the thumbnail image.
            drawCircle((MARKER_WIDTH / 2).toFloat(), (MARKER_WIDTH / 2).toFloat(),
                ((MARKER_WIDTH / 2) - (MARKER_WIDTH / 16)).toFloat(), markerPaintSrc)

            // Draw the thumbnail, which will be clipped by the circle mask above.
            drawBitmap(thumbnailBitmap, srcRect, markerRect, markerPaintSrcIn)

            // Draw the marker frame on top of the clipped thumbnail.
            drawBitmap(markerBitmapBase, markerBitmapBaseRect, markerRect, null)
        }
        return result
    }

    override fun onLinkPreviewLoadPage(title: PageTitle, entry: HistoryEntry, inNewTab: Boolean) {
        startActivity(if (inNewTab) PageActivity.newIntentForNewTab(requireActivity(), entry, entry.title) else PageActivity.newIntentForCurrentTab(requireActivity(), entry, entry.title, false))
    }

    override fun onLinkPreviewCopyLink(title: PageTitle) {
        ClipboardUtil.setPlainText(requireContext(), text = title.uri)
        FeedbackUtil.showMessage(this, R.string.address_copied)
    }

    override fun onLinkPreviewAddToList(title: PageTitle) {
        ExclusiveBottomSheetPresenter.showAddToListDialog(childFragmentManager, title, Constants.InvokeSource.LINK_PREVIEW_MENU)
    }

    override fun onLinkPreviewShareLink(title: PageTitle) {
        ShareUtil.shareText(requireContext(), title)
    }

    override fun onMapClick(point: LatLng): Boolean {
        val screenPoint = mapboxMap!!.projection.toScreenLocation(point)
        val rect = RectF(screenPoint.x - 10, screenPoint.y - 10, screenPoint.x + 10, screenPoint.y + 10)

        // Zoom-in 2 levels on click of a cluster circle. Do not handle other click events
        val featureList: List<Feature> = mapboxMap?.queryRenderedFeatures(rect, CLUSTER_CIRCLE_LAYER_ID)!!
        if (featureList.isNotEmpty()) {
            mapboxMap?.cameraPosition?.zoom?.let {
                mapboxMap!!.cameraPosition = CameraPosition.Builder()
                    .target(point)
                    .zoom(it + 2)
                    .build()
            }
            return true
        }
        return false
    }

    private fun fetchAttributeId(attribute: Int): Int {
        return ResourceUtil.getThemedAttributeId(requireContext(), attribute)
    }

    companion object {
        const val MARKER_DRAWABLE = "markerDrawable"
        const val POINT_COUNT = "point_count"
        const val MAX_ANNOTATIONS = 64
        const val THUMB_SIZE = 160
        const val ITEMS_PER_REQUEST = 50
        const val CLUSTER_TEXT_LAYER_ID = "mapbox-android-cluster-text"
        const val CLUSTER_CIRCLE_LAYER_ID = "mapbox-android-cluster-circle0"
        val MARKER_FONT_STACK = arrayOf("Open Sans Regular")
        val MARKER_WIDTH = DimenUtil.roundedDpToPx(48f)
        val MARKER_HEIGHT = DimenUtil.roundedDpToPx(60f)

        fun newInstance(wiki: WikiSite): NearbyFragment {
            return NearbyFragment().apply {
                arguments = bundleOf(NearbyActivity.EXTRA_WIKI to wiki)
            }
        }

        /**
         * Rough conversion of latitude degrees to meters, bounded by the limits accepted by the API.
         */
        fun latitudeDiffToMeters(latitudeDiff: Double): Int {
            return (111132 * latitudeDiff).toInt().coerceIn(10, 10000)
        }
    }
}
