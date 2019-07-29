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

import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.mainstreetcode.teammate.R;
import com.mainstreetcode.teammate.adapters.GameAdapter;
import com.mainstreetcode.teammate.baseclasses.BaseViewHolder;
import com.mainstreetcode.teammate.model.Competitor;
import com.mainstreetcode.teammate.model.Game;
import com.mainstreetcode.teammate.util.AppBarListener;
import com.mainstreetcode.teammate.util.ViewHolderUtil;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.mainstreetcode.teammate.util.ViewHolderUtil.THUMBNAIL_SIZE;


public class GameViewHolder extends BaseViewHolder<GameAdapter.AdapterListener> {

    private static final float ONE_F = 1F;

    private final int animationPadding;

    private Game model;
    private View highlight;
    private TextView ended;
    private TextView score;
    private TextView date;
    private TextView homeText;
    private TextView awayText;
    private CircleImageView homeThumbnail;
    private CircleImageView awayThumbnail;

    public GameViewHolder(View itemView, GameAdapter.AdapterListener adapterListener) {
        super(itemView, adapterListener);
        date = itemView.findViewById(R.id.date);
        score = itemView.findViewById(R.id.score);
        ended = itemView.findViewById(R.id.ended);
        homeText = itemView.findViewById(R.id.home);
        awayText = itemView.findViewById(R.id.away);
        highlight = itemView.findViewById(R.id.highlight);
        homeThumbnail = itemView.findViewById(R.id.home_thumbnail);
        awayThumbnail = itemView.findViewById(R.id.away_thumbnail);

        animationPadding = itemView.getResources().getDimensionPixelSize(R.dimen.quarter_margin);
        itemView.setOnClickListener(view -> adapterListener.onGameClicked(model));
        ViewHolderUtil.updateForegroundDrawable(itemView);
    }

    public void bind(Game model) {
        this.model = model;
        Competitor home = model.getHome();
        Competitor away = model.getAway();
        Competitor winner = model.getWinner();

        date.setText(model.getDate());
        score.setText(model.getScore());
        homeText.setText(home.getName());
        awayText.setText(away.getName());
        ended.setVisibility(model.isEnded() ? View.VISIBLE : View.INVISIBLE);
        highlight.setVisibility(model.isEnded() ? View.INVISIBLE : View.VISIBLE);
        homeText.setTypeface(homeText.getTypeface(), home.equals(winner) ? Typeface.BOLD : Typeface.NORMAL);
        awayText.setTypeface(awayText.getTypeface(), away.equals(winner) ? Typeface.BOLD : Typeface.NORMAL);

        tintScore();
        String homeUrl = home.getImageUrl();
        String awayUrl = away.getImageUrl();

        if (TextUtils.isEmpty(homeUrl)) homeThumbnail.setImageResource(R.color.dark_grey);
        else Picasso.get().load(homeUrl)
                .resize(THUMBNAIL_SIZE, THUMBNAIL_SIZE).centerInside().into(homeThumbnail);

        if (TextUtils.isEmpty(awayUrl)) awayThumbnail.setImageResource(R.color.dark_grey);
        else Picasso.get().load(awayUrl).
                resize(THUMBNAIL_SIZE, THUMBNAIL_SIZE).centerInside().into(awayThumbnail);
    }

    public void animate(AppBarListener.OffsetProps props) {
        if (props.appBarUnmeasured()) return;

        int offset = props.getOffset();
        float fraction = props.getFraction();
        float scale = ONE_F - (fraction * 1.8F);
        int drop = Math.min(offset, (int) (homeText.getY() - homeThumbnail.getY()) - animationPadding);

        if (scale < 0 || scale > 1) return;

        homeText.setAlpha(scale);
        awayText.setAlpha(scale);
        homeThumbnail.setAlpha(scale);
        awayThumbnail.setAlpha(scale);
        homeThumbnail.setScaleX(scale);
        awayThumbnail.setScaleX(scale);
        homeThumbnail.setScaleY(scale);
        awayThumbnail.setScaleY(scale);
        homeThumbnail.setTranslationY(drop);
        awayThumbnail.setTranslationY(drop);
    }

    private void tintScore() {
        boolean noTournament = model.getTournament().isEmpty();
        int borderWidth = noTournament ? 0 : itemView.getResources().getDimensionPixelSize(R.dimen.sixteenth_margin);
        homeThumbnail.setBorderWidth(borderWidth);
        awayThumbnail.setBorderWidth(borderWidth);
    }
}
