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

package com.mainstreetcode.teammate.persistence;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.core.util.Pair;

import com.mainstreetcode.teammate.App;
import com.mainstreetcode.teammate.BuildConfig;
import com.mainstreetcode.teammate.model.Chat;
import com.mainstreetcode.teammate.model.Media;
import com.mainstreetcode.teammate.persistence.entity.CompetitorEntity;
import com.mainstreetcode.teammate.persistence.entity.EventEntity;
import com.mainstreetcode.teammate.persistence.entity.GameEntity;
import com.mainstreetcode.teammate.persistence.entity.GuestEntity;
import com.mainstreetcode.teammate.persistence.entity.JoinRequestEntity;
import com.mainstreetcode.teammate.persistence.entity.RoleEntity;
import com.mainstreetcode.teammate.persistence.entity.StatEntity;
import com.mainstreetcode.teammate.persistence.entity.TeamEntity;
import com.mainstreetcode.teammate.persistence.entity.TournamentEntity;
import com.mainstreetcode.teammate.persistence.entity.UserEntity;
import com.mainstreetcode.teammate.persistence.migrations.Migration1To2;
import com.mainstreetcode.teammate.persistence.migrations.Migration2To3;
import com.mainstreetcode.teammate.persistence.migrations.Migration3To4;
import com.mainstreetcode.teammate.persistence.typeconverters.CharSequenceConverter;
import com.mainstreetcode.teammate.persistence.typeconverters.CompetitiveTypeConverter;
import com.mainstreetcode.teammate.persistence.typeconverters.CompetitorTypeConverter;
import com.mainstreetcode.teammate.persistence.typeconverters.DateTypeConverter;
import com.mainstreetcode.teammate.persistence.typeconverters.EventTypeConverter;
import com.mainstreetcode.teammate.persistence.typeconverters.GameTypeConverter;
import com.mainstreetcode.teammate.persistence.typeconverters.LatLngTypeConverter;
import com.mainstreetcode.teammate.persistence.typeconverters.PositionTypeConverter;
import com.mainstreetcode.teammate.persistence.typeconverters.SportTypeConverter;
import com.mainstreetcode.teammate.persistence.typeconverters.StatAttributesTypeConverter;
import com.mainstreetcode.teammate.persistence.typeconverters.StatTypeTypeConverter;
import com.mainstreetcode.teammate.persistence.typeconverters.TeamTypeConverter;
import com.mainstreetcode.teammate.persistence.typeconverters.TournamentStyleTypeConverter;
import com.mainstreetcode.teammate.persistence.typeconverters.TournamentTypeConverter;
import com.mainstreetcode.teammate.persistence.typeconverters.TournamentTypeTypeConverter;
import com.mainstreetcode.teammate.persistence.typeconverters.UserTypeConverter;
import com.mainstreetcode.teammate.persistence.typeconverters.VisibilityTypeConverter;
import com.mainstreetcode.teammate.util.Logger;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;

/**
 * App Database
 */

@Database(entities = {UserEntity.class, TeamEntity.class, EventEntity.class,
        RoleEntity.class, JoinRequestEntity.class, GuestEntity.class,
        TournamentEntity.class, CompetitorEntity.class, GameEntity.class, StatEntity.class,
        Chat.class, Media.class}, version = 4)

@TypeConverters({LatLngTypeConverter.class, DateTypeConverter.class, CharSequenceConverter.class,
        UserTypeConverter.class, TeamTypeConverter.class, EventTypeConverter.class,
        TournamentTypeConverter.class, CompetitorTypeConverter.class, GameTypeConverter.class,
        SportTypeConverter.class, PositionTypeConverter.class, VisibilityTypeConverter.class,
        TournamentTypeTypeConverter.class, TournamentStyleTypeConverter.class,
        StatTypeTypeConverter.class, CompetitiveTypeConverter.class,
        StatAttributesTypeConverter.class})

public abstract class AppDatabase extends RoomDatabase {

    private static final String TAG = "AppDatabase";
    private static final String PROD_DB = "database-name";
    private static final String DEV_DB = "teammate-dev-db";
    private static AppDatabase INSTANCE;

    public static AppDatabase getInstance() {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(App.getInstance(), AppDatabase.class, BuildConfig.DEV ? DEV_DB : PROD_DB)
                    .addMigrations(new Migration1To2())
                    .addMigrations(new Migration2To3())
                    .addMigrations(new Migration3To4())
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return INSTANCE;
    }

    public abstract UserDao userDao();

    public abstract TeamDao teamDao();

    public abstract RoleDao roleDao();

    public abstract GameDao gameDao();

    public abstract StatDao statDao();

    public abstract EventDao eventDao();

    public abstract MediaDao mediaDao();

    public abstract GuestDao guestDao();

    public abstract ChatDao teamChatDao();

    public abstract TournamentDao tournamentDao();

    public abstract CompetitorDao competitorDao();

    public abstract JoinRequestDao joinRequestDao();


    public PrefsDao prefsDao() {return new PrefsDao();}

    public DeviceDao deviceDao() {return new DeviceDao();}

    public ConfigDao configDao() {return new ConfigDao();}

    public TeamMemberDao teamMemberDao() {return new TeamMemberDao();}

    public Single<List<Pair<String, Integer>>> clearTables() {
        final List<Single<Pair<String, Integer>>> singles = new ArrayList<>();
        final List<Pair<String, Integer>> collector = new ArrayList<>();

        singles.add(clearTable(competitorDao()));
        singles.add(clearTable(statDao()));
        singles.add(clearTable(gameDao()));
        singles.add(clearTable(tournamentDao()));
        singles.add(clearTable(teamChatDao()));
        singles.add(clearTable(joinRequestDao()));
        singles.add(clearTable(guestDao()));
        singles.add(clearTable(eventDao()));
        singles.add(clearTable(mediaDao()));
        singles.add(clearTable(roleDao()));
        singles.add(clearTable(teamDao()));
        singles.add(clearTable(userDao()));
        singles.add(clearTable(deviceDao()));
        singles.add(clearTable(configDao()));

        return Single.concat(singles).collectInto(collector, List::add);
    }

    private Single<Pair<String, Integer>> clearTable(EntityDao<?> entityDao) {
        final String tableName = entityDao.getTableName();

        return entityDao.deleteAll()
                .map(rowsDeleted -> new Pair<>(tableName, rowsDeleted))
                .onErrorResumeNext(throwable -> {
                    Logger.log(TAG, "Error clearing table: " + tableName, throwable);
                    return Single.just(new Pair<>(tableName, 0));
                });
    }
}
