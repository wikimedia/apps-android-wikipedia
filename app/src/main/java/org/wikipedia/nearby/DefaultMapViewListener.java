package org.wikipedia.nearby;

import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.views.MapView;
import com.mapbox.mapboxsdk.views.MapViewListener;

class DefaultMapViewListener implements MapViewListener {
    @Override
    public void onShowMarker(MapView mapView, Marker marker) {
    }

    @Override
    public void onHideMarker(MapView mapView, Marker marker) {
    }

    @Override
    public void onTapMarker(MapView mapView, Marker marker) {
    }

    @Override
    public void onLongPressMarker(MapView mapView, Marker marker) {
    }

    @Override
    public void onTapMap(MapView mapView, ILatLng iLatLng) {
    }

    @Override
    public void onLongPressMap(MapView mapView, ILatLng iLatLng) {
    }
}
