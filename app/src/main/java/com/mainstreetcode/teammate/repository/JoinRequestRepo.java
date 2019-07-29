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

import com.mainstreetcode.teammate.model.JoinRequest;
import com.mainstreetcode.teammate.model.Team;
import com.mainstreetcode.teammate.model.User;
import com.mainstreetcode.teammate.persistence.AppDatabase;
import com.mainstreetcode.teammate.persistence.EntityDao;
import com.mainstreetcode.teammate.persistence.JoinRequestDao;
import com.mainstreetcode.teammate.rest.TeammateApi;
import com.mainstreetcode.teammate.rest.TeammateService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.functions.Function;

import static io.reactivex.schedulers.Schedulers.io;

public class JoinRequestRepo extends ModelRepo<JoinRequest> {

    private final TeammateApi api;
    private final JoinRequestDao joinRequestDao;

    JoinRequestRepo() {
        api = TeammateService.getApiInstance();
        joinRequestDao = AppDatabase.getInstance().joinRequestDao();
    }

    @Override
    public EntityDao<? super JoinRequest> dao() {
        return joinRequestDao;
    }

    @Override
    public Single<JoinRequest> createOrUpdate(JoinRequest model) {
        Single<JoinRequest> call = model.isUserApproved() ? api.joinTeam(model) : api.inviteUser(model);
        return call.map(getLocalUpdateFunction(model)).map(getSaveFunction());
    }

    @Override
    public Flowable<JoinRequest> get(String id) {
        Maybe<JoinRequest> local = joinRequestDao.get(id).subscribeOn(io());
        Maybe<JoinRequest> remote = api.getJoinRequest(id).toMaybe();

        return fetchThenGetModel(local, remote);
    }

    @Override
    public Single<JoinRequest> delete(JoinRequest model) {
        return api.deleteJoinRequest(model.getId())
                .map(this::deleteLocally)
                .doOnError(throwable -> deleteInvalidModel(model, throwable));
    }

    @Override
    Function<List<JoinRequest>, List<JoinRequest>> provideSaveManyFunction() {
        return models -> {
            List<Team> teams = new ArrayList<>(models.size());
            List<User> users = new ArrayList<>(models.size());

            for (JoinRequest request : models) {
                teams.add(request.getTeam());
                users.add(request.getUser());
            }

            if (!teams.isEmpty()) RepoProvider.forModel(Team.class).saveAsNested().apply(teams);
            if (!users.isEmpty()) RepoProvider.forModel(User.class).saveAsNested().apply(users);

            joinRequestDao.upsert(Collections.unmodifiableList(models));

            return models;
        };
    }
}
