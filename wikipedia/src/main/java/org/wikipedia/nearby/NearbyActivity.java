package org.wikipedia.nearby;

import android.content.Context;
import android.content.Intent;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.squareup.picasso.Picasso;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import org.mediawiki.api.json.ApiException;
import org.wikipedia.PageTitle;
import org.wikipedia.R;
import org.wikipedia.Site;
import org.wikipedia.ThemedActionBarActivity;
import org.wikipedia.Utils;
import org.wikipedia.WikipediaApp;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageActivity;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Displays a list of nearby pages.
 */
public class NearbyActivity extends ThemedActionBarActivity implements SensorEventListener {
    public static final int ACTIVITY_RESULT_NEARBY_SELECT = 1;
    private static final int MIN_TIME_MILLIS = 2000;
    private static final int MIN_DISTANCE_METERS = 2;
    private static final int ONE_KM = 1000;
    private static final double ONE_KM_D = 1000.0d;

    private ListView nearbyList;
    private View nearbyLoadingContainer;
    private View nearbyEmptyContainer;
    private NearbyAdapter adapter;

    private WikipediaApp app;
    private Site site;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private boolean refreshing;
    private Location lastLocation;
    private Location nextLocation;

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

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (WikipediaApp) getApplicationContext();
        site = app.getPrimarySite();

        setContentView(R.layout.activity_nearby);
        nearbyList = (ListView) findViewById(R.id.nearby_list);
        nearbyLoadingContainer = findViewById(R.id.nearby_loading_container);
        nearbyEmptyContainer = findViewById(R.id.nearby_empty_container);

        nearbyEmptyContainer.setVisibility(View.GONE);

        adapter = new NearbyAdapter(this, new ArrayList<NearbyPage>());
        nearbyList.setAdapter(adapter);
        nearbyList.setEmptyView(nearbyLoadingContainer);

        nearbyList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                NearbyPage nearbyPage = adapter.getItem(position);
                PageTitle title = new PageTitle(nearbyPage.getTitle(), site, nearbyPage.getThumblUrl());
                HistoryEntry newEntry = new HistoryEntry(title, HistoryEntry.SOURCE_NEARBY);

                Intent intent = new Intent();
                intent.setClass(NearbyActivity.this, PageActivity.class);
                intent.setAction(PageActivity.ACTION_PAGE_FOR_TITLE);
                intent.putExtra(PageActivity.EXTRA_PAGETITLE, title);
                intent.putExtra(PageActivity.EXTRA_HISTORYENTRY, newEntry);
                setResult(ACTIVITY_RESULT_NEARBY_SELECT, intent);
                finish();
            }
        });

        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
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

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        compassViews = new ArrayList<NearbyCompassView>();

    }

    @Override
    protected void onResume() {
        super.onResume();
        requestLocationUpdates();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onPause() {
        stopLocationUpdates();
        mSensorManager.unregisterListener(this);
        compassViews.clear();
        super.onPause();
    }

    private void stopLocationUpdates() {
        setRefreshingState(false);
        locationManager.removeUpdates(locationListener);
    }

    private void requestLocationUpdates() {
        setRefreshingState(true);
        // Register the listener with the Location Manager to receive location updates
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_MILLIS, MIN_DISTANCE_METERS, locationListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_MILLIS, MIN_DISTANCE_METERS, locationListener);
    }

    private void makeUseOfNewLocation(Location location) {
        nextLocation = location;
        if (lastLocation == null || (refreshing && getDistance(lastLocation) >= MIN_DISTANCE_METERS)) {

            //update geomagnetic field data, to give us our precise declination from true north.
            geomagneticField = new GeomagneticField((float)nextLocation.getLatitude(), (float)nextLocation.getLongitude(), 0, (new Date()).getTime());

            new NearbyFetchTask(NearbyActivity.this, site, location) {
                @Override
                public void onFinish(List<NearbyPage> result) {
                    showNearbyPages(result);
                }

                @Override
                public void onCatch(Throwable caught) {
                    if (caught instanceof ApiException && caught.getCause() instanceof UnknownHostException) {
                        Crouton.makeText(NearbyActivity.this, R.string.nearby_no_network, Style.ALERT).show();
                    } else if (caught instanceof NearbyFetchException) {
                        Log.e("Wikipedia", "Could not get list of nearby places: " + caught.toString());
                        Crouton.makeText(NearbyActivity.this, R.string.nearby_server_error, Style.ALERT).show();
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

    private void showNearbyPages(List<NearbyPage> result) {
        nearbyList.setEmptyView(nearbyEmptyContainer);
        lastLocation = nextLocation;
        sortByDistance(result);
        adapter.clear();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            adapter.addAll(result);
        } else {
            for (NearbyPage page : result) {
                adapter.add(page);
            }
        }
        compassViews.clear();

        setRefreshingState(false);
    }

    private void setRefreshingState(boolean newState) {
        refreshing = newState;
        if (refreshing) {
            nearbyLoadingContainer.setVisibility(View.VISIBLE);
        } else {
            nearbyLoadingContainer.setVisibility(View.GONE);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            invalidateOptionsMenu();
        }
    }

    private void sortByDistance(List<NearbyPage> nearbyPages) {
        Collections.sort(nearbyPages, new Comparator<NearbyPage>() {
            public int compare(NearbyPage a, NearbyPage b) {
                return getDistance(a.getLocation()) - getDistance(b.getLocation());
            }
        });
    }

    private int getDistance(Location otherLocation) {
        return (int) nextLocation.distanceTo(otherLocation);
    }

    private String getDistanceLabel(Location otherLocation) {
        final int distance = getDistance(otherLocation);
        if (distance < ONE_KM) {
            return getString(R.string.nearby_distance_in_meters, distance);
        } else {
            return getString(R.string.nearby_distance_in_kilometers, distance / ONE_KM_D);
        }
    }

    private void updateDistances() {
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_nearby, menu);
        app.adjustDrawableToTheme(menu.findItem(R.id.menu_refresh_nearby).getIcon());
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // invalidateOptionsMenu is only available since API 11
            menu.findItem(R.id.menu_refresh_nearby).setEnabled(!refreshing);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_refresh_nearby:
                requestLocationUpdates();
                return true;
            default:
                throw new RuntimeException("Unknown menu item clicked!");
        }
    }


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
                viewHolder.distance = (TextView) convertView.findViewById(R.id.nearby_distance);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.title.setText(nearbyPage.getTitle());
            viewHolder.distance.setText(getDistanceLabel(nearbyPage.getLocation()));

            // simplified angle between two vectors...
            // vector pointing towards north from our location = [0, 1]
            // vector pointing towards destination from our location = [a1, a2]
            double a1 = nearbyPage.getLocation().getLongitude() - nextLocation.getLongitude();
            double a2 = nearbyPage.getLocation().getLatitude() - nextLocation.getLatitude();
            // cos θ = (v1*a1 + v2*a2) / (√(v1²+v2²) * √(a1²+a2²))
            double angle = Math.toDegrees(Math.acos(a2 / Math.sqrt(a1 * a1 + a2 * a2)));
            // since the acos function only goes between 0 to 180 degrees, we'll manually
            // negate the angle if the destination's longitude is on the opposite side.
            if (a1 < 0f) {
                angle = -angle;
            }
            // set the calculated angle as the base angle for our compass view
            viewHolder.thumbnail.setAngle((float) angle);
            viewHolder.thumbnail.setMaskColor(getResources().getColor(Utils.getThemedAttributeId(NearbyActivity.this, R.attr.window_background_color)));
            viewHolder.thumbnail.setTickColor(getResources().getColor(R.color.blue_progressive));
            if (!compassViews.contains(viewHolder.thumbnail)) {
                compassViews.add(viewHolder.thumbnail);
            }

            Picasso.with(NearbyActivity.this)
                    .load(nearbyPage.getThumblUrl())
                    .placeholder(R.drawable.ic_pageimage_placeholder)
                    .error(R.drawable.ic_pageimage_placeholder)
                    .into(viewHolder.thumbnail);
            return convertView;
        }


        private class ViewHolder {
            private NearbyCompassView thumbnail;
            private TextView title;
            private TextView distance;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { }

    @Override
    public void onSensorChanged(SensorEvent event) {
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
        int rotation = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
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
