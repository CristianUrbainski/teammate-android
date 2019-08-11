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

import android.annotation.SuppressLint;
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
import com.mainstreetcode.teammate.model.enums.BlockReason;
import com.mainstreetcode.teammate.util.IdCache;
import com.mainstreetcode.teammate.util.ModelUtils;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static com.mainstreetcode.teammate.util.ModelUtils.EMPTY_STRING;

@SuppressLint("ParcelCreator")
public class BlockedUser implements UserHost,
        TeamHost,
        Model<BlockedUser>,
        HeaderedModel<BlockedUser>,
        ListableModel<BlockedUser> {

    private String id;
    private final User user;
    private final Team team;
    private final BlockReason reason;
    private final Date created;

    @Ignore private static final IdCache holder = IdCache.cache(3);

    private BlockedUser(String id, User user, Team team, BlockReason reason, Date created) {
        this.id = id;
        this.user = user;
        this.team = team;
        this.reason = reason;
        this.created = created;
    }

    public static BlockedUser block(User user, Team team, BlockReason reason) {
        return new BlockedUser("", user, team, reason, new Date());
    }

    @Override
    public String getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Team getTeam() {
        return team;
    }

    public BlockReason getReason() {
        return reason;
    }

    public Date getCreated() {
        return created;
    }

    @Override
    public Item<BlockedUser> getHeaderItem() {
        return Item.Companion.text(EMPTY_STRING, 0, Item.IMAGE, R.string.profile_picture, Item.Companion.nullToEmpty(user.getImageUrl()), Item.Companion::ignore, this);
    }

    @Override
    public List<Item<BlockedUser>> asItems() {
        User user = getUser();
        return Arrays.asList(
                Item.Companion.text(holder.get(0), 0, Item.INPUT, R.string.first_name, user::getFirstName, user::setFirstName, this),
                Item.Companion.text(holder.get(1), 1, Item.INPUT, R.string.last_name, user::getLastName, user::setLastName, this),
                Item.Companion.text(holder.get(2), 2, Item.ROLE, R.string.team_role, reason::getCode, Item.Companion::ignore, this)
                        .textTransformer(value -> Config.reasonFromCode(value.toString()).getName())
        );
    }

    @Override
    public void update(BlockedUser updated) {
        this.id = updated.id;
        this.reason.update(updated.reason);
        if (updated.user.hasMajorFields()) this.user.update(updated.user);
        if (updated.team.hasMajorFields()) this.team.update(updated.team);
    }

    @Override
    public boolean isEmpty() {
        return TextUtils.isEmpty(id);
    }

    @Override
    public String getImageUrl() {
        return user.getImageUrl();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlockedUser)) return false;
        BlockedUser that = (BlockedUser) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id);
    }

    @Override
    public int compareTo(@NonNull BlockedUser o) {
        return 0;
    }

    public static class GsonAdapter implements
            JsonSerializer<BlockedUser>,
            JsonDeserializer<BlockedUser> {

        private static final String UID_KEY = "_id";
        private static final String USER_KEY = "user";
        private static final String TEAM_KEY = "team";
        private static final String REASON_KEY = "reason";
        private static final String CREATED_KEY = "created";

        @Override
        public JsonElement serialize(BlockedUser src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject serialized = new JsonObject();

            if (!src.isEmpty()) serialized.addProperty(UID_KEY, src.getId());
            serialized.addProperty(REASON_KEY, src.reason.getCode());
            serialized.addProperty(USER_KEY, src.user.getId());
            serialized.addProperty(TEAM_KEY, src.team.getId());

            return serialized;
        }

        @Override
        public BlockedUser deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonPrimitive()) {
                return new BlockedUser(json.getAsString(), User.empty(), Team.empty(), BlockReason.Companion.empty(), new Date());
            }

            JsonObject jsonObject = json.getAsJsonObject();

            String id = ModelUtils.asString(UID_KEY, jsonObject);
            String reasonCode = ModelUtils.asString(REASON_KEY, jsonObject);
            BlockReason reason = Config.reasonFromCode(reasonCode);
            Team team = context.deserialize(jsonObject.get(TEAM_KEY), Team.class);
            User user = context.deserialize(jsonObject.get(USER_KEY), User.class);
            Date created = ModelUtils.parseDate(ModelUtils.asString(CREATED_KEY, jsonObject));

            return new BlockedUser(id, user, team, reason, created);
        }
    }
}
