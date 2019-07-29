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

import android.content.Context;

import androidx.constraintlayout.widget.ConstraintLayout;

import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.mainstreetcode.teammate.R;
import com.mainstreetcode.teammate.adapters.TeamChatAdapter;
import com.mainstreetcode.teammate.model.Chat;
import com.mainstreetcode.teammate.model.Team;
import com.squareup.picasso.Picasso;
import com.tunjid.androidbootstrap.recyclerview.InteractiveViewHolder;
import com.tunjid.androidbootstrap.view.util.ViewUtil;

/**
 * Viewholder for a {@link Team}
 */
public class TeamChatViewHolder extends InteractiveViewHolder<TeamChatAdapter.ChatAdapterListener> {

    private Chat item;
    private View space;
    private ImageView image;
    private TextView today;
    private TextView content;
    private TextView details;


    public TeamChatViewHolder(View itemView, TeamChatAdapter.ChatAdapterListener listener) {
        super(itemView, listener);
        image = itemView.findViewById(R.id.image);
        space = itemView.findViewById(R.id.mid_guide);
        today = itemView.findViewById(R.id.today);
        details = itemView.findViewById(R.id.details);
        content = itemView.findViewById(R.id.content);
        content.setOnClickListener(view -> adapterListener.onChatClicked(item));
    }

    public void bind(Chat item, boolean isSignedInUser, boolean showDetails, boolean showPicture,
                     boolean isFirstMessageToday) {

        this.item = item;
        Context context = itemView.getContext();

        content.setText(item.getContent());
        itemView.setAlpha(item.isEmpty() ? 0.6F : 1F);
        image.setVisibility(showPicture ? View.VISIBLE : View.GONE);
        space.setVisibility(showPicture ? View.VISIBLE : View.GONE);
        today.setVisibility(isFirstMessageToday ? View.VISIBLE : View.GONE);
        details.setVisibility(showDetails || item.isEmpty() ? View.VISIBLE : View.GONE);
        content.setBackgroundResource(isSignedInUser ? R.drawable.bg_chat_box : R.drawable.bg_chat_box_alt);

        if (!isSignedInUser && !TextUtils.isEmpty(item.getImageUrl()))
            Picasso.get().load(item.getImageUrl()).fit().centerCrop().into(image);

        if (item.isEmpty()) details.setText(R.string.chat_sending);
        else if (showDetails) details.setText(getDetailsText(item, isSignedInUser, context));

        if (isFirstMessageToday)
            ViewUtil.getLayoutParams(content).topMargin = context.getResources().getDimensionPixelSize(R.dimen.single_margin);

        ConstraintLayout.LayoutParams detailsParams = (ConstraintLayout.LayoutParams) details.getLayoutParams();
        ConstraintLayout.LayoutParams leftParams = (ConstraintLayout.LayoutParams) image.getLayoutParams();

        leftParams.horizontalBias = detailsParams.horizontalBias = isSignedInUser ? 1F : 0F;
    }

    private CharSequence getDetailsText(Chat item, boolean isSignedInUser, Context context) {
        CharSequence date = item.getCreatedDate();
        CharSequence firstName = item.getUser().getFirstName();
        return isSignedInUser ? date : context.getString(R.string.chat_details, firstName, date);
    }
}
