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

package com.mainstreetcode.teammate;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import com.mainstreetcode.teammate.model.Media;
import com.mainstreetcode.teammate.model.Message;
import com.mainstreetcode.teammate.model.Team;
import com.mainstreetcode.teammate.model.User;
import com.mainstreetcode.teammate.notifications.MediaNotifier;
import com.mainstreetcode.teammate.notifications.NotifierProvider;
import com.mainstreetcode.teammate.repository.ModelRepo;
import com.mainstreetcode.teammate.repository.RepoProvider;
import com.mainstreetcode.teammate.util.ErrorHandler;
import com.mainstreetcode.teammate.util.ModelUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import androidx.annotation.Nullable;
import io.reactivex.Flowable;

public class MediaTransferIntentService extends IntentService {

    private static final String ACTION_UPLOAD = "com.mainstreetcode.teammates.action.UPLOAD";
    private static final String ACTION_DOWNLOAD = "com.mainstreetcode.teammates.action.DOWNLOAD";

    private static final String EXTRA_USER = "com.mainstreetcode.teammates.extra.user";
    private static final String EXTRA_TEAM = "com.mainstreetcode.teammates.extra.team";
    private static final String EXTRA_URIS = "com.mainstreetcode.teammates.extra.uris";
    private static final String EXTRA_MEDIA = "com.mainstreetcode.teammates.extra.media";
    private static final String APP_ROOT_DIR = "teammate";

    private static UploadStats uploadStats;
    private static DownloadStats downloadStats;

    public MediaTransferIntentService() {
        super("MediaUploadIntentService");
    }

    public static void startActionUpload(Context context, User user, Team team, List<Uri> mediaUris) {
        Intent intent = new Intent(context, MediaTransferIntentService.class);
        intent.setAction(ACTION_UPLOAD);
        intent.putExtra(EXTRA_USER, user);
        intent.putExtra(EXTRA_TEAM, team);
        intent.putParcelableArrayListExtra(EXTRA_URIS, new ArrayList<>(mediaUris));
        context.startService(intent);
    }

    @SuppressLint("CheckResult")
    public static void startActionDownload(Context context, List<Media> mediaList) {
        Intent intent = new Intent(context, MediaTransferIntentService.class);
        intent.setAction(ACTION_DOWNLOAD);

        //noinspection ResultOfMethodCallIgnored
        Flowable.fromIterable(mediaList)
                .filter(media -> !TextUtils.isEmpty(media.getUrl()))
                .collectInto(new ArrayList<Media>(mediaList.size()), List::add)
                .subscribe(media -> {
                    intent.putParcelableArrayListExtra(EXTRA_MEDIA, media);
                    context.startService(intent);
                }, ErrorHandler.EMPTY);
    }

    public static UploadStats getUploadStats() {
        return uploadStats;
    }

    public static DownloadStats getDownloadStats() {
        return downloadStats;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) return;
        final String action = intent.getAction();
        switch (action == null ? "" : action) {
            case ACTION_UPLOAD: {
                final User user = intent.getParcelableExtra(EXTRA_USER);
                final Team team = intent.getParcelableExtra(EXTRA_TEAM);
                final List<Uri> uris = intent.getParcelableArrayListExtra(EXTRA_URIS);
                handleActionUpload(user, team, uris);
                break;
            }
            case ACTION_DOWNLOAD: {
                handleActionDownload(intent.getParcelableArrayListExtra(EXTRA_MEDIA));
                break;
            }
        }
    }

    /**
     * Handle action Upload in the provided background thread with the provided
     * parameters.
     */
    private void handleActionUpload(User user, Team team, List<Uri> mediaUris) {
        if (uploadStats == null) uploadStats = new UploadStats();
        for (Uri uri : mediaUris) uploadStats.enqueue(Media.Companion.fromUri(user, team, uri));
    }

    private void handleActionDownload(List<Media> mediaList) {
        if (downloadStats == null) downloadStats = new DownloadStats();
        for (Media media : mediaList) downloadStats.enqueue(media);
    }

    public static class UploadStats {

        private int numErrors = 0;
        private int numToUpload = 0;
        private int numAttempted = 0;
        private boolean isOnGoing;
        private String maxStorageMessage = "";

        private final Queue<Media> uploadQueue = new ConcurrentLinkedQueue<>();
        private final ModelRepo<Media> repository = RepoProvider.Companion.forModel(Media.class);

        void enqueue(Media media) {
            numToUpload++;
            uploadQueue.add(media);

            if (uploadQueue.isEmpty()) numErrors = 0;
            if (!isOnGoing) invoke();
        }

        @SuppressLint("CheckResult")
        private void invoke() {
            if (uploadQueue.isEmpty()) {
                numToUpload = 0;
                numAttempted = 0;
                isOnGoing = false;
                return;
            }

            numAttempted++;
            isOnGoing = true;

            //noinspection ResultOfMethodCallIgnored
            repository.createOrUpdate(uploadQueue.remove())
                    .doOnSuccess(media -> maxStorageMessage = "")
                    .doOnError(this::onError)
                    .doFinally(this::invoke)
                    .subscribe(ignored -> {}, ErrorHandler.EMPTY);
        }

        public int getNumErrors() {return numErrors;}

        public int getNumToUpload() {return numToUpload;}

        public int getNumAttempted() {return numAttempted;}

        public boolean isComplete() {return uploadQueue.isEmpty();}

        public boolean isAtMaxStorage() {return !TextUtils.isEmpty(maxStorageMessage);}

        public String getMaxStorageMessage() {return maxStorageMessage;}

        @Override
        public String toString() {
            return "Queue size: " + uploadQueue.size() +
                    ", numErrors : " + numErrors +
                    ", numToUpload : " + numToUpload +
                    ", numAttempted : " + numAttempted;
        }

        private void onError(Throwable throwable) {
            numErrors++;
            Message message = Message.fromThrowable(throwable);
            if (message == null || !message.isAtMaxStorage()) return;

            maxStorageMessage = message.getMessage();
        }
    }

    public static class DownloadStats {

        private Set<Long> downloadQueue = new HashSet<>();

        void enqueue(Media media) {
            if (!isExternalStorageWritable()) return;

            App app = App.getInstance();
            DownloadManager downloadManager = (DownloadManager) app.getSystemService(DOWNLOAD_SERVICE);
            if (downloadManager == null) return;

            String url = media.getUrl();

            File destination = getDownloadDestination(media);
            if (destination == null) return;

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle(getMediaTitle(media, app));
            request.allowScanningByMediaScanner();

            downloadQueue.add(downloadManager.enqueue(request
                    .setDestinationUri(Uri.fromFile(destination))
                    .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                    .setAllowedOverRoaming(false)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)));
        }

        void onDownloadComplete(long id) {
            if (downloadQueue.remove(id) && downloadQueue.isEmpty())
                NotifierProvider.Companion.forNotifier(MediaNotifier.class).notifyDownloadComplete();
        }

        private String getMediaTitle(Media media, App app) {
            return app.getString(R.string.media_download_title, media.getTeam().getName(), ModelUtils.prettyPrinter.format(media.getCreated()));
        }

        private boolean isExternalStorageWritable() {
            String state = Environment.getExternalStorageState();
            return Environment.MEDIA_MOUNTED.equals(state);
        }

        @Nullable
        private String getExtension(String url) {
            return MimeTypeMap.getFileExtensionFromUrl(url);
        }

        @Nullable
        private File getDownloadDestination(Media media) {
            String url = media.getUrl();
            String fileName = media.getId();
            String extension = getExtension(url);
            if (extension != null) fileName += "." + extension;

            File directory = new File(Environment.getExternalStorageDirectory() + File.separator + APP_ROOT_DIR);

            boolean canWrite = directory.exists() || directory.mkdir();
            if (!canWrite) return null;

            File output = new File(directory, fileName);
            if (output.exists()) return null;

            return output;
        }
    }
}
