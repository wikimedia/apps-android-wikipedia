package org.wikipedia.nearby;

import org.wikipedia.R;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.PermissionUtil;
import org.wikipedia.util.log.L;

import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.Sprite;
import com.mapbox.mapboxsdk.constants.MyLocationTracking;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngZoom;
import com.mapbox.mapboxsdk.views.MapView;

import android.Manifest;
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

    private Sprite markerIconPassive;

    private Site site;
    private NearbyResult lastResult;
    @Nullable private Location currentLocation;

    private boolean firstLocationLock = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        site = WikipediaApp.getInstance().getSite();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_nearby, container, false);
        rootView.setPadding(0, getContentTopOffsetPx(getActivity()), 0, 0);

        mapView = (MapView) rootView.findViewById(R.id.mapview);
        mapView.setAccessToken(getString(R.string.mapbox_public_token));
        markerIconPassive = mapView.getSpriteFactory().fromResource(R.drawable.ic_map_marker);

        // TODO: pass savedInstanceState into mapView.onCreate once the MapView starts managing
        // runtime permissions in a better way. This way, the MapView will start uninitialized
        // so that we get a chance to query for runtime permissions before enabling the user's
        // location in the MapView.
        mapView.onCreate(null);

        rootView.findViewById(R.id.user_location_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkLocationPermissionsToGoToUserLocation();
            }
        });

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);

        if (lastResult != null) {
            showNearbyPages(lastResult);
        } else if (savedInstanceState != null) {
            currentLocation = savedInstanceState.getParcelable(NEARBY_CURRENT_LOCATION);
            if (currentLocation != null) {
                lastResult = savedInstanceState.getParcelable(NEARBY_LAST_RESULT);
                showNearbyPages(lastResult);
            }
        }

        setRefreshingState(true);
        initializeMap();
        checkLocationPermissionsToGoToUserLocation();
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
    public void onStart() {
        super.onStart();
        mapView.onStart();
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
    public void onStop() {
        super.onStop();
        mapView.onStop();
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
        mapView.setMyLocationTrackingMode(MyLocationTracking.TRACKING_NONE);
        mapView.setLogoVisibility(View.GONE);
        mapView.setAttributionVisibility(View.GONE);

        mapView.setOnMyLocationChangeListener(new MapView.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(@Nullable Location location) {
                makeUseOfNewLocation(location);
                if (!firstLocationLock) {
                    goToUserLocation();
                    firstLocationLock = true;
                }
            }
        });

        mapView.setOnScrollListener(new MapView.OnScrollListener() {
            @Override
            public void onScroll() {
                fetchNearbyPages();
            }
        });

        mapView.setOnMarkerClickListener(new MapView.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {
                NearbyPage page = findNearbyPageFromMarker(marker);
                if (page != null) {
                    PageTitle title = new PageTitle(page.getTitle(), site, page.getThumblUrl());
                    ((PageActivity) getActivity()).showLinkPreview(title, HistoryEntry.SOURCE_NEARBY, page.getLocation());
                    return true;
                } else {
                    return false;
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
        } else {
            mapView.setMyLocationEnabled(true);
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
                if (PermissionUtil.isPermitted(grantResults)) {
                    mapView.setMyLocationEnabled(true);
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
        Location location = mapView.getMyLocation();
        if (location != null) {
            LatLngZoom pos = new LatLngZoom(location.getLatitude(), location.getLongitude(),
                    getResources().getInteger(R.integer.map_default_zoom));
            mapView.setCenterCoordinate(pos, true);
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
            if (!isResumed()) {
                return;
            }

            LatLng latLng = mapView.getCenterCoordinate();
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
        LatLng leftTop = mapView.fromScreenLocation(new PointF(0.0f, 0.0f));
        LatLng rightTop = mapView.fromScreenLocation(new PointF(mapView.getWidth(), 0.0f));
        LatLng leftBottom = mapView.fromScreenLocation(new PointF(0.0f, mapView.getHeight()));
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
        getActivity().invalidateOptionsMenu();
        mMarkerList.clear();
        // Since Marker is a descendant of Annotation, this will remove all Markers.
        mapView.removeAllAnnotations();

        List<MarkerOptions> optionsList = new ArrayList<>();
        for (NearbyPage item : result.getList()) {
            if (item.getLocation() != null) {
                optionsList.add(createMarkerOptions(item));
            }
        }
        mMarkerList.addAll(mapView.addMarkers(optionsList));
    }

    @NonNull
    private MarkerOptions createMarkerOptions(NearbyPage page) {
        Location location = page.getLocation();
        return new MarkerOptions()
                .position(new LatLng(location.getLatitude(), location.getLongitude()))
                .title(page.getTitle())
                .icon(markerIconPassive);
    }

    private void setRefreshingState(boolean isRefreshing) {
        ((PageActivity)getActivity()).updateProgressBar(isRefreshing, true, 0);
    }
}
