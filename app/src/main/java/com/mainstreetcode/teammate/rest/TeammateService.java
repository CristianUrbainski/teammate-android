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

package com.mainstreetcode.teammate.rest;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;

import com.facebook.login.LoginResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;
import com.mainstreetcode.teammate.App;
import com.mainstreetcode.teammate.BuildConfig;
import com.mainstreetcode.teammate.R;
import com.mainstreetcode.teammate.model.BlockedUser;
import com.mainstreetcode.teammate.model.Chat;
import com.mainstreetcode.teammate.model.Competitor;
import com.mainstreetcode.teammate.model.Config;
import com.mainstreetcode.teammate.model.Device;
import com.mainstreetcode.teammate.model.Event;
import com.mainstreetcode.teammate.model.EventSearchRequest;
import com.mainstreetcode.teammate.model.Game;
import com.mainstreetcode.teammate.model.Guest;
import com.mainstreetcode.teammate.model.HeadToHead;
import com.mainstreetcode.teammate.model.JoinRequest;
import com.mainstreetcode.teammate.model.Media;
import com.mainstreetcode.teammate.model.Message;
import com.mainstreetcode.teammate.model.Role;
import com.mainstreetcode.teammate.model.Row;
import com.mainstreetcode.teammate.model.Standings;
import com.mainstreetcode.teammate.model.Stat;
import com.mainstreetcode.teammate.model.StatAggregate;
import com.mainstreetcode.teammate.model.StatRank;
import com.mainstreetcode.teammate.model.Team;
import com.mainstreetcode.teammate.model.TeamMember;
import com.mainstreetcode.teammate.model.Tournament;
import com.mainstreetcode.teammate.model.User;
import com.mainstreetcode.teammate.model.enums.AndroidVariant;
import com.mainstreetcode.teammate.model.enums.BlockReason;
import com.mainstreetcode.teammate.model.enums.Position;
import com.mainstreetcode.teammate.model.enums.Sport;
import com.mainstreetcode.teammate.model.enums.StatAttribute;
import com.mainstreetcode.teammate.model.enums.StatType;
import com.mainstreetcode.teammate.model.enums.TournamentStyle;
import com.mainstreetcode.teammate.model.enums.TournamentType;
import com.mainstreetcode.teammate.model.enums.Visibility;
import com.mainstreetcode.teammate.notifications.FeedItem;
import com.mainstreetcode.teammate.util.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import io.reactivex.schedulers.Schedulers;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

import static android.text.TextUtils.isEmpty;

/**
 * Teammates RESTful API
 */

public class TeammateService {

    public static final String API_BASE_URL = BuildConfig.DEV ? "https://dev.teammate.app/" : "https://teammate.app/";
    public static final String SESSION_PREFS = "session.prefs";
    private static final String TAG = "API Service";

    public static final String SESSION_COOKIE = "linesman.id";
    private static final Gson GSON = getGson();

    private static TeammateApi api;
    private static OkHttpClient httpClient;

    public static TeammateApi getApiInstance() {
        if (api == null) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.NONE);

            OkHttpClient.Builder builder = new OkHttpClient.Builder().cookieJar(new SessionCookieJar());
            if (BuildConfig.DEV) builder.addInterceptor(loggingInterceptor);

            httpClient = builder.build();

            api = new Retrofit.Builder()
                    .baseUrl(API_BASE_URL)
                    .client(httpClient)
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                    .addConverterFactory(GsonConverterFactory.create(GSON))
                    .build()
                    .create(TeammateApi.class);
        }

        return api;
    }

    public static Gson getGson() {

        return new GsonBuilder()
                .registerTypeAdapter(Row.class, new Row.GsonAdapter())
                .registerTypeAdapter(Team.class, new Team.GsonAdapter())
                .registerTypeAdapter(User.class, new User.GsonAdapter())
                .registerTypeAdapter(Role.class, new Role.GsonAdapter())
                .registerTypeAdapter(Game.class, new Game.GsonAdapter())
                .registerTypeAdapter(Stat.class, new Stat.GsonAdapter())
                .registerTypeAdapter(Chat.class, new Chat.GsonAdapter())
                .registerTypeAdapter(Event.class, new Event.GsonAdapter())
                .registerTypeAdapter(Media.class, new Media.GsonAdapter())
                .registerTypeAdapter(Guest.class, new Guest.GsonAdapter())
                .registerTypeAdapter(Sport.class, new Sport.GsonAdapter())
                .registerTypeAdapter(Config.class, new Config.GsonAdapter())
                .registerTypeAdapter(Device.class, new Device.GsonAdapter())
                .registerTypeAdapter(Message.class, new Message.GsonAdapter())
                .registerTypeAdapter(FeedItem.class, new FeedItem.GsonAdapter())
                .registerTypeAdapter(Position.class, new Position.GsonAdapter())
                .registerTypeAdapter(StatRank.class, new StatRank.GsonAdapter())
                .registerTypeAdapter(StatType.class, new StatType.GsonAdapter())
                .registerTypeAdapter(Standings.class, new Standings.GsonAdapter())
                .registerTypeAdapter(Tournament.class, new Tournament.GsonAdapter())
                .registerTypeAdapter(Competitor.class, new Competitor.GsonAdapter())
                .registerTypeAdapter(Visibility.class, new Visibility.GsonAdapter())
                .registerTypeAdapter(TeamMember.class, new TeamMember.GsonAdapter())
                .registerTypeAdapter(BlockReason.class, new BlockReason.GsonAdapter())
                .registerTypeAdapter(JoinRequest.class, new JoinRequest.GsonAdapter())
                .registerTypeAdapter(BlockedUser.class, new BlockedUser.GsonAdapter())
                .registerTypeAdapter(StatAttribute.class, new StatAttribute.GsonAdapter())
                .registerTypeAdapter(AndroidVariant.class, new AndroidVariant.GsonAdapter())
                .registerTypeAdapter(TournamentType.class, new TournamentType.GsonAdapter())
                .registerTypeAdapter(TournamentStyle.class, new TournamentStyle.GsonAdapter())
                .registerTypeAdapter(HeadToHead.Result.class, new HeadToHead.Result.GsonAdapter())
                .registerTypeAdapter(HeadToHead.Request.class, new HeadToHead.Request.GsonAdapter())
                .registerTypeAdapter(EventSearchRequest.class, new EventSearchRequest.GsonAdapter())
                .registerTypeAdapter(StatAggregate.Result.class, new StatAggregate.Result.GsonAdapter())
                .registerTypeAdapter(StatAggregate.Request.class, new StatAggregate.Request.GsonAdapter())
                .registerTypeAdapter(LoginResult.class, (JsonSerializer<LoginResult>) (src, typeOfSrc, context) -> {
                    JsonObject body = new JsonObject();
                    body.addProperty("access_token", src.getAccessToken().getToken());
                    return body;
                })
                .create();
    }

    public static OkHttpClient getHttpClient() {
        if (api == null) getApiInstance();

        return httpClient;
    }

    @SuppressWarnings("unused")
    private static void assignSSLSocketFactory(OkHttpClient.Builder builder) {
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            InputStream stream = App.Companion.getInstance().getResources().openRawResource(BuildConfig.DEV ? R.raw.dev : R.raw.prod);

            Certificate certificate = certificateFactory.generateCertificate(stream);
            stream.close();

            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", certificate);

            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(tmfAlgorithm);
            trustManagerFactory.init(keyStore);

            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

            if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
            }

            X509TrustManager trustManager = (X509TrustManager) trustManagers[0];

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

            builder.sslSocketFactory(sslContext.getSocketFactory(), trustManager);
        }
        catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException | KeyManagementException | IllegalStateException e) {
            Logger.log(TAG, "Unable to parse SSL cert", e);
        }
    }

    private static class SessionCookieJar implements CookieJar {
        private static final String SIGN_IN_PATH = "/api/signIn";
        private static final String SIGN_UP_PATH = "/api/signUp";

        App app = App.Companion.getInstance();

        @Override
        public void saveFromResponse(@NonNull HttpUrl url, @NonNull List<Cookie> cookies) {
            String path = url.encodedPath();
            if (!path.equals(SIGN_UP_PATH) && !path.equals(SIGN_IN_PATH)) return;

            for (Cookie cookie : cookies) {
                if (cookie.name().equals(SESSION_COOKIE)) {
                    app.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)
                            .edit().putString(SESSION_COOKIE, cookie.toString()).apply();
                    break;
                }
            }
        }

        @Override
        public List<Cookie> loadForRequest(@NonNull HttpUrl url) {
            List<Cookie> cookies = new ArrayList<>();

            SharedPreferences preferences = app
                    .getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE);

            String serializedCookie = preferences.getString(SESSION_COOKIE, "");

            if (!isEmpty(serializedCookie)) cookies.add(Cookie.parse(url, serializedCookie));

            return cookies;
        }
    }
}
