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

package com.mainstreetcode.teammate.fragments.headless;


import android.os.Bundle;
import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.mainstreetcode.teammate.R;
import com.mainstreetcode.teammate.adapters.TeamAdapter;
import com.mainstreetcode.teammate.baseclasses.BottomSheetController;
import com.mainstreetcode.teammate.baseclasses.MainActivityFragment;
import com.mainstreetcode.teammate.fragments.main.ChatFragment;
import com.mainstreetcode.teammate.fragments.main.EventsFragment;
import com.mainstreetcode.teammate.fragments.main.GamesFragment;
import com.mainstreetcode.teammate.fragments.main.MediaFragment;
import com.mainstreetcode.teammate.fragments.main.TeamMembersFragment;
import com.mainstreetcode.teammate.fragments.main.TeamsFragment;
import com.mainstreetcode.teammate.fragments.main.TournamentsFragment;
import com.mainstreetcode.teammate.model.Team;

public class TeamPickerFragment extends MainActivityFragment implements TeamAdapter.AdapterListener {

    private static final String TAG = "TeamPickerFragment";
    private static final String ARGS_CHANGING = "ARGS_CHANGING";
    private static final String ARGS_REQUEST_CODE = "ARGS_REQUEST_CODE";

    @IdRes
    private int requestCode;
    private boolean isChanging;

    public static void pick(FragmentActivity host, @IdRes int requestCode) {
        assureInstance(host, false, requestCode);

        TeamPickerFragment instance = getInstance(host, false, requestCode);
        if (instance == null) return;

        instance.pick();
    }

    public static void change(FragmentActivity host, @IdRes int requestCode) {
        assureInstance(host, true, requestCode);

        TeamPickerFragment instance = getInstance(host, true, requestCode);
        if (instance == null) return;

        instance.pick();
    }

    private static TeamPickerFragment newInstance(boolean isChanging, @IdRes int requestCode) {
        TeamPickerFragment fragment = new TeamPickerFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARGS_CHANGING, isChanging);
        args.putInt(ARGS_REQUEST_CODE, requestCode);
        fragment.setArguments(args);
        return fragment;
    }

    private static void assureInstance(FragmentActivity host, boolean isChanging, @IdRes int requestCode) {
        FragmentManager fragmentManager = host.getSupportFragmentManager();
        Fragment instance = getInstance(host, isChanging, requestCode);

        if (instance == null) fragmentManager.beginTransaction()
                .add(newInstance(isChanging, requestCode), makeTag(isChanging, requestCode))
                .commitNow();
    }

    private static TeamPickerFragment getInstance(FragmentActivity host, boolean isChanging, @IdRes int requestCode) {
        return (TeamPickerFragment) host.getSupportFragmentManager().findFragmentByTag(makeTag(isChanging, requestCode));
    }

    private static String makeTag(boolean isChanging, @IdRes int requestCode) {
        return TAG + "-" + isChanging + "-" + requestCode;
    }

    @Override
    public String getStableTag() {
        return TAG;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isChanging = getArguments().getBoolean(ARGS_CHANGING);
        requestCode = getArguments().getInt(ARGS_REQUEST_CODE);
    }

    @Override
    public void onTeamClicked(Team item) {
        teamViewModel.updateDefaultTeam(item);
        switch (requestCode) {
            case R.id.request_game_team_pick:
                showFragment(GamesFragment.newInstance(item));
                break;
            case R.id.request_chat_team_pick:
                showFragment(ChatFragment.newInstance(item));
                break;
            case R.id.request_event_team_pick:
                showFragment(EventsFragment.newInstance(item));
                break;
            case R.id.request_media_team_pick:
                showFragment(MediaFragment.newInstance(item));
                break;
            case R.id.request_tournament_team_pick:
                showFragment(TournamentsFragment.newInstance(item));
                break;
            case R.id.request_default_team_pick:
                showFragment(TeamMembersFragment.newInstance(item));
                break;
        }
        hideBottomSheet();
    }

    private void pick() {
        Team team = teamViewModel.getDefaultTeam();
        if (!isChanging && !team.isEmpty()) onTeamClicked(team);
        else showPicker();
    }

    private void showPicker() {
        TeamsFragment teamsFragment = TeamsFragment.newInstance();
        teamsFragment.setTargetFragment(this, requestCode);

        int menuRes = requestCode != R.id.request_event_team_pick || teamViewModel.isOnATeam()
                ? R.menu.empty : R.menu.fragment_events_team_pick;

        showBottomSheet(BottomSheetController.Args.builder()
                .setMenuRes(menuRes)
                .setTitle(getString(R.string.pick_team))
                .setFragment(teamsFragment)
                .build());
    }
}
