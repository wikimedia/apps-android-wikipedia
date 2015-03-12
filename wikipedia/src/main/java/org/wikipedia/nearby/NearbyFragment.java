package org.wikipedia.nearby;

import org.wikipedia.PageTitle;
import org.wikipedia.R;
import org.wikipedia.Site;
import org.wikipedia.Utils;
import org.wikipedia.WikipediaApp;
import org.wikipedia.history.HistoryEntry;
import org.mediawiki.api.json.ApiException;
import org.wikipedia.page.PageActivity;

import com.squareup.picasso.Picasso;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;

/**
 * Displays a list of nearby pages.
 */
public class NearbyFragment extends Fragment implements SensorEventListener {
    private static final String PREF_KEY_UNITS = "nearbyUnits";
    private static final String NEARBY_LAST_RESULT = "lastRes";
    private static final String NEARBY_LAST_LOCATION = "lastLoc";
    private static final String NEARBY_NEXT_LOCATION = "curLoc";
    private static final int MIN_TIME_MILLIS = 5000;
    private static final int MIN_DISTANCE_METERS = 2;
    private static final int ONE_THOUSAND = 1000;
    private static final double ONE_THOUSAND_D = 1000.0d;
    private static final double METER_TO_FEET = 3.280839895;
    private static final int ONE_MILE = 5280;

    private ViewGroup nearbyContainer;
    private ListView nearbyList;
    private View nearbyEmptyContainer;
    private NearbyAdapter adapter;
    private SwipeRefreshLayout refreshView;

    private WikipediaApp app;
    private Site site;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private boolean refreshing;
    private Location lastLocation;
    private Location nextLocation;
    private NearbyResult lastResult;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;

    //this holds the actual data from the accelerometer and magnetometer, and automatically
    //maintains a moving average (low-pass filter) to reduce jitter.
    private MovingAverageArray accelData;
    private MovingAverageArray magneticData;

    //The size with which we'll initialize our low-pass filters. This size seems like
    //a good balance between effectively removing jitter, and good response speed.
    //(Mimics a physical compass needle)
    private static final int MOVING_AVERAGE_SIZE = 8;

    //geomagnetic field data, to be updated whenever we update location.
    //(will provide us with declination from true north)
    private GeomagneticField geomagneticField;

    //we'll maintain a list of CompassViews that are currently being displayed, and update them
    //whenever we receive updates from sensors.
    private List<NearbyCompassView> compassViews;

    //whether to display distances in imperial units (feet/miles) instead of metric
    private boolean showImperial = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = WikipediaApp.getInstance();
        site = app.getPrimarySite();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_nearby, container, false);

        nearbyContainer = (ViewGroup) rootView.findViewById(R.id.nearby_container);
        nearbyList = (ListView) rootView.findViewById(R.id.nearby_list);
        nearbyEmptyContainer = rootView.findViewById(R.id.nearby_empty_container);
        nearbyEmptyContainer.setVisibility(View.GONE);
        refreshView = (SwipeRefreshLayout) rootView.findViewById(R.id.nearby_refresh_container);
        refreshView.setSize(SwipeRefreshLayout.LARGE);
        // if we want to give it a custom color:
        //refreshView.setProgressBackgroundColor(R.color.swipe_refresh_circle);
        refreshView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                setRefreshingState(true);
                requestLocationUpdates();
                refreshView.setRefreshing(false);
            }
        });
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        adapter = new NearbyAdapter(getActivity(), new ArrayList<NearbyPage>());
        nearbyList.setAdapter(adapter);

        nearbyList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                NearbyPage nearbyPage = adapter.getItem(position);
                PageTitle title = new PageTitle(nearbyPage.getTitle(), site, nearbyPage.getThumblUrl());
                HistoryEntry newEntry = new HistoryEntry(title, HistoryEntry.SOURCE_NEARBY);
                ((PageActivity)getActivity()).displayNewPage(title, newEntry);
            }
        });

        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                if (!isAdded()) {
                    return;
                }
                makeUseOfNewLocation(location);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.d("Wikipedia", "onStatusChanged " + provider);
            }

            public void onProviderEnabled(String provider) {
                Log.d("Wikipedia", "onProviderEnabled " + provider);
            }

            public void onProviderDisabled(String provider) {
                Log.d("Wikipedia", "onProviderDisabled " + provider);
            }
        };

        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        compassViews = new ArrayList<>();

        if (!adapter.isEmpty()) {
            //we
            setupGeomagneticField();
            showNearbyPages(lastResult);
        } else if (savedInstanceState != null) {
            nextLocation = savedInstanceState.getParcelable(NEARBY_NEXT_LOCATION);
            if (nextLocation != null) {
                lastLocation = savedInstanceState.getParcelable(NEARBY_LAST_LOCATION);
                lastResult = savedInstanceState.getParcelable(NEARBY_LAST_RESULT);
                setupGeomagneticField();
                showNearbyPages(lastResult);
            } else {
                setRefreshingState(true);
            }
        } else {
            setRefreshingState(true);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        //do we already have a preference for metric/imperial units?
        if (prefs.contains(PREF_KEY_UNITS)) {
            setImperialUnits(prefs.getBoolean(PREF_KEY_UNITS, false));
        } else {
            //if our locale is set to US, then use imperial units by default.
            try {
                if (Locale.getDefault().getISO3Country().equalsIgnoreCase(Locale.US.getISO3Country())) {
                    setImperialUnits(true);
                }
            } catch (MissingResourceException e) {
                // getISO3Country can throw MissingResourceException: No 3-letter country code for locale: zz_ZZ
                // Just ignore it.
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (lastResult != null) {
            outState.putParcelable(NEARBY_LAST_LOCATION, lastLocation);
            outState.putParcelable(NEARBY_NEXT_LOCATION, nextLocation);
            outState.putParcelable(NEARBY_LAST_RESULT, lastResult);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_UI);
        requestLocationUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopLocationUpdates();
        mSensorManager.unregisterListener(this);
        compassViews.clear();
    }

    private void stopLocationUpdates() {
        setRefreshingState(false);
        locationManager.removeUpdates(locationListener);
    }

    private void requestLocationUpdates() {
        boolean atLeastOneEnabled = false;
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            try {
                requestLocation(LocationManager.NETWORK_PROVIDER);
                atLeastOneEnabled = true;
            } catch (SecurityException e) {
                Log.e("Wikipedia", "Could not request location from "
                        + LocationManager.NETWORK_PROVIDER, e);
            }
        }
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            try {
                requestLocation(LocationManager.GPS_PROVIDER);
                atLeastOneEnabled = true;
            } catch (SecurityException e) {
                Log.e("Wikipedia", "Could not request location from "
                        + LocationManager.GPS_PROVIDER, e);
            }
        }
        // if neither of the location providers are enabled, then give the user the option
        // to go to Settings, so that they enable Location in the actual OS.
        if (!atLeastOneEnabled) {
            showDialogForSettings();
        }
    }

    private void requestLocation(String provider) {
        locationManager.requestLocationUpdates(provider, MIN_TIME_MILLIS, MIN_DISTANCE_METERS, locationListener);
    }

    private void showDialogForSettings() {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setMessage(R.string.nearby_dialog_goto_settings);
        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                try {
                    Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(myIntent);
                } catch (ActivityNotFoundException e) {
                    // it's highly unusual for a device not to have a Settings activity,
                    // but nevertheless, let's not crash in case it happens.
                    e.printStackTrace();
                }
            }
        });
        alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        alert.setCancelable(false);
        AlertDialog ad = alert.create();
        ad.show();
    }

    private void makeUseOfNewLocation(Location location) {
        if (!isBetterLocation(location, lastLocation)) {
            return;
        }
        nextLocation = location;
        setupGeomagneticField();
        if (lastLocation == null || (refreshing && getDistance(lastLocation) >= MIN_DISTANCE_METERS)) {

            new NearbyFetchTask(getActivity(), site, location) {
                @Override
                public void onFinish(NearbyResult result) {
                    if (!isAdded()) {
                        return;
                    }
                    lastResult = result;
                    showNearbyPages(result);
                }

                @Override
                public void onCatch(Throwable caught) {
                    if (!isAdded()) {
                        return;
                    }
                    if (caught instanceof ApiException) {
                        if (caught.getCause() instanceof UnknownHostException) {
                            Crouton.makeText(getActivity(), R.string.nearby_no_network, Style.ALERT, nearbyContainer).show();
                        } else {
                            Crouton.makeText(getActivity(), R.string.error_network_error, Style.ALERT, nearbyContainer).show();
                        }
                    } else if (caught instanceof NearbyFetchException) {
                        Log.e("Wikipedia", "Could not get list of nearby places: " + caught.toString());
                        Crouton.makeText(getActivity(), R.string.nearby_server_error, Style.ALERT, nearbyContainer).show();
                    } else {
                        super.onCatch(caught);
                    }
                    setRefreshingState(false);
                }
            }.execute();
        } else {
            updateDistances();
        }
    }

    /** Updates geomagnetic field data, to give us our precise declination from true north. */
    private void setupGeomagneticField() {
        geomagneticField = new GeomagneticField((float)nextLocation.getLatitude(), (float)nextLocation.getLongitude(), 0, (new Date()).getTime());
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
        nearbyList.setEmptyView(nearbyEmptyContainer);
        lastLocation = nextLocation;
        sortByDistance(result.getList());
        adapter.clear();
        addResultsToAdapter(result.getList());
        compassViews.clear();

        setRefreshingState(false);
    }

    private void addResultsToAdapter(List<NearbyPage> result) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            adapter.addAll(result);
        } else {
            for (NearbyPage page : result) {
                adapter.add(page);
            }
        }
    }

    private void setRefreshingState(boolean newState) {
        refreshing = newState;
        if (refreshing) {
            ((PageActivity)getActivity()).updateProgressBar(true, true, 0);
        } else {
            ((PageActivity)getActivity()).updateProgressBar(false, true, 0);
        }
    }

    private void sortByDistance(List<NearbyPage> nearbyPages) {
        calcDistances(nearbyPages);

        Collections.sort(nearbyPages, new Comparator<NearbyPage>() {
            public int compare(NearbyPage a, NearbyPage b) {
                return a.getDistance() - b.getDistance();
            }
        });
    }

    /**
     * Calculates the distances from the origin to the given pages.
     * This method should be called before sorting.
     */
    private void calcDistances(List<NearbyPage> pages) {
        for (NearbyPage page : pages) {
            page.setDistance(getDistance(page.getLocation()));
        }
    }

    private int getDistance(Location otherLocation) {
        if (otherLocation == null) {
            return Integer.MAX_VALUE;
        } else {
            return (int) nextLocation.distanceTo(otherLocation);
        }
    }

    private String getDistanceLabel(Location otherLocation) {
        final int meters = getDistance(otherLocation);
        if (showImperial) {
            final double feet = meters * METER_TO_FEET;
            if (feet < ONE_THOUSAND) {
                return getString(R.string.nearby_distance_in_feet, (int)feet);
            } else {
                return getString(R.string.nearby_distance_in_miles, feet / ONE_MILE);
            }
        } else {
            if (meters < ONE_THOUSAND) {
                return getString(R.string.nearby_distance_in_meters, meters);
            } else {
                return getString(R.string.nearby_distance_in_kilometers, meters / ONE_THOUSAND_D);
            }
        }
    }

    private void updateDistances() {
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!isAdded() || ((PageActivity)getActivity()).isSearching()) {
            return;
        }
        inflater.inflate(R.menu.menu_nearby, menu);
        menu.findItem(R.id.menu_metric_imperial).setTitle(showImperial
                ? getString(R.string.nearby_set_metric)
                : getString(R.string.nearby_set_imperial));
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (!isAdded() || ((PageActivity)getActivity()).isSearching()) {
            return;
        }
        menu.findItem(R.id.menu_metric_imperial).setTitle(showImperial
                ? getString(R.string.nearby_set_metric)
                : getString(R.string.nearby_set_imperial));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                return false;
            case R.id.menu_refresh_nearby:
                setRefreshingState(true);
                requestLocationUpdates();
                return true;
            case R.id.menu_metric_imperial:
                setImperialUnits(!showImperial);
                adapter.notifyDataSetInvalidated();
                return true;
            default:
                throw new RuntimeException("Unknown menu item clicked!");
        }
    }

    private void setImperialUnits(boolean imperial) {
        showImperial = imperial;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.edit().putBoolean(PREF_KEY_UNITS, showImperial).apply();
        getActivity().supportInvalidateOptionsMenu();
    }


    private View.OnClickListener markerClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            NearbyPage nearbyPage = (NearbyPage)v.getTag();
            PageTitle title = new PageTitle(nearbyPage.getTitle(), site, nearbyPage.getThumblUrl());
            String geoUri = String.format(Locale.ENGLISH,
                                          "geo:0,0?q=%s,%s(%s)",
                                          nearbyPage.getLocation().getLatitude(),
                                          nearbyPage.getLocation().getLongitude(),
                                          title.getDisplayText()
            );
            Intent geoIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(geoUri));
            try {
                startActivity(geoIntent);
            } catch (ActivityNotFoundException e) {
                // Means no map application was installed to handle geo://
                // I think this case is rare enough for us to just ignore
                // This would mean clicking the secondary action won't do anything,
                // which is fine, I think
            }
        }
    };

    private View.OnLongClickListener markerLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            int[] pos = new int[2];
            v.getLocationInWindow(pos);
            // display a toast that shows a tooltip based on the button's content description,
            // like the standard ActionBar does.
            Toast t = Toast.makeText(getActivity(), v.getContentDescription(), Toast.LENGTH_SHORT);
            t.setGravity(Gravity.TOP | Gravity.END, 0, pos[1]);
            t.show();
            return true;
        }
    };

    private class NearbyAdapter extends ArrayAdapter<NearbyPage> {
        private static final int LAYOUT_ID = R.layout.item_nearby_entry;

        public NearbyAdapter(Context context, ArrayList<NearbyPage> pages) {
            super(context, LAYOUT_ID, pages);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            NearbyPage nearbyPage = getItem(position);
            ViewHolder viewHolder;
            if (convertView == null) {
                viewHolder = new ViewHolder();
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(LAYOUT_ID, parent, false);
                viewHolder.thumbnail = (NearbyCompassView) convertView.findViewById(R.id.nearby_thumbnail);
                viewHolder.title = (TextView) convertView.findViewById(R.id.nearby_title);
                viewHolder.description = (TextView) convertView.findViewById(R.id.nearby_description);
                viewHolder.distance = (TextView) convertView.findViewById(R.id.nearby_distance);
                viewHolder.markerButton = convertView.findViewById(R.id.nearby_marker);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.title.setText(nearbyPage.getTitle());
            if (TextUtils.isEmpty(nearbyPage.getDescription())) {
                viewHolder.description.setVisibility(View.GONE);
            } else {
                viewHolder.description.setText(nearbyPage.getDescription());
                viewHolder.description.setVisibility(View.VISIBLE);
            }

            if (nearbyPage.getLocation() != null) {
                // set the calculated angle as the base angle for our compass view
                viewHolder.thumbnail.setAngle((float) calculateAngle(nearbyPage.getLocation()));
                viewHolder.thumbnail.setMaskColor(getResources().getColor(Utils.getThemedAttributeId(getActivity(), R.attr.page_background_color)));
                viewHolder.thumbnail.setTickColor(getResources().getColor(R.color.button_light));
                if (!compassViews.contains(viewHolder.thumbnail)) {
                    compassViews.add(viewHolder.thumbnail);
                }

                viewHolder.distance.setText(getDistanceLabel(nearbyPage.getLocation()));
                viewHolder.distance.setVisibility(View.VISIBLE);
                viewHolder.markerButton.setTag(nearbyPage);
                viewHolder.markerButton.setOnClickListener(markerClickListener);
                viewHolder.markerButton.setOnLongClickListener(markerLongClickListener);
                viewHolder.markerButton.setVisibility(View.VISIBLE);
                viewHolder.thumbnail.setEnabled(true);
            } else {
                // Strangely, we don't know the full coordinates of this nearby place.
                // Something in the DB must have gotten out of sync; may happen intermittently.
                viewHolder.distance.setVisibility(View.INVISIBLE); // don't affect the layout measurements
                viewHolder.thumbnail.setEnabled(false);
                viewHolder.markerButton.setVisibility(View.INVISIBLE);
            }

            if (app.showImages()) {
                Picasso.with(getActivity())
                       .load(nearbyPage.getThumblUrl())
                       .placeholder(R.drawable.ic_pageimage_placeholder)
                       .error(R.drawable.ic_pageimage_placeholder)
                       .into(viewHolder.thumbnail);
            } else {
                Picasso.with(getActivity())
                       .load(R.drawable.ic_pageimage_placeholder)
                       .into(viewHolder.thumbnail);
            }
            return convertView;
        }

        private double calculateAngle(Location otherLocation) {
            // simplified angle between two vectors...
            // vector pointing towards north from our location = [0, 1]
            // vector pointing towards destination from our location = [a1, a2]
            double a1 = otherLocation.getLongitude() - nextLocation.getLongitude();
            double a2 = otherLocation.getLatitude() - nextLocation.getLatitude();
            // cos θ = (v1*a1 + v2*a2) / (√(v1²+v2²) * √(a1²+a2²))
            double angle = Math.toDegrees(Math.acos(a2 / Math.sqrt(a1 * a1 + a2 * a2)));
            // since the acos function only goes between 0 to 180 degrees, we'll manually
            // negate the angle if the destination's longitude is on the opposite side.
            if (a1 < 0f) {
                angle = -angle;
            }
            return angle;
        }


        private class ViewHolder {
            private NearbyCompassView thumbnail;
            private TextView title;
            private TextView description;
            private TextView distance;
            private View markerButton;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!isAdded()) {
            return;
        }
        //acquire raw data from sensors...
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            if (accelData == null) {
                accelData = new MovingAverageArray(event.values.length, MOVING_AVERAGE_SIZE);
            }
            accelData.addData(event.values);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            if (magneticData == null) {
                magneticData = new MovingAverageArray(event.values.length, MOVING_AVERAGE_SIZE);
            }
            magneticData.addData(event.values);
        }
        if (accelData == null || magneticData == null) {
            return;
        }

        final int matrixSize = 9;
        final int orientationSize = 3;
        final int quarterTurn = 90;
        float[] mR = new float[matrixSize];
        //get the device's rotation matrix with respect to world coordinates, based on the sensor data
        if (!SensorManager.getRotationMatrix(mR, null, accelData.getData(), magneticData.getData())) {
            Log.e("NearbyActivity", "getRotationMatrix failed.");
            return;
        }

        //get device's orientation with respect to world coordinates, based on the
        //rotation matrix acquired above.
        float[] orientation = new float[orientationSize];
        SensorManager.getOrientation(mR, orientation);
        // orientation[0] = azimuth
        // orientation[1] = pitch
        // orientation[2] = roll
        float azimuth = (float) Math.toDegrees(orientation[0]);

        //adjust for declination from magnetic north...
        float declination = 0f;
        if (geomagneticField != null) {
            declination = geomagneticField.getDeclination();
        }
        azimuth += declination;

        //adjust for device screen rotation
        int rotation = ((WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_90:
                azimuth += quarterTurn;
                break;
            case Surface.ROTATION_180:
                azimuth += quarterTurn * 2;
                break;
            case Surface.ROTATION_270:
                azimuth -= quarterTurn;
                break;
            default:
                break;
        }

        //update views!
        for (NearbyCompassView view : compassViews) {
            view.setAzimuth(-azimuth);
        }
    }
}
