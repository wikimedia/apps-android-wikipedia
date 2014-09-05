package org.wikipedia.nearby;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
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
import org.wikipedia.WikipediaApp;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageActivity;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Displays a list of nearby pages.
 */
public class NearbyActivity extends ThemedActionBarActivity {
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestLocationUpdates();
    }

    @Override
    public void onPause() {
        stopLocationUpdates();
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
                viewHolder.thumbnail = (ImageView) convertView.findViewById(R.id.nearby_thumbnail);
                viewHolder.title = (TextView) convertView.findViewById(R.id.nearby_title);
                viewHolder.distance = (TextView) convertView.findViewById(R.id.nearby_distance);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.title.setText(nearbyPage.getTitle());
            viewHolder.distance.setText(getDistanceLabel(nearbyPage.getLocation()));
            Picasso.with(NearbyActivity.this)
                    .load(nearbyPage.getThumblUrl())
                    .placeholder(R.drawable.ic_pageimage_placeholder)
                    .error(R.drawable.ic_pageimage_placeholder)
                    .into(viewHolder.thumbnail);
            return convertView;
        }


        private class ViewHolder {
            private ImageView thumbnail;
            private TextView title;
            private TextView distance;
        }
    }
}
