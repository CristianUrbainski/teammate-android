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

import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.mainstreetcode.teammate.R;
import com.mainstreetcode.teammate.baseclasses.BaseViewHolder;
import com.mainstreetcode.teammate.model.RemoteImage;
import com.mainstreetcode.teammate.util.ViewHolderUtil;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.tunjid.androidbootstrap.recyclerview.InteractiveAdapter;

import androidx.annotation.StringRes;

import static com.mainstreetcode.teammate.util.ViewHolderUtil.THUMBNAIL_SIZE;


public class ModelCardViewHolder<H extends RemoteImage, T extends InteractiveAdapter.AdapterListener> extends BaseViewHolder<T> {

    protected H model;

    TextView title;
    TextView subtitle;
    public ImageView thumbnail;

    ModelCardViewHolder(View itemView, T adapterListener) {
        super(itemView, adapterListener);
        title = itemView.findViewById(R.id.item_title);
        subtitle = itemView.findViewById(R.id.item_subtitle);
        thumbnail = itemView.findViewById(R.id.thumbnail);

        ViewHolderUtil.updateForegroundDrawable(itemView);
        if (isThumbnail()) thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
    }

    public void bind(H model) {
        this.model = model;
        String imageUrl = model.getImageUrl();

        if (TextUtils.isEmpty(imageUrl)) thumbnail.setImageResource(R.color.dark_grey);
        else load(imageUrl, thumbnail);
    }

    public ImageView getThumbnail() { return thumbnail; }

    public boolean isThumbnail() {return true;}

    public ModelCardViewHolder<H, T> withTitle(@StringRes int titleRes) {
        title.setText(titleRes);
        return this;
    }

    public ModelCardViewHolder<H, T> withSubTitle(@StringRes int subTitleRes) {
        subtitle.setText(subTitleRes);
        return this;
    }

    void setTitle(CharSequence title) {
        if (!TextUtils.isEmpty(title)) this.title.setText(title);
    }

    void setSubTitle(CharSequence subTitle) {
        if (!TextUtils.isEmpty(subTitle)) this.subtitle.setText(subTitle);
    }

    void load(String imageUrl, ImageView destination) {
        RequestCreator creator = Picasso.get().load(imageUrl);

        if (!isThumbnail()) creator.fit().centerCrop();
        else creator.placeholder(R.drawable.bg_image_placeholder)
                .resize(THUMBNAIL_SIZE, THUMBNAIL_SIZE)
                .centerInside();

        creator.into(destination);
    }
}
