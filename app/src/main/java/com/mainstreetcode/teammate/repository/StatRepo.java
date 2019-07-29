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

package com.mainstreetcode.teammate.repository;


import com.mainstreetcode.teammate.model.Game;
import com.mainstreetcode.teammate.model.Stat;
import com.mainstreetcode.teammate.model.Team;
import com.mainstreetcode.teammate.model.User;
import com.mainstreetcode.teammate.persistence.AppDatabase;
import com.mainstreetcode.teammate.persistence.EntityDao;
import com.mainstreetcode.teammate.persistence.StatDao;
import com.mainstreetcode.teammate.rest.TeammateApi;
import com.mainstreetcode.teammate.rest.TeammateService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import androidx.annotation.Nullable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.functions.Function;

import static io.reactivex.schedulers.Schedulers.io;

public class StatRepo extends QueryRepo<Stat, Game, Date> {

    private final TeammateApi api;
    private final StatDao statDao;

    StatRepo() {
        api = TeammateService.getApiInstance();
        statDao = AppDatabase.getInstance().statDao();
    }

    @Override
    public EntityDao<? super Stat> dao() {
        return statDao;
    }

    @Override
    public Single<Stat> createOrUpdate(Stat stat) {
        Single<Stat> statSingle = stat.isEmpty()
                ? api.createStat(stat.getGame().getId(), stat).map(getLocalUpdateFunction(stat))
                : api.updateStat(stat.getId(), stat)
                .map(getLocalUpdateFunction(stat))
                .doOnError(throwable -> deleteInvalidModel(stat, throwable));

        return statSingle.map(getSaveFunction());
    }

    @Override
    public Flowable<Stat> get(String id) {
        Maybe<Stat> local = statDao.get(id).subscribeOn(io());
        Maybe<Stat> remote = api.getStat(id).subscribeOn(io()).toMaybe();

        return fetchThenGetModel(local, remote);
    }

    @Override
    public Single<Stat> delete(Stat stat) {
        return api.deleteStat(stat.getId())
                .map(this::deleteLocally)
                .doOnError(throwable -> deleteInvalidModel(stat, throwable));
    }

    @Override
    Maybe<List<Stat>> localModelsBefore(Game game, @Nullable Date date) {
        if (date == null) date = getFutureDate();
        return statDao.getStats(game.getId(), date, DEF_QUERY_LIMIT).subscribeOn(io());
    }

    @Override
    Maybe<List<Stat>> remoteModelsBefore(Game game, @Nullable Date date) {
        return api.getStats(game.getId(), date, DEF_QUERY_LIMIT).map(getSaveManyFunction())
                .doOnSuccess(stats -> { for (Stat stat : stats) stat.getGame().update(game); })
                .toMaybe();
    }

    @Override
    Function<List<Stat>, List<Stat>> provideSaveManyFunction() {
        return models -> {
            List<User> users = new ArrayList<>(models.size());
            List<Team> teams = new ArrayList<>(models.size());
            List<Game> games = new ArrayList<>(models.size());

            for (Stat stat : models) {
                users.add(stat.getUser());
                teams.add(stat.getTeam());
                games.add(stat.getGame());
            }

            RepoProvider.forModel(User.class).saveAsNested().apply(users);
            RepoProvider.forModel(Team.class).saveAsNested().apply(teams);
            RepoProvider.forModel(Game.class).saveAsNested().apply(games);
            statDao.upsert(Collections.unmodifiableList(models));

            return models;
        };
    }
}
