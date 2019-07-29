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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.mainstreetcode.teammate.R;
import com.mainstreetcode.teammate.adapters.JoinRequestAdapter;
import com.mainstreetcode.teammate.adapters.viewholders.input.InputViewHolder;
import com.mainstreetcode.teammate.baseclasses.HeaderedFragment;
import com.mainstreetcode.teammate.model.JoinRequest;
import com.mainstreetcode.teammate.model.Team;
import com.mainstreetcode.teammate.model.User;
import com.mainstreetcode.teammate.util.ScrollManager;
import com.mainstreetcode.teammate.viewmodel.gofers.Gofer;
import com.mainstreetcode.teammate.viewmodel.gofers.JoinRequestGofer;
import com.mainstreetcode.teammate.viewmodel.gofers.TeamHostingGofer;
import com.tunjid.androidbootstrap.view.util.InsetFlags;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.DiffUtil;

import static com.mainstreetcode.teammate.viewmodel.gofers.JoinRequestGofer.ACCEPTING;
import static com.mainstreetcode.teammate.viewmodel.gofers.JoinRequestGofer.APPROVING;
import static com.mainstreetcode.teammate.viewmodel.gofers.JoinRequestGofer.INVITING;
import static com.mainstreetcode.teammate.viewmodel.gofers.JoinRequestGofer.JOINING;
import static com.mainstreetcode.teammate.viewmodel.gofers.JoinRequestGofer.WAITING;

/**
 * Invites a Team member
 */

public class JoinRequestFragment extends HeaderedFragment<JoinRequest>
        implements
        JoinRequestAdapter.AdapterListener {

    static final String ARG_JOIN_REQUEST = "join-request";

    private JoinRequest request;
    private JoinRequestGofer gofer;

    static JoinRequestFragment inviteInstance(Team team) {
        JoinRequestFragment fragment = newInstance(JoinRequest.invite(team));
        fragment.setEnterExitTransitions();

        return fragment;
    }

    public static JoinRequestFragment joinInstance(Team team, User user) {
        JoinRequestFragment fragment = newInstance(JoinRequest.join(team, user));
        fragment.setEnterExitTransitions();

        return fragment;
    }

    static JoinRequestFragment viewInstance(JoinRequest request) {
        JoinRequestFragment fragment = newInstance(request);
        fragment.setEnterExitTransitions();

        return fragment;
    }

    private static JoinRequestFragment newInstance(JoinRequest joinRequest) {
        JoinRequestFragment fragment = new JoinRequestFragment();
        Bundle args = new Bundle();

        args.putParcelable(ARG_JOIN_REQUEST, joinRequest);
        fragment.setArguments(args);
        fragment.setEnterExitTransitions();

        return fragment;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public String getStableTag() {
        return Gofer.tag(super.getStableTag(), getArguments().getParcelable(ARG_JOIN_REQUEST));
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        request = getArguments().getParcelable(ARG_JOIN_REQUEST);
        gofer = teamMemberViewModel.gofer(request);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_headered, container, false);

        scrollManager = ScrollManager.<InputViewHolder>with(rootView.findViewById(R.id.model_list))
                .withRefreshLayout(rootView.findViewById(R.id.refresh_layout), this::refresh)
                .withAdapter(new JoinRequestAdapter(gofer.getItems(), this))
                .addScrollListener((dx, dy) -> updateFabForScrollState(dy))
                .withInconsistencyHandler(this::onInconsistencyDetected)
                .withRecycledViewPool(inputRecycledViewPool())
                .withLinearLayoutManager()
                .build();

        scrollManager.getRecyclerView().requestFocus();
        return rootView;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean isEmpty = request.isEmpty();
        boolean canBlockUser = gofer.hasPrivilegedRole();
        boolean canDeleteRequest = canBlockUser || gofer.isRequestOwner();

        MenuItem block_item = menu.findItem(R.id.action_block);
        MenuItem deleteItem = menu.findItem(R.id.action_kick);

        block_item.setVisible(!isEmpty && canBlockUser);
        deleteItem.setVisible(!isEmpty && canDeleteRequest);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_kick:
                showDeletePrompt();
                return true;
            case R.id.action_block:
                blockUser(request.getUser(), request.getTeam());
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    @StringRes
    protected int getFabStringResource() { return gofer.getFabTitle(); }

    @Override
    @DrawableRes
    protected int getFabIconResource() { return R.drawable.ic_check_white_24dp; }

    @Override
    protected int getToolbarMenu() {
        return R.menu.fragment_user_edit;
    }

    @Override
    public CharSequence getToolbarTitle() { return gofer.getToolbarTitle(this); }

    @Override
    public InsetFlags insetFlags() { return NO_TOP; }

    @Override
    public boolean showsFab() { return gofer.showsFab(); }

    @Override
    public void onImageClick() {}

    @Override
    protected JoinRequest getHeaderedModel() {return request;}

    @Override
    protected TeamHostingGofer<JoinRequest> gofer() { return gofer; }

    @Override
    protected void onPrepComplete() {
        requireActivity().invalidateOptionsMenu();
        super.onPrepComplete();
    }

    @Override
    protected void onModelUpdated(DiffUtil.DiffResult result) {
        viewHolder.bind(getHeaderedModel());
        scrollManager.onDiff(result);
        toggleProgress(false);
    }

    @Override
    public boolean canEditFields() {
        return gofer.canEditFields();
    }

    @Override
    public boolean canEditRole() {
        return gofer.canEditRole();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() != R.id.fab) return;

        @JoinRequestGofer.JoinRequestState
        int state = gofer.getState();

        if (state == WAITING) return;

        if (state == APPROVING || state == ACCEPTING) saveRequest();
        else if (request.getPosition().isInvalid()) showSnackbar(getString(R.string.select_role));
        else if (state == JOINING || state == INVITING) createJoinRequest();
    }

    private void createJoinRequest() {
        toggleProgress(true);
        disposables.add(gofer.save().subscribe(this::onJoinRequestSent, defaultErrorHandler));
    }

    private void saveRequest() {
        toggleProgress(true);
        disposables.add(gofer.save().subscribe(ignored -> onRequestSaved(), defaultErrorHandler));
    }

    private void deleteRequest() {
        toggleProgress(true);
        disposables.add(gofer.remove().subscribe(this::onRequestDeleted, defaultErrorHandler));
    }

    private void onJoinRequestSent(DiffUtil.DiffResult result) {
        scrollManager.onDiff(result);
        hideBottomSheet();
        toggleProgress(false);
        togglePersistentUi();
        showSnackbar(getString(request.isTeamApproved()
                ? R.string.user_invite_sent
                : R.string.team_submitted_join_request));
    }

    private void onRequestDeleted() {
        CharSequence name = request.getUser().getFirstName();
        if (!gofer.isRequestOwner()) showSnackbar(getString(R.string.removed_user, name));
        requireActivity().onBackPressed();
    }

    private void onRequestSaved() {
        CharSequence name = request.getUser().getFirstName();
        if (!gofer.isRequestOwner()) showSnackbar(getString(R.string.added_user, name));
        requireActivity().onBackPressed();
    }

    private void showDeletePrompt() {
        User requestUser = request.getUser();
        final String prompt = gofer.isRequestOwner()
                ? getString(R.string.confirm_request_leave, request.getTeam().getName())
                : getString(R.string.confirm_request_drop, requestUser.getFirstName());

        new AlertDialog.Builder(requireActivity()).setTitle(prompt)
                .setPositiveButton(R.string.yes, (dialog, which) -> deleteRequest())
                .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss())
                .show();
    }
}
