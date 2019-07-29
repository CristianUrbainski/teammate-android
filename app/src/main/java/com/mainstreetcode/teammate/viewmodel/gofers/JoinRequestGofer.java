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

package com.mainstreetcode.teammate.viewmodel.gofers;

import com.mainstreetcode.teammate.R;
import com.mainstreetcode.teammate.model.Item;
import com.mainstreetcode.teammate.model.JoinRequest;
import com.mainstreetcode.teammate.model.TeamMember;
import com.mainstreetcode.teammate.repository.RepoProvider;
import com.mainstreetcode.teammate.repository.TeamMemberRepo;
import com.mainstreetcode.teammate.util.FunctionalDiff;
import com.tunjid.androidbootstrap.functions.BiFunction;
import com.tunjid.androidbootstrap.recyclerview.diff.Differentiable;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.arch.core.util.Function;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static java.lang.annotation.RetentionPolicy.SOURCE;

public class JoinRequestGofer extends TeamHostingGofer<JoinRequest> {

    @Retention(SOURCE)
    @IntDef({INVITING, JOINING, APPROVING, ACCEPTING, WAITING})
    public @interface JoinRequestState {}

    public static final int INVITING = 0;
    public static final int JOINING = 1;
    public static final int APPROVING = 2;
    public static final int ACCEPTING = 3;
    public static final int WAITING = 4;

    private int state;
    private int index;

    private final Function<JoinRequest, Flowable<JoinRequest>> getFunction;
    private final BiFunction<JoinRequest, Boolean, Single<JoinRequest>> joinCompleter;

    public JoinRequestGofer(JoinRequest model,
                            Consumer<Throwable> onError,
                            Function<JoinRequest, Flowable<JoinRequest>> getFunction,
                            BiFunction<JoinRequest, Boolean, Single<JoinRequest>> joinCompleter) {
        super(model, onError);
        this.getFunction = getFunction;
        this.joinCompleter = joinCompleter;
        index = getIndex(model);
        updateState();
        items.addAll(filteredItems(model));
    }

    public boolean showsFab() {
        switch (state) {
            case INVITING:
            case JOINING:
                return true;
            case APPROVING:
                return hasPrivilegedRole();
            case ACCEPTING:
                return isRequestOwner();
            default:
                return false;
        }
    }

    public boolean canEditFields() {
        return state == INVITING;
    }

    public boolean canEditRole() {
        return state == INVITING || state == JOINING;
    }

    public boolean isRequestOwner() {
        return getSignedInUser().equals(model.getUser());
    }

    private void updateState() {
        boolean isEmpty = model.isEmpty();
        boolean isRequestOwner = isRequestOwner();
        boolean isUserEmpty = model.getUser().isEmpty();
        boolean isUserApproved = model.isUserApproved();
        boolean isTeamApproved = model.isTeamApproved();

        state = isEmpty && isUserEmpty && isTeamApproved
                ? INVITING
                : isEmpty && isUserApproved && isRequestOwner
                ? JOINING
                : (!isEmpty && isUserApproved && isRequestOwner) || (!isEmpty && isTeamApproved && !isRequestOwner)
                ? WAITING
                : isTeamApproved && isRequestOwner ? ACCEPTING : APPROVING;
    }

    @JoinRequestState
    public int getState() {
        return state;
    }

    public String getToolbarTitle(Fragment fragment) {
        return fragment.getString(state == JOINING
                ? R.string.join_team
                : state == INVITING
                ? R.string.invite_user
                : state == WAITING
                ? R.string.pending_request
                : state == APPROVING ? R.string.approve_request : R.string.accept_request);
    }

    @StringRes
    public int getFabTitle() {
        switch (state) {
            case JOINING:
                return R.string.join_team;
            case INVITING:
                return R.string.invite;
            case APPROVING:
                return R.string.approve;
            default:
            case WAITING:
            case ACCEPTING:
                return R.string.accept;
        }
    }

    @Nullable
    @Override
    public String getImageClickMessage(Fragment fragment) {
        return fragment.getString(R.string.no_permission);
    }

    @Override
    public Flowable<DiffUtil.DiffResult> fetch() {
        Flowable<List<Differentiable>> source = getFunction.apply(model).map(JoinRequest::asDifferentiables);
        return FunctionalDiff.of(source, getItems(), (items, updated) -> filteredItems(model));
    }

    @Override
    Single<DiffUtil.DiffResult> upsert() {
        Single<JoinRequest> single = model.isEmpty() ? joinTeam() : approveRequest();
        Single<List<Differentiable>> source = single.map(JoinRequest::asDifferentiables).doOnSuccess(ignored -> updateState());
        return FunctionalDiff.of(source, getItems(), (items, updated) -> filteredItems(model));
    }

    @Override
    public Completable delete() {
        return joinCompleter.apply(model, false).ignoreElement().observeOn(mainThread());
    }

    private Single<JoinRequest> joinTeam() {
        TeamMember<JoinRequest> member = TeamMember.fromModel(model);
        @SuppressWarnings("unchecked") TeamMemberRepo<JoinRequest> repository = RepoProvider.forRepo(TeamMemberRepo.class);

        return repository.createOrUpdate(member).map(ignored -> model);
    }

    private Single<JoinRequest> approveRequest() {
        return Single.defer(() -> joinCompleter.apply(model, true));
    }

    private boolean filter(Item<JoinRequest> item) {
        boolean isEmpty = model.isEmpty();
        int sortPosition = item.getSortPosition();

        if (item.getItemType() == Item.ROLE) return true;

        // Joining a team
        boolean joining = state == JOINING || state == ACCEPTING || (state == WAITING && isRequestOwner());
        if (joining) return sortPosition > index;

        // Inviting a user
        boolean ignoreTeam = sortPosition <= index;

        int stringRes = item.getStringRes();

        // About field should not show when inviting a user, email field should not show when trying
        // to join a team.
        return isEmpty
                ? ignoreTeam && stringRes != R.string.user_about
                : ignoreTeam && stringRes != R.string.email;
    }

    private Integer getIndex(JoinRequest model) {
        return Flowable.range(0, model.asItems().size() - 1)
                .filter(index -> model.asItems().get(index).getItemType() == Item.ROLE)
                .first(0)
                .blockingGet();
    }

    private List<Differentiable> filteredItems(JoinRequest request) {
        return Flowable.fromIterable(request.asItems())
                .filter(this::filter)
                .collect(ArrayList<Differentiable>::new, List::add)
                .blockingGet();
    }
}
