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

package com.mainstreetcode.teammate.fragments.headless;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;

import com.mainstreetcode.teammate.R;
import com.mainstreetcode.teammate.baseclasses.MainActivityFragment;
import com.mainstreetcode.teammate.model.Team;
import com.mainstreetcode.teammate.util.ErrorHandler;
import com.theartofdev.edmodo.cropper.CropImage;
import com.tunjid.androidbootstrap.core.abstractclasses.BaseFragment;
import com.tunjid.androidbootstrap.recyclerview.InteractiveAdapter;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.MaybeEmitter;
import io.reactivex.MaybeOnSubscribe;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.BiFunction;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.M;
import static com.mainstreetcode.teammate.util.Logger.log;
import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;

/**
 * Inner fragment hosting code for interacting with image and cropping APIs
 */

public class ImageWorkerFragment extends MainActivityFragment {

    private static final int CROP_CHOOSER = 1;
    private static final int MULTIPLE_MEDIA_CHOOSER = 2;
    private static final int MEDIA_DOWNLOAD_CHOOSER = 3;

    private static final String TAG = "ImageWorkerFragment";
    private static final String IMAGE_SELECTION = "image/*";
    private static final String[] MIME_TYPES = {"image/*", "video/*"};
    private static final String[] STORAGE_PERMISSIONS = {WRITE_EXTERNAL_STORAGE};

    private boolean isPicking;

    public static ImageWorkerFragment newInstance() {
        return new ImageWorkerFragment();
    }

    public static void attach(BaseFragment host) {
        if (getInstance(host) != null) return;

        ImageWorkerFragment instance = ImageWorkerFragment.newInstance();

        host.getChildFragmentManager().beginTransaction()
                .add(instance, makeTag(host))
                .commit();
    }

    public static void requestCrop(BaseFragment host) {
        requireInstanceWithActivity(host, (instance, activity) -> {
            boolean noPermit = noStoragePermission(activity);

            if (noPermit) instance.requestPermissions(STORAGE_PERMISSIONS, CROP_CHOOSER);
            else instance.startImagePicker();
        });
    }

    public static boolean isPicking(BaseFragment host) {
        ImageWorkerFragment instance = getInstance(host);

        return instance != null && instance.isPicking;
    }

    public static void requestMultipleMedia(BaseFragment host) {
        requireInstanceWithActivity(host, (instance, activity) -> {
            boolean noPermit = noStoragePermission(activity);

            if (noPermit) instance.requestPermissions(STORAGE_PERMISSIONS, MULTIPLE_MEDIA_CHOOSER);
            else instance.startMultipleMediaPicker();
        });
    }

    public static boolean requestDownload(BaseFragment host, Team team) {
        return requireInstanceWithActivity(host, (instance, activity) -> {
            boolean noPermit = noStoragePermission(activity);
            boolean started = false;

            if (noPermit) instance.requestPermissions(STORAGE_PERMISSIONS, MEDIA_DOWNLOAD_CHOOSER);
            else started = instance.mediaViewModel.downloadMedia(team);

            if (started) instance.showSnackbar(activity.getString(R.string.media_download_started));

            return started;
        }, false);
    }

    private static boolean noStoragePermission(Activity activity) {
        return SDK_INT >= M && ContextCompat.checkSelfPermission(activity,
                WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean gotPermission = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (!gotPermission) return;

        switch (requestCode) {
            case CROP_CHOOSER:
                startImagePicker();
                break;
            case MULTIPLE_MEDIA_CHOOSER:
                startMultipleMediaPicker();
                break;
            case MEDIA_DOWNLOAD_CHOOSER:
                Fragment target = getParentFragment();
                if (target == null) return;
                if (target instanceof DownloadRequester) {
                    DownloadRequester requester = ((DownloadRequester) target);
                    Team team = requester.requestedTeam();
                    boolean started = mediaViewModel.downloadMedia(team);

                    requester.startedDownLoad(started);
                    if (started) showSnackbar(getString(R.string.media_download_started));
                }
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        boolean isCropListener = isCropListener();
        boolean isMediaListener = isMediaListener();
        boolean failed = resultCode != Activity.RESULT_OK;

        Fragment target = getParentFragment();
        if (target == null) return;

        Activity activity = getActivity();
        if (activity == null) return;

        if (failed && (requestCode == CROP_CHOOSER || requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE))
            isPicking = false;

        if (failed || (!isCropListener && !isMediaListener)) return;

        if (requestCode == CROP_CHOOSER && isCropListener) {
            CropImage.activity(data.getData())
                    .setFixAspectRatio(true)
                    .setAspectRatio(1, 1)
                    .setMinCropWindowSize(80, 80)
                    .setOutputCompressFormat(Bitmap.CompressFormat.PNG)
                    .start(activity, this);
        }
        else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && isCropListener) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            Uri resultUri = result.getUri();
            ((CropListener) target).onImageCropped(resultUri);
            isPicking = false;
        }
        else if (requestCode == MULTIPLE_MEDIA_CHOOSER && isMediaListener) {
            MediaListener listener = (MediaListener) target;
            Maybe<List<Uri>> filesMaybe = Maybe.create(new MediaQuery(data)).subscribeOn(io()).observeOn(mainThread());
            disposables.add(filesMaybe.subscribe(listener::onFilesSelected, ErrorHandler.EMPTY));
        }
    }

    private boolean isCropListener() {
        Fragment target = getParentFragment();
        return target instanceof CropListener;
    }

    private boolean isMediaListener() {
        Fragment target = getParentFragment();
        return target instanceof MediaListener;
    }

    private static String makeTag(BaseFragment host) {
        return TAG + "-" + host.getStableTag();
    }

    @Nullable
    private static ImageWorkerFragment getInstance(BaseFragment host) {
        return (ImageWorkerFragment) host.getChildFragmentManager().findFragmentByTag(makeTag(host));
    }

    private void startImagePicker() {
        isPicking = true;
        Intent intent = new Intent();
        intent.setType(IMAGE_SELECTION);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, getString(R.string.pick_picture)), CROP_CHOOSER);
    }

    private void startMultipleMediaPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_MIME_TYPES, MIME_TYPES);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

        startActivityForResult(intent, MULTIPLE_MEDIA_CHOOSER);
    }

    private static void requireInstanceWithActivity(BaseFragment host, BiConsumer<ImageWorkerFragment, Activity> biConsumer) {
        requireInstanceWithActivity(host, (instance, activity) -> {
            try {biConsumer.accept(instance, activity);}
            catch (Exception e) {log(TAG, "Could not get Instance", e);}
            return Void.TYPE;
        }, Void.TYPE);
    }

    private static <T> T requireInstanceWithActivity(BaseFragment host, BiFunction<ImageWorkerFragment, Activity, T> biFunction, T defaultValue) {
        ImageWorkerFragment instance = getInstance(host);
        if (instance == null) {
            attach(host);
            return defaultValue;
        }

        Activity activity = host.getActivity();
        if (activity == null) return defaultValue;

        try {return biFunction.apply(instance, activity);}
        catch (Exception e) {log(TAG, "Could not get Instance", e);}
        return defaultValue;
    }

    public interface CropListener {
        void onImageCropped(Uri uri);
    }

    public interface MediaListener {
        void onFilesSelected(List<Uri> uris);
    }

    public interface DownloadRequester {
        Team requestedTeam();

        void startedDownLoad(boolean started);
    }

    public interface ImagePickerListener extends InteractiveAdapter.AdapterListener {
        void onImageClick();
    }

    static class MediaQuery implements MaybeOnSubscribe<List<Uri>> {

        private Intent data;

        MediaQuery(Intent data) {
            this.data = data;
        }

        @Override
        public void subscribe(MaybeEmitter<List<Uri>> emitter) {
            emitter.onSuccess(onData());
        }

        private List<Uri> onData() {
            List<Uri> uris = new ArrayList<>();
            ClipData clip = data.getClipData();
            int count = clip == null ? 0 : clip.getItemCount();


            if (count != 0) for (int i = 0; i < count; i++) uris.add(clip.getItemAt(i).getUri());
            else if (data.getData() != null) uris.add(data.getData());

            if (data.hasExtra("uris")) uris.addAll(data.getParcelableArrayListExtra("uris"));
            // For Xiaomi Phones
            if (uris.isEmpty() && data.hasExtra("pick-result-data")) {
                uris.addAll(data.getParcelableArrayListExtra("pick-result-data"));
            }

            return uris;
        }
    }
}
