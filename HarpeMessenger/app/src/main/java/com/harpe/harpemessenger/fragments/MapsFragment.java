package com.harpe.harpemessenger.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.harpe.harpemessenger.R;
import com.harpe.harpemessenger.models.HEPicture;

import java.util.ArrayList;

public class MapsFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private View rootView;
    private MapView mapView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if (rootView == null) {
            rootView = inflater.inflate(R.layout.fragment_maps, container, false);
        }
        mapView = (MapView) rootView.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        mapView.onResume(); // needed to get the map to display immediately

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mapView.getMapAsync(this);
        return rootView;
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap map) {
        this.googleMap = map;
        updateMakers();
    }

    public void updateMakers() {
        ArrayList<HEPicture> pictures = new ArrayList<>(HEPicture.getPictures().values());
        LatLng coordinates = null;
        for (int i = 0; i < pictures.size(); i++) {
            HEPicture currentPicture = pictures.get(i);
            coordinates = new LatLng(currentPicture.getLatitude(), currentPicture.getLongitude());
            googleMap.addMarker(new MarkerOptions().position(coordinates).title(currentPicture.getLastPathSegment()));
        }
        // Will move the camera to the last coordinates
        if (coordinates!=null) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(coordinates));
        }
    }
}
