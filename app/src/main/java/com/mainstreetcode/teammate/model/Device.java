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
import com.mainstreetcode.teammate.util.ModelUtils;

import java.lang.reflect.Type;

@SuppressLint("ParcelCreator")
public class Device implements Model<Device> {

    private String id = "";
    private String fcmToken = "";
    private final String operatingSystem = "Android";

    public static Device empty() { return  new Device(); }

    public static Device withFcmToken(String fcmToken) {
        Device device = new Device();
        device.fcmToken = fcmToken;
        return device;
    }

    private Device() {}

    public Device(String id) { this.id = id; }

    public String getFcmToken() {
        return fcmToken;
    }

    public Device setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
        return this;
    }

    @Override
    public void update(Device updated) {
        this.id = updated.id;
        this.fcmToken = updated.fcmToken;
    }

    @Override
    public int compareTo(@NonNull Device o) {
        return id.compareTo(o.id);
    }

    @Override
    public boolean isEmpty() { return TextUtils.isEmpty(id); }

    @Override
    public String getId() { return id; }

    @Override
    public String getImageUrl() { return ""; }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) { }

    public static class GsonAdapter
            implements
            JsonSerializer<Device>,
            JsonDeserializer<Device> {

        private static final String ID_KEY = "_id";
        private static final String FCM_TOKEN_KEY = "fcmToken";
        private static final String OPERATING_SYSTEM_KEY = "operatingSystem";

        @Override
        public JsonElement serialize(Device src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject serialized = new JsonObject();

            serialized.addProperty(FCM_TOKEN_KEY, src.fcmToken);
            serialized.addProperty(OPERATING_SYSTEM_KEY, src.operatingSystem);

            return serialized;
        }

        @Override
        public Device deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

            JsonObject deviceJson = json.getAsJsonObject();

            String id = ModelUtils.asString(ID_KEY, deviceJson);
            String fcmToken = ModelUtils.asString(FCM_TOKEN_KEY, deviceJson);

            Device device = new Device(id);
            device.setFcmToken(fcmToken);

            return device;
        }
    }
}
