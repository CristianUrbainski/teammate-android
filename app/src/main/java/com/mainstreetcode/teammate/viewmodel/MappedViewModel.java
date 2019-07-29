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

package com.mainstreetcode.teammate.viewmodel;


import android.annotation.SuppressLint;

import com.mainstreetcode.teammate.model.Message;
import com.mainstreetcode.teammate.model.Model;
import com.mainstreetcode.teammate.notifications.NotifierProvider;
import com.mainstreetcode.teammate.util.ErrorHandler;
import com.mainstreetcode.teammate.util.FunctionalDiff;
import com.tunjid.androidbootstrap.recyclerview.diff.Differentiable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.arch.core.util.Function;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.DiffUtil;
import io.reactivex.Flowable;
import io.reactivex.Single;

import static com.mainstreetcode.teammate.model.Message.fromThrowable;
import static com.tunjid.androidbootstrap.functions.collections.Lists.findLast;

public abstract class MappedViewModel<K, V extends Differentiable> extends BaseViewModel {

    private AtomicInteger pullToRefreshCount = new AtomicInteger(0);

    MappedViewModel() {}

    abstract Class<V> valueClass();

    public abstract List<Differentiable> getModelList(K key);

    abstract Flowable<List<V>> fetch(K key, boolean fetchLatest);

    Flowable<Differentiable> checkForInvalidObject(Flowable<? extends Differentiable> source, K key, V value) {
        return source.cast(Differentiable.class).doOnError(throwable -> checkForInvalidObject(throwable, value, key));
    }

    Single<Differentiable> checkForInvalidObject(Single<? extends Differentiable> source,
                                                 @SuppressWarnings("SameParameterValue") K key, V value) {
        return source.cast(Differentiable.class).doOnError(throwable -> checkForInvalidObject(throwable, value, key));
    }

    public Flowable<DiffUtil.DiffResult> getMany(K key, boolean fetchLatest) {
        return fetchLatest ? getLatest(key) : getMore(key);
    }

    public Flowable<DiffUtil.DiffResult> getMore(K key) {
        return FunctionalDiff.of(fetch(key, false).map(this::toDifferentiable), getModelList(key), this::preserveList)
                .doOnError(throwable -> checkForInvalidKey(throwable, key));
    }

    public Flowable<DiffUtil.DiffResult> refresh(K key) {
        return FunctionalDiff.of(fetch(key, true).map(this::toDifferentiable), getModelList(key), this::pullToRefresh)
                .doOnError(throwable -> checkForInvalidKey(throwable, key))
                .doOnTerminate(() -> pullToRefreshCount.set(0));
    }

    private Flowable<DiffUtil.DiffResult> getLatest(K key) {
        return FunctionalDiff.of(fetch(key, true).map(this::toDifferentiable), getModelList(key), this::preserveList)
                .doOnError(throwable -> checkForInvalidKey(throwable, key));
    }

    public void clearNotifications(V value) {
        clearNotification(notificationCancelMap(value));
    }

    @SuppressLint("CheckResult")
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void clearNotifications(K key) {
        Flowable.fromIterable(getModelList(key))
                .map(this::notificationCancelMap)
                .subscribe(this::clearNotification, ErrorHandler.EMPTY);
    }

    private List<Differentiable> pullToRefresh(List<Differentiable> source, List<Differentiable> additions) {
        if (pullToRefreshCount.getAndIncrement() == 0) source.clear();

        preserveList(source, additions);
        afterPullToRefreshDiff(source);
        return source;
    }

    void afterPullToRefreshDiff(List<Differentiable> source) {}

    void onInvalidKey(K key) {}

    void onErrorMessage(Message message, K key, Differentiable invalid) {
        if (message.isInvalidObject()) getModelList(key).remove(invalid);
    }

    Pair<Model, Class> notificationCancelMap(Differentiable identifiable) {
        if (!(identifiable instanceof Model)) return new Pair<>(null, null);
        Model model = (Model) identifiable;
        return new Pair<>(model, model.getClass());
    }

    @SuppressWarnings("unchecked")
    private void clearNotification(Pair<Model, Class> pair) {
        if (pair.first == null || pair.second == null) return;
        NotifierProvider.forModel(pair.second).clearNotifications(pair.first);
    }

    void checkForInvalidObject(Throwable throwable, V model, K key) {
        Message message = fromThrowable(throwable);
        if (message != null) onErrorMessage(message, key, model);
    }

    Date getQueryDate(boolean fetchLatest, K key, Function<V, Date> dateFunction) {
        if (fetchLatest) return null;

        V value = findLast(getModelList(key), valueClass());
        return value == null ? null : dateFunction.apply(value);
    }

    private void checkForInvalidKey(Throwable throwable, K key) {
        Message message = fromThrowable(throwable);
        boolean isInvalidModel = message != null && !message.isValidModel();

        if (isInvalidModel) onInvalidKey(key);
    }

    final List<Differentiable> toDifferentiable(List<V> source) {
        return new ArrayList<>(source);
    }
}
