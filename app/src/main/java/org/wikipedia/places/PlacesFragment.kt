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
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
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
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
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
import org.wikipedia.search.SearchActivity
import org.wikipedia.search.SearchFragment
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.DrawableItemDecoration
import org.wikipedia.views.ViewUtil
import java.util.Locale

class PlacesFragment : Fragment(), MapboxMap.OnMapClickListener {

    private var _binding: FragmentPlacesBinding? = null
    private val binding get() = _binding!!
    private var statusBarInsets: Insets? = null
    private var navBarInsets: Insets? = null

    private val viewModel: PlacesFragmentViewModel by viewModels { PlacesFragmentViewModel.Factory(requireArguments()) }

    private var mapboxMap: MapboxMap? = null
    private var symbolManager: SymbolManager? = null

    private val annotationCache = ArrayDeque<PlacesFragmentViewModel.NearbyPage>()
    private var lastLocationUpdated: Location? = null
    private var lastZoom = 15.0

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
                startLocationTracking()
                goToLocation(1000, viewModel.location)
            }
            else -> {
                FeedbackUtil.showMessage(requireActivity(), R.string.places_permissions_denied)
            }
        }
    }

    private val placesSearchLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
              val location = it.data?.getParcelableExtra<Location>(PlacesActivity.EXTRA_LOCATION)!!
              viewModel.pageTitle = it.data?.getParcelableExtra(Constants.ARG_TITLE)!!
              Prefs.placesWikiCode = viewModel.pageTitle?.wikiSite?.languageCode
                  ?: WikipediaApp.instance.appOrSystemLanguageCode
              updateSearchText(viewModel.pageTitle?.displayText.orEmpty())
              goToLocation(1000, location)
              viewModel.fetchNearbyPages(location.latitude, location.longitude, searchRadius, ITEMS_PER_REQUEST)
          }
    }

    private val filterLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
              val languageChanged = it.data?.getBooleanExtra(PlacesFilterActivity.EXTRA_LANG_CHANGED, false)!!
            if (languageChanged) {
                annotationCache.clear()
                symbolManager?.deleteAll()
                  viewModel.fetchNearbyPages(lastLocationUpdated?.latitude ?: 0.0,
                      lastLocationUpdated?.longitude ?: 0.0, searchRadius, ITEMS_PER_REQUEST)
                  goToLocation(1000, lastLocationUpdated, lastZoom)
              }
          }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupMarkerPaints()
        markerBitmapBase = Bitmap.createBitmap(MARKER_SIZE, MARKER_SIZE, Bitmap.Config.ARGB_8888).applyCanvas {
            drawMarker(this)
        }

        Mapbox.getInstance(requireActivity().applicationContext)

        HttpRequestImpl.setOkHttpClient(OkHttpConnectionFactory.client)

        requireArguments().getParcelable<PageTitle>(Constants.ARG_TITLE)?.let {
            Prefs.placesWikiCode = it.wikiSite.languageCode
        }

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
            openSearchActivity(if (binding.searchTextView.text.toString() == getString(R.string.places_search_hint)) null
            else binding.searchTextView.text.toString())
        }

        binding.backButton.setOnClickListener {
            requireActivity().finish()
        }

        binding.searchLangContainer.setOnClickListener {
            lastZoom = mapboxMap?.cameraPosition?.zoom ?: 15.0
            filterLauncher.launch(PlacesFilterActivity.newIntent(requireActivity()))
        }

        binding.searchCloseBtn.setOnClickListener {
            updateSearchText(getString(R.string.places_search_hint))
        }

        binding.myLocationButton.setOnClickListener {
            if (haveLocationPermissions()) {
                goToLocation(0)
            } else {
                locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }
        }

        binding.viewButtonsGroup.post {
            binding.viewButtonsGroup.check(R.id.mapViewButton)
            binding.viewButtonsGroup.isVisible = true
        }

        binding.viewButtonsGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) {
                return@addOnButtonCheckedListener
            }
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
        binding.listRecyclerView.adapter = RecyclerViewAdapter()
        binding.listRecyclerView.addItemDecoration(DrawableItemDecoration(requireContext(), R.attr.list_divider, drawStart = true, skipSearchBar = true))

        return binding.root
    }

    private fun openSearchActivity(query: String?) {
        val intent = SearchActivity.newIntent(requireActivity(), Constants.InvokeSource.PLACES, query)
        val options = binding.searchContainer.let {
            ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(),
                binding.searchContainer, getString(R.string.transition_search_bar))
        }
        placesSearchLauncher.launch(intent, options)
    }

    private fun updateSearchText(searchText: String) {
        binding.searchTextView.text = searchText
        binding.searchCloseBtn.isVisible = searchText != getString(R.string.places_search_hint) && searchText.isNotEmpty()
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

                style.addImage(MARKER_DRAWABLE, markerBitmapBase)

                // TODO: Currently the style seems to break when zooming beyond 16.0. See if we can fix this.
                map.setMaxZoomPreference(15.999)

                map.uiSettings.isLogoEnabled = false
                val defMargin = DimenUtil.roundedDpToPx(16f)
                val navBarMargin = if (navBarInsets != null) navBarInsets!!.bottom else 0
                val statusBarMargin = if (statusBarInsets != null) statusBarInsets!!.top else 0

                // TODO: Needs to be optimized when changing the orientation of the device
                map.uiSettings.setCompassImage(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_compass_with_bg)!!)
                map.uiSettings.compassGravity = Gravity.TOP or Gravity.END
                map.uiSettings.setCompassMargins(defMargin, defMargin + statusBarMargin + binding.searchContainer.height, DimenUtil.roundedDpToPx(8f), defMargin)

                map.uiSettings.attributionGravity = Gravity.BOTTOM or Gravity.START
                map.uiSettings.setAttributionTintColor(ResourceUtil.getThemedColor(requireContext(), R.attr.placeholder_color))
                map.uiSettings.setAttributionMargins(defMargin, 0, defMargin, navBarMargin + DimenUtil.roundedDpToPx(36f))

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
                        updateSearchText(it.pageTitle.displayText)
                        val entry = HistoryEntry(it.pageTitle, HistoryEntry.SOURCE_PLACES)
                        resetMagnifiedSymbol()
                        magnifiedMarker = it.annotation
                        magnifiedMarker?.iconSize = 1.75f
                        symbolManager?.update(magnifiedMarker)
                        ExclusiveBottomSheetPresenter.show(childFragmentManager, LinkPreviewDialog.newInstance(entry, null))
                    }
                    true
                }

                if (haveLocationPermissions()) {
                    startLocationTracking()
                    if (savedInstanceState == null) {
                        goToLocation(1000, viewModel.location)
                    }
                } else {
                    locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
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
        val tintColor = ResourceUtil.getThemedColorStateList(requireContext(), if (isMapVisible) R.attr.paper_color else R.attr.background_color)
        binding.mapView.isVisible = isMapVisible
        binding.listRecyclerView.isVisible = !isMapVisible
        binding.searchContainer.backgroundTintList = tintColor
        // TODO: update list button color
    }

    private fun resetMagnifiedSymbol() {
        // Reset the magnified marker to regular size
        magnifiedMarker?.let {
            it.iconSize = 1.0f
            symbolManager?.update(it)
        }
    }

    private fun setupMarkerPaints() {
        markerPaintSrc = Paint().apply {
            isAntiAlias = true
            color = ResourceUtil.getThemedColor(requireContext(), R.attr.success_color)
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

        lastLocationUpdated = Location("").also {
            it.latitude = latLng.latitude
            it.longitude = latLng.longitude
        }

        LatLng(latLng.latitude, latLng.longitude)

        if ((mapboxMap?.cameraPosition?.zoom ?: 0.0) < 3.0) {
            // Don't fetch pages if the map is zoomed out too far.
            return
        }

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
                        .withIconOffset(arrayOf(0f, -32f))
                )
                if (StringUtil.removeUnderscores(viewModel.pageTitle?.text.orEmpty()) ==
                    StringUtil.removeUnderscores(it.pageTitle.text)
                ) {
                    magnifiedMarker = it.annotation
                    magnifiedMarker?.iconSize = 1.75f
                    // Reset the page title so that the marker doesn't get magnified again
                    viewModel.pageTitle = null
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
        binding.listRecyclerView.adapter?.notifyDataSetChanged()
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

    private fun goToLocation(delayMillis: Long, preferredLocation: Location? = null, zoom: Double = 15.0) {
        binding.mapView.postDelayed({
            if (isAdded && haveLocationPermissions()) {
                mapboxMap?.let {
                    val currentLocation = it.locationComponent.lastKnownLocation
                    var currentLatLngLoc: LatLng? = null
                    currentLocation?.let { loc -> currentLatLngLoc = LatLng(loc.latitude, loc.longitude) }
                    val location = preferredLocation?.let { loc -> LatLng(loc.latitude, loc.longitude) }
                    val targetLocation = location ?: currentLatLngLoc
                    targetLocation?.let { target -> it.animateCamera(CameraUpdateFactory.newLatLngZoom(target, zoom)) }
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

    override fun onMapClick(point: LatLng): Boolean {
        mapboxMap?.let {
            val screenPoint = it.projection.toScreenLocation(point)
            val rect = RectF(screenPoint.x - 10, screenPoint.y - 10, screenPoint.x + 10, screenPoint.y + 10)

            // Reset any enhanced markers to regular size
            resetMagnifiedSymbol()

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

    private inner class RecyclerViewAdapter : RecyclerView.Adapter<RecyclerViewItemHolder>() {
        override fun getItemCount(): Int {
            return viewModel.nearbyPages.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerViewItemHolder {
            return RecyclerViewItemHolder(ItemPlacesListBinding.inflate(layoutInflater, parent, false))
        }

        override fun onBindViewHolder(holder: RecyclerViewItemHolder, position: Int) {
            holder.bindItem(viewModel.nearbyPages[position], position)
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
            lastLocationUpdated?.let{
                binding.listItemDistance.text = GeoUtil.getDistanceWithUnit(it, item.location, Locale.getDefault())
            }
            item.pageTitle.thumbUrl?.let {
                ViewUtil.loadImage(binding.listItemThumbnail, it)
                binding.listItemThumbnail.isVisible = true
            } ?: run {
                binding.listItemThumbnail.isVisible = false
            }
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
