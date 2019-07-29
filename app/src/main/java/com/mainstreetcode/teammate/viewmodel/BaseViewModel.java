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

import androidx.lifecycle.ViewModel;

import com.mainstreetcode.teammate.App;
import com.tunjid.androidbootstrap.recyclerview.diff.Differentiable;
import com.mainstreetcode.teammate.util.ErrorHandler;
import com.mainstreetcode.teammate.util.ModelUtils;
import com.mainstreetcode.teammate.viewmodel.events.Alert;

import java.util.LinkedList;
import java.util.List;

import io.reactivex.disposables.CompositeDisposable;


abstract class BaseViewModel extends ViewModel {

    private static final int AD_THRESH = 5;

    private LinkedList<Differentiable> ads = new LinkedList<>();
    private CompositeDisposable disposable = new CompositeDisposable();

    BaseViewModel() {
        disposable.add(App.getInstance().alerts().subscribe(this::onModelAlert, ErrorHandler.EMPTY));
        fetchAds();
    }

    boolean hasNativeAds() {return true;}

    boolean sortsAscending() {return false;}

    void pushModelAlert(Alert alert) { App.getInstance().pushAlert(alert); }

    void onModelAlert(Alert alert) {}

    @Override
    protected void onCleared() {
        disposable.clear();
        super.onCleared();
    }

    final List<Differentiable> preserveList(List<Differentiable> source, List<Differentiable> additions) {
        if (sortsAscending()) ModelUtils.preserveAscending(source, additions);
        else ModelUtils.preserveDescending(source, additions);

        afterPreserveListDiff(source);
        if (hasNativeAds()) distributeAds(source);
        return source;
    }

    void afterPreserveListDiff(List<Differentiable> source) {}

    private void distributeAds(List<Differentiable> source) {
        //filterAds(source);
        if (source.isEmpty() || ads.isEmpty()) return;

        int numToShuffle = 0;
        int adSize = ads.size();
        int sourceSize = source.size();
        int count = 0;

        if (sourceSize <= AD_THRESH) {
            source.add(ads.get(0));
            shuffleAds(++numToShuffle);
            return;
        }

        for (int i = AD_THRESH; i < sourceSize; i += AD_THRESH) {
            if (count >= adSize) break;
            source.add(i, ads.get(count));
            numToShuffle++;
            count++;
        }
        shuffleAds(numToShuffle);
    }

    private void fetchAds() {
//        App app = App.getInstance();
//        AdLoader adLoader = new AdLoader.Builder(app, app.getString(R.string.admob_ad_id))
//                .forAppInstallAd(appInstallAd -> ads.add(new InstallAd(appInstallAd)))
//                .forContentAd(contentAd -> ads.add(new ContentAd(contentAd)))
//                .withAdListener(new AdListener() {
//                    @Override
//                    public void onAdFailedToLoad(int errorCode) {
//                        // Handle the failure by logging, altering the UI, and so on.
//                    }
//                })
//                .withNativeAdOptions(new NativeAdOptions.Builder()
//                        .build())
//                .build();
//
//        adLoader.loadAds(new AdRequest.Builder().build(), 2);
    }
    //        while (iterator.hasNext()) if (iterator.next() instanceof ContentAd) iterator.remove();
    //        Iterator<Differentiable> iterator = source.iterator();
//    private void filterAds(List<Differentiable> source) {

//    }

    private void shuffleAds(int count) {
        for (int i = 0; i < count; i++) ads.add(ads.removeFirst());
    }

}
