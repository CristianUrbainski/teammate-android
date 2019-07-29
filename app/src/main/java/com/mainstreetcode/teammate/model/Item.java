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

package com.mainstreetcode.teammate.model;

import android.text.InputType;
import android.text.TextUtils;

import com.mainstreetcode.teammate.App;
import com.mainstreetcode.teammate.R;
import com.tunjid.androidbootstrap.functions.Supplier;
import com.tunjid.androidbootstrap.recyclerview.diff.Differentiable;

import java.lang.annotation.Retention;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.arch.core.util.Function;

import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Item for listing properties of a {@link Model}
 */
public class Item<T> implements Differentiable, Comparable<Item> {

    @Retention(SOURCE)
    @IntDef({INPUT, IMAGE, ROLE, DATE, CITY, LOCATION, INFO, TEXT, NUMBER, SPORT, VISIBILITY})
    @interface ItemType {}

    public static final int INPUT = 2;
    public static final int IMAGE = 3;
    public static final int ROLE = 4;
    public static final int DATE = 5;

    public static final int CITY = 6;
    public static final int ZIP = 8;
    public static final int STATE = 7;
    public static final int SPORT = 13;

    public static final int LOCATION = 9;

    public static final int INFO = 10;
    public static final int TEXT = 11;
    public static final int NUMBER = 12;
    public static final int DESCRIPTION = 14;
    public static final int VISIBILITY = 15;
    public static final int ABOUT = 16;
    public static final int NICKNAME = 17;
    public static final int STAT_TYPE = 18;
    public static final int TOURNAMENT_TYPE = 19;
    public static final int TOURNAMENT_STYLE = 20;
    public static final int COMPETITOR = 21;

    public static final Runnable NO_CLICK = null;
    public static final Runnable EMPTY_CLICK = () -> {};

    public static final Function<Item, Integer> NO_ICON = item -> 0;

    public static final Function<Item, Boolean> TRUE = item -> true;
    public static final Function<Item, Boolean> FALSE = item -> false;

    public static final Function<Item, CharSequence> ALL_INPUT_VALID = input -> "";
    public static final Function<Item, CharSequence> NON_EMPTY = input -> TextUtils.isEmpty(input.getValue()) ? App.getInstance().getString(R.string.team_invalid_empty_field) : "";

    @SuppressWarnings("unused")
    public static <T> void ignore(T ignored) {}

    private final int sortPosition;
    private final int inputType;
    private @ItemType final int itemType;
    private @StringRes final int stringRes;
    private @Nullable final ValueChangeCallBack changeCallBack;
    private @Nullable Function<CharSequence, CharSequence> textTransformer;

    private final T itemizedObject;
    private final String id;

    private CharSequence value;

    Item(String id, int sortPosition, int inputType, int itemType, int stringRes,
         CharSequence value, @Nullable ValueChangeCallBack changeCallBack, T itemizedObject) {
        this.id = id;
        this.sortPosition = sortPosition;
        this.inputType = inputType;
        this.itemType = itemType;
        this.stringRes = stringRes;
        this.value = value;
        this.changeCallBack = changeCallBack;
        this.itemizedObject = itemizedObject;
    }


    public static <T> Item<T> number(String id, int sortPosition, int itemType, int stringRes,
                                     Supplier<CharSequence> supplier, @Nullable ValueChangeCallBack changeCallBack,
                                     T itemizedObject) {
        return new Item<>(id, sortPosition, InputType.TYPE_CLASS_NUMBER, itemType, stringRes, supplier.get(), changeCallBack, itemizedObject);
    }

    public static <T> Item<T> text(String id, int sortPosition, int itemType, int stringRes,
                                   Supplier<CharSequence> supplier, @Nullable ValueChangeCallBack changeCallBack,
                                   T itemizedObject) {
        return new Item<>(id, sortPosition, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE, itemType, stringRes, supplier.get(), changeCallBack, itemizedObject);
    }

    public static <T> Item<T> email(String id, int sortPosition, int itemType, int stringRes,
                                    Supplier<CharSequence> supplier, @Nullable ValueChangeCallBack changeCallBack,
                                    T itemizedObject) {
        return new Item<>(id, sortPosition, InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, itemType, stringRes, supplier.get(), changeCallBack, itemizedObject);
    }

    @SuppressWarnings("WeakerAccess")
    public static Supplier<CharSequence> nullToEmpty(@Nullable CharSequence source) {
        CharSequence finalSource = source == null ? "" : source;
        return () -> finalSource;
    }

    public void setValue(CharSequence value) {
        this.value = value;
        if (changeCallBack != null) changeCallBack.onValueChanged(value.toString());
    }

    public Item<T> textTransformer(Function<CharSequence, CharSequence> textTransformer) {
        this.textTransformer = textTransformer;
        return this;
    }

    public int getSortPosition() {return sortPosition;}

    public int getInputType() {return inputType;}

    public int getItemType() {return this.itemType;}

    public int getStringRes() {return this.stringRes;}

    public CharSequence getValue() {return textTransformer == null ? value : textTransformer.apply(value);}

    public String getRawValue() {return value.toString();}

    @Override
    public String getId() {return id;}

    @Override
    public int compareTo(@NonNull Item o) { return Integer.compare(sortPosition, o.sortPosition); }

    @Override
    public boolean areContentsTheSame(Differentiable other) {
        if (other instanceof Item) return value.equals(((Item) other).value);
        return id.equals(other.getId());
    }

    @Override
    public Object getChangePayload(Differentiable other) {
        return other;
    }

    @SuppressWarnings("unused")
    public T getItemizedObject() {
        return itemizedObject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Item)) return false;

        Item<?> item = (Item<?>) o;

        return id != null ? id.equals(item.id) : item.id == null;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    // Used to change the value of the Team's fields
    public interface ValueChangeCallBack {
        void onValueChanged(String value);
    }
}
