package org.wikipedia.places

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
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
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMap.CancelableCallback
import org.maplibre.android.maps.Style
import org.maplibre.android.module.http.HttpRequestImpl
import org.maplibre.android.plugins.annotation.ClusterOptions
import org.maplibre.android.plugins.annotation.Symbol
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.textAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.textFont
import org.maplibre.android.style.layers.PropertyFactory.textIgnorePlacement
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.PlacesEvent
import org.wikipedia.databinding.FragmentPlacesBinding
import org.wikipedia.databinding.ItemPlacesListBinding
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory
import org.wikipedia.dataclient.page.NearbyPage
import org.wikipedia.extensions.parcelable
import org.wikipedia.extensions.parcelableExtra
import org.wikipedia.gallery.ImagePipelineBitmapGetter
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.page.linkpreview.LinkPreviewDialog
import org.wikipedia.page.tabs.TabActivity
import org.wikipedia.readinglist.LongPressMenu
import org.wikipedia.readinglist.ReadingListBehaviorsUtil
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.search.SearchActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.TabUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.DrawableItemDecoration
import org.wikipedia.views.ViewUtil
import java.util.Locale
import kotlin.math.abs

class PlacesFragment : Fragment(), LinkPreviewDialog.LoadPageCallback, LinkPreviewDialog.DismissCallback, MapLibreMap.OnMapClickListener {

    private var _binding: FragmentPlacesBinding? = null
    private val binding get() = _binding!!
    private var statusBarInsets: Insets? = null
    private var navBarInsets: Insets? = null

    private val viewModel: PlacesFragmentViewModel by viewModels { PlacesFragmentViewModel.Factory(requireArguments()) }

    private var mapboxMap: MapLibreMap? = null
    private var symbolManager: SymbolManager? = null

    private val annotationCache = ArrayDeque<NearbyPage>()
    private var lastCheckedId = R.id.mapViewButton
    private var lastLocation: Location? = null
    private var lastLocationQueried: Location? = null
    private var lastZoom = 15.0
    private var lastZoomQueried = 0.0

    private lateinit var markerBitmapBase: Bitmap
    private lateinit var markerPaintSrc: Paint
    private lateinit var markerPaintSrcIn: Paint
    private lateinit var markerBorderPaint: Paint
    private val markerRect = Rect(0, 0, MARKER_SIZE, MARKER_SIZE)
    private val searchRadius
        get() = mapboxMap?.let {
            latitudeDiffToMeters(it.projection.visibleRegion.latLngBounds.latitudeSpan / 2)
        } ?: 50
    private var magnifiedMarker: Symbol? = null

    private val locationPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                PlacesEvent.logAction("location_permission_granted", "map_view")
                startLocationTracking()
                goToLocation(viewModel.location)
            }
            else -> {
                PlacesEvent.logAction("location_permission_denied", "map_view")
                FeedbackUtil.showMessage(requireActivity(), R.string.places_permissions_denied)
            }
        }
    }

    private val placesSearchLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == SearchActivity.RESULT_LINK_SUCCESS) {
            val location = it.data?.parcelableExtra<Location>(PlacesActivity.EXTRA_LOCATION)!!
            val pageTitle = it.data?.parcelableExtra<PageTitle>(SearchActivity.EXTRA_RETURN_LINK_TITLE)!!
            viewModel.highlightedPageTitle = pageTitle
            Prefs.placesWikiCode = pageTitle.wikiSite.languageCode
            goToLocation(preferredLocation = location, zoom = 15.9)
            viewModel.fetchNearbyPages(location.latitude, location.longitude, searchRadius, ITEMS_PER_REQUEST)
        }
    }

    private val filterLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            val languageChanged = it.data?.getBooleanExtra(PlacesFilterActivity.EXTRA_LANG_CHANGED, false) ?: false
            if (languageChanged) {
                clearAnnotationCache()
                viewModel.highlightedPageTitle = null
                symbolManager?.deleteAll()
                viewModel.fetchNearbyPages(lastLocation?.latitude ?: 0.0,
                    lastLocation?.longitude ?: 0.0, searchRadius, ITEMS_PER_REQUEST)
                goToLocation(lastLocation, lastZoom)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupMarkerPaints()
        markerBitmapBase = Bitmap.createBitmap(MARKER_SIZE, MARKER_SIZE, Bitmap.Config.ARGB_8888).applyCanvas {
            val bitmap = ResourceUtil.bitmapFromVectorDrawable(requireContext(), R.drawable.ic_w_logo_circle)
            drawMarker(this, bitmap)
        }

        MapLibre.getInstance(requireActivity().applicationContext)

        HttpRequestImpl.setOkHttpClient(OkHttpConnectionFactory.client)

        requireArguments().parcelable<PageTitle>(Constants.ARG_TITLE)?.let {
            Prefs.placesWikiCode = it.wikiSite.languageCode
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentPlacesBinding.inflate(inflater, container, false)

        binding.root.setOnApplyWindowInsetsListener { view, windowInsets ->
            val insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(windowInsets, view)
            val newStatusBarInsets = insetsCompat.getInsets(WindowInsetsCompat.Type.statusBars())
            val newNavBarInsets = insetsCompat.getInsets(WindowInsetsCompat.Type.navigationBars())

            var params = binding.controlsContainer.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = newStatusBarInsets.top + newNavBarInsets.top
            params.leftMargin = newStatusBarInsets.left + newNavBarInsets.left
            params.rightMargin = newStatusBarInsets.right + newNavBarInsets.right
            params.bottomMargin = newStatusBarInsets.bottom + newNavBarInsets.bottom

            params = binding.myLocationButton.layoutParams as ViewGroup.MarginLayoutParams
            params.bottomMargin = newNavBarInsets.bottom + DimenUtil.roundedDpToPx(16f)
            params.leftMargin = newStatusBarInsets.left + newNavBarInsets.left + DimenUtil.roundedDpToPx(16f)
            params.rightMargin = newStatusBarInsets.right + newNavBarInsets.right + DimenUtil.roundedDpToPx(16f)
            binding.myLocationButton.layoutParams = params

            statusBarInsets = newStatusBarInsets
            navBarInsets = newNavBarInsets
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.navigationBars(), navBarInsets!!)
                .build().toWindowInsets() ?: windowInsets
        }

        binding.tabsButton.setOnClickListener {
            PlacesEvent.logAction("tabs_view_click", "search_bar_view")
            if (WikipediaApp.instance.tabCount == 1) {
                startActivity(PageActivity.newIntent(requireActivity()))
            } else {
                startActivity(TabActivity.newIntent(requireActivity()))
            }
        }

        binding.searchTextView.setOnClickListener {
            PlacesEvent.logAction("search_view_click", "search_bar_view")
            val intent = SearchActivity.newIntent(requireActivity(), Constants.InvokeSource.PLACES,
                viewModel.highlightedPageTitle?.displayText, true)
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(),
                    binding.searchContainer.getChildAt(0), getString(R.string.transition_search_bar))
            placesSearchLauncher.launch(intent, options)
        }

        binding.backButton.setOnClickListener {
            PlacesEvent.logAction("back_click", "search_bar_view")
            requireActivity().finish()
        }

        binding.langCodeButton.setOnClickListener {
            PlacesEvent.logAction("filter_click", "search_bar_view")
            filterLauncher.launch(PlacesFilterActivity.newIntent(requireActivity()))
        }

        binding.searchCloseBtn.setOnClickListener {
            PlacesEvent.logAction("search_clear_click", "search_bar_view")
            updateSearchText()
        }

        binding.myLocationButton.setOnClickListener {
            PlacesEvent.logAction("current_location_click", "map_view")
            if (haveLocationPermissions()) {
                goToLocation()
            } else {
                locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }
        }

        binding.viewButtonsGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) {
                return@addOnButtonCheckedListener
            }
            lastCheckedId = checkedId
            val mapViewChecked = checkedId == R.id.mapViewButton
            updateToggleViews(mapViewChecked)

            val progressColor = ResourceUtil.getThemedColorStateList(requireContext(), R.attr.progressive_color)
            val additionColor = ResourceUtil.getThemedColorStateList(requireContext(), R.attr.addition_color)
            val placeholderColor = ResourceUtil.getThemedColorStateList(requireContext(), R.attr.placeholder_color)
            val paperColor = ResourceUtil.getThemedColorStateList(requireContext(), R.attr.paper_color)
            val backgroundColor = ResourceUtil.getThemedColorStateList(requireContext(), R.attr.background_color)
            if (mapViewChecked) {
                binding.mapViewButton.setTextColor(progressColor)
                binding.mapViewButton.backgroundTintList = additionColor
                binding.mapViewButton.strokeColor = paperColor
                binding.listViewButton.setTextColor(placeholderColor)
                binding.listViewButton.backgroundTintList = paperColor
                binding.listViewButton.strokeColor = paperColor
            } else {
                binding.mapViewButton.setTextColor(placeholderColor)
                binding.mapViewButton.backgroundTintList = backgroundColor
                binding.mapViewButton.strokeColor = backgroundColor
                binding.listViewButton.setTextColor(progressColor)
                binding.listViewButton.backgroundTintList = additionColor
                binding.listViewButton.strokeColor = backgroundColor
            }
        }

        binding.listRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.listRecyclerView.addItemDecoration(DrawableItemDecoration(requireContext(), R.attr.list_divider, drawStart = true, skipSearchBar = true))
        binding.listEmptyMessage.text = StringUtil.fromHtml(getString(R.string.places_empty_list))
        binding.listEmptyMessage.movementMethod = LinkMovementMethodExt { _ ->
            binding.viewButtonsGroup.check(R.id.mapViewButton)
        }

        return binding.root
    }

    private fun updateSearchText(searchText: String = "") {
        if (searchText.isEmpty()) {
            binding.searchTextView.text = getString(R.string.places_search_hint)
            binding.searchCloseBtn.isVisible = false
            resetMagnifiedSymbol()
        } else {
            binding.searchCloseBtn.isVisible = true
            binding.searchTextView.text = StringUtil.fromHtml(searchText)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mapView.onCreate(savedInstanceState)

        PlacesEvent.logImpression("map_view")

        binding.mapView.getMapAsync { map ->
            val assetForTheme = if (WikipediaApp.instance.currentTheme.isDark) "asset://mapstyle-dark.json" else "asset://mapstyle.json"
            map.setStyle(Style.Builder().fromUri(assetForTheme)) { style ->
                mapboxMap = map

                style.addImage(MARKER_DRAWABLE, markerBitmapBase)

                map.setMaxZoomPreference(20.0)

                map.uiSettings.isLogoEnabled = false
                val defMargin = DimenUtil.roundedDpToPx(16f)

                val navBarLeft = navBarInsets?.left ?: 0
                val navBarRight = navBarInsets?.right ?: 0
                val navBarTop = navBarInsets?.top ?: 0
                val navBarBottom = navBarInsets?.bottom ?: 0
                val statusBarLeft = statusBarInsets?.left ?: 0
                val statusBarRight = statusBarInsets?.right ?: 0
                val statusBarTop = statusBarInsets?.top ?: 0
                val statusBarBottom = statusBarInsets?.bottom ?: 0

                map.uiSettings.setCompassImage(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_compass_with_bg)!!)
                map.uiSettings.compassGravity = Gravity.TOP or Gravity.END
                map.uiSettings.attributionGravity = Gravity.BOTTOM or Gravity.START
                map.uiSettings.setAttributionTintColor(ResourceUtil.getThemedColor(requireContext(), R.attr.placeholder_color))

                map.uiSettings.setCompassMargins(defMargin + navBarLeft + statusBarLeft,
                    defMargin + navBarTop + statusBarTop + binding.searchContainer.height,
                    DimenUtil.roundedDpToPx(12f) + navBarRight + statusBarRight, defMargin)

                map.uiSettings.setAttributionMargins(defMargin + navBarLeft + statusBarLeft,
                    0, defMargin + navBarRight + statusBarRight,
                    navBarBottom + statusBarBottom + DimenUtil.roundedDpToPx(36f))

                map.addOnCameraIdleListener {
                    mapboxMap?.cameraPosition?.target?.let {
                        onUpdateCameraPosition(it)
                    }
                }

                map.addOnMapClickListener(this)

                setUpSymbolManagerWithClustering(map, style)

                symbolManager?.iconAllowOverlap = true
                symbolManager?.textAllowOverlap = true
                symbolManager?.addClickListener { symbol ->
                    L.d(">>>> clicked: " + symbol.latLng.latitude + ", " + symbol.latLng.longitude)
                    PlacesEvent.logAction("marker_click", "map_view")
                    annotationCache.find { it.annotation == symbol }?.let {
                        val location = Location("").apply {
                            latitude = symbol.latLng.latitude
                            longitude = symbol.latLng.longitude
                        }
                        resetMagnifiedSymbol()
                        setMagnifiedSymbol(it.annotation)
                        viewModel.highlightedPageTitle = it.pageTitle
                        symbolManager?.update(it.annotation)
                        showLinkPreview(it.pageTitle, location)
                    }
                    true
                }

                if (haveLocationPermissions()) {
                    startLocationTracking()
                }

                viewModel.location?.let {
                    goToLocation(it)
                } ?: run {
                    val lastLocationAndZoomLevel = Prefs.placesLastLocationAndZoomLevel
                    goToLocation(lastLocationAndZoomLevel?.first, lastLocationAndZoomLevel?.second ?: lastZoom)

                    if (!haveLocationPermissions()) {
                        locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }
                }
            }
        }

        viewModel.nearbyPagesLiveData.observe(viewLifecycleOwner) {
            if (it is Resource.Success) {
                updateMapMarkers(it.data)
            } else if (it is Resource.Error) {
                FeedbackUtil.showError(requireActivity(), it.throwable)
            }
        }
    }

    private fun updateToggleViews(isMapVisible: Boolean) {
        if ((binding.listRecyclerView.isVisible || binding.listEmptyContainer.isVisible) && isMapVisible) {
            PlacesEvent.logAction("map_view_click", "map_view")
        }
        if (binding.mapView.isVisible && !isMapVisible) {
            PlacesEvent.logAction("list_view_click", "list_view")
        }
        val tintColor = ResourceUtil.getThemedColorStateList(requireContext(), if (isMapVisible) R.attr.paper_color else R.attr.background_color)
        binding.mapView.isVisible = isMapVisible
        binding.listRecyclerView.isVisible = !isMapVisible && (binding.listRecyclerView.adapter?.itemCount ?: 0) > 0
        binding.listEmptyContainer.isVisible = !isMapVisible && (binding.listRecyclerView.adapter?.itemCount ?: 0) == 0
        binding.searchContainer.backgroundTintList = tintColor
        binding.myLocationButton.isVisible = isMapVisible
    }

    private fun showLinkPreview(pageTitle: PageTitle, location: Location) {
        PlacesEvent.logImpression("detail_view")
        val entry = HistoryEntry(pageTitle, HistoryEntry.SOURCE_PLACES)
        updateSearchText(pageTitle.displayText)

        ExclusiveBottomSheetPresenter.show(childFragmentManager,
            LinkPreviewDialog.newInstance(entry, location, getLastKnownUserLocation()))
    }

    private fun resetMagnifiedSymbol() {
        // Reset the magnified marker to regular size
        magnifiedMarker?.let {
            it.iconSize = 1.0f
            symbolManager?.update(it)
        }
        viewModel.highlightedPageTitle = null
    }

    private fun setMagnifiedSymbol(symbol: Symbol?) {
        magnifiedMarker?.symbolSortKey = 0f
        magnifiedMarker = symbol
        magnifiedMarker?.iconSize = 1.75f
        magnifiedMarker?.symbolSortKey = 1f
    }

    private fun setupMarkerPaints() {
        markerPaintSrc = Paint().apply {
            isAntiAlias = true
            color = ResourceUtil.getThemedColor(requireContext(), R.attr.secondary_color)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
        }
        markerPaintSrcIn = Paint().apply {
            isAntiAlias = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        }
        markerBorderPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = MARKER_BORDER_SIZE
            color = ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color)
            isAntiAlias = true
        }
    }

    private fun updateSearchCardViews() {
        val tabsCount = WikipediaApp.instance.tabCount
        binding.tabsButton.isVisible = tabsCount != 0
        binding.tabsButton.updateTabCount(false)

        if (!WikipediaApp.instance.languageState.appLanguageCodes.contains(Prefs.placesWikiCode)) {
            Prefs.placesWikiCode = WikipediaApp.instance.appOrSystemLanguageCode
        }
        binding.langCodeButton.setLangCode(Prefs.placesWikiCode)

        FeedbackUtil.setButtonTooltip(binding.tabsButton, binding.langCodeButton)
    }

    private fun setUpSymbolManagerWithClustering(mapboxMap: MapLibreMap, style: Style) {
        val clusterOptions = ClusterOptions()
            .withClusterRadius(60)
            .withTextSize(Expression.literal(16f))
            .withTextField(Expression.toString(Expression.get(POINT_COUNT)))
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
                    circleColor(ContextCompat.getColor(requireActivity(), ResourceUtil.getThemedAttributeId(requireContext(), R.attr.secondary_color))),
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
        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
        }

        binding.mapView.onResume()
        updateSearchCardViews()
        updateToggleViews(lastCheckedId == R.id.mapViewButton)
        ExclusiveBottomSheetPresenter.dismiss(childFragmentManager)
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
        lastLocation?.let {
            Prefs.placesLastLocationAndZoomLevel = Pair(it, lastZoom)
        }
        binding.mapView.onDestroy()
        _binding = null

        clearAnnotationCache()
        markerBitmapBase.recycle()
        super.onDestroyView()
    }

    private fun clearAnnotationCache() {
        annotationCache.forEach {
            if (it.bitmap != null) {
                Glide.get(requireContext()).bitmapPool.put(it.bitmap!!)
            }
        }
        annotationCache.clear()
    }

    private fun onUpdateCameraPosition(latLng: LatLng) {
        lastLocation = Location("").also {
            it.latitude = latLng.latitude
            it.longitude = latLng.longitude
        }

        lastZoom = mapboxMap?.cameraPosition?.zoom ?: 15.0

        if (lastZoom < 3.0) {
            // Don't fetch pages if the map is zoomed out too far.
            return
        }

        // Fetch new pages within the current viewport, but only if the map has moved a significant distance.
        val latEpsilon = (mapboxMap?.projection?.visibleRegion?.latLngBounds?.latitudeSpan ?: 0.0) * 0.2
        val lngEpsilon = (mapboxMap?.projection?.visibleRegion?.latLngBounds?.longitudeSpan ?: 0.0) * 0.2
        if (lastLocationQueried != null &&
            abs(latLng.latitude - (lastLocationQueried?.latitude ?: 0.0)) < latEpsilon &&
            abs(latLng.longitude - (lastLocationQueried?.longitude ?: 0.0)) < lngEpsilon &&
            abs(lastZoom - lastZoomQueried) < 0.5) {
            return
        }
        lastLocationQueried = lastLocation
        lastZoomQueried = lastZoom

        L.d(">>> requesting update: " + latLng.latitude + ", " + latLng.longitude + ", " + mapboxMap?.cameraPosition?.zoom)
        viewModel.fetchNearbyPages(latLng.latitude, latLng.longitude, searchRadius, ITEMS_PER_REQUEST)
    }

    private fun updateMapMarkers(pages: List<NearbyPage>) {
        symbolManager?.let { manager ->

            pages.filter {
                annotationCache.find { item -> item.pageId == it.pageId } == null
            }.forEach {
                it.annotation = manager.create(
                    SymbolOptions()
                        .withLatLng(LatLng(it.latitude, it.longitude))
                        .withTextFont(MARKER_FONT_STACK)
                        .withIconImage(MARKER_DRAWABLE)
                )
                if (viewModel.highlightedPageTitle?.prefixedText.orEmpty() == it.pageTitle.prefixedText) {
                    setMagnifiedSymbol(it.annotation)
                }
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

    private fun getLastKnownUserLocation(): Location? {
        return if (mapboxMap?.locationComponent?.isLocationComponentActivated == true)
            mapboxMap?.locationComponent?.lastKnownLocation else null
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

    private fun goToLocation(preferredLocation: Location? = null, zoom: Double = 15.0) {
        binding.viewButtonsGroup.check(R.id.mapViewButton)
        mapboxMap?.let {
            viewModel.lastKnownLocation = getLastKnownUserLocation()
            var currentLatLngLoc: LatLng? = null
            viewModel.lastKnownLocation?.let { loc -> currentLatLngLoc = LatLng(loc.latitude, loc.longitude) }
            val location = preferredLocation?.let { loc -> LatLng(loc.latitude, loc.longitude) }
            val targetLocation = location ?: currentLatLngLoc
            targetLocation?.let { target ->
                it.moveCamera(CameraUpdateFactory.newLatLngZoom(target, zoom), object : CancelableCallback {
                    override fun onCancel() { }

                    override fun onFinish() {
                        if (isAdded && preferredLocation != null && viewModel.highlightedPageTitle != null) {
                            showLinkPreview(viewModel.highlightedPageTitle!!, preferredLocation)
                        }
                    }
                })
            }
        }
    }

    private fun queueImageForAnnotation(page: NearbyPage) {
        val url = page.pageTitle.thumbUrl
        if (!Prefs.isImageDownloadEnabled || url.isNullOrEmpty()) {
            return
        }

        ImagePipelineBitmapGetter(requireContext(), url) { bitmap ->
            if (!isAdded) {
                return@ImagePipelineBitmapGetter
            }
            annotationCache.find { it.pageId == page.pageId }?.let {
                val bmp = getMarkerBitmap(bitmap)
                it.bitmap = bmp

                mapboxMap?.style?.addImage(url, BitmapDrawable(resources, bmp))

                it.annotation?.let { annotation ->
                    annotation.iconImage = url
                    symbolManager?.update(annotation)
                }
            }
        }
    }

    private fun getMarkerBitmap(thumbnailBitmap: Bitmap): Bitmap {

        // Retrieve an unused bitmap from the pool
        val result = Glide.get(requireContext()).bitmapPool
            .getDirty(MARKER_SIZE, MARKER_SIZE, Bitmap.Config.ARGB_8888)

        result.applyCanvas {
            this.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            drawMarker(this, thumbnailBitmap)
        }
        return result
    }

    private fun drawMarker(canvas: Canvas, thumbnailBitmap: Bitmap? = null) {
        val radius = MARKER_SIZE / 2f
        canvas.drawCircle(radius, radius, radius, markerPaintSrc)
        thumbnailBitmap?.let {
            val thumbnailRect = Rect(0, 0, it.width, it.height)
            canvas.drawBitmap(it, thumbnailRect, markerRect, markerPaintSrcIn)
        }
        canvas.drawCircle(radius, radius, radius - MARKER_BORDER_SIZE / 2, markerBorderPaint)
    }

    override fun onLinkPreviewLoadPage(title: PageTitle, entry: HistoryEntry, inNewTab: Boolean) {
        if (inNewTab) {
            TabUtil.openInNewBackgroundTab(entry)
            requireActivity().invalidateOptionsMenu()
            binding.tabsButton.isVisible = WikipediaApp.instance.tabCount > 0
            binding.tabsButton.updateTabCount(true)
        } else {
            startActivity(PageActivity.newIntentForNewTab(requireActivity(), entry, entry.title))
        }
    }

    override fun onLinkPreviewDismiss() {
        updateSearchText()
    }

    override fun onMapClick(point: LatLng): Boolean {
        mapboxMap?.let {
            val screenPoint = it.projection.toScreenLocation(point)
            val rect = RectF(screenPoint.x - 10, screenPoint.y - 10, screenPoint.x + 10, screenPoint.y + 10)

            updateSearchText()
            // Zoom-in 2 levels on click of a cluster circle. Do not handle other click events
            val featureList = it.queryRenderedFeatures(rect, CLUSTER_CIRCLE_LAYER_ID)
            if (featureList.isNotEmpty()) {
                PlacesEvent.logAction("cluster_click", "map_view")
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

    private inner class RecyclerViewAdapter(val nearbyPages: List<NearbyPage>) : RecyclerView.Adapter<RecyclerViewItemHolder>() {
        override fun getItemCount(): Int {
            return nearbyPages.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerViewItemHolder {
            return RecyclerViewItemHolder(ItemPlacesListBinding.inflate(layoutInflater, parent, false))
        }

        override fun onBindViewHolder(holder: RecyclerViewItemHolder, position: Int) {
            holder.bindItem(nearbyPages[position], viewModel.lastKnownLocation)
        }
    }

    private inner class RecyclerViewItemHolder(val binding: ItemPlacesListBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnClickListener, View.OnLongClickListener {

        private lateinit var page: NearbyPage

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
            DeviceUtil.setContextClickAsLongClick(itemView)
        }

        fun bindItem(page: NearbyPage, locationForDistance: Location?) {
            this.page = page
            binding.listItemTitle.text = StringUtil.fromHtml(page.pageTitle.displayText)
            binding.listItemDescription.text = StringUtil.fromHtml(page.pageTitle.description)
            binding.listItemDescription.isVisible = !page.pageTitle.description.isNullOrEmpty()
            locationForDistance?.let {
                binding.listItemDistance.text = GeoUtil.getDistanceWithUnit(it, page.location, Locale.getDefault())
            }
            page.pageTitle.thumbUrl?.let {
                ViewUtil.loadImage(binding.listItemThumbnail, it, circleShape = true)
                binding.listItemThumbnail.isVisible = true
            } ?: run {
                binding.listItemThumbnail.isVisible = false
            }
        }

        override fun onClick(v: View) {
            PlacesEvent.logAction("read_click", "list_view")
            val entry = HistoryEntry(page.pageTitle, HistoryEntry.SOURCE_PLACES)
            startActivity(PageActivity.newIntentForNewTab(requireActivity(), entry, entry.title))
        }

        override fun onLongClick(v: View): Boolean {
            val entry = HistoryEntry(page.pageTitle, HistoryEntry.SOURCE_PLACES)
            val location = page.location
            LongPressMenu(v, menuRes = R.menu.menu_places_long_press, location = location, callback = object : LongPressMenu.Callback {
                override fun onOpenInNewTab(entry: HistoryEntry) {
                    onLinkPreviewLoadPage(entry.title, entry, true)
                }

                override fun onAddRequest(entry: HistoryEntry, addToDefault: Boolean) {
                    ReadingListBehaviorsUtil.addToDefaultList(requireActivity(), entry.title, addToDefault, Constants.InvokeSource.PLACES)
                }

                override fun onMoveRequest(page: ReadingListPage?, entry: HistoryEntry) {
                    page?.let {
                        ReadingListBehaviorsUtil.moveToList(requireActivity(), it.listId, entry.title, Constants.InvokeSource.PLACES)
                    }
                }
            }).show(entry)
            return true
        }
    }

    companion object {
        const val MARKER_DRAWABLE = "markerDrawable"
        const val POINT_COUNT = "point_count"
        const val MAX_ANNOTATIONS = 250
        const val THUMB_SIZE = 160
        const val ITEMS_PER_REQUEST = 75
        const val CLUSTER_TEXT_LAYER_ID = "mapbox-android-cluster-text"
        const val CLUSTER_CIRCLE_LAYER_ID = "mapbox-android-cluster-circle0"
        const val ZOOM_IN_ANIMATION_DURATION = 1000

        val CLUSTER_FONT_STACK = arrayOf("Open Sans Semibold")
        val MARKER_FONT_STACK = arrayOf("Open Sans Regular")
        val MARKER_SIZE = DimenUtil.roundedDpToPx(40f)
        val MARKER_BORDER_SIZE = DimenUtil.dpToPx(2f)

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
