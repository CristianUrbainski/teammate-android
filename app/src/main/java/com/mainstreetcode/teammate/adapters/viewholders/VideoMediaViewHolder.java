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

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.transition.Fade;
import androidx.transition.TransitionManager;
import androidx.core.view.ViewCompat;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.devbrackets.android.exomedia.ui.widget.VideoControls;
import com.devbrackets.android.exomedia.ui.widget.VideoControlsCore;
import com.devbrackets.android.exomedia.ui.widget.VideoView;
import com.mainstreetcode.teammate.R;
import com.mainstreetcode.teammate.adapters.MediaAdapter;
import com.mainstreetcode.teammate.model.Media;


public class VideoMediaViewHolder extends MediaViewHolder<VideoView> {


    @SuppressLint("ClickableViewAccessibility")
    public VideoMediaViewHolder(View itemView, MediaAdapter.MediaAdapterListener adapterListener) {
        super(itemView, adapterListener);

        //noinspection AndroidLintClickableViewAccessibility
        fullResView.setOnTouchListener(new TouchListener(itemView.getContext()));

        if (adapterListener.isFullScreen()) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) fullResView.findViewById(R.id.exomedia_video_view).getLayoutParams();
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

            VideoControls videoControls = fullResView.getVideoControls();
            if (videoControls == null || videoControls.getChildCount() < 1) return;

            View inner = videoControls.getChildAt(0);
            ViewCompat.setOnApplyWindowInsetsListener(inner, (controls, insets) -> {
                controls.setPadding(0, 0, 0, 0);
                return insets;
            });
        }
        else {
            fullResView.setVisibility(View.GONE);
            itemView.setOnClickListener(view -> adapterListener.onMediaClicked(media));
        }
    }

    @Override
    public void fullBind(Media media) {
        super.fullBind(media);

        itemView.setBackgroundResource(R.color.black);

        String videoUrl = media.getUrl();

        if (TextUtils.isEmpty(videoUrl)) return;

        fullResView.setVideoPath(videoUrl);
        fullResView.setOnPreparedListener(() -> {
            TransitionManager.beginDelayedTransition((ViewGroup) itemView, new Fade());
            fullResView.setVisibility(View.VISIBLE);
            fullResView.start();
        });
        fullResView.setOnCompletionListener(() -> {
            fullResView.setOnPreparedListener(null);
            fullResView.setVideoPath(videoUrl);
        });
    }

    @Override
    public void unBind() {
        super.unBind();
        fullResView.setVisibility(View.INVISIBLE);
        fullResView.stopPlayback();
    }

    @Override
    public int getThumbnailId() {
        return R.id.thumbnail;
    }

    @Override
    public int getFullViewId() {return R.id.video_thumbnail;}

    private class TouchListener extends GestureDetector.SimpleOnGestureListener implements View.OnTouchListener {
        GestureDetector gestureDetector;

        TouchListener(Context context) {
            gestureDetector = new GestureDetector(context, this);
        }

        @Override
        @SuppressLint("ClickableViewAccessibility")
        public boolean onTouch(View view, MotionEvent event) {
            gestureDetector.onTouchEvent(event);
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event) {
            if (fullResView == null) return true;
            VideoControlsCore videoControls = fullResView.getVideoControlsCore();

            if (videoControls != null && videoControls.isVisible()) videoControls.hide(false);
            else fullResView.showControls();

            adapterListener.onMediaClicked(media);
            return true;
        }
    }
}
