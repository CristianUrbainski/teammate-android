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

import androidx.annotation.Nullable;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mainstreetcode.teammate.App;
import com.mainstreetcode.teammate.R;
import com.mainstreetcode.teammate.rest.TeammateService;
import com.mainstreetcode.teammate.util.Logger;
import com.mainstreetcode.teammate.util.ModelUtils;

import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Objects;

import io.reactivex.exceptions.CompositeException;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import retrofit2.HttpException;

import static com.tunjid.androidbootstrap.functions.collections.Lists.findFirst;

/**
 * Messages from the {@link com.mainstreetcode.teammate.rest.TeammateApi}
 */

public class Message {

    private static final String UNKNOWN_ERROR_CODE = "unknown.error";
    private static final String MAX_STORAGE_ERROR_CODE = "maximum.storage.error";
    private static final String ILLEGAL_TEAM_MEMBER_ERROR_CODE = "illegal.team.member.error";
    private static final String UNAUTHENTICATED_USER_ERROR_CODE = "unauthenticated.user.error";
    private static final String INVALID_OBJECT_REFERENCE_ERROR_CODE = "invalid.object.reference.error";

    private final String message;
    private final String errorCode;

    public Message(String message) {
        this.message = message;
        this.errorCode = UNKNOWN_ERROR_CODE;
    }

    private Message(String message, String errorCode) {
        this.message = message;
        this.errorCode = errorCode;
    }

    public Message(HttpException exception) {
        Message parsed = getMessage(exception);
        this.message = parsed.message;
        this.errorCode = parsed.errorCode;
    }

    public String getMessage() {
        return message;
    }

    @Nullable
    public static Message fromThrowable(Throwable throwable) {
        if (throwable instanceof HttpException) {
            return new Message((HttpException) throwable);
        }
        else if (throwable instanceof CompositeException) {
            CompositeException compositeException = (CompositeException) throwable;
            HttpException httpException = findFirst(compositeException.getExceptions(), HttpException.class);
            if (httpException != null) return new Message(httpException);
        }
        return null;
    }

    public boolean isValidModel() { return !isIllegalTeamMember() && !isInvalidObject(); }

    public boolean isInvalidObject() { return INVALID_OBJECT_REFERENCE_ERROR_CODE.equals(errorCode);}

    public boolean isIllegalTeamMember() { return ILLEGAL_TEAM_MEMBER_ERROR_CODE.equals(errorCode);}

    public boolean isUnauthorizedUser() {return UNAUTHENTICATED_USER_ERROR_CODE.equals(errorCode);}

    public boolean isAtMaxStorage() {return MAX_STORAGE_ERROR_CODE.equals(errorCode);}

    private Message getMessage(HttpException throwable) {
        try {
            ResponseBody errorBody = throwable.response().errorBody();
            if (errorBody != null) {
                BufferedSource source = errorBody.source();
                source.request(Long.MAX_VALUE); // request the entire body.
                Buffer buffer = source.buffer();
                // clone buffer before reading from it
                String json = buffer.clone().readString(Charset.forName("UTF-8"));
                return TeammateService.getGson().fromJson(json, Message.class);
            }
        }
        catch (Exception e) {
            Logger.log("ApiMessage", "Unable to read API error message", e);
        }
        return new Message(App.getInstance().getString(R.string.error_default));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message other = (Message) o;
        return Objects.equals(message, other.message) &&
                Objects.equals(errorCode, other.errorCode);
    }

    @Override public int hashCode() {
        return Objects.hash(message, errorCode);
    }

    public static class GsonAdapter implements com.google.gson.JsonDeserializer<Message> {

        private static final String MESSAGE_KEY = "message";
        private static final String ERROR_CODE_KEY = "errorCode";

        @Override
        public Message deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject messageJson = json.getAsJsonObject();
            String message = messageJson.has(MESSAGE_KEY) ? messageJson.get(MESSAGE_KEY).getAsString() : "Sorry, an error occurred";
            String errorCode = ModelUtils.asString(ERROR_CODE_KEY, messageJson);

            return new Message(message, errorCode);
        }

    }
}
