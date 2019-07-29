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
import android.os.Bundle;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.tabs.TabLayout;
import com.mainstreetcode.teammate.R;
import com.mainstreetcode.teammate.adapters.TournamentRoundAdapter;
import com.mainstreetcode.teammate.adapters.viewholders.EmptyViewHolder;
import com.mainstreetcode.teammate.adapters.viewholders.ModelCardViewHolder;
import com.mainstreetcode.teammate.adapters.viewholders.TeamViewHolder;
import com.mainstreetcode.teammate.adapters.viewholders.UserViewHolder;
import com.mainstreetcode.teammate.baseclasses.MainActivityFragment;
import com.mainstreetcode.teammate.model.Competitive;
import com.mainstreetcode.teammate.model.Competitor;
import com.mainstreetcode.teammate.model.Team;
import com.mainstreetcode.teammate.model.Tournament;
import com.mainstreetcode.teammate.model.User;
import com.mainstreetcode.teammate.util.ErrorHandler;
import com.mainstreetcode.teammate.util.ModelUtils;
import com.mainstreetcode.teammate.viewmodel.gofers.Gofer;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import static com.google.android.material.tabs.TabLayout.MODE_FIXED;
import static com.google.android.material.tabs.TabLayout.MODE_SCROLLABLE;

public class TournamentDetailFragment extends MainActivityFragment {

    private static final String ARG_TOURNAMENT = "tournament";
    private static final String ARG_COMPETITOR = "competitor";

    private Tournament tournament;
    private Competitor competitor;

    private ViewPager viewPager;
    private TabLayout tabLayout;
    private EmptyViewHolder viewHolder;
    private RecyclerView.RecycledViewPool gamesRecycledViewPool;

    public static TournamentDetailFragment newInstance(Tournament tournament) {
        TournamentDetailFragment fragment = new TournamentDetailFragment();
        Bundle args = new Bundle();

        args.putParcelable(ARG_TOURNAMENT, tournament);
        fragment.setArguments(args);
        fragment.setEnterExitTransitions();

        return fragment;
    }

    @SuppressWarnings("ConstantConditions")
    TournamentDetailFragment pending(Competitor competitor) {
        getArguments().putParcelable(ARG_COMPETITOR, competitor);
        return this;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public String getStableTag() {
        return Gofer.tag(super.getStableTag(), getArguments().getParcelable(ARG_TOURNAMENT));
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        tournament = args.getParcelable(ARG_TOURNAMENT);
        competitor = args.getParcelable(ARG_COMPETITOR);
        gamesRecycledViewPool = new RecyclerView.RecycledViewPool();

        if (competitor == null) competitor = Competitor.empty();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_games_parent, container, false);
        viewPager = root.findViewById(R.id.view_pager);
        tabLayout = root.findViewById(R.id.tab_layout);
        viewHolder = new EmptyViewHolder(root, R.drawable.ic_score_white_24dp, R.string.tournament_games_desc);

        viewPager.setAdapter(new TournamentRoundAdapter(tournament, getChildFragmentManager()));
        viewPager.setCurrentItem(tournament.getCurrentRound());

        setUpWinner((ViewGroup) root, tournament.getNumRounds());

        return root;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean hasPrivilegedRole = localRoleViewModel.hasPrivilegedRole();
        menu.findItem(R.id.action_edit).setVisible(hasPrivilegedRole);
        menu.findItem(R.id.action_delete).setVisible(hasPrivilegedRole);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit:
                showFragment(TournamentEditFragment.newInstance(tournament));
                break;
            case R.id.action_standings:
                showFragment(StatDetailFragment.newInstance(tournament));
                break;
            case R.id.action_delete:
                Context context = getContext();
                if (context == null) return true;
                new AlertDialog.Builder(context).setTitle(getString(R.string.delete_tournament_prompt))
                        .setPositiveButton(R.string.yes, (dialog, which) -> deleteTournament())
                        .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss())
                        .show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        checkCompetitor();
        watchForRoleChanges(tournament.getHost(), this::togglePersistentUi);

        int rounds = tournament.getNumRounds();
        disposables.add(tournamentViewModel.checkForWinner(tournament).subscribe(changed -> setUpWinner((ViewGroup) getView(), rounds), defaultErrorHandler));
    }

    @Override
    public void onDestroyView() {
        viewPager = null;
        tabLayout = null;
        viewHolder = null;
        super.onDestroyView();
    }

    RecyclerView.RecycledViewPool getGamesRecycledViewPool() {
        return gamesRecycledViewPool;
    }

    @Override
    @StringRes
    protected int getFabStringResource() { return R.string.add_tournament_competitors; }

    @Override
    @DrawableRes
    protected int getFabIconResource() { return R.drawable.ic_group_add_white_24dp; }

    @Override
    protected int getToolbarMenu() { return R.menu.fragment_tournament_detail; }

    @Override
    public boolean showsFab() {
        return localRoleViewModel.hasPrivilegedRole() && !tournament.hasCompetitors();
    }

    @Override
    protected CharSequence getToolbarTitle() {
        return getString(R.string.tournament_fixtures);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.fab) showFragment(CompetitorsFragment.newInstance(tournament));
    }

    private void checkCompetitor() {
        if (competitor.isEmpty() || competitor.isAccepted()) return;
        if (restoredFromBackStack()) // Don't prompt for the same competitor multiple times.
            disposables.add(competitorViewModel.updateCompetitor(competitor).subscribe(this::promptForCompetitor, ErrorHandler.EMPTY));
        else promptForCompetitor();
    }

    private void promptForCompetitor() {
        if (competitor.isEmpty() || competitor.isAccepted()) return;
        showChoices(choiceBar -> choiceBar.setText(getString(R.string.tournament_accept))
                .setPositiveText(getText(R.string.accept))
                .setNegativeText(getText(R.string.decline))
                .setPositiveClickListener(v -> respond(true))
                .setNegativeClickListener(v -> respond(false)));
    }

    private void respond(boolean accept) {
        toggleProgress(true);
        disposables.add(competitorViewModel.respond(competitor, accept)
                .subscribe(ignored -> toggleProgress(false), defaultErrorHandler));
    }

    private void deleteTournament() {
        disposables.add(tournamentViewModel.delete(tournament).subscribe(this::onTournamentDeleted, defaultErrorHandler));
    }

    private void onTournamentDeleted(Tournament deleted) {
        showSnackbar(getString(R.string.deleted_team, deleted.getName()));
        removeEnterExitTransitions();
        requireActivity().onBackPressed();
    }

    @SuppressWarnings("unchecked")
    private void setUpWinner(@Nullable ViewGroup root, int prevAdapterCount) {
        if (root == null) return;

        TextView winnerText = root.findViewById(R.id.winner);
        ViewGroup winnerView = root.findViewById(R.id.item_container);
        PagerAdapter adapter = root.<ViewPager>findViewById(R.id.view_pager).getAdapter();

        if (prevAdapterCount != tournament.getNumRounds() && adapter != null)
            adapter.notifyDataSetChanged();

        TransitionManager.beginDelayedTransition(root, new AutoTransition()
                .addTarget(tabLayout)
                .addTarget(viewPager)
                .addTarget(winnerView)
                .addTarget(winnerText));

        boolean hasCompetitors = tournament.getNumCompetitors() > 0;
        tabLayout.setTabMode(tournament.getNumRounds() > 4 ? MODE_SCROLLABLE : MODE_FIXED);
        tabLayout.setVisibility(hasCompetitors ? View.VISIBLE : View.GONE);
        tabLayout.setupWithViewPager(viewPager);
        viewHolder.setColor(R.attr.alt_empty_view_holder_tint);
        viewHolder.toggle(!hasCompetitors);

        Competitor winner = tournament.getWinner();
        if (winner.isEmpty()) return;

        Competitive competitive = winner.getEntity();

        ModelCardViewHolder viewHolder = competitive instanceof User
                ? new UserViewHolder(winnerView, user -> {})
                : competitive instanceof Team
                ? new TeamViewHolder(winnerView, team -> {})
                : null;

        if (viewHolder == null) return;
        viewHolder.bind(competitive);

        winnerText.setVisibility(View.VISIBLE);
        winnerView.setVisibility(View.VISIBLE);

        winnerText.setText(ModelUtils.processString(getString(R.string.tournament_winner)));
    }
}
