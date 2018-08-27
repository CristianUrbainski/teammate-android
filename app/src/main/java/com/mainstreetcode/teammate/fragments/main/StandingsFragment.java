package com.mainstreetcode.teammate.fragments.main;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mainstreetcode.teammate.R;
import com.mainstreetcode.teammate.adapters.StandingsAdapter;
import com.mainstreetcode.teammate.adapters.viewholders.EmptyViewHolder;
import com.mainstreetcode.teammate.adapters.viewholders.StandingRowViewHolder;
import com.mainstreetcode.teammate.baseclasses.MainActivityFragment;
import com.mainstreetcode.teammate.model.Event;
import com.mainstreetcode.teammate.model.Standings;
import com.mainstreetcode.teammate.model.Tournament;
import com.mainstreetcode.teammate.util.ScrollManager;
import com.tunjid.androidbootstrap.core.abstractclasses.BaseRecyclerViewAdapter;

/**
 * Lists {@link Event tournaments}
 */

public final class StandingsFragment extends MainActivityFragment {

    private static final String ARG_TOURNAMENT = "team";

    private Tournament tournament;
    private Standings standings;
    private StandingRowViewHolder viewHolder;

    public static StandingsFragment newInstance(Tournament team) {
        StandingsFragment fragment = new StandingsFragment();
        Bundle args = new Bundle();

        args.putParcelable(ARG_TOURNAMENT, team);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public String getStableTag() {
        String superResult = super.getStableTag();
        Tournament tempTournament = getArguments().getParcelable(ARG_TOURNAMENT);

        return (tempTournament != null)
                ? superResult + "-" + tempTournament.hashCode()
                : superResult;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        tournament = getArguments().getParcelable(ARG_TOURNAMENT);
        standings = tournamentViewModel.getStandings(tournament);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_standings, container, false);

        Runnable refreshAction = () -> fetchStandings(true);

        scrollManager = ScrollManager.withRecyclerView(rootView.findViewById(R.id.team_list))
                .withEmptyViewholder(new EmptyViewHolder(rootView, R.drawable.ic_table_24dp, R.string.tournament_no_standings))
                .withRefreshLayout(rootView.findViewById(R.id.refresh_layout), refreshAction)
                .withEndlessScrollCallback(() -> fetchStandings(false))
                .withInconsistencyHandler(this::onInconsistencyDetected)
                .withAdapter(new StandingsAdapter(standings.getTable(), new BaseRecyclerViewAdapter.AdapterListener() {}))
                .withLinearLayoutManager()
                .build();

        viewHolder = new StandingRowViewHolder(rootView.findViewById(R.id.item_container), new BaseRecyclerViewAdapter.AdapterListener() {});
        viewHolder.thumbnail.setVisibility(View.GONE);
        viewHolder.position.setVisibility(View.GONE);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchStandings(false);
    }

    @Override
    public void togglePersistentUi() {/* Do nothing */}

    @Override
    public boolean showsFab() { return false; }

    void fetchStandings(boolean isRefreshing) {
        if (isRefreshing) scrollManager.setRefreshing();
        else toggleProgress(true);

        disposables.add(tournamentViewModel.fetchStandings(tournament).subscribe(this::onTournamentsUpdated, defaultErrorHandler));
    }

    private void onTournamentsUpdated() {
        scrollManager.notifyDataSetChanged();
        viewHolder.bindColumns(standings.getColumnNames());
        toggleProgress(false);
    }
}
