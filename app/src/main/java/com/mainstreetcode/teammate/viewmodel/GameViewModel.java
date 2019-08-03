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

package com.mainstreetcode.teammate.viewmodel;

import android.annotation.SuppressLint;

import com.mainstreetcode.teammate.model.Competitive;
import com.mainstreetcode.teammate.repository.GameRepo;
import com.mainstreetcode.teammate.repository.GameRoundRepo;
import com.mainstreetcode.teammate.repository.RepoProvider;
import com.mainstreetcode.teammate.util.FunctionalDiff;
import com.mainstreetcode.teammate.model.Game;
import com.mainstreetcode.teammate.model.HeadToHead;
import com.mainstreetcode.teammate.model.Message;
import com.mainstreetcode.teammate.model.Role;
import com.mainstreetcode.teammate.model.Team;
import com.mainstreetcode.teammate.model.Tournament;
import com.mainstreetcode.teammate.repository.UserRepo;
import com.mainstreetcode.teammate.rest.TeammateApi;
import com.mainstreetcode.teammate.rest.TeammateService;
import com.mainstreetcode.teammate.util.ErrorHandler;
import com.mainstreetcode.teammate.util.ModelUtils;
import com.mainstreetcode.teammate.viewmodel.events.Alert;
import com.mainstreetcode.teammate.viewmodel.gofers.GameGofer;
import com.tunjid.androidbootstrap.recyclerview.diff.Differentiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.arch.core.util.Function;
import androidx.recyclerview.widget.DiffUtil;
import io.reactivex.Flowable;
import io.reactivex.Single;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;

public class GameViewModel extends TeamMappedViewModel<Game> {

    private final TeammateApi api = TeammateService.getApiInstance();

    private final Map<Tournament, Map<Integer, List<Differentiable>>> gameRoundMap = new HashMap<>();
    private final List<Differentiable> headToHeadMatchUps = new ArrayList<>();

    private final GameRoundRepo gameRoundRepository = RepoProvider.Companion.forRepo(GameRoundRepo.class);
    private final GameRepo gameRepository = RepoProvider.Companion.forRepo(GameRepo.class);

    public GameGofer gofer(Game game) {
        return new GameGofer(game, onError(game), this::getGame, this::updateGame, this::delete, GameViewModel::getEligibleTeamsForGame);
    }

    @Override
    Class<Game> valueClass() { return Game.class; }

    @Override
    @SuppressLint("CheckResult")
    void onModelAlert(Alert alert) {
        super.onModelAlert(alert);

        //noinspection unchecked
        Alert.matches(alert,
                Alert.of(Alert.Deletion.class, Game.class, this::onGameDeleted),
                Alert.of(Alert.Deletion.class, Tournament.class, this::onTournamentDeleted)
        );
    }

    @Override
    void onErrorMessage(Message message, Team key, Differentiable invalid) {
        super.onErrorMessage(message, key, invalid);
        if (message.isInvalidObject()) headToHeadMatchUps.remove(invalid);
        if (invalid instanceof Game) ((Game) invalid).setEnded(false);
    }

    @Override
    Flowable<List<Game>> fetch(Team key, boolean fetchLatest) {
        return gameRepository.modelsBefore(key, getQueryDate(fetchLatest, key, Game::getCreated))
                .map(games -> filterDeclinedGamed(key, games));
    }

    @SuppressLint("UseSparseArrays")
    public List<Differentiable> getGamesForRound(Tournament tournament, int round) {
        Map<Integer, List<Differentiable>> roundMap = ModelUtils.get(tournament, gameRoundMap, HashMap::new);
        return ModelUtils.get(round, roundMap, ArrayList::new);
    }

    public List<Differentiable> getHeadToHeadMatchUps() {
        return headToHeadMatchUps;
    }

    public Flowable<DiffUtil.DiffResult> fetchGamesInRound(Tournament tournament, int round) {
        Flowable<List<Game>> flowable = gameRoundRepository.modelsBefore(tournament, round);
        Function<List<Game>, List<Differentiable>> listMapper = ArrayList<Differentiable>::new;
        return FunctionalDiff.of(flowable.map(listMapper::apply), getGamesForRound(tournament, round), this::preserveList);
    }

    public Single<HeadToHead.Summary> headToHead(HeadToHead.Request request) {
        return api.headToHead(request).map(result -> result.getSummary(request)).observeOn(mainThread());
    }

    public Single<DiffUtil.DiffResult> getMatchUps(HeadToHead.Request request) {
        Single<List<Differentiable>> sourceSingle = api.matchUps(request).map(games -> {
            Collections.sort(games, FunctionalDiff.COMPARATOR);
            return new ArrayList<>(games);
        });
        return FunctionalDiff.of(sourceSingle, headToHeadMatchUps, ModelUtils::replaceList).observeOn(mainThread());
    }

    public Flowable<Game> getGame(Game game) {
        return gameRoundRepository.get(game).observeOn(mainThread());
    }

    public Single<Game> endGame(Game game) {
        game.setEnded(true);
        return updateGame(game);
    }

    private Single<Game> updateGame(Game game) {
        return gameRoundRepository.createOrUpdate(game)
                .doOnError(onError(game)).observeOn(mainThread());
    }

    private Single<Game> delete(final Game game) {
        return gameRepository.delete(game)
                .doOnSuccess(getModelList(game.getTeam())::remove)
                .doOnSuccess(deleted -> pushModelAlert(Alert.deletion(deleted)));
    }

    static Flowable<Team> getEligibleTeamsForGame(Game game) {
        if (game.betweenUsers()) return Flowable.just(game.getHost());
        return Flowable.fromIterable(RoleViewModel.roles)
                .filter(identifiable -> identifiable instanceof Role).cast(Role.class)
                .filter(Role::isPrivilegedRole).map(Role::getTeam)
                .filter(team -> isParticipant(game, team));
    }

    private static boolean isParticipant(Game game, Team team) {
        return game.getHome().getEntity().equals(team) || game.getAway().getEntity().equals(team);
    }

    @SuppressLint("CheckResult")
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void onTournamentDeleted(Tournament tournament) {
        Map<?, List<Differentiable>> tournamentMap = ModelUtils.get(tournament, gameRoundMap, Collections::emptyMap);
        Function<Map<?, List<Differentiable>>, Flowable<Differentiable>> mapListFunction = listMap ->
                Flowable.fromIterable(listMap.values())
                        .flatMap(Flowable::fromIterable);

        Flowable<Differentiable> teamMapped = mapListFunction.apply(modelListMap);
        Flowable<Differentiable> tournamentMapped = mapListFunction.apply(tournamentMap);
        Flowable.concat(teamMapped, tournamentMapped)
                .distinct()
                .filter(item -> item instanceof Game)
                .cast(Game.class)
                .filter(game -> tournament.equals(game.getTournament()))
                .subscribe(game -> pushModelAlert(Alert.deletion(game)), ErrorHandler.EMPTY);
    }

    private void onGameDeleted(Game game) {
        headToHeadMatchUps.remove(game);
        getModelList(game.getHost()).remove(game);

        Competitive home = game.getHome().getEntity();
        Competitive away = game.getAway().getEntity();
        if (home instanceof Team) getModelList((Team) home).remove(game);
        if (away instanceof Team) getModelList((Team) away).remove(game);

        gameRepository.queueForLocalDeletion(game);
    }

    @NonNull
    private List<Game> filterDeclinedGamed(Team key, List<Game> games) {
        Iterator<Game> iterator = games.iterator();
        while (iterator.hasNext()) {
            Game game = iterator.next();
            Competitive entity = game.betweenUsers() ? RepoProvider.Companion.forRepo(UserRepo.class).getCurrentUser() : key;
            boolean isAway = entity.equals(game.getAway().getEntity());
            if (isAway && game.getAway().isDeclined()) iterator.remove();
        }

        return games;
    }
}
