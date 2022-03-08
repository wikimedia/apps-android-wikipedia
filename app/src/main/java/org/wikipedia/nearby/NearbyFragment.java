package org.wikipedia.nearby;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Projection;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.annotation.Symbol;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.mwapi.NearbyPage;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.main.MainActivity;
import org.wikipedia.richtext.RichTextUtil;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.PermissionUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.ThrowableUtil;
import org.wikipedia.util.log.L;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Displays a list of nearby pages.
 */
public class NearbyFragment extends Fragment implements OnMapReadyCallback, Style.OnStyleLoaded {
    public interface Callback {
        void onLoading();
        void onLoaded();
        void onLoadPage(@NonNull HistoryEntry entry, @Nullable Location location);
    }

    private static final String NEARBY_LAST_RESULT = "lastRes";
    private static final String NEARBY_LAST_CAMERA_POS = "lastCameraPos";
    private static final String NEARBY_FIRST_LOCATION_LOCK = "firstLocationLock";
    private static final String ID_ICON = "id-icon";
    private static final int GO_TO_LOCATION_PERMISSION_REQUEST = 50;
    private static final int MAX_RADIUS = 10_000;

    @BindView(R.id.mapview) MapView mapView;
    @BindView(R.id.osm_license) TextView osmLicenseTextView;
    @BindView(R.id.user_location_button) FloatingActionButton locationButton;
    private Unbinder unbinder;
    private CompositeDisposable disposables = new CompositeDisposable();

    @Nullable private MapboxMap mapboxMap;
    private SymbolManager symbolManager;
    private List<Symbol> currentSymbols = new ArrayList<>();

    private NearbyResult currentResults = new NearbyResult();
    private boolean clearResultsOnNextCall;

    @Nullable private CameraPosition lastCameraPos;
    private boolean firstLocationLock;

    @NonNull public static NearbyFragment newInstance() {
        return new NearbyFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(requireContext().getApplicationContext(),
                getString(R.string.mapbox_public_token));

        if (Mapbox.getTelemetry() != null) {
            Mapbox.getTelemetry().setUserTelemetryRequestState(false);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_nearby, container, false);
        unbinder = ButterKnife.bind(this, view);

        osmLicenseTextView.setText(StringUtil.fromHtml(getString(R.string.nearby_osm_license)));
        osmLicenseTextView.setMovementMethod(LinkMovementMethod.getInstance());
        RichTextUtil.removeUnderlinesFromLinks(osmLicenseTextView);

        mapView.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        if (savedInstanceState != null) {
            lastCameraPos = savedInstanceState.getParcelable(NEARBY_LAST_CAMERA_POS);
            firstLocationLock = savedInstanceState.getBoolean(NEARBY_FIRST_LOCATION_LOCK);
            if (savedInstanceState.containsKey(NEARBY_LAST_RESULT)) {
                currentResults = GsonUnmarshaller.unmarshal(NearbyResult.class, savedInstanceState.getString(NEARBY_LAST_RESULT));
            }
        }

        onLoading();
        mapView.getMapAsync(this);
        return view;
    }

    @Override
    public void onStart() {
        mapView.onStart();
        super.onStart();
    }

    @Override
    public void onPause() {
        if (mapboxMap != null) {
            lastCameraPos = mapboxMap.getCameraPosition();
        }
        mapView.onPause();
        super.onPause();
    }

    @Override
    public void onResume() {
        mapView.onResume();

        locationButton.animate().translationY(((MainActivity) requireActivity()).isFloatingQueueEnabled()
                ? -((MainActivity) requireActivity()).getFloatingQueueImageView().getHeight() : 0).start();

        super.onResume();
    }

    @Override
    public void onStop() {
        mapView.onStop();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        if (mapboxMap != null) {
            mapboxMap.removeOnCameraMoveListener(this::fetchNearbyPages);
            mapboxMap = null;
        }
        if (symbolManager != null) {
            symbolManager.onDestroy();
            symbolManager = null;
        }
        mapView.onDestroy();
        unbinder.unbind();
        unbinder = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        disposables.clear();
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) {
            mapView.onSaveInstanceState(outState);
        }
        outState.putBoolean(NEARBY_FIRST_LOCATION_LOCK, firstLocationLock);
        if (mapboxMap != null) {
            outState.putParcelable(NEARBY_LAST_CAMERA_POS, mapboxMap.getCameraPosition());
        }
        if (currentResults != null) {
            outState.putString(NEARBY_LAST_RESULT, GsonMarshaller.marshal(currentResults));
        }
    }

    @Override public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (mapView == null || mapboxMap == null) {
            return;
        }

        if (isVisibleToUser && !firstLocationLock) {
            goToUserLocationOrPromptPermissions();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) {
            mapView.onLowMemory();
        }
    }

    @OnClick(R.id.user_location_button) void onClick() {
        if (!locationPermitted()) {
            requestLocationRuntimePermissions(GO_TO_LOCATION_PERMISSION_REQUEST);
        } else if (mapboxMap != null) {
            goToUserLocation();
        }
    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        if (!isAdded()) {
            return;
        }
        this.mapboxMap = mapboxMap;

        mapboxMap.getUiSettings().setAttributionEnabled(false);
        mapboxMap.getUiSettings().setLogoEnabled(false);
        mapboxMap.addOnCameraMoveListener(this::fetchNearbyPages);
        mapboxMap.setStyle(new Style.Builder().fromUrl("asset://mapstyle.json")
                .withImage(ID_ICON, ResourceUtil.bitmapFromVectorDrawable(requireContext(), R.drawable.ic_map_marker, null)),
                this);
    }

    @Override
    public void onStyleLoaded(@NonNull Style style) {
        if (!isAdded() || mapboxMap == null) {
            return;
        }
        symbolManager = new SymbolManager(mapView, mapboxMap, style);
        symbolManager.setIconAllowOverlap(true);
        symbolManager.setTextAllowOverlap(true);
        symbolManager.addClickListener(symbol -> {
            NearbyPage page = findNearbyPageFromSymbol(symbol);
            if (page != null) {
                onLoadPage(new HistoryEntry(page.getTitle(), HistoryEntry.SOURCE_NEARBY), page.getLocation());
            }
        });

        enableUserLocationMarker();
        if (lastCameraPos != null) {
            mapboxMap.setCameraPosition(lastCameraPos);
        } else {
            goToUserLocationOrPromptPermissions();
        }
        showNearbyPages();
    }

    @Nullable
    private NearbyPage findNearbyPageFromSymbol(Symbol symbol) {
        for (NearbyPage page : currentResults.getList()) {
            if (page.getTitle().getDisplayText().equals(symbol.getTextAnchor())) {
                return page;
            }
        }
        return null;
    }

    private boolean locationPermitted() {
        return ContextCompat.checkSelfPermission(WikipediaApp.getInstance(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationRuntimePermissions(int requestCode) {
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, requestCode);
        // once permission is granted/denied it will continue with onRequestPermissionsResult
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case GO_TO_LOCATION_PERMISSION_REQUEST:
                if (PermissionUtil.isPermitted(grantResults) && mapboxMap != null) {
                    goToUserLocation();
                } else {
                    onLoaded();
                    FeedbackUtil.showMessage(getActivity(), R.string.nearby_zoom_to_location);
                }
                break;
            default:
                throw new RuntimeException("unexpected permission request code " + requestCode);
        }
    }

    @SuppressWarnings("MissingPermission")
    private void enableUserLocationMarker() {
        if (mapboxMap != null && locationPermitted()) {
            LocationComponentOptions options = LocationComponentOptions.builder(requireActivity())
                    .trackingGesturesManagement(true)
                    .build();

            LocationComponent locationComponent = mapboxMap.getLocationComponent();
            locationComponent.activateLocationComponent(requireActivity(), mapboxMap.getStyle(), options);
            locationComponent.setLocationComponentEnabled(true);
            locationComponent.setCameraMode(CameraMode.TRACKING);
            locationComponent.setRenderMode(RenderMode.COMPASS);
        }
    }

    @SuppressWarnings("MissingPermission")
    private void goToUserLocation() {
        if (mapboxMap == null || !getUserVisibleHint()) {
            return;
        }
        if (!DeviceUtil.isLocationServiceEnabled(requireContext().getApplicationContext())) {
            showLocationDisabledSnackbar();
            return;
        }

        if (locationPermitted()) {
            Location location = mapboxMap.getLocationComponent().getLastKnownLocation();
            if (location != null) {
                goToLocation(location);
            }
        }
        fetchNearbyPages();
    }

    private void goToLocation(@NonNull Location location) {
        if (mapboxMap == null) {
            return;
        }
        CameraPosition pos = new CameraPosition.Builder()
                .target(new LatLng(location))
                .zoom(getResources().getInteger(R.integer.map_default_zoom))
                .build();
        mapboxMap.moveCamera(CameraUpdateFactory.newCameraPosition(pos));
    }

    private void goToUserLocationOrPromptPermissions() {
        if (locationPermitted()) {
            goToUserLocation();
        } else if (getUserVisibleHint()) {
            showLocationPermissionSnackbar();
        }
    }

    private void showLocationDisabledSnackbar() {
        Snackbar snackbar = FeedbackUtil.makeSnackbar(getActivity(),
                getString(R.string.location_service_disabled),
                FeedbackUtil.LENGTH_DEFAULT);
        snackbar.setAction(R.string.enable_location_service, (v) -> {
                Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                requireContext().startActivity(settingsIntent);
        });
        snackbar.show();
    }

    private void showLocationPermissionSnackbar() {
        Snackbar snackbar = FeedbackUtil.makeSnackbar(getActivity(),
                getString(R.string.location_permissions_enable_prompt),
                FeedbackUtil.LENGTH_DEFAULT);
        snackbar.setAction(R.string.location_permissions_enable_action, (v) -> requestLocationRuntimePermissions(GO_TO_LOCATION_PERMISSION_REQUEST));
        snackbar.show();
    }

    private void fetchNearbyPages() {
        if (mapView == null) {
            return;
        }
        final int fetchTaskDelayMillis = 500;
        mapView.removeCallbacks(fetchTaskRunnable);
        mapView.postDelayed(fetchTaskRunnable, fetchTaskDelayMillis);
    }

    private Runnable fetchTaskRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isResumed() || mapboxMap == null) {
                return;
            }

            onLoading();
            clearResultsOnNextCall = true;

            String latLng = String.format(Locale.ROOT, "%f|%f",
                    mapboxMap.getCameraPosition().target.getLatitude(),
                    mapboxMap.getCameraPosition().target.getLongitude());
            double radius = Math.min(MAX_RADIUS, getMapRadius());

            // kick off client calls for all supported languages
            disposables.add(Observable.fromIterable(WikipediaApp.getInstance().language().getAppLanguageCodes())
                    .flatMap(lang -> ServiceFactory.get(WikiSite.forLanguageCode(lang)).nearbySearch(latLng, radius).subscribeOn(Schedulers.io()), Pair::new)
                    .toList()
                    .observeOn(AndroidSchedulers.mainThread())
                    .map(pairs -> {
                        List<NearbyPage> pages = new ArrayList<>();
                        for (Pair<String, MwQueryResponse> pair : pairs) {
                            if (pair.second != null && pair.second.query() != null) {
                                // noinspection ConstantConditions
                                pages.addAll(pair.second.query().nearbyPages(WikiSite.forLanguageCode(pair.first)));
                            }
                        }
                        return pages;
                    })
                    .doFinally(() -> onLoaded())
                    .subscribe(pages -> {
                        if (clearResultsOnNextCall) {
                            currentResults.clear();
                            clearResultsOnNextCall = false;
                        }
                        currentResults.add(pages);
                        showNearbyPages();
                    }, caught -> {
                        ThrowableUtil.AppError error = ThrowableUtil.getAppError(requireActivity(), caught);
                        Toast.makeText(getActivity(), error.getError(), Toast.LENGTH_SHORT).show();
                        L.e(caught);
                    }));
        }
    };

    private double getMapRadius() {
        if (mapboxMap == null) {
            return 0;
        }

        Projection proj = mapboxMap.getProjection();
        LatLng leftTop = proj.fromScreenLocation(new PointF(0.0f, 0.0f));
        LatLng rightTop = proj.fromScreenLocation(new PointF(mapView.getWidth(), 0.0f));
        LatLng leftBottom = proj.fromScreenLocation(new PointF(0.0f, mapView.getHeight()));
        double width = leftTop.distanceTo(rightTop);
        double height = leftTop.distanceTo(leftBottom);
        return Math.max(width, height) / 2;
    }

    private void showNearbyPages() {
        if (mapboxMap == null || getActivity() == null) {
            return;
        }

        getActivity().invalidateOptionsMenu();
        symbolManager.delete(currentSymbols);
        currentSymbols.clear();

        List<SymbolOptions> optionsList = new ArrayList<>();

        for (NearbyPage item : currentResults.getList()) {
            if (item.getLocation() != null) {
                optionsList.add(getSymbolOptions(item));
            }
        }
        currentSymbols = symbolManager.create(optionsList);
    }

    @NonNull
    private SymbolOptions getSymbolOptions(NearbyPage page) {
        return new SymbolOptions()
                .withLatLng(new LatLng(page.getLocation()))
                .withTextAnchor(page.getTitle().getDisplayText())
                .withIconImage(ID_ICON);
    }

    private void onLoading() {
        Callback callback = callback();
        if (callback != null) {
            callback.onLoading();
        }
    }

    private void onLoaded() {
        Callback callback = callback();
        if (callback != null) {
            callback.onLoaded();
        }
    }

    private void onLoadPage(@NonNull HistoryEntry entry, @Nullable Location location) {
        Callback callback = callback();
        if (callback != null) {
            callback.onLoadPage(entry, location);
        }
    }

    @Nullable
    private Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }
}
