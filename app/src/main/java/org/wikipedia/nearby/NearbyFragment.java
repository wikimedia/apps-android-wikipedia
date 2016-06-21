package org.wikipedia.nearby;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.MapboxConstants;
import com.mapbox.mapboxsdk.constants.MyLocationTracking;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Projection;
import com.mapbox.mapboxsdk.telemetry.MapboxEventManager;

import org.wikipedia.R;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.MainActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.PermissionUtil;
import org.wikipedia.util.log.L;

import java.util.ArrayList;
import java.util.List;

import static org.wikipedia.util.DimenUtil.getContentTopOffsetPx;

/**
 * Displays a list of nearby pages.
 */
public class NearbyFragment extends Fragment {
    private static final String NEARBY_LAST_RESULT = "lastRes";
    private static final String NEARBY_CURRENT_LOCATION = "currentLoc";
    private static final int GO_TO_LOCATION_PERMISSION_REQUEST = 50;

    private final List<Marker> mMarkerList = new ArrayList<>();

    private MapView mapView;
    @Nullable private MapboxMap mapboxMap;

    private Icon markerIconPassive;

    private Site site;
    private NearbyResult lastResult;
    @Nullable private Location currentLocation;

    private boolean firstLocationLock = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        site = WikipediaApp.getInstance().getSite();

        disableTelemetry();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_nearby, container, false);
        rootView.setPadding(0, getContentTopOffsetPx(getActivity()), 0, 0);

        mapView = (MapView) rootView.findViewById(R.id.mapview);
        markerIconPassive = IconFactory.getInstance(getActivity()).fromResource(R.drawable.ic_map_marker);

        mapView.onCreate(savedInstanceState);

        rootView.findViewById(R.id.user_location_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkLocationPermissionsToGoToUserLocation();
            }
        });

        setHasOptionsMenu(true);

        if (savedInstanceState != null) {
            currentLocation = savedInstanceState.getParcelable(NEARBY_CURRENT_LOCATION);
            if (currentLocation != null) {
                lastResult = savedInstanceState.getParcelable(NEARBY_LAST_RESULT);
            }
        }

        setRefreshingState(true);
        initializeMap();

        return rootView;
    }

    @Override
    public void onDestroyView() {
        mapboxMap = null;
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
        if (lastResult != null) {
            outState.putParcelable(NEARBY_CURRENT_LOCATION, currentLocation);
            outState.putParcelable(NEARBY_LAST_RESULT, lastResult);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    private void initializeMap() {
        mapView.setStyleUrl("asset://mapstyle.json");

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull MapboxMap mapboxMap) {
                NearbyFragment.this.mapboxMap = mapboxMap;

                mapboxMap.getTrackingSettings().setMyLocationTrackingMode(MyLocationTracking.TRACKING_NONE);

                mapboxMap.setOnMyLocationChangeListener(new MapboxMap.OnMyLocationChangeListener() {
                    @Override
                    public void onMyLocationChange(@Nullable Location location) {
                        makeUseOfNewLocation(location);
                        if (!firstLocationLock) {
                            goToUserLocation();
                            firstLocationLock = true;
                        }
                    }
                });

                mapboxMap.setOnScrollListener(new MapboxMap.OnScrollListener() {
                    @Override
                    public void onScroll() {
                        fetchNearbyPages();
                    }
                });
                mapboxMap.setOnMarkerClickListener(new MapboxMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(@NonNull Marker marker) {
                        NearbyPage page = findNearbyPageFromMarker(marker);
                        if (page != null) {
                            PageTitle title = new PageTitle(page.getTitle(), site, page.getThumblUrl());
                            ((MainActivity) getActivity()).showLinkPreview(title, HistoryEntry.SOURCE_NEARBY, page.getLocation());
                            return true;
                        } else {
                            return false;
                        }
                    }
                });

                checkLocationPermissionsToGoToUserLocation();

                if (currentLocation != null && lastResult != null) {
                    showNearbyPages(lastResult);
                }
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

    private void checkLocationPermissionsToGoToUserLocation() {
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationRuntimePermissions(GO_TO_LOCATION_PERMISSION_REQUEST);
        } else if (mapboxMap != null) {
            mapboxMap.setMyLocationEnabled(true);
            goToUserLocation();
        }
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
                    mapboxMap.setMyLocationEnabled(true);
                    goToUserLocation();
                } else {
                    setRefreshingState(false);
                    FeedbackUtil.showMessage(getActivity(), R.string.nearby_zoom_to_location);
                }
                break;
            default:
                throw new RuntimeException("unexpected permission request code " + requestCode);
        }
    }

    private void goToUserLocation() {
        if (mapboxMap == null) {
            return;
        }

        Location location = mapboxMap.getMyLocation();
        if (location != null) {
            CameraPosition pos = new CameraPosition.Builder()
                    .target(new LatLng(location))
                    .zoom(getResources().getInteger(R.integer.map_default_zoom))
                    .build();
            mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(pos));
        }
        fetchNearbyPages();
    }

    private void makeUseOfNewLocation(Location location) {
        if (!isBetterLocation(location, currentLocation)) {
            return;
        }
        currentLocation = location;
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

            LatLng latLng = mapboxMap.getCameraPosition().target;
            setRefreshingState(true);
            new NearbyFetchTask(getActivity(), site, latLng.getLatitude(), latLng.getLongitude(), getMapRadius()) {
                @Override
                public void onFinish(NearbyResult result) {
                    if (!isResumed()) {
                        return;
                    }
                    lastResult = result;
                    showNearbyPages(result);
                    setRefreshingState(false);
                }

                @Override
                public void onCatch(Throwable caught) {
                    if (!isResumed()) {
                        return;
                    }
                    L.e(caught);
                    FeedbackUtil.showError(getActivity(), caught);
                    setRefreshingState(false);
                }
            }.execute();
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

    /** Determines whether one Location reading is better than the current Location fix.
     * lifted from http://developer.android.com/guide/topics/location/strategies.html
     * @param location  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new one
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        final int twoMinutes = 1000 * 60 * 2;
        final int accuracyThreshold = 200;
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > twoMinutes;
        boolean isSignificantlyOlder = timeDelta < -twoMinutes;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > accuracyThreshold;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                                                    currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    private void showNearbyPages(NearbyResult result) {
        if (mapboxMap == null) {
            return;
        }

        getActivity().invalidateOptionsMenu();
        mMarkerList.clear();
        // Since Marker is a descendant of Annotation, this will remove all Markers.
        mapboxMap.removeAnnotations();

        List<MarkerOptions> optionsList = new ArrayList<>();
        for (NearbyPage item : result.getList()) {
            if (item.getLocation() != null) {
                optionsList.add(createMarkerOptions(item));
            }
        }
        mMarkerList.addAll(mapboxMap.addMarkers(optionsList));
    }

    @NonNull
    private MarkerOptions createMarkerOptions(NearbyPage page) {
        Location location = page.getLocation();
        return new MarkerOptions()
                .position(new LatLng(location.getLatitude(), location.getLongitude()))
                .title(page.getTitle())
                .icon(markerIconPassive);
    }

    @SuppressLint("CommitPrefEdits")
    private void disableTelemetry() {
        // setTelemetryEnabled() does not write to shared prefs unless a change is detected.
        // However, it is initialized to false and then defaulted to true when retrieving from
        // shared prefs later. This means either calling setTelemetryEnabled(true) first or writing
        // to Mapbox's private shared prefs directly. setTelemetryEnabled(true) would start the
        // service at least briefly so the latter approach is used.

        // Lint recommends editor.apply() instead of commit() because it blocks but we really want
        // to be certain that telemetry isn't enabled.

        SharedPreferences prefs = getContext().getSharedPreferences(MapboxConstants.MAPBOX_SHARED_PREFERENCES_FILE,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(MapboxConstants.MAPBOX_SHARED_PREFERENCE_KEY_TELEMETRY_ENABLED, false);
        editor.commit();

        MapboxEventManager.getMapboxEventManager().setTelemetryEnabled(false);
    }

    private void setRefreshingState(boolean isRefreshing) {
        ((MainActivity)getActivity()).updateProgressBar(isRefreshing, true, 0);
    }
}
