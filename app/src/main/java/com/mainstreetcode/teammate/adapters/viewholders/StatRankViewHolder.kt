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

package com.mainstreetcode.teammate.adapters.viewholders

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.mainstreetcode.teammate.R
import com.mainstreetcode.teammate.model.StatRank

class StatRankViewHolder(
        itemView: View,
        delegate: (StatRank) -> Unit
) : ModelCardViewHolder<StatRank>(itemView) {

    private val count: TextView
    private val inset: ImageView

    override val isThumbnail: Boolean
        get() = false

    init {
        itemView.setOnClickListener { delegate.invoke(model) }
        count = itemView.findViewById(R.id.item_position)
        inset = itemView.findViewById(R.id.inset)
    }

    override fun bind(model: StatRank) {
        super.bind(model)

        count.text = model.rank
        title.text = model.title
        subtitle.text = model.subtitle

        val imageUrl = model.inset

        if (imageUrl.isNotBlank()) load(imageUrl, inset)
    }

}
