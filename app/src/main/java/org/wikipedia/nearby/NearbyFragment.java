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
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.MyLocationTracking;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Projection;
import com.mapbox.services.android.telemetry.MapboxTelemetry;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;
import com.mapbox.services.android.telemetry.location.LocationEngineProvider;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.page.PageTitle;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import retrofit2.Call;

/**
 * Displays a list of nearby pages.
 */
public class NearbyFragment extends Fragment {
    public interface Callback {
        void onLoading();
        void onLoaded();
        void onLoadPage(PageTitle title, int entrySource, @Nullable Location location);
    }

    private static final String NEARBY_LAST_RESULT = "lastRes";
    private static final String NEARBY_LAST_CAMERA_POS = "lastCameraPos";
    private static final String NEARBY_FIRST_LOCATION_LOCK = "firstLocationLock";
    private static final int GO_TO_LOCATION_PERMISSION_REQUEST = 50;

    @BindView(R.id.mapview) MapView mapView;
    @BindView(R.id.osm_license) TextView osmLicenseTextView;
    private Unbinder unbinder;

    @Nullable private MapboxMap mapboxMap;
    private Icon markerIconPassive;
    private LocationEngine locationEngine;

    private NearbyClient client;
    private NearbyResult lastResult;

    private LocationChangeListener locationChangeListener = new LocationChangeListener();
    @Nullable private CameraPosition lastCameraPos;
    private boolean firstLocationLock;

    @NonNull public static NearbyFragment newInstance() {
        return new NearbyFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        client = new NearbyClient();

        Mapbox.getInstance(getContext().getApplicationContext(),
                getString(R.string.mapbox_public_token));
        MapboxTelemetry.getInstance().setTelemetryEnabled(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_nearby, container, false);
        unbinder = ButterKnife.bind(this, view);

        markerIconPassive = IconFactory.getInstance(getContext())
                .fromBitmap(ResourceUtil.bitmapFromVectorDrawable(getContext(),
                        R.drawable.ic_map_marker));

        osmLicenseTextView.setText(StringUtil.fromHtml(getString(R.string.nearby_osm_license)));
        osmLicenseTextView.setMovementMethod(LinkMovementMethod.getInstance());
        RichTextUtil.removeUnderlinesFromLinks(osmLicenseTextView);

        mapView.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        if (savedInstanceState != null) {
            lastCameraPos = savedInstanceState.getParcelable(NEARBY_LAST_CAMERA_POS);
            firstLocationLock = savedInstanceState.getBoolean(NEARBY_FIRST_LOCATION_LOCK);
            if (savedInstanceState.containsKey(NEARBY_LAST_RESULT)) {
                lastResult = GsonUnmarshaller.unmarshal(NearbyResult.class, savedInstanceState.getString(NEARBY_LAST_RESULT));
            }
        }

        locationEngine = new LocationEngineProvider(getContext()).obtainBestLocationEngineAvailable();
        locationEngine.addLocationEngineListener(locationChangeListener);

        onLoading();
        initializeMap();
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
        super.onResume();
    }

    @Override
    public void onStop() {
        mapView.onStop();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        locationEngine.removeLocationEngineListener(locationChangeListener);
        mapView.onDestroy();
        mapboxMap = null;
        unbinder.unbind();
        unbinder = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        WikipediaApp.getInstance().getRefWatcher().watch(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) {
            mapView.onSaveInstanceState(outState);
        }
        outState.putBoolean(NEARBY_FIRST_LOCATION_LOCK, firstLocationLock);
        if (mapboxMap != null) {
            outState.putParcelable(NEARBY_LAST_CAMERA_POS, mapboxMap.getCameraPosition());
        }
        if (lastResult != null) {
            outState.putString(NEARBY_LAST_RESULT, GsonMarshaller.marshal(lastResult));
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

    private void initializeMap() {
        mapView.getMapAsync((@NonNull MapboxMap mapboxMap) -> {
            if (!isAdded()) {
                return;
            }
            NearbyFragment.this.mapboxMap = mapboxMap;

            enableUserLocationMarker();
            mapboxMap.getTrackingSettings().setMyLocationTrackingMode(MyLocationTracking.TRACKING_NONE);

            mapboxMap.setOnScrollListener(this::fetchNearbyPages);
            mapboxMap.setOnMarkerClickListener((@NonNull Marker marker) -> {
                NearbyPage page = findNearbyPageFromMarker(marker);
                if (page != null) {
                    PageTitle title = new PageTitle(page.getTitle(), lastResult.getWiki(), page.getThumbUrl());
                    onLoadPage(title, HistoryEntry.SOURCE_NEARBY, page.getLocation());
                    return true;
                } else {
                    return false;
                }
            });

            if (lastCameraPos != null) {
                mapboxMap.setCameraPosition(lastCameraPos);
            } else {
                goToUserLocationOrPromptPermissions();
            }
            if (lastResult != null) {
                showNearbyPages(lastResult);
            }
        });
    }

    @Nullable
    private NearbyPage findNearbyPageFromMarker(Marker marker) {
        for (NearbyPage page : lastResult.getList()) {
            if (page.getTitle().equals(marker.getTitle())) {
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

    private void enableUserLocationMarker() {
        if (mapboxMap != null && locationPermitted()) {
            mapboxMap.setMyLocationEnabled(true);
        }
    }

    private void goToUserLocation() {
        if (mapboxMap == null || !getUserVisibleHint()) {
            return;
        }
        if (!DeviceUtil.isLocationServiceEnabled(getContext().getApplicationContext())) {
            showLocationDisabledSnackbar();
            return;
        }

        enableUserLocationMarker();
        Location location = mapboxMap.getMyLocation();
        if (location != null) {
            goToLocation(location);
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
        mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(pos));
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
                getContext().startActivity(settingsIntent);
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

            WikiSite wiki = WikipediaApp.getInstance().getWikiSite();
            client.request(wiki, mapboxMap.getCameraPosition().target.getLatitude(),
                    mapboxMap.getCameraPosition().target.getLongitude(), getMapRadius(),
                    new NearbyClient.Callback() {
                        @Override public void success(@NonNull Call<MwQueryResponse> call,
                                                      @NonNull NearbyResult result) {
                            if (!isResumed()) {
                                return;
                            }
                            lastResult = result;
                            showNearbyPages(result);
                            onLoaded();
                        }

                        @Override public void failure(@NonNull Call<MwQueryResponse> call,
                                                      @NonNull Throwable caught) {
                            if (!isResumed()) {
                                return;
                            }
                            ThrowableUtil.AppError error = ThrowableUtil.getAppError(getActivity(), caught);
                            Toast.makeText(getActivity(), error.getError(), Toast.LENGTH_SHORT).show();
                            L.e(caught);
                            onLoaded();
                        }
                    });
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

    private void showNearbyPages(NearbyResult result) {
        if (mapboxMap == null || getActivity() == null) {
            return;
        }

        getActivity().invalidateOptionsMenu();
        // Since Marker is a descendant of Annotation, this will remove all Markers.
        mapboxMap.removeAnnotations();

        List<MarkerOptions> optionsList = new ArrayList<>();
        for (NearbyPage item : result.getList()) {
            if (item.getLocation() != null) {
                optionsList.add(createMarkerOptions(item));
            }
        }
        mapboxMap.addMarkers(optionsList);
    }

    @NonNull
    private MarkerOptions createMarkerOptions(NearbyPage page) {
        Location location = page.getLocation();
        return new MarkerOptions()
                .position(new LatLng(location.getLatitude(), location.getLongitude()))
                .title(page.getTitle())
                .icon(markerIconPassive);
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

    private void onLoadPage(@NonNull PageTitle title, int entrySource, @Nullable Location location) {
        Callback callback = callback();
        if (callback != null) {
            callback.onLoadPage(title, entrySource, location);
        }
    }

    private class LocationChangeListener implements LocationEngineListener {
        @Override
        public void onConnected() {
        }

        @Override
        public void onLocationChanged(Location location) {
            if (!firstLocationLock) {
                goToUserLocation();
                firstLocationLock = true;
            }
        }
    }

    @Nullable
    private Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }
}
