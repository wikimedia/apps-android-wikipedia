package org.wikipedia.places

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
import android.location.Location
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.graphics.applyCanvas
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.button.MaterialButton
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
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleColor
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleStrokeColor
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleStrokeWidth
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.textAllowOverlap
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.textFont
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.textIgnorePlacement
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.FragmentPlacesBinding
import org.wikipedia.databinding.ItemPlacesListBinding
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.page.linkpreview.LinkPreviewDialog
import org.wikipedia.page.tabs.TabActivity
import org.wikipedia.search.SearchFragment
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.DrawableItemDecoration
import org.wikipedia.views.ViewUtil
import kotlin.math.abs

class PlacesFragment : Fragment(), MapboxMap.OnMapClickListener {

    private var _binding: FragmentPlacesBinding? = null
    private val binding get() = _binding!!
    private var statusBarInsets: Insets? = null
    private var navBarInsets: Insets? = null

    private val viewModel: PlacesFragmentViewModel by viewModels { PlacesFragmentViewModel.Factory(requireArguments()) }

    private var mapboxMap: MapboxMap? = null
    private var symbolManager: SymbolManager? = null

    private val annotationCache = ArrayDeque<PlacesFragmentViewModel.NearbyPage>()
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
                goToLastKnownLocation(1000, viewModel.location, viewModel.pageTitle != null)
            }
            else -> {
                FeedbackUtil.showMessage(requireActivity(), R.string.places_permissions_denied)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        markerBitmapBase = ResourceUtil.bitmapFromVectorDrawable(requireContext(), R.drawable.map_marker_outline, null)
        markerBitmapBaseRect = Rect(0, 0, markerBitmapBase.width, markerBitmapBase.height)

        Mapbox.getInstance(requireActivity().applicationContext)

        HttpRequestImpl.setOkHttpClient(OkHttpConnectionFactory.client)

        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentPlacesBinding.inflate(inflater, container, false)

        binding.root.setOnApplyWindowInsetsListener { view, windowInsets ->
            val insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(windowInsets, view)
            statusBarInsets = insetsCompat.getInsets(WindowInsetsCompat.Type.statusBars())
            var params = binding.searchContainer.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = statusBarInsets!!.top + DimenUtil.roundedDpToPx(4f)

            navBarInsets = insetsCompat.getInsets(WindowInsetsCompat.Type.navigationBars())
            params = binding.myLocationButton.layoutParams as ViewGroup.MarginLayoutParams
            params.bottomMargin = navBarInsets!!.bottom + DimenUtil.roundedDpToPx(16f)
            binding.myLocationButton.layoutParams = params

            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.navigationBars(), navBarInsets!!)
                .build().toWindowInsets() ?: windowInsets
        }

        binding.tabsButton.setOnClickListener {
            if (WikipediaApp.instance.tabCount == 1) {
                startActivity(PageActivity.newIntent(requireActivity()))
            } else {
                startActivity(TabActivity.newIntent(requireActivity()))
            }
        }

        binding.searchTextView.setOnClickListener {
            // TODO: search
        }

        binding.backButton.setOnClickListener {
            requireActivity().finish()
        }

        binding.searchLangContainer.setOnClickListener {
            // TODO: change language
        }

        binding.searchCloseBtn.setOnClickListener {
            // TODO: clear search
        }

        binding.myLocationButton.setOnClickListener {
            if (haveLocationPermissions()) {
                goToLastKnownLocation(0)
            } else {
                locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }
        }

        binding.viewButtonsGroup.addOnButtonCheckedListener { group, id, isChecked ->
            val backgroundColor = if (isChecked) R.attr.addition_color else R.attr.paper_color
            val textColor = if (isChecked) R.attr.progressive_color else R.attr.placeholder_color
            val buttonView = group.findViewById<MaterialButton>(id)
            buttonView.setTextColor(ResourceUtil.getThemedColorStateList(requireContext(), textColor))
            buttonView.backgroundTintList = ResourceUtil.getThemedColorStateList(requireContext(), backgroundColor)
        }

        binding.listRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.listRecyclerView.addItemDecoration(DrawableItemDecoration(requireContext(), R.attr.list_divider, skipSearchBar = true))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Style JSON taken from:
        // https://gerrit.wikimedia.org/r/c/mediawiki/extensions/Kartographer/+/663867 (mvt-style.json)
        // https://tegola-wikimedia.s3.amazonaws.com/wikimedia-tilejson.json (for some reason)

        binding.mapView.onCreate(savedInstanceState)

        binding.mapView.getMapAsync { map ->
            val assetForTheme = if (WikipediaApp.instance.currentTheme.isDark) "asset://mapstyle-dark.json" else "asset://mapstyle.json"
            map.setStyle(Style.Builder().fromUri(assetForTheme)) { style ->
                mapboxMap = map

                style.addImage(MARKER_DRAWABLE, AppCompatResources.getDrawable(requireActivity(), R.drawable.map_marker)!!)

                // TODO: Currently the style seems to break when zooming beyond 16.0. See if we can fix this.
                map.setMaxZoomPreference(15.999)

                map.uiSettings.isLogoEnabled = false
                val defMargin = DimenUtil.roundedDpToPx(16f)
                val navBarMargin = if (navBarInsets != null) navBarInsets!!.bottom else 0

                map.uiSettings.compassGravity = Gravity.BOTTOM or Gravity.START
                map.uiSettings.setCompassMargins(defMargin, 0, defMargin, navBarMargin + defMargin)

                map.uiSettings.attributionGravity = Gravity.BOTTOM or Gravity.END
                map.uiSettings.setAttributionMargins(defMargin * 2 + (if (L10nUtil.isDeviceRTL) binding.myLocationButton.width else 0),
                    0, defMargin * 2 + (if (L10nUtil.isDeviceRTL) 0 else binding.myLocationButton.width), navBarMargin + defMargin)

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
                        val entry = HistoryEntry(it.pageTitle, HistoryEntry.SOURCE_PLACES)
                        ExclusiveBottomSheetPresenter.show(childFragmentManager, LinkPreviewDialog.newInstance(entry, null))
                    }
                    true
                }

                if (haveLocationPermissions()) {
                    startLocationTracking()
                    if (savedInstanceState == null) {
                        goToLastKnownLocation(1000, viewModel.location, viewModel.pageTitle != null)
                    }
                } else {
                    locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
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

        binding.viewButtonsGroup.post {
            binding.viewButtonsGroup.check(R.id.mapViewButton)
            binding.viewButtonsGroup.isVisible = true
        }
    }

    private fun updateSearchCardViews() {
        val tabsCount = WikipediaApp.instance.tabCount
        binding.tabsButton.isVisible = tabsCount != 0
        binding.tabsButton.updateTabCount(false)

        if (!WikipediaApp.instance.languageState.appLanguageCodes.contains(Prefs.placesWikiCode)) {
            Prefs.placesWikiCode = WikipediaApp.instance.appOrSystemLanguageCode
        }
        binding.searchLangCode.text = Prefs.placesWikiCode
        ViewUtil.formatLangButton(binding.searchLangCode, Prefs.placesWikiCode,
            SearchFragment.LANG_BUTTON_TEXT_SIZE_SMALLER, SearchFragment.LANG_BUTTON_TEXT_SIZE_LARGER)
    }

    private fun setUpSymbolManagerWithClustering(mapboxMap: MapboxMap, style: Style) {
        val clusterOptions = ClusterOptions()
            .withClusterRadius(60)
            .withTextSize(Expression.literal(16f))
            .withTextField(Expression.toString(get(POINT_COUNT)))
            .withTextColor(Expression.color(ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color)))

        symbolManager = SymbolManager(binding.mapView, mapboxMap, style, null, null, clusterOptions)

        // Clustering with SymbolManager doesn't expose a few style specifications we need.
        // Accessing the styles in a fail-safe manner
        try {
            style.getLayer(CLUSTER_TEXT_LAYER_ID)?.apply {
                this.setProperties(
                    textFont(CLUSTER_FONT_STACK),
                    textIgnorePlacement(true),
                    textAllowOverlap(true)
                )
            }
            style.getLayer(CLUSTER_CIRCLE_LAYER_ID)?.apply {
                this.setProperties(
                    circleColor(ContextCompat.getColor(requireActivity(), ResourceUtil.getThemedAttributeId(requireContext(), R.attr.success_color))),
                    circleStrokeColor(ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color)),
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
        updateSearchCardViews()
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

    private fun updateMapMarkers(pages: List<PlacesFragmentViewModel.NearbyPage>) {
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

        binding.listRecyclerView.adapter = RecyclerViewAdapter(pages)
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

    private fun goToLastKnownLocation(delayMillis: Long, targetLocation: Location? = null, shouldZoomToMax: Boolean = false) {
        binding.mapView.postDelayed({
            if (isAdded) {
                mapboxMap?.let {
                    val location = targetLocation ?: it.locationComponent.lastKnownLocation
                    if (location != null) {
                        val latLng = LatLng(location.latitude, location.longitude)
                        val zoomLevel = if (shouldZoomToMax) 15.999 else 15.0
                        it.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel))
                    }
                }
            }
        }, delayMillis)
    }

    private fun queueImageForAnnotation(page: PlacesFragmentViewModel.NearbyPage) {
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

    override fun onMapClick(point: LatLng): Boolean {
        mapboxMap?.let {
            val screenPoint = it.projection.toScreenLocation(point)
            val rect = RectF(screenPoint.x - 10, screenPoint.y - 10, screenPoint.x + 10, screenPoint.y + 10)

            // Zoom-in 2 levels on click of a cluster circle. Do not handle other click events
            val featureList = it.queryRenderedFeatures(rect, CLUSTER_CIRCLE_LAYER_ID)
            if (featureList.isNotEmpty()) {
                val cameraPosition = CameraPosition.Builder()
                    .target(point)
                    .zoom(it.cameraPosition.zoom + 2)
                    .build()
                it.easeCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), ZOOM_IN_ANIMATION_DURATION)
                return true
            }
        }
        return false
    }

    private inner class RecyclerViewAdapter(val list: List<PlacesFragmentViewModel.NearbyPage>) : RecyclerView.Adapter<RecyclerViewItemHolder>() {
        override fun getItemCount(): Int {
            return list.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerViewItemHolder {
            return RecyclerViewItemHolder(ItemPlacesListBinding.inflate(layoutInflater, parent, false))
        }

        override fun onBindViewHolder(holder: RecyclerViewItemHolder, position: Int) {
            holder.bindItem(list[position], position)
        }
    }

    private inner class RecyclerViewItemHolder(val binding: ItemPlacesListBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnClickListener, View.OnLongClickListener {

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
            DeviceUtil.setContextClickAsLongClick(itemView)
        }

        fun bindItem(item: PlacesFragmentViewModel.NearbyPage, position: Int) {
            binding.listItemTitle.text = StringUtil.fromHtml(item.pageTitle.displayText)
            binding.listItemDescription.text = StringUtil.fromHtml(item.pageTitle.description)
            ViewUtil.loadImage(binding.listItemThumbnail, item.pageTitle.thumbUrl)
        }


        override fun onClick(v: View) {
            // TODO: implement this
        }

        override fun onLongClick(v: View): Boolean {
            // TODO: implement this)
            return true
        }
    }

    companion object {
        const val MARKER_DRAWABLE = "markerDrawable"
        const val POINT_COUNT = "point_count"
        const val MAX_ANNOTATIONS = 64
        const val THUMB_SIZE = 160
        const val ITEMS_PER_REQUEST = 50
        const val CLUSTER_TEXT_LAYER_ID = "mapbox-android-cluster-text"
        const val CLUSTER_CIRCLE_LAYER_ID = "mapbox-android-cluster-circle0"
        const val ZOOM_IN_ANIMATION_DURATION = 1000
        val CLUSTER_FONT_STACK = arrayOf("Open Sans Semibold")
        val MARKER_FONT_STACK = arrayOf("Open Sans Regular")
        val MARKER_WIDTH = DimenUtil.roundedDpToPx(48f)
        val MARKER_HEIGHT = DimenUtil.roundedDpToPx(60f)

        fun newInstance(pageTitle: PageTitle?, location: Location?): PlacesFragment {
            return PlacesFragment().apply {
                arguments = bundleOf(
                    Constants.ARG_TITLE to pageTitle,
                    PlacesActivity.EXTRA_LOCATION to location
                )
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
