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

import android.annotation.SuppressLint;
import android.location.Address;

import com.mainstreetcode.teammate.R;
import com.mainstreetcode.teammate.model.BlockedUser;
import com.mainstreetcode.teammate.model.Event;
import com.mainstreetcode.teammate.model.Guest;
import com.mainstreetcode.teammate.model.User;
import com.mainstreetcode.teammate.repository.GuestRepo;
import com.mainstreetcode.teammate.repository.RepoProvider;
import com.mainstreetcode.teammate.util.ErrorHandler;
import com.mainstreetcode.teammate.util.FunctionalDiff;
import com.mainstreetcode.teammate.util.ModelUtils;
import com.tunjid.androidbootstrap.recyclerview.diff.Differentiable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;

public class EventGofer extends TeamHostingGofer<Event> {

    private boolean isSettingLocation;
    private final Function<Guest, Single<Guest>> rsvpFunction;
    private final Function<Event, Flowable<Event>> getFunction;
    private final Function<Event, Single<Event>> deleteFunction;
    private final Function<Event, Single<Event>> updateFunction;
    private final GuestRepo guestRepository;

    @SuppressLint("CheckResult")
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public EventGofer(Event model,
                      Consumer<Throwable> onError,
                      Flowable<BlockedUser> blockedUserFlowable,
                      Function<Event, Flowable<Event>> getFunction,
                      Function<Event, Single<Event>> upsertFunction,
                      Function<Event, Single<Event>> deleteFunction,
                      Function<Guest, Single<Guest>> rsvpFunction) {
        super(model, onError);
        this.getFunction = getFunction;
        this.rsvpFunction = rsvpFunction;
        this.updateFunction = upsertFunction;
        this.deleteFunction = deleteFunction;
        this.guestRepository = RepoProvider.forRepo(GuestRepo.class);

        items.addAll(model.asDifferentiables());
        items.add(model.getTeam());

        blockedUserFlowable.subscribe(this::onUserBlocked, ErrorHandler.EMPTY);
    }

    public void setSettingLocation(boolean settingLocation) {
        isSettingLocation = settingLocation;
    }

    public boolean isSettingLocation() {
        return isSettingLocation;
    }

    public CharSequence getToolbarTitle(Fragment fragment) {
        return model.isEmpty()
                ? fragment.getString(R.string.create_event)
                : model.getName();
    }

    @Nullable
    @Override
    public String getImageClickMessage(Fragment fragment) {
        if (hasPrivilegedRole()) return null;
        return fragment.getString(R.string.no_permission);
    }

    @Override
    Flowable<DiffUtil.DiffResult> fetch() {
        if (isSettingLocation) return Flowable.empty();
        Flowable<List<Differentiable>> eventFlowable = getFunction.apply(model).map(Event::asDifferentiables);
        Flowable<List<Differentiable>> guestsFlowable = guestRepository.modelsBefore(model, new Date()).map(ModelUtils::asDifferentiables);
        Flowable<List<Differentiable>> sourceFlowable = Flowable.concatDelayError(Arrays.asList(eventFlowable, guestsFlowable));
        return FunctionalDiff.of(sourceFlowable, getItems(), this::preserveItems);
    }

    Single<DiffUtil.DiffResult> upsert() {
        Single<List<Differentiable>> source = updateFunction.apply(model).map(Event::asDifferentiables);
        return FunctionalDiff.of(source, getItems(), this::preserveItems);
    }

    public Single<DiffUtil.DiffResult> rsvpEvent(boolean attending) {
        Single<List<Differentiable>> single = rsvpFunction.apply(Guest.forEvent(model, attending)).map(Collections::singletonList);
        return FunctionalDiff.of(single, getItems(), (staleCopy, singletonGuestList) -> {
            staleCopy.removeAll(singletonGuestList);
            staleCopy.addAll(singletonGuestList);
            return staleCopy;
        });
    }

    public Single<Integer> getRSVPStatus() {
        return Single.defer(() -> Flowable.fromIterable(new ArrayList<>(items))
                .filter(identifiable -> identifiable instanceof Guest)
                .cast(Guest.class)
                .filter(Guest::isAttending)
                .map(Guest::getUser)
                .filter(getSignedInUser()::equals)
                .collect(ArrayList::new, List::add)
                .map(List::isEmpty)
                .map(notAttending -> notAttending ? R.drawable.ic_event_available_white_24dp : R.drawable.ic_event_busy_white_24dp)
        ).subscribeOn(Schedulers.io()).observeOn(mainThread());
    }

    Completable delete() {
        return deleteFunction.apply(model).ignoreElement();
    }

    public Single<DiffUtil.DiffResult> setAddress(Address address) {
        isSettingLocation = true;
        model.setAddress(address);
        return FunctionalDiff.of(Single.just(model.asDifferentiables()), getItems(), this::preserveItems).doFinally(() -> isSettingLocation = false);
    }

    private void onUserBlocked(BlockedUser blockedUser) {
        if (!blockedUser.getTeam().equals(model.getTeam())) return;

        Iterator<Differentiable> iterator = items.iterator();

        while (iterator.hasNext()) {
            Differentiable identifiable = iterator.next();
            if (!(identifiable instanceof Guest)) continue;
            User blocked = blockedUser.getUser();
            User guestUser = ((Guest) identifiable).getUser();

            if (blocked.equals((guestUser))) iterator.remove();
        }
    }
}
