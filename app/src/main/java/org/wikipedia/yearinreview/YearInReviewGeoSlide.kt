package org.wikipedia.yearinreview

import android.Manifest
import android.location.Location
import android.view.Gravity
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.module.http.HttpRequestImpl
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.PlacesEvent
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory
import org.wikipedia.places.PlacesFragment.Companion.MARKER_DRAWABLE
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.util.log.L

@Composable
fun GeoScreenContent(
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues,
    screenData: YearInReviewScreenData.GeoScreen,
    screenCaptureMode: Boolean = false,
) {
    val scrollState = rememberScrollState()
    val headerAspectRatio = 3f / 2f
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        MapLibre.getInstance(context.applicationContext)
        HttpRequestImpl.setOkHttpClient(OkHttpConnectionFactory.client)
        MapView(context, MapLibreMapOptions.createFromAttributes(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    Column(
        verticalArrangement = Arrangement.Top,
        modifier = modifier.padding(innerPadding).verticalScroll(scrollState)
    ) {
        Box(
            modifier = Modifier.clip(RoundedCornerShape(16.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(headerAspectRatio)
            ) {
                AndroidView(
                    factory = { mapView },
                    modifier = Modifier.matchParentSize(),
                    update = { view ->
                        view.getMapAsync { map ->
                            val assetForTheme = if (WikipediaApp.instance.currentTheme.isDark) "asset://mapstyle-dark.json" else "asset://mapstyle.json"
                            map.setStyle(Style.Builder().fromUri(assetForTheme)) { style ->
                                //mapboxMap = map

                                //style.addImage(MARKER_DRAWABLE, markerBitmapBase)

                                map.setMaxZoomPreference(20.0)

                                map.uiSettings.isLogoEnabled = false
                                val defMargin = DimenUtil.roundedDpToPx(16f)

                                map.uiSettings.setCompassImage(AppCompatResources.getDrawable(context, R.drawable.ic_compass_with_bg)!!)
                                map.uiSettings.compassGravity = Gravity.TOP or Gravity.END
                                map.uiSettings.attributionGravity = Gravity.BOTTOM or Gravity.START
                                map.uiSettings.setAttributionTintColor(ResourceUtil.getThemedColor(context, R.attr.placeholder_color))


                                /*
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

                                viewModel.location?.let {
                                    goToLocation(it)
                                } ?: run {
                                    if (Prefs.placesDefaultLocationLatLng != null) {
                                        goToLocation(getDefaultLocation())
                                    } else {
                                        val lastLocationAndZoomLevel = Prefs.placesLastLocationAndZoomLevel
                                        goToLocation(lastLocationAndZoomLevel?.first, lastLocationAndZoomLevel?.second ?: lastZoom)
                                    }
                                }
                                */

                            }
                        }
                    }
                )
            }
        }
        Column {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    modifier = Modifier
                        .padding(top = 10.dp, start = 16.dp, end = 8.dp)
                        .height(IntrinsicSize.Min)
                        .weight(1f),
                    text = processString(screenData.headlineText),
                    color = WikipediaTheme.colors.primaryColor,
                    style = MaterialTheme.typography.headlineMedium
                )
                if (!screenCaptureMode) {
                    IconButton(
                        onClick = {
                            UriUtil.handleExternalLink(
                                context = context,
                                uri = context.getString(R.string.year_in_review_media_wiki_faq_url).toUri()
                            )
                        }) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_info_24),
                            tint = WikipediaTheme.colors.primaryColor,
                            contentDescription = stringResource(R.string.year_in_review_information_icon)
                        )
                    }
                }
            }
            HtmlText(
                modifier = Modifier
                    .padding(top = 10.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .height(IntrinsicSize.Min),
                text = processString(screenData.bodyText),
                color = WikipediaTheme.colors.primaryColor,
                linkStyle = TextLinkStyles(
                    style = SpanStyle(
                        color = WikipediaTheme.colors.progressiveColor,
                        fontSize = 16.sp
                    )
                ),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
