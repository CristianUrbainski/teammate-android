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

package com.mainstreetcode.teammate.model;

import androidx.room.Ignore;
import android.os.Parcel;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.mainstreetcode.teammate.R;
import com.mainstreetcode.teammate.model.enums.Sport;
import com.mainstreetcode.teammate.persistence.entity.GameEntity;
import com.mainstreetcode.teammate.util.IdCache;
import com.mainstreetcode.teammate.util.ModelUtils;
import com.tunjid.androidbootstrap.recyclerview.diff.Differentiable;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.mainstreetcode.teammate.util.ModelUtils.EMPTY_STRING;
import static com.mainstreetcode.teammate.util.ModelUtils.areNotEmpty;

/**
 * Event events
 */

public class Game extends GameEntity
        implements
        TeamHost,
        Model<Game>,
        HeaderedModel<Game>,
        ListableModel<Game> {

    @Ignore private static final IdCache holder = IdCache.cache(5);

    public Game(@NonNull String id, String name, String refPath, String score, String matchUp,
                String homeEntityId, String awayEntityId, String winnerEntityId,
                Date created, Sport sport, User referee, Team host, Event event, Tournament tournament,
                Competitor home, Competitor away, Competitor winner,
                int seed, int leg, int round, int homeScore, int awayScore,
                boolean ended, boolean canDraw) {
        super(id, name, refPath, score, matchUp, homeEntityId, awayEntityId, winnerEntityId, created, sport, referee, host, event, tournament, home, away, winner, seed, leg, round, homeScore, awayScore, ended, canDraw);
    }

    public static Game empty(Team team) {
        Sport sport = team.getSport();
        return new Game("", "", sport.refType(), "TBD", "", "", "", "",
                new Date(), sport, User.empty(), team, Event.empty(), Tournament.empty(),
                Competitor.empty(), Competitor.empty(), Competitor.empty(), 0, 0, 0, 0, 0, false, true);
    }

    public static Game withId(String id) {
        Game empty = empty(Team.empty());
        empty.id = id;
        return empty;
    }

    protected Game(Parcel in) {
        super(in);
    }

    @Override
    public List<Item<Game>> asItems() {
        return Arrays.asList(
                Item.Companion.text(holder.get(0), 0, Item.NUMBER, R.string.game_competitors, this::getName, ignored -> {}, this),
                Item.Companion.number(holder.get(1), 1, Item.INPUT, R.string.game_home_score, () -> String.valueOf(homeScore), this::setHomeScore, this),
                Item.Companion.number(holder.get(2), 2, Item.INPUT, R.string.game_away_score, () -> String.valueOf(awayScore), this::setAwayScore, this),
                Item.Companion.number(holder.get(3), 3, Item.NUMBER, R.string.game_round, () -> String.valueOf(round), ignored -> {}, this),
                Item.Companion.number(holder.get(4), 4, Item.NUMBER, R.string.game_leg, () -> String.valueOf(leg), ignored -> {}, this)
        );
    }

    @Override
    public Item<Game> getHeaderItem() {
        return Item.Companion.text(EMPTY_STRING, 0, Item.IMAGE, R.string.team_logo, () -> "", url -> {}, this);
    }

    @Override
    public boolean areContentsTheSame(Differentiable other) {
        if (!(other instanceof Game)) return id.equals(other.getId());
        Game casted = (Game) other;
        return score.equals(casted.score)
                && home.areContentsTheSame(casted.home)
                && away.areContentsTheSame(casted.away);
    }

    @Override
    public boolean hasMajorFields() {
        return areNotEmpty(id, refPath, score) && home.hasMajorFields() && away.hasMajorFields();
    }

    @Override
    public Object getChangePayload(Differentiable other) {
        return other;
    }

    @Override
    public boolean isEmpty() {
        return TextUtils.isEmpty(id);
    }

    @Override
    public Team getTeam() { return host; }

    @Override
    @SuppressWarnings("unchecked")
    public void update(Game updatedGame) {
        this.id = updatedGame.id;
        this.score = updatedGame.score;
        this.matchUp = updatedGame.matchUp;
        this.created = updatedGame.created;
        this.leg = updatedGame.leg;
        this.seed = updatedGame.seed;
        this.round = updatedGame.round;
        this.homeScore = updatedGame.homeScore;
        this.awayScore = updatedGame.awayScore;
        this.ended = updatedGame.ended;
        this.canDraw = updatedGame.canDraw;
        this.sport.update(updatedGame.sport);
        this.homeEntityId = updatedGame.homeEntityId;
        this.awayEntityId = updatedGame.awayEntityId;
        this.winnerEntityId = updatedGame.winnerEntityId;
        if (updatedGame.referee.hasMajorFields())
            this.referee.update(updatedGame.referee);
        if (updatedGame.host.hasMajorFields())
            this.host.update(updatedGame.host);
        if (updatedGame.tournament.hasMajorFields())
            this.tournament.update(updatedGame.tournament);
        if (updatedGame.home.hasMajorFields() && this.home.hasSameType(updatedGame.home))
            this.home.update(updatedGame.home);
        else this.home = updatedGame.home;
        if (updatedGame.away.hasMajorFields() && this.away.hasSameType(updatedGame.away))
            this.away.update(updatedGame.away);
        else this.away = updatedGame.away;
        if (updatedGame.winner.hasMajorFields() && this.winner.hasSameType(updatedGame.winner))
            this.winner.update(updatedGame.winner);
        else this.winner = updatedGame.winner;
        if (updatedGame.event.hasMajorFields()) this.event.update(updatedGame.event);
    }

    @Override
    public int compareTo(@NonNull Game o) {
        int createdComparison = created.compareTo(o.created);

        return createdComparison != 0 ? createdComparison : id.compareTo(o.id);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Game> CREATOR = new Creator<Game>() {
        @Override
        public Game createFromParcel(Parcel in) {
            return new Game(in);
        }

        @Override
        public Game[] newArray(int size) {
            return new Game[size];
        }
    };

    public static class GsonAdapter
            implements
            JsonSerializer<Game>,
            JsonDeserializer<Game> {

        private static final String ID_KEY = "_id";
        private static final String NAME = "name";
        private static final String REF_PATH = "refPath";
        private static final String SCORE = "score";
        private static final String MATCH_UP = "matchUp";
        private static final String CREATED_KEY = "created";
        private static final String SPORT_KEY = "sport";
        private static final String REFEREE = "referee";
        private static final String EVENT = "event";
        private static final String TOURNAMENT = "tournament";
        private static final String HOST = "host";
        private static final String HOME_ENTITY_ID = "homeEntity";
        private static final String AWAY_ENTITY_ID = "awayEntity";
        private static final String WINNER_ENTITY_ID = "winnerEntity";
        private static final String HOME = "home";
        private static final String AWAY = "away";
        private static final String WINNER = "winner";
        private static final String LEG = "leg";
        private static final String SEED = "seed";
        private static final String ROUND = "round";
        private static final String HOME_SCORE = "homeScore";
        private static final String AWAY_SCORE = "awayScore";
        private static final String ENDED = "ended";
        private static final String CAN_DRAW = "canDraw";

        @Override
        public JsonElement serialize(Game src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject body = new JsonObject();
            body.addProperty(ENDED, src.ended);
            body.addProperty(REF_PATH, src.refPath);
            body.addProperty(HOME_SCORE, src.homeScore);
            body.addProperty(AWAY_SCORE, src.awayScore);
            body.addProperty(HOME, src.home.getEntity().getId());
            body.addProperty(AWAY, src.away.getEntity().getId());
            body.addProperty(REFEREE, src.referee.isEmpty() ? null : src.referee.getId());
            return body;
        }

        @Override
        public Game deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonPrimitive()) {
                return new Game(json.getAsString(), "", "", "TBD", "", "", "", "",
                        new Date(), Sport.empty(), User.empty(), Team.empty(), Event.empty(), Tournament.empty(Team.empty()),
                        Competitor.empty(), Competitor.empty(), Competitor.empty(),
                        0, 0, 0, 0, 0, false, false);
            }

            JsonObject body = json.getAsJsonObject();

            String id = ModelUtils.asString(ID_KEY, body);
            String name = ModelUtils.asString(NAME, body);
            String refPath = ModelUtils.asString(REF_PATH, body);
            String score = ModelUtils.asString(SCORE, body);
            String matchUp = ModelUtils.asString(MATCH_UP, body);
            String homeEntityId = ModelUtils.asString(HOME_ENTITY_ID, body);
            String awayEntityId = ModelUtils.asString(AWAY_ENTITY_ID, body);
            String winnerEntityId = ModelUtils.asString(WINNER_ENTITY_ID, body);
            String created = ModelUtils.asString(CREATED_KEY, body);
            String sportCode = ModelUtils.asString(SPORT_KEY, body);

            int seed = (int) ModelUtils.asFloat(SEED, body);
            int leg = (int) ModelUtils.asFloat(LEG, body);
            int round = (int) ModelUtils.asFloat(ROUND, body);
            int homeScore = (int) ModelUtils.asFloat(HOME_SCORE, body);
            int awayScore = (int) ModelUtils.asFloat(AWAY_SCORE, body);
            boolean ended = ModelUtils.asBoolean(ENDED, body);
            boolean canDraw = ModelUtils.asBoolean(CAN_DRAW, body);

            Sport sport = Config.sportFromCode(sportCode);
            User referee = context.deserialize(body.get(REFEREE), User.class);
            Team host = context.deserialize(body.get(HOST), Team.class);
            Event event = context.deserialize(body.get(EVENT), Event.class);
            Tournament tournament = context.deserialize(body.get(TOURNAMENT), Tournament.class);
            Competitor home = context.deserialize(body.get(HOME), Competitor.class);
            Competitor away = context.deserialize(body.get(AWAY), Competitor.class);
            Competitor winner = body.has(WINNER) ? context.deserialize(body.get(WINNER), Competitor.class) : Competitor.empty();

            if (referee == null) referee = User.empty();
            if (host == null) host = Team.empty();
            if (event == null) event = Event.empty();
            if (home == null) home = Competitor.empty();
            if (away == null) away = Competitor.empty();
            if (tournament == null) tournament = Tournament.empty();

            return new Game(id, name, refPath, score, matchUp, homeEntityId, awayEntityId, winnerEntityId,
                    ModelUtils.parseDate(created), sport, referee, host, event, tournament,
                    home, away, winner, seed, leg, round, homeScore, awayScore, ended, canDraw);
        }
    }
}
