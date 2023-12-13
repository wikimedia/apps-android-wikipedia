package org.wikipedia.places

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff.Mode
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.graphics.applyCanvas
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.mapbox.mapboxsdk.Mapbox
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
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.FragmentPlacesBinding
import org.wikipedia.dataclient.WikiSite
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
import org.wikipedia.util.ClipboardUtil
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.ShareUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.ViewUtil
import kotlin.math.abs

class PlacesFragment : Fragment(), LinkPreviewDialog.Callback {

    private var _binding: FragmentPlacesBinding? = null
    private val binding get() = _binding!!

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
    private var searchRadius: Int = 50
    private var filterMode = false

    private val locationPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                startLocationTracking()
                goToLastKnownLocation(1000)
            }
            else -> {
                FeedbackUtil.showMessage(requireActivity(), R.string.places_permissions_denied)
            }
        }
    }

    private val placesSearchLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
          if (it.resultCode == RESULT_OK) {
              val location = it.data?.getParcelableExtra<LatLng>(SearchActivity.EXTRA_LOCATION)!!
              Prefs.placesWikiCode = it.data?.getStringExtra(SearchActivity.EXTRA_LANG_CODE)
                  ?: WikipediaApp.instance.appOrSystemLanguageCode
              goToLocation(1000, location)
              viewModel.fetchNearbyPages(location.latitude, location.longitude, searchRadius, ITEMS_PER_REQUEST)
          }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        markerBitmapBase = ResourceUtil.bitmapFromVectorDrawable(requireContext(), R.drawable.map_marker_outline,
            null) // ResourceUtil.getThemedAttributeId(requireContext(), R.attr.secondary_color)
        markerBitmapBaseRect = Rect(0, 0, markerBitmapBase.width, markerBitmapBase.height)

        Mapbox.getInstance(requireActivity().applicationContext)

        HttpRequestImpl.setOkHttpClient(OkHttpConnectionFactory.client)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentPlacesBinding.inflate(inflater, container, false)

        binding.searchCard.tabsCountContainer.setOnClickListener {
            if (WikipediaApp.instance.tabCount == 1) {
                startActivity(PageActivity.newIntent(requireActivity()))
            } else {
                startActivity(TabActivity.newIntent(requireActivity()))
            }
        }

        binding.searchCard.searchTextView.let { textView ->
            textView.setOnClickListener {
                openSearchActivity(if (textView.text.toString() == getString(R.string.places_search_hint)) null
                else textView.text.toString())
            }
        }

        binding.searchCard.searchBack.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.searchCard.searchLangCode.setOnClickListener {
            filterMode = true
            startActivity(PlacesFilterActivity.newIntent(requireActivity()))
        }

        binding.searchCard.searchCloseBtn.setOnClickListener {
            binding.searchCard.searchTextView.text = getString(R.string.places_search_hint)
            it.visibility = View.GONE
        }

        binding.myLocationButton.setOnClickListener {
            if (haveLocationPermissions()) {
                goToLastKnownLocation(0)
            } else {
                locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }
        }

        return binding.root
    }

    private fun openSearchActivity(query: String?) {
        val intent = SearchActivity.newIntent(requireActivity(), Constants.InvokeSource.PLACES, query)
        val options = binding.searchCard.searchContainer.let {
            ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(),
                binding.searchCard.searchContainer, getString(R.string.transition_search_bar))
        }
        placesSearchLauncher.launch(intent, options)
    }

    private fun updateTabsView() {
        val tabsCount = WikipediaApp.instance.tabCount
        binding.searchCard.searchTabsCountView.isVisible = tabsCount != 0
        binding.searchCard.searchTabsCountView.text = tabsCount.toString()
        binding.searchCard.searchLangCode.text = Prefs.placesWikiCode
        ViewUtil.formatLangButton(binding.searchCard.searchLangCode, Prefs.placesWikiCode,
            SearchFragment.LANG_BUTTON_TEXT_SIZE_SMALLER, SearchFragment.LANG_BUTTON_TEXT_SIZE_LARGER)
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
                searchRadius = latitudeDiffToMeters(mapboxMap?.projection?.visibleRegion?.latLngBounds?.latitudeSpan ?: 0.0) / 2

                style.addImage(MARKER_DRAWABLE, AppCompatResources.getDrawable(requireActivity(), R.drawable.map_marker)!!)

                // TODO: Currently the style seems to break when zooming beyond 16.0. See if we can fix this.
                map.setMaxZoomPreference(15.999)

                map.uiSettings.isLogoEnabled = false
                val attribMargin = DimenUtil.roundedDpToPx(16f)
                map.uiSettings.setAttributionMargins(attribMargin, 0, attribMargin, attribMargin)
                map.uiSettings.compassGravity = Gravity.START or Gravity.BOTTOM
                val compassMargin = DimenUtil.roundedDpToPx(36f)
                map.uiSettings.setCompassMargins(attribMargin, 0, 0, compassMargin)

                map.addOnCameraIdleListener {
                    onUpdateCameraPosition(mapboxMap?.cameraPosition?.target)
                }

                symbolManager = SymbolManager(binding.mapView, map, style)

                symbolManager?.iconAllowOverlap = true
                symbolManager?.textAllowOverlap = true
                symbolManager?.addClickListener { symbol ->
                    L.d(">>>> clicked: " + symbol.latLng.latitude + ", " + symbol.latLng.longitude)
                    annotationCache.find { it.annotation == symbol }?.let {
                        binding.searchCard.searchTextView.text = it.pageTitle.displayText
                        binding.searchCard.searchCloseBtn.isVisible = true
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
        updateTabsView()
        if (filterMode) {
            filterMode = false
            symbolManager?.deleteAll()
            goToLastKnownLocation(1000)
        }
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

        L.d(">>> requesting update: " + latLng.latitude + ", " + latLng.longitude + ", " + mapboxMap?.cameraPosition?.zoom)
        viewModel.fetchNearbyPages(latLng.latitude, latLng.longitude, searchRadius, ITEMS_PER_REQUEST)
    }

    private fun updateMapMarkers(pages: List<PlacesFragmentViewModel.NearbyPage>) {
        symbolManager?.let { manager ->

            pages.filter {
                annotationCache.find { item -> item.pageId == it.pageId } == null
            }.forEach {
                it.annotation = manager.create(SymbolOptions()
                    .withLatLng(LatLng(it.latitude, it.longitude))
                    .withTextFont(MARKER_FONT_STACK)
                    .withTextField(it.pageTitle.displayText)
                    .withTextSize(10f)
                    .withTextOffset(arrayOf(0f, 1f))
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

    private fun goToLocation(delayMillis: Long, location: LatLng?) {
        binding.mapView.postDelayed({
            if (isAdded) {
                mapboxMap?.let {
                    location?.let { loc ->
                        val latLng = LatLng(loc.latitude, loc.longitude)
                        it.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12.0))
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

    companion object {
        const val MARKER_DRAWABLE = "markerDrawable"
        const val MAX_ANNOTATIONS = 64
        const val THUMB_SIZE = 160
        const val ITEMS_PER_REQUEST = 50
        val MARKER_FONT_STACK = arrayOf("Open Sans Regular")
        val MARKER_WIDTH = DimenUtil.roundedDpToPx(48f)
        val MARKER_HEIGHT = DimenUtil.roundedDpToPx(60f)

        fun newInstance(wiki: WikiSite): PlacesFragment {
            return PlacesFragment().apply {
                arguments = bundleOf(PlacesActivity.EXTRA_WIKI to wiki)
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
