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

package com.mainstreetcode.teammate.adapters.viewholders;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.mainstreetcode.teammate.R;
import com.mainstreetcode.teammate.fragments.headless.ImageWorkerFragment;
import com.mainstreetcode.teammate.model.HeaderedModel;
import com.mainstreetcode.teammate.util.DiffWatcher;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.tunjid.androidbootstrap.recyclerview.InteractiveViewHolder;

import java.io.File;

import static com.mainstreetcode.teammate.util.ViewHolderUtil.FULL_RES_LOAD_DELAY;
import static com.mainstreetcode.teammate.util.ViewHolderUtil.THUMBNAIL_SIZE;

public class HeaderedImageViewHolder extends InteractiveViewHolder<ImageWorkerFragment.ImagePickerListener>
        implements View.OnClickListener {

    private ImageView fullRes;
    private ImageView thumbnail;

    private DiffWatcher<String> diff;

    public HeaderedImageViewHolder(View itemView, ImageWorkerFragment.ImagePickerListener listener) {
        super(itemView, listener);
        fullRes = itemView.findViewById(R.id.image_full_res);
        thumbnail = itemView.findViewById(R.id.image);
        thumbnail.setOnClickListener(this);
        diff = new DiffWatcher<>(this::getImage);
        animateHeader();
    }

    @Override
    public void onClick(View view) {adapterListener.onImageClick();}

    public ImageView getThumbnail() {return thumbnail;}

    public void bind(HeaderedModel model) {
        CharSequence url = model.getHeaderItem().getValue();
        if (TextUtils.isEmpty(url)) return;

        diff.push(url.toString());
    }

    public void unBind() { diff.stop(); }

    private void getImage(String url) {
        RequestCreator creator = getCreator(url);
        if (creator == null) return;

        creator.resize(THUMBNAIL_SIZE, THUMBNAIL_SIZE).centerInside().into(thumbnail);
        fullRes.postDelayed(new DeferredImageLoader(url), FULL_RES_LOAD_DELAY);
    }

    @Nullable
    private RequestCreator getCreator(String url) {
        if (TextUtils.isEmpty(url)) return null;

        File file = new File(url);
        Picasso picasso = Picasso.get();
        return file.exists() ? picasso.load(file) : picasso.load(url);
    }

    private void animateHeader() {
        final int endColor = ContextCompat.getColor(itemView.getContext(), R.color.black_50);
        final int startColor = Color.TRANSPARENT;

        ValueAnimator animator = ValueAnimator.ofObject(new ArgbEvaluator(), startColor, endColor);
        animator.setDuration(2000);
        animator.addUpdateListener(animation -> {
            Integer color = (Integer) animation.getAnimatedValue();
            if (color == null) return;
            thumbnail.setColorFilter(color);
            fullRes.setColorFilter(color);
        });
        animator.start();
    }

    private final class DeferredImageLoader implements Runnable, Callback {
        private final String url;

        private DeferredImageLoader(String url) {this.url = url;}

        @Override
        public void run() {
            RequestCreator delayed = getCreator(url);
            if (delayed != null) delayed.fit().centerCrop().into(fullRes, this);
        }

        @Override
        public void onSuccess() {fullRes.setVisibility(View.VISIBLE);}

        @Override
        public void onError(Exception e) {diff.restart();}
    }
}
