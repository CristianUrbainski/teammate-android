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

package com.mainstreetcode.teammate.persistence.entity;


import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;

import com.mainstreetcode.teammate.model.Competitive;
import com.mainstreetcode.teammate.model.EmptyCompetitor;
import com.mainstreetcode.teammate.model.Team;
import com.mainstreetcode.teammate.model.User;

import java.util.Date;

import static androidx.room.ForeignKey.CASCADE;

@Entity(
        tableName = "competitors",
        foreignKeys = {
                @ForeignKey(entity = TournamentEntity.class, parentColumns = "tournament_id", childColumns = "competitor_tournament", onDelete = CASCADE),
                @ForeignKey(entity = GameEntity.class, parentColumns = "game_id", childColumns = "competitor_game", onDelete = CASCADE),
        }
)
public class CompetitorEntity implements Parcelable {

    @NonNull @PrimaryKey
    @ColumnInfo(name = "competitor_id") protected String id;
    @ColumnInfo(name = "competitor_ref_path") protected String refPath;
    @ColumnInfo(name = "competitor_tournament") protected String tournamentId;
    @ColumnInfo(name = "competitor_game") protected String gameId;
    @ColumnInfo(name = "competitor_entity") protected Competitive entity;
    @ColumnInfo(name = "competitor_created") protected Date created;
    @ColumnInfo(name = "competitor_seed") protected int seed;
    @ColumnInfo(name = "competitor_accepted") protected boolean accepted;
    @ColumnInfo(name = "competitor_declined") protected boolean declined;

    public CompetitorEntity(@NonNull String id, String refPath, String tournamentId, String gameId,
                            Competitive entity, Date created,
                            int seed, boolean accepted, boolean declined) {
        this.id = id;
        this.refPath = refPath;
        this.tournamentId = tournamentId;
        this.gameId = gameId;
        this.entity = entity;
        this.created = created;
        this.seed = seed;
        this.accepted = accepted;
        this.declined = declined;
    }

    protected CompetitorEntity(Parcel in) {
        id = in.readString();
        refPath = in.readString();
        tournamentId = in.readString();
        gameId = in.readString();
        entity = fromParcel(refPath, in);
        created = new Date(in.readLong());
        seed = in.readInt();
        accepted = in.readByte() != 0x00;
        declined = in.readByte() != 0x00;
    }

    public String getId() { return id; }

    public String getRefPath() { return refPath; }

    public String getSeedText() {return seed > -1 ? String.valueOf(seed + 1) : "";}

    public String getTournamentId() { return tournamentId; }

    public String getGameId() { return gameId; }

    public Competitive getEntity() { return entity; }

    public Date getCreated() { return created; }

    public int getSeed() { return seed; }

    public void setSeed(int seed) { this.seed = seed; }

    public boolean isAccepted() { return accepted; }

    public boolean isDeclined() { return declined; }

    public boolean hasNotResponded() { return !isAccepted() && !isDeclined();}

    public void accept() { declined = !(accepted = true); }

    public void decline() { accepted = !(declined = true); }

    private static Competitive fromParcel(String refPath, Parcel in) {
        switch (refPath) {
            case User.COMPETITOR_TYPE:
                return (User) in.readValue(User.class.getClassLoader());
            case Team.COMPETITOR_TYPE:
                return (Team) in.readValue(Team.class.getClassLoader());
            default:
                return (EmptyCompetitor) in.readValue(EmptyCompetitor.class.getClassLoader());
        }
    }

    private static void writeToParcel(Competitive competitive, Parcel dest) {
        dest.writeValue(competitive);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CompetitorEntity)) return false;

        CompetitorEntity that = (CompetitorEntity) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(refPath);
        dest.writeString(tournamentId);
        dest.writeString(gameId);
        writeToParcel(entity, dest);
        dest.writeLong(created.getTime());
        dest.writeInt(seed);
        dest.writeByte((byte) (accepted ? 0x01 : 0x00));
        dest.writeByte((byte) (declined ? 0x01 : 0x00));
    }

    public static final Creator<CompetitorEntity> CREATOR = new Creator<CompetitorEntity>() {
        @Override
        public CompetitorEntity createFromParcel(Parcel in) {
            return new CompetitorEntity(in);
        }

        @Override
        public CompetitorEntity[] newArray(int size) {
            return new CompetitorEntity[size];
        }
    };
}
