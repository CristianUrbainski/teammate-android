/*
 * MIT License
 *
 * Copyright (c) 2019 Adetunji Dahunsi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.mainstreetcode.teammate.fragments.main;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.mainstreetcode.teammate.R;
import com.mainstreetcode.teammate.activities.MainActivity;
import com.mainstreetcode.teammate.adapters.EventSearchRequestAdapter;
import com.mainstreetcode.teammate.baseclasses.BaseViewHolder;
import com.mainstreetcode.teammate.baseclasses.MainActivityFragment;
import com.mainstreetcode.teammate.model.Event;
import com.mainstreetcode.teammate.util.ExpandingToolbar;
import com.mainstreetcode.teammate.util.Logger;
import com.mainstreetcode.teammate.util.ScrollManager;
import com.tunjid.androidbootstrap.core.abstractclasses.BaseFragment;
import com.tunjid.androidbootstrap.view.util.InsetFlags;

import java.util.Arrays;
import java.util.List;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentTransaction;

import static android.app.Activity.RESULT_OK;
import static com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom;
import static com.mainstreetcode.teammate.util.ViewHolderUtil.getLayoutParams;
import static com.mainstreetcode.teammate.viewmodel.LocationViewModel.PERMISSIONS_REQUEST_LOCATION;

public class EventSearchFragment extends MainActivityFragment {

    private static final int MAP_ZOOM = 10;

    private boolean leaveMap;

    private MapView mapView;
    private ExpandingToolbar expandingToolbar;

    public static EventSearchFragment newInstance() {
        EventSearchFragment fragment = new EventSearchFragment();
        Bundle args = new Bundle();

        fragment.setArguments(args);
        fragment.setEnterExitTransitions();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_public_event, container, false);
        getLayoutParams(root.findViewById(R.id.status_bar_dimmer)).height = MainActivity.topInset;

        scrollManager = ScrollManager.<BaseViewHolder>with(root.findViewById(R.id.search_options))
                .withAdapter(new EventSearchRequestAdapter(eventViewModel.getEventRequest(), this::startPlacePicker))
                .withInconsistencyHandler(this::onInconsistencyDetected)
                .withRecycledViewPool(inputRecycledViewPool())
                .withLinearLayoutManager()
                .build();

        mapView = root.findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this::onMapReady);

        expandingToolbar = ExpandingToolbar.create(root.findViewById(R.id.card_view_wrapper), () -> mapView.getMapAsync(this::fetchPublicEvents));
        expandingToolbar.setTitle(R.string.event_public_search);
        expandingToolbar.setTitleIcon(false);

        return root;
    }

    @Override
    public void onResume() {
        mapView.onResume();
        super.onResume();
    }

    @Override
    public void onStart() {
        mapView.onStart();
        super.onStart();
        requestLocation();
    }

    @Override
    public void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        try { mapView.onSaveInstanceState(outState);}
        catch (Exception e) {Logger.log(getStableTag(), "Error in mapview onSaveInstanceState", e);}
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStop() {
        mapView.onStop();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        mapView.onDestroy();
        mapView = null;
        expandingToolbar = null;
        super.onDestroyView();
    }

    @Override
    public void onLowMemory() {
        mapView.onLowMemory();
        super.onLowMemory();
    }

    @Override
    public boolean showsToolBar() { return false; }

    @Override
    public boolean showsBottomNav() { return false; }

    @Override
    public boolean showsFab() { return locationViewModel.hasPermission(this); }

    @Override
    public InsetFlags insetFlags() {return NO_TOP;}

    @Override
    @StringRes
    protected int getFabStringResource() { return R.string.event_my_location; }

    @Override
    @DrawableRes
    protected int getFabIconResource() { return R.drawable.ic_crosshairs_gps_white_24dp; }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.fab) disposables.add(locationViewModel.getLastLocation(this)
                .subscribe(location -> onLocationFound(location, true), defaultErrorHandler));
    }

    @Nullable
    @Override
    public FragmentTransaction provideFragmentTransaction(BaseFragment fragmentTo) {
        return super.provideFragmentTransaction(fragmentTo);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != PLACE_PICKER_REQUEST) return;
        if (resultCode != RESULT_OK) return;

        try {onLocationFound(Autocomplete.getPlaceFromIntent(data).getLatLng(), true);}
        catch (Exception e) {Logger.log(getStableTag(), "Unable to retrieve place", e);}
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        boolean permissionGranted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (permissionGranted && requestCode == PERMISSIONS_REQUEST_LOCATION) requestLocation();
    }

    private void fetchPublicEvents(GoogleMap map) {
        disposables.add(eventViewModel.getPublicEvents(map).subscribe(events -> populateMap(map, events), defaultErrorHandler));
    }

    private void requestLocation() {
        LatLng lastLocation = eventViewModel.getLastPublicSearchLocation();

        if (lastLocation != null) onLocationFound(lastLocation, false);
        else disposables.add(locationViewModel.getLastLocation(this)
                .subscribe(location -> onLocationFound(location, true), defaultErrorHandler));
    }

    private void onLocationFound(LatLng location, boolean animate) {
        mapView.getMapAsync(map -> {
            if (animate) map.animateCamera(newLatLngZoom(location, MAP_ZOOM));
            else map.moveCamera(newLatLngZoom(location, MAP_ZOOM));
        });
    }

    private void startPlacePicker() {
        Autocomplete.IntentBuilder builder = new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN,
                Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));

        try {startActivityForResult(builder.build(requireActivity()), PLACE_PICKER_REQUEST);}
        catch (Exception e) {Logger.log(getStableTag(), "Unable to start places api", e);}
    }

    private void onMapReady(GoogleMap map) {
        map.setOnCameraIdleListener(() -> onMapIdle(map));
        map.setOnCameraMoveStartedListener(this::onCameraMoveStarted);
        map.setOnInfoWindowClickListener(this::onMarkerInfoWindowClicked);
    }

    private void populateMap(GoogleMap map, List<Event> events) {
        if (leaveMap) return;
        map.clear();
        for (Event event : events) map.addMarker(event.getMarkerOptions()).setTag(event);
    }

    private void onMapIdle(GoogleMap map) {
        fetchPublicEvents(map);
        disposables.add(locationViewModel.fromMap(map).subscribe(this::onAddressFound, defaultErrorHandler));
        scrollManager.notifyDataSetChanged();
    }

    private void onCameraMoveStarted(int reason) {
        if (scrollManager.getRecyclerView().getVisibility() == View.VISIBLE)
            expandingToolbar.changeVisibility(true);

        switch (reason) {
            case GoogleMap.OnCameraMoveStartedListener.REASON_API_ANIMATION:
                leaveMap = true;
                break;
            case GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE:
            case GoogleMap.OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION:
                leaveMap = false;
                break;
        }
    }

    private void onMarkerInfoWindowClicked(Marker marker) {
        Object tag = marker.getTag();
        if (!(tag instanceof Event)) return;
        showFragment(EventEditFragment.newInstance((Event) tag));
    }

    private void onAddressFound(Address address) {
        eventViewModel.getEventRequest().setAddress(address);
        scrollManager.notifyItemChanged(0);
    }
}
