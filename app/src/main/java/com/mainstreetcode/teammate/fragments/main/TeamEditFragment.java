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

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.os.Bundle;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.DiffUtil;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.mainstreetcode.teammate.R;
import com.mainstreetcode.teammate.adapters.TeamEditAdapter;
import com.mainstreetcode.teammate.adapters.viewholders.input.InputViewHolder;
import com.mainstreetcode.teammate.baseclasses.HeaderedFragment;
import com.mainstreetcode.teammate.model.Team;
import com.mainstreetcode.teammate.util.Logger;
import com.mainstreetcode.teammate.util.ScrollManager;
import com.mainstreetcode.teammate.viewmodel.gofers.Gofer;
import com.mainstreetcode.teammate.viewmodel.gofers.TeamGofer;
import com.mainstreetcode.teammate.viewmodel.gofers.TeamHostingGofer;
import com.tunjid.androidbootstrap.view.util.InsetFlags;

import java.util.Arrays;

import static android.app.Activity.RESULT_OK;

/**
 * Creates, edits or lets a {@link com.mainstreetcode.teammate.model.User} join a {@link Team}
 */

public class TeamEditFragment extends HeaderedFragment<Team>
        implements
        TeamEditAdapter.TeamEditAdapterListener {

    private static final String ARG_TEAM = "team";

    private Team team;
    private TeamGofer gofer;

    static TeamEditFragment newCreateInstance() {return newInstance(Team.empty());}

    static TeamEditFragment newEditInstance(Team team) {return newInstance(team);}

    private static TeamEditFragment newInstance(Team team) {
        TeamEditFragment fragment = new TeamEditFragment();
        Bundle args = new Bundle();

        args.putParcelable(ARG_TEAM, team);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public String getStableTag() {
        return Gofer.tag(super.getStableTag(), getArguments().getParcelable(ARG_TEAM));
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        team = getArguments().getParcelable(ARG_TEAM);
        gofer = teamViewModel.gofer(team);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_headered, container, false);

        scrollManager = ScrollManager.<InputViewHolder>with(rootView.findViewById(R.id.model_list))
                .withRefreshLayout(rootView.findViewById(R.id.refresh_layout), this::refresh)
                .withAdapter(new TeamEditAdapter(gofer.getItems(), this))
                .addScrollListener((dx, dy) -> updateFabForScrollState(dy))
                .withInconsistencyHandler(this::onInconsistencyDetected)
                .withRecycledViewPool(inputRecycledViewPool())
                .withLinearLayoutManager()
                .build();

        scrollManager.getRecyclerView().requestFocus();
        return rootView;
    }

    @Override
    @StringRes
    protected int getFabStringResource() { return team.isEmpty() ? R.string.team_create : R.string.team_update; }

    @Override
    @DrawableRes
    protected int getFabIconResource() { return R.drawable.ic_check_white_24dp; }

    @Override
    protected CharSequence getToolbarTitle() {
        return gofer.getToolbarTitle(this);
    }

    @Override
    public InsetFlags insetFlags() {return NO_TOP;}

    @Override
    public boolean showsFab() {
        return gofer.canEditTeam();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() != R.id.fab) return;

        boolean wasEmpty = team.isEmpty();
        toggleProgress(true);
        disposables.add(gofer.save()
                .subscribe(result -> {
                    String message = wasEmpty ? getString(R.string.created_team, team.getName()) : getString(R.string.updated_team);
                    onModelUpdated(result);
                    showSnackbar(message);
                }, defaultErrorHandler));
    }

    @Override
    protected Team getHeaderedModel() {return team;}

    @Override
    protected TeamHostingGofer<Team> gofer() { return gofer; }

    @Override
    protected void onModelUpdated(DiffUtil.DiffResult result) {
        viewHolder.bind(getHeaderedModel());
        scrollManager.onDiff(result);
        toggleProgress(false);
    }

    protected void onPrepComplete() {
        scrollManager.notifyDataSetChanged();
        togglePersistentUi();
        super.onPrepComplete();
    }

    @Override
    protected boolean cantGetModel() {
        return super.cantGetModel() || gofer.isSettingAddress();
    }

    @Override
    public void onAddressClicked() {
        gofer.setSettingAddress(true);

        Autocomplete.IntentBuilder builder = new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN,
                Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));

        try {startActivityForResult(builder.build(requireActivity()), PLACE_PICKER_REQUEST);}
        catch (Exception e) {Logger.log(getStableTag(), "Unable to start places api", e);}
    }

    @Override
    public boolean canEditFields() {
        return gofer.canEditTeam();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        boolean failed = resultCode != RESULT_OK;
        boolean isFromPlacePicker = requestCode == PLACE_PICKER_REQUEST;

        if (failed && isFromPlacePicker) gofer.setSettingAddress(false);
        if (failed || !isFromPlacePicker) return;

        Context context = getContext();
        if (context == null) return;

        toggleProgress(true);
        Place place = Autocomplete.getPlaceFromIntent(data);
        disposables.add(locationViewModel.fromPlace(place)
                .subscribe(this::onAddressFound, defaultErrorHandler));
    }

    private void onAddressFound(Address address) {
        disposables.add(gofer.setAddress(address).subscribe(this::onModelUpdated, emptyErrorHandler));
        toggleProgress(false);
    }
}
