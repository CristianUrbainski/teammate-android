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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.mainstreetcode.teammate.R;
import com.mainstreetcode.teammate.adapters.TournamentAdapter;
import com.mainstreetcode.teammate.adapters.viewholders.EmptyViewHolder;
import com.mainstreetcode.teammate.adapters.viewholders.TournamentViewHolder;
import com.mainstreetcode.teammate.baseclasses.MainActivityFragment;
import com.mainstreetcode.teammate.fragments.headless.TeamPickerFragment;
import com.mainstreetcode.teammate.model.Event;
import com.mainstreetcode.teammate.model.ListState;
import com.mainstreetcode.teammate.model.Team;
import com.mainstreetcode.teammate.model.Tournament;
import com.mainstreetcode.teammate.util.ScrollManager;
import com.tunjid.androidbootstrap.core.abstractclasses.BaseFragment;
import com.tunjid.androidbootstrap.recyclerview.InteractiveViewHolder;
import com.tunjid.androidbootstrap.recyclerview.diff.Differentiable;

import java.util.List;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DiffUtil;

import static com.mainstreetcode.teammate.util.ViewHolderUtil.getTransitionName;

/**
 * Lists {@link Event tournaments}
 */

public final class TournamentsFragment extends MainActivityFragment
        implements
        TournamentAdapter.TournamentAdapterListener {

    private static final String ARG_TEAM = "team";

    private Team team;
    private List<Differentiable> items;

    public static TournamentsFragment newInstance(Team team) {
        TournamentsFragment fragment = new TournamentsFragment();
        Bundle args = new Bundle();

        args.putParcelable(ARG_TEAM, team);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public String getStableTag() {
        String superResult = super.getStableTag();
        Team tempTeam = getArguments().getParcelable(ARG_TEAM);

        return (tempTeam != null)
                ? superResult + "-" + tempTeam.hashCode()
                : superResult;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        team = getArguments().getParcelable(ARG_TEAM);
        items = tournamentViewModel.getModelList(team);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_list_with_refresh, container, false);

        Runnable refreshAction = () -> disposables.add(tournamentViewModel.refresh(team).subscribe(TournamentsFragment.this::onTournamentsUpdated, defaultErrorHandler));

        scrollManager = ScrollManager.<InteractiveViewHolder>with(rootView.findViewById(R.id.list_layout))
                .withPlaceholder(new EmptyViewHolder(rootView, R.drawable.ic_trophy_white_24dp, R.string.no_tournaments))
                .withRefreshLayout(rootView.findViewById(R.id.refresh_layout), refreshAction)
                .withEndlessScroll(() -> fetchTournaments(false))
                .addScrollListener((dx, dy) -> updateFabForScrollState(dy))
                .addScrollListener((dx, dy) -> updateTopSpacerElevation())
                .withInconsistencyHandler(this::onInconsistencyDetected)
                .withAdapter(new TournamentAdapter(items, this))
                .withLinearLayoutManager()
                .build();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchTournaments(true);
        watchForRoleChanges(team, this::togglePersistentUi);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_pick_team:
                TeamPickerFragment.change(getActivity(), R.id.request_tournament_team_pick);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected int getToolbarMenu() { return R.menu.fragment_tournaments; }

    @Override
    @StringRes
    protected int getFabStringResource() { return R.string.tournament_add; }

    @Override
    @DrawableRes
    protected int getFabIconResource() { return R.drawable.ic_add_white_24dp; }

    @Override
    protected CharSequence getToolbarTitle() {
        return getString(R.string.tournaments);
    }

    @Override
    public boolean showsFab() {
        return team.getSport().supportsCompetitions() && localRoleViewModel.hasPrivilegedRole();
    }

    @Override
    public void onTournamentClicked(Tournament tournament) {
        showFragment(TournamentDetailFragment.newInstance(tournament));
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fab:
                Tournament tournament = Tournament.empty(team);
                showFragment(TournamentEditFragment.newInstance(tournament));
                break;
        }
    }

    @Nullable
    @Override
    public FragmentTransaction provideFragmentTransaction(BaseFragment fragmentTo) {
        FragmentTransaction superResult = super.provideFragmentTransaction(fragmentTo);

        if (fragmentTo.getStableTag().contains(TournamentEditFragment.class.getSimpleName())) {
            Bundle args = fragmentTo.getArguments();
            if (args == null) return superResult;

            Tournament tournament = args.getParcelable(TournamentEditFragment.ARG_TOURNAMENT);
            if (tournament == null) return superResult;

            TournamentViewHolder viewHolder = (TournamentViewHolder) scrollManager.findViewHolderForItemId(tournament.hashCode());
            if (viewHolder == null) return superResult;

            return beginTransaction()
                    .addSharedElement(viewHolder.itemView, getTransitionName(tournament, R.id.fragment_header_background))
                    .addSharedElement(viewHolder.getImage(), getTransitionName(tournament, R.id.fragment_header_thumbnail));

        }
        return superResult;
    }

    private void fetchTournaments(boolean fetchLatest) {
        if (fetchLatest) scrollManager.setRefreshing();
        else toggleProgress(true);

        disposables.add(tournamentViewModel.getMany(team, fetchLatest).subscribe(this::onTournamentsUpdated, defaultErrorHandler));
    }

    private void onTournamentsUpdated(DiffUtil.DiffResult result) {
        toggleProgress(false);
        boolean supportsTournaments = team.getSport().supportsCompetitions();
        scrollManager.onDiff(result);
        scrollManager.updateForEmptyList(ListState.of(R.drawable.ic_trophy_white_24dp, supportsTournaments
                ? R.string.no_tournaments : R.string.no_tournament_support));
    }
}
