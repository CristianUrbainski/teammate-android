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

import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.mainstreetcode.teammate.R;
import com.mainstreetcode.teammate.adapters.StatAdapter;
import com.mainstreetcode.teammate.adapters.UserAdapter;
import com.mainstreetcode.teammate.adapters.viewholders.ChoiceBar;
import com.mainstreetcode.teammate.adapters.viewholders.EmptyViewHolder;
import com.mainstreetcode.teammate.adapters.viewholders.GameViewHolder;
import com.mainstreetcode.teammate.baseclasses.BottomSheetController;
import com.mainstreetcode.teammate.baseclasses.MainActivityFragment;
import com.mainstreetcode.teammate.model.Competitor;
import com.mainstreetcode.teammate.model.Event;
import com.mainstreetcode.teammate.model.Game;
import com.mainstreetcode.teammate.model.ListState;
import com.mainstreetcode.teammate.model.Stat;
import com.mainstreetcode.teammate.model.User;
import com.mainstreetcode.teammate.util.AppBarListener;
import com.mainstreetcode.teammate.util.ErrorHandler;
import com.mainstreetcode.teammate.util.ScrollManager;
import com.mainstreetcode.teammate.util.ViewHolderUtil;
import com.mainstreetcode.teammate.viewmodel.gofers.GameGofer;
import com.tunjid.androidbootstrap.core.abstractclasses.BaseFragment;
import com.tunjid.androidbootstrap.recyclerview.InteractiveViewHolder;
import com.tunjid.androidbootstrap.recyclerview.diff.Differentiable;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.DiffUtil;

/**
 * Lists {@link Event games}
 */

public final class GameFragment extends MainActivityFragment
        implements UserAdapter.AdapterListener {

    private static final String ARG_GAME = "game";

    private Game game;
    private List<Differentiable> items;

    private boolean hasChoiceBar;
    private GameGofer gofer;
    private AtomicBoolean editableStatus;
    private AtomicBoolean privilegeStatus;
    private GameViewHolder gameViewHolder;

    private Chip refereeChip;

    public static GameFragment newInstance(Game game) {
        GameFragment fragment = new GameFragment();
        Bundle args = new Bundle();

        args.putParcelable(ARG_GAME, game);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public String getStableTag() {
        String superResult = super.getStableTag();
        Game tempGame = getArguments().getParcelable(ARG_GAME);

        return (tempGame != null)
                ? superResult + "-" + tempGame.hashCode()
                : superResult;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle arguments = getArguments();
        game = arguments.getParcelable(ARG_GAME);
        items = statViewModel.getModelList(game);
        editableStatus = new AtomicBoolean();
        privilegeStatus = new AtomicBoolean();
        gofer = gameViewModel.gofer(game);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_game, container, false);
        View appBar = rootView.findViewById(R.id.app_bar);
        refereeChip = rootView.findViewById(R.id.referee_chip);
        gameViewHolder = new GameViewHolder(appBar, ignored -> {});
        gameViewHolder.bind(game);

        scrollManager = ScrollManager.<InteractiveViewHolder>with(rootView.findViewById(R.id.model_list))
                .withPlaceholder(new EmptyViewHolder(rootView, R.drawable.ic_stat_white_24dp, R.string.no_stats))
                .withAdapter(new StatAdapter(items, stat -> showFragment(StatEditFragment.newInstance(stat))))
                .withRefreshLayout(rootView.findViewById(R.id.refresh_layout), this::refresh)
                .withEndlessScroll(() -> fetchStats(false))
                .addScrollListener((dx, dy) -> updateFabForScrollState(dy))
                .withInconsistencyHandler(this::onInconsistencyDetected)
                .withLinearLayoutManager()
                .build();

        AppBarListener.with().appBarLayout(rootView.findViewById(R.id.app_bar))
                .offsetDiffListener(gameViewHolder::animate).create();

        scrollManager.setViewHolderColor(R.attr.alt_empty_view_holder_tint);

        refereeChip.setCloseIconResource(R.drawable.ic_close_24dp);
        refereeChip.setOnCloseIconClickListener(v -> onRemoveRefereeClicked());
        refereeChip.setOnClickListener(v -> onRefereeClicked());
        rootView.findViewById(R.id.home_thumbnail).setOnClickListener(view -> showCompetitor(game.getHome()));
        rootView.findViewById(R.id.away_thumbnail).setOnClickListener(view -> showCompetitor(game.getAway()));
        rootView.findViewById(R.id.score).setOnClickListener(view -> showFragment(GameEditFragment.newInstance(game)));
        rootView.findViewById(R.id.date).setOnClickListener(view -> showFragment(EventEditFragment.newInstance(game)));

        return rootView;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.action_delete_game).setVisible(gofer.canDelete(userViewModel.getCurrentUser()));
        menu.findItem(R.id.action_end_game).setVisible(showsFab());
        menu.findItem(R.id.action_event).setVisible(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_end_game:
                endGameRequest();
                return true;
            case R.id.action_event:
                showFragment(EventEditFragment.newInstance(game));
                return true;
            case R.id.action_delete_game:
                Context context = getContext();
                if (context == null) return true;
                new AlertDialog.Builder(context).setTitle(getString(R.string.game_delete_prompt))
                        .setPositiveButton(R.string.yes, (dialog, which) -> deleteGame())
                        .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss())
                        .show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchGame();
        fetchStats(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        refereeChip = null;
    }

    @Override
    protected void onKeyBoardChanged(boolean appeared) {
        super.onKeyBoardChanged(appeared);
        if (!appeared && isBottomSheetShowing()) hideBottomSheet();
    }

    @Override @StringRes
    protected int getFabStringResource() { return R.string.stat_add; }

    @Override @DrawableRes
    protected int getFabIconResource() { return R.drawable.ic_add_white_24dp; }

    @Override
    protected int getToolbarMenu() { return R.menu.fragment_game; }

    @Override
    protected CharSequence getToolbarTitle() {
        return getString(R.string.game_stats);
    }

    @Override
    public boolean showsFab() {
        return !isBottomSheetShowing() && editableStatus.get() && !game.isEnded();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.fab) showFragment(StatEditFragment.newInstance(Stat.empty(game)));
    }

    @Override
    public void onUserClicked(User item) {
        hideKeyboard();
        refereeChip.postDelayed(() -> {
            hideBottomSheet();
            refereeChip.postDelayed(() -> addRefereeRequest(item), 150);
        }, 200);
    }

    private void updateGame() {
        disposables.add(gofer.save().subscribe(ignored -> onGameUpdated(), defaultErrorHandler));
    }

    private void fetchGame() {
        disposables.add(gofer.fetch().subscribe(ignored -> onGameUpdated(), defaultErrorHandler));
    }

    private void refresh() {
        fetchGame();
        disposables.add(statViewModel.refresh(game).subscribe(GameFragment.this::onStatsFetched, defaultErrorHandler));
    }

    private void updateStatuses() {
        disposables.add(statViewModel.isPrivilegedInGame(game)
                .subscribe(privilegeStatus::set, defaultErrorHandler));
        disposables.add(statViewModel.canEditGameStats(game).doOnSuccess(editableStatus::set)
                .subscribe(ignored -> togglePersistentUi(), defaultErrorHandler));

        if (game.competitorsDeclined())
            scrollManager.updateForEmptyList(ListState.of(R.drawable.ic_stat_white_24dp, R.string.no_competitor_declined));

        else if (game.competitorsNotAccepted())
            scrollManager.updateForEmptyList(ListState.of(R.drawable.ic_stat_white_24dp, R.string.no_competitor_acceptance));

        else
            scrollManager.updateForEmptyList(ListState.of(R.drawable.ic_stat_white_24dp, R.string.no_stats));
    }

    private void onRemoveRefereeClicked() {
        if (!privilegeStatus.get()) return;
        game.getReferee().update(User.empty());
        toggleProgress(true);
        updateGame();
    }

    private void onRefereeClicked() {
        User referee = game.getReferee();
        boolean hasReferee = !referee.isEmpty();
        if (hasReferee) showFragment(UserEditFragment.newInstance(referee));
        else if (privilegeStatus.get()) {
            BaseFragment fragment = UserSearchFragment.newInstance();
            fragment.setTargetFragment(this, R.id.request_user_pick);
            showBottomSheet(BottomSheetController.Args.builder()
                    .setMenuRes(R.menu.empty)
                    .setFragment(fragment)
                    .build());
        }
    }

    private void endGameRequest() {
        new AlertDialog.Builder(requireActivity())
                .setTitle(R.string.game_end_request)
                .setMessage(R.string.game_end_prompt)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    game.setEnded(true);
                    toggleProgress(true);
                    disposables.add(gofer.save().subscribe(ignored -> onGameUpdated(), defaultErrorHandler));
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void addRefereeRequest(User user) {
        new AlertDialog.Builder(requireActivity())
                .setTitle(R.string.game_add_referee_title)
                .setMessage(R.string.game_add_referee_prompt)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    game.getReferee().update(user);
                    toggleProgress(true);
                    updateGame();
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void fetchStats(boolean fetchLatest) {
        if (fetchLatest) scrollManager.setRefreshing();
        else toggleProgress(true);

        disposables.add(statViewModel.getMany(game, fetchLatest).subscribe(this::onStatsFetched, defaultErrorHandler));
        updateStatuses();
    }

    private void onStatsFetched(DiffUtil.DiffResult result) {
        toggleProgress(false);
        scrollManager.onDiff(result);
    }

    private void onGameUpdated() {
        disposables.add(gofer.watchForChange().subscribe(changed -> requireActivity().invalidateOptionsMenu(), ErrorHandler.EMPTY));
        gameViewHolder.bind(game);
        toggleProgress(false);
        checkCompetitor();
        updateStatuses();
        bindReferee();
    }

    private void respond(boolean accept, Competitor competitor) {
        toggleProgress(true);
        disposables.add(competitorViewModel.respond(competitor, accept)
                .subscribe(ignored -> {
                    if (accept) fetchGame();
                    else toggleProgress(false);
                }, defaultErrorHandler));
    }

    private void deleteGame() {
        disposables.add(gofer.remove().subscribe(this::onGameDeleted, defaultErrorHandler));
    }

    private void onGameDeleted() {
        showSnackbar(getString(R.string.game_deleted));
        removeEnterExitTransitions();
        requireActivity().onBackPressed();
    }

    private void checkCompetitor() {
        if (hasChoiceBar) return;
        disposables.add(roleViewModel.hasPendingCompetitor(game).subscribe(competitor -> {
            hasChoiceBar = true;
            showChoices(choiceBar -> choiceBar.setText(getString(R.string.game_accept))
                    .setPositiveText(getText(R.string.accept))
                    .setNegativeText(getText(R.string.decline))
                    .setPositiveClickListener(v -> respond(true, competitor))
                    .setNegativeClickListener(v -> respond(false, competitor))
                    .addCallback(new BaseTransientBottomBar.BaseCallback<ChoiceBar>() {
                        public void onDismissed(ChoiceBar shown, int event) { hasChoiceBar = false; }
                    }));
        }, ErrorHandler.EMPTY));
    }

    private void bindReferee() {
        ViewGroup root = (ViewGroup) getView();
        if (root == null) return;

        int size = refereeChip.getResources().getDimensionPixelSize(R.dimen.double_margin);
        User referee = game.getReferee();
        TransitionManager.beginDelayedTransition(root, new AutoTransition().addTarget(refereeChip));

        refereeChip.setCloseIconVisible(!referee.isEmpty() && privilegeStatus.get());
        refereeChip.setText(referee.isEmpty()
                ? getString(privilegeStatus.get() && !game.isEnded()
                ? R.string.game_choose_referee : R.string.game_no_referee)
                : getString(R.string.game_referee, referee.getName()));

        disposables.add(ViewHolderUtil.fetchRoundedDrawable(refereeChip.getContext(), referee.getImageUrl(), size)
                .subscribe(refereeChip::setChipIcon, ErrorHandler.EMPTY));
    }
}
