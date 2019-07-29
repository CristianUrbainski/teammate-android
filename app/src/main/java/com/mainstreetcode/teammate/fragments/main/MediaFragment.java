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

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.mainstreetcode.teammate.MediaTransferIntentService;
import com.mainstreetcode.teammate.R;
import com.mainstreetcode.teammate.adapters.MediaAdapter;
import com.mainstreetcode.teammate.adapters.viewholders.EmptyViewHolder;
import com.mainstreetcode.teammate.adapters.viewholders.MediaViewHolder;
import com.mainstreetcode.teammate.baseclasses.MainActivityFragment;
import com.mainstreetcode.teammate.fragments.headless.ImageWorkerFragment;
import com.mainstreetcode.teammate.fragments.headless.TeamPickerFragment;
import com.mainstreetcode.teammate.model.Media;
import com.mainstreetcode.teammate.model.Team;
import com.mainstreetcode.teammate.util.ScrollManager;
import com.tunjid.androidbootstrap.core.abstractclasses.BaseFragment;
import com.tunjid.androidbootstrap.recyclerview.InteractiveViewHolder;
import com.tunjid.androidbootstrap.recyclerview.diff.Differentiable;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.util.Pair;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DiffUtil;

import static com.mainstreetcode.teammate.util.ViewHolderUtil.getTransitionName;

public class MediaFragment extends MainActivityFragment
        implements
        MediaAdapter.MediaAdapterListener,
        ImageWorkerFragment.MediaListener,
        ImageWorkerFragment.DownloadRequester {

    private static final int MEDIA_DELETE_SNACKBAR_DELAY = 350;
    private static final String ARG_TEAM = "team";

    private Team team;
    private List<Differentiable> items;
    private AtomicBoolean bottomBarState;
    private AtomicBoolean altToolBarState;

    public static MediaFragment newInstance(Team team) {
        MediaFragment fragment = new MediaFragment();
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
        items = mediaViewModel.getModelList(team);
        bottomBarState = new AtomicBoolean();
        altToolBarState = new AtomicBoolean();

        ImageWorkerFragment.attach(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_media, container, false);

        Runnable refreshAction = () -> disposables.add(mediaViewModel.refresh(team).subscribe(MediaFragment.this::onMediaUpdated, defaultErrorHandler));

        scrollManager = ScrollManager.<InteractiveViewHolder>with(rootView.findViewById(R.id.team_media))
                .withPlaceholder(new EmptyViewHolder(rootView, R.drawable.ic_video_library_black_24dp, R.string.no_media))
                .withRefreshLayout(rootView.findViewById(R.id.refresh_layout), refreshAction)
                .withEndlessScroll(() -> fetchMedia(false))
                .addScrollListener((dx, dy) -> updateFabForScrollState(dy))
                .withInconsistencyHandler(this::onInconsistencyDetected)
                .withAdapter(new MediaAdapter(items, this))
                .withGridLayoutManager(4)
                .build();

        bottomBarState.set(true);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchMedia(true);
        toggleContextMenu(mediaViewModel.hasSelections(team));
        disposables.add(mediaViewModel.listenForUploads().subscribe(this::onMediaUpdated, emptyErrorHandler));
    }

    @Override
    @StringRes
    protected int getFabStringResource() { return R.string.media_add; }

    @Override
    @DrawableRes
    protected int getFabIconResource() { return R.drawable.ic_add_white_24dp; }

    @Override
    protected int getAltToolbarMenu() {
        return R.menu.fragment_media_context;
    }

    @Override
    protected CharSequence getToolbarTitle() {
        return getString(R.string.media_title, team.getName());
    }

    @Override
    protected CharSequence getAltToolbarTitle() {
        return getString(R.string.multi_select, mediaViewModel.getNumSelected(team));
    }

    private void fetchMedia(boolean fetchLatest) {
        if (fetchLatest) scrollManager.setRefreshing();
        else toggleProgress(true);

        disposables.add(mediaViewModel.getMany(team, fetchLatest).subscribe(this::onMediaUpdated, defaultErrorHandler));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_pick_team:
                TeamPickerFragment.change(getActivity(), R.id.request_media_team_pick);
                return true;
            case R.id.action_delete:
                disposables.add(mediaViewModel.deleteMedia(team, localRoleViewModel.hasPrivilegedRole())
                        .subscribe(this::onMediaDeleted, defaultErrorHandler));
                return true;
            case R.id.action_download:
                if (ImageWorkerFragment.requestDownload(this, team))
                    scrollManager.notifyDataSetChanged();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean handledBackPress() {
        if (!mediaViewModel.hasSelections(team)) return false;
        mediaViewModel.clearSelections(team);
        scrollManager.notifyDataSetChanged();
        toggleContextMenu(false);
        return true;
    }

    @Override
    public boolean showsFab() { return true; }

    @Override
    public boolean showsAltToolBar() {
        return altToolBarState.get();
    }

    @Override
    public boolean showsBottomNav() { return bottomBarState.get(); }

    @Override protected int getToolbarMenu() {
        return R.menu.fragment_media;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab:
                ImageWorkerFragment.requestMultipleMedia(this);
                break;
        }
    }

    @Override
    @Nullable
    @SuppressLint("CommitTransaction")
    @SuppressWarnings("ConstantConditions")
    public FragmentTransaction provideFragmentTransaction(BaseFragment fragmentTo) {
        if (fragmentTo.getStableTag().contains(MediaDetailFragment.class.getSimpleName())) {
            Media media = fragmentTo.getArguments().getParcelable(MediaDetailFragment.ARG_MEDIA);

            if (media == null) return null;
            MediaViewHolder holder = (MediaViewHolder) scrollManager.findViewHolderForItemId(media.hashCode());
            if (holder == null) return null;

            holder.bind(media); // Rebind, to make sure transition names remain.
            return beginTransaction()
                    .addSharedElement(holder.itemView, getTransitionName(media, R.id.fragment_media_background))
                    .addSharedElement(holder.thumbnailView, getTransitionName(media, R.id.fragment_media_thumbnail));
        }
        return null;
    }

    @Override
    public void onMediaClicked(Media item) {
        if (mediaViewModel.hasSelections(team)) longClickMedia(item);
        else {
            bottomBarState.set(false);
            togglePersistentUi();
            showFragment(MediaDetailFragment.newInstance(item));
        }
    }

    @Override
    public boolean onMediaLongClicked(Media media) {
        boolean result = mediaViewModel.select(media);
        boolean hasSelections = mediaViewModel.hasSelections(team);

        toggleContextMenu(hasSelections);
        return result;
    }

    @Override
    public boolean isSelected(Media media) {
        return mediaViewModel.isSelected(media);
    }

    @Override
    public boolean isFullScreen() {
        return false;
    }

    @Override
    public void onFilesSelected(List<Uri> uris) {
        MediaTransferIntentService.startActionUpload(getContext(), userViewModel.getCurrentUser(), team, uris);
    }

    @Override
    public Team requestedTeam() {
        return team;
    }

    @Override
    public void startedDownLoad(boolean started) {
        toggleContextMenu(!started);
        if (started) scrollManager.notifyDataSetChanged();
    }

    private void onMediaUpdated(DiffUtil.DiffResult result) {
        scrollManager.onDiff(result);
        toggleProgress(false);
    }

    private void toggleContextMenu(boolean show) {
        altToolBarState.set(show);
        togglePersistentUi();
    }

    private void longClickMedia(Media media) {
        MediaViewHolder holder = (MediaViewHolder) scrollManager.findViewHolderForItemId(media.hashCode());
        if (holder == null) return;

        holder.performLongClick();
    }

    private void onMediaDeleted(Pair<Boolean, DiffUtil.DiffResult> pair) {
        toggleContextMenu(false);

        boolean partialDelete = pair.first == null ? false : pair.first;
        DiffUtil.DiffResult diffResult = pair.second;

        if (diffResult != null) scrollManager.onDiff(diffResult);
        if (!partialDelete) return;

        scrollManager.notifyDataSetChanged();
        scrollManager.getRecyclerView().postDelayed(() -> showSnackbar(getString(R.string.partial_delete_message)), MEDIA_DELETE_SNACKBAR_DELAY);
    }
}
