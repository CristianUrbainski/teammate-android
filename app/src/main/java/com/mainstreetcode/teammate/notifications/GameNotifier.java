package com.mainstreetcode.teammate.notifications;


import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import com.mainstreetcode.teammate.R;
import com.mainstreetcode.teammate.model.Game;
import com.mainstreetcode.teammate.repository.ModelRepo;
import com.mainstreetcode.teammate.repository.RepoProvider;


public class GameNotifier extends Notifier<Game> {

    private static GameNotifier INSTANCE;

    private GameNotifier() {

    }

    public static GameNotifier getInstance() {
        if (INSTANCE == null) INSTANCE = new GameNotifier();
        return INSTANCE;
    }

    @Override
    String getNotifyId() {return FeedItem.GAME;}

    @Override
    protected ModelRepo<Game> getRepository() { return RepoProvider.forModel(Game.class); }

    @TargetApi(Build.VERSION_CODES.O)
    @Override
    protected NotificationChannel[] getNotificationChannels() {
        return new NotificationChannel[]{buildNotificationChannel(FeedItem.GAME, R.string.games, R.string.games_notifier_description, NotificationManager.IMPORTANCE_DEFAULT)};
    }
}
