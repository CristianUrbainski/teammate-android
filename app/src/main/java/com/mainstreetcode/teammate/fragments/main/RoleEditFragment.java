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
import com.mainstreetcode.teammate.adapters.RoleEditAdapter;
import com.mainstreetcode.teammate.adapters.viewholders.input.InputViewHolder;
import com.mainstreetcode.teammate.baseclasses.HeaderedFragment;
import com.mainstreetcode.teammate.model.Role;
import com.mainstreetcode.teammate.util.ScrollManager;
import com.mainstreetcode.teammate.viewmodel.gofers.Gofer;
import com.mainstreetcode.teammate.viewmodel.gofers.RoleGofer;
import com.mainstreetcode.teammate.viewmodel.gofers.TeamHostingGofer;
import com.tunjid.androidbootstrap.view.util.InsetFlags;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.DiffUtil;

/**
 * Edits a Team member
 */

public class RoleEditFragment extends HeaderedFragment<Role>
        implements
        RoleEditAdapter.RoleEditAdapterListener {

    static final String ARG_ROLE = "role";

    private Role role;
    private RoleGofer gofer;

    public static RoleEditFragment newInstance(Role role) {
        RoleEditFragment fragment = new RoleEditFragment();
        Bundle args = new Bundle();

        args.putParcelable(ARG_ROLE, role);
        fragment.setArguments(args);
        fragment.setEnterExitTransitions();

        return fragment;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public String getStableTag() {
        return Gofer.tag(super.getStableTag(), getArguments().getParcelable(ARG_ROLE));
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        role = getArguments().getParcelable(ARG_ROLE);
        gofer = teamMemberViewModel.gofer(role);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_headered, container, false);

        scrollManager = ScrollManager.<InputViewHolder>with(rootView.findViewById(R.id.model_list))
                .withRefreshLayout(rootView.findViewById(R.id.refresh_layout), this::refresh)
                .withAdapter(new RoleEditAdapter(gofer.getItems(), this))
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
        MenuItem kickItem = menu.findItem(R.id.action_kick);
        MenuItem blockItem = menu.findItem(R.id.action_block);

        kickItem.setVisible(gofer.canChangeRoleFields());
        blockItem.setVisible(canChangeRolePosition());
    }

    @Override
    protected int getToolbarMenu() { return R.menu.fragment_user_edit; }

    @Override
    @StringRes
    protected int getFabStringResource() { return R.string.role_update; }

    @Override
    @DrawableRes
    protected int getFabIconResource() { return R.drawable.ic_check_white_24dp; }

    @Override
    protected CharSequence getToolbarTitle() {
        return getString(R.string.role_edit);
    }

    @Override
    public InsetFlags insetFlags() {return NO_TOP;}

    @Override
    public boolean showsFab() {return gofer.canChangeRoleFields();}

    @Override
    protected Role getHeaderedModel() {return role;}

    @Override
    protected TeamHostingGofer<Role> gofer() { return gofer; }

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
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fab:
                if (role.getPosition().isInvalid()) showSnackbar(getString(R.string.select_role));
                else updateRole();
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_kick:
                showDropRolePrompt();
                return true;
            case R.id.action_block:
                blockUser(role.getUser(), role.getTeam());
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean canChangeRolePosition() {
        return gofer.hasPrivilegedRole();
    }

    @Override
    public boolean canChangeRoleFields() {
        return gofer.canChangeRoleFields();
    }

    private void showDropRolePrompt() {
        new AlertDialog.Builder(requireActivity()).setTitle(gofer.getDropRolePrompt(this))
                .setPositiveButton(R.string.yes, (dialog, which) -> disposables.add(gofer.remove().subscribe(this::onRoleDropped, defaultErrorHandler)))
                .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void updateRole() {
        toggleProgress(true);
        disposables.add(gofer.save().subscribe(this::onRoleUpdated, defaultErrorHandler));
    }

    private void onRoleUpdated(DiffUtil.DiffResult result) {
        viewHolder.bind(getHeaderedModel());
        scrollManager.onDiff(result);
        togglePersistentUi();
        toggleProgress(false);
        requireActivity().invalidateOptionsMenu();
        showSnackbar(getString(R.string.updated_user, role.getUser().getFirstName()));
    }

    private void onRoleDropped() {
        showSnackbar(getString(R.string.dropped_user, role.getUser().getFirstName()));
        removeEnterExitTransitions();
        requireActivity().onBackPressed();
    }
}
