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

import androidx.arch.core.util.Function;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;

import com.mainstreetcode.teammate.model.Competitive;
import com.mainstreetcode.teammate.model.Competitor;
import com.mainstreetcode.teammate.util.FunctionalDiff;
import com.mainstreetcode.teammate.model.Game;
import com.tunjid.androidbootstrap.recyclerview.diff.Differentiable;
import com.mainstreetcode.teammate.model.Team;
import com.mainstreetcode.teammate.model.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;

public class GameGofer extends Gofer<Game> {

    private final List<Team> eligibleTeams;
    private final Function<Game, Flowable<Game>> getFunction;
    private final Function<Game, Single<Game>> upsertFunction;
    private final Function<Game, Single<Game>> deleteFunction;
    private final Function<Game, Flowable<Team>> eligibleTeamSource;


    public GameGofer(Game model, Consumer<Throwable> onError,
                     Function<Game, Flowable<Game>> getFunction,
                     Function<Game, Single<Game>> upsertFunction,
                     Function<Game, Single<Game>> deleteFunction,
                     Function<Game, Flowable<Team>> eligibleTeamSource) {
        super(model, onError);
        this.getFunction = getFunction;
        this.upsertFunction = upsertFunction;
        this.deleteFunction = deleteFunction;
        this.eligibleTeamSource = eligibleTeamSource;

        this.eligibleTeams = new ArrayList<>();
        this.items.addAll(model.isEmpty()
                ? Arrays.asList(model.getHome(), model.getAway())
                : model.asItems());
    }

    public boolean canEdit() {
        boolean canEdit = !model.isEnded() && !eligibleTeams.isEmpty() && !model.competitorsNotAccepted();
        return model.isEmpty() || canEdit;
    }

    public boolean canDelete(User user) {
        if (!model.getTournament().isEmpty()) return false;
        if (model.getHome().isEmpty()) return true;
        if (model.getAway().isEmpty()) return true;

        Competitive entity = model.getHome().getEntity();
        if (entity.equals(user)) return true;
        for (Team team : eligibleTeams) if (entity.equals(team)) return true;

        return false;
    }

    @Override
    Flowable<Boolean> changeEmitter() {
        int count = eligibleTeams.size();
        eligibleTeams.clear();
        return eligibleTeamSource.apply(model)
                .collectInto(eligibleTeams, List::add)
                .map(list -> count != list.size())
                .toFlowable();
    }

    @Override
    public Flowable<DiffUtil.DiffResult> fetch() {
        Flowable<List<Differentiable>> source = getFunction.apply(model).map(Game::asDifferentiables);
        return FunctionalDiff.of(source, getItems(), this::preserveItems);
    }

    public Completable delete() {
        return Single.defer(() -> deleteFunction.apply(model)).toCompletable();
    }

    @Override
    @Nullable
    public String getImageClickMessage(Fragment fragment) {
        return null;
    }

    Single<DiffUtil.DiffResult> upsert() {
        Single<List<Differentiable>> source = upsertFunction.apply(model).map(Game::asDifferentiables);
        return FunctionalDiff.of(source, getItems(), this::preserveItems);
    }

    @Override
    List<Differentiable> preserveItems(List<Differentiable> old, List<Differentiable> fetched) {
        List<Differentiable> result = super.preserveItems(old, fetched);
        Iterator<Differentiable> iterator = result.iterator();
        Function<Differentiable, Boolean> filter = item -> item instanceof Competitor && ((Competitor) item).isEmpty();

        int currentSize = result.size();
        while (iterator.hasNext()) if (filter.apply(iterator.next())) iterator.remove();

        if (currentSize == result.size() || model.isEmpty()) return result;
        if (currentSize != model.asItems().size()) return result;

        result.add(model.getHome());
        result.add(model.getAway());

        return result;
    }
}
