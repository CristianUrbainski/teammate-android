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

package com.mainstreetcode.teammate.baseclasses;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.mainstreetcode.teammate.R;
import com.mainstreetcode.teammate.adapters.viewholders.ChoiceBar;
import com.mainstreetcode.teammate.adapters.viewholders.LoadingBar;
import com.mainstreetcode.teammate.model.UiState;
import com.mainstreetcode.teammate.util.FabInteractor;
import com.tunjid.androidbootstrap.core.abstractclasses.BaseActivity;
import com.tunjid.androidbootstrap.core.abstractclasses.BaseFragment;
import com.tunjid.androidbootstrap.functions.Consumer;
import com.tunjid.androidbootstrap.view.animator.ViewHider;
import com.tunjid.androidbootstrap.view.util.InsetFlags;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.AutoTransition;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.O;
import static android.view.KeyEvent.ACTION_UP;
import static android.view.View.GONE;
import static android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
import static android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
import static android.view.View.VISIBLE;
import static android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;
import static androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener;
import static com.google.android.material.snackbar.Snackbar.LENGTH_INDEFINITE;
import static com.google.android.material.snackbar.Snackbar.LENGTH_LONG;
import static com.mainstreetcode.teammate.util.ViewHolderUtil.TOOLBAR_ANIM_DELAY;
import static com.mainstreetcode.teammate.util.ViewHolderUtil.getLayoutParams;
import static com.mainstreetcode.teammate.util.ViewHolderUtil.isDisplayingSystemUI;
import static com.mainstreetcode.teammate.util.ViewHolderUtil.updateToolBar;
import static com.tunjid.androidbootstrap.view.animator.ViewHider.BOTTOM;
import static com.tunjid.androidbootstrap.view.animator.ViewHider.TOP;

/**
 * Base Activity for the app
 */

public abstract class TeammatesBaseActivity extends BaseActivity
        implements PersistentUiController {

    private static final int DEFAULT_SYSTEM_UI_FLAGS = SYSTEM_UI_FLAG_LAYOUT_STABLE
            | SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;

    protected static final int HIDER_DURATION = 300;
    private static final String UI_STATE = "APP_UI_STATE";

    public static int topInset;
    private int leftInset;
    private int rightInset;
    public int bottomInset;

    private boolean insetsApplied;

    private View bottomInsetView;
    private View topInsetView;

    private CoordinatorLayout coordinatorLayout;
    private ConstraintLayout constraintLayout;
    private FrameLayout fragmentContainer;
    private LoadingBar loadingBar;
    private View padding;

    protected Toolbar toolbar;

    private ViewHider fabHider;
    private ViewHider toolbarHider;
    private FabInteractor fabInteractor;

    protected UiState uiState;

    private final List<BaseTransientBottomBar> transientBottomBars = new ArrayList<>();

    private final BaseTransientBottomBar.BaseCallback callback = new BaseTransientBottomBar.BaseCallback() {
        @SuppressWarnings("SuspiciousMethodCalls")
        public void onDismissed(Object bar, int event) { transientBottomBars.remove(bar); }
    };

    final FragmentManager.FragmentLifecycleCallbacks fragmentViewCreatedCallback = new FragmentManager.FragmentLifecycleCallbacks() {
        @Override
        public void onFragmentViewCreated(@NonNull FragmentManager fm, @NonNull Fragment f, @NonNull View v, Bundle savedInstanceState) {
            if (isNotInMainFragmentContainer(v)) return;

            clearTransientBars();
            adjustSystemInsets(f);
            setOnApplyWindowInsetsListener(v, (view, insets) -> consumeFragmentInsets(insets));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(fragmentViewCreatedCallback, false);
        if (SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.transparent));

        uiState = savedInstanceState == null ? UiState.freshState() : savedInstanceState.getParcelable(UI_STATE);
    }

    @Override
    protected void onPause() {
        clearTransientBars();
        super.onPause();
    }

    @Override
    @SuppressLint("WrongViewCast")
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);

        MaterialButton fab = findViewById(R.id.fab);
        fragmentContainer = findViewById(R.id.main_fragment_container);
        coordinatorLayout = findViewById(R.id.coordinator);
        constraintLayout = findViewById(R.id.content_view);
        bottomInsetView = findViewById(R.id.bottom_inset);
        topInsetView = findViewById(R.id.top_inset);
        toolbar = findViewById(R.id.toolbar);
        padding = findViewById(R.id.padding);
        toolbarHider = ViewHider.of(toolbar).setDuration(HIDER_DURATION).setDirection(TOP).build();
        fabHider = ViewHider.of(fab).setDuration(HIDER_DURATION).setDirection(BOTTOM).build();
        fabInteractor = new FabInteractor(fab);

        //noinspection AndroidLintClickableViewAccessibility
        padding.setOnTouchListener((view, event) -> {
            if (event.getAction() == ACTION_UP) setKeyboardPadding(bottomInset);
            return true;
        });

        toolbar.setOnMenuItemClickListener(item -> {
            BaseFragment fragment = getCurrentFragment();
            boolean selected = fragment != null && fragment.onOptionsItemSelected(item);
            return selected || onOptionsItemSelected(item);
        });

        View decorView = getDecorView();
        decorView.setSystemUiVisibility(DEFAULT_SYSTEM_UI_FLAGS);
        decorView.setOnSystemUiVisibilityChangeListener(visibility -> toggleToolbar(!isDisplayingSystemUI(decorView)));
        setOnApplyWindowInsetsListener(constraintLayout, (view, insets) -> consumeSystemInsets(insets));
        showSystemUI();
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateUI(true, uiState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(UI_STATE, uiState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void invalidateOptionsMenu() {
        super.invalidateOptionsMenu();
        toolbar.postDelayed(() -> {
            TeammatesBaseFragment fragment = getCurrentFragment();
            if (fragment != null) fragment.onPrepareOptionsMenu(toolbar.getMenu());
        }, TOOLBAR_ANIM_DELAY);
    }

    @Override
    public TeammatesBaseFragment getCurrentFragment() {
        return (TeammatesBaseFragment) super.getCurrentFragment();
    }

    @Override
    public void update(UiState state) {
        updateUI(false, state);
    }

    @Override
    public void updateMainToolBar(int menu, CharSequence title) {
        updateToolBar(toolbar, menu, title);
    }

    @Override
    public void updateAltToolbar(int menu, CharSequence title) { }

    @Override
    public void toggleToolbar(boolean show) {
        if (toolbarHider == null) return;
        if (show) toolbarHider.show();
        else toolbarHider.hide();
    }

    @Override
    public void toggleAltToolbar(boolean show) {}

    @Override
    public void toggleBottombar(boolean show) {}

    @Override
    public void toggleFab(boolean show) {
        if (fabHider == null) return;
        if (show) fabHider.show();
        else fabHider.hide();
    }

    @Override
    @SuppressLint({"Range", "WrongConstant"})
    public void toggleProgress(boolean show) {
        if (show && loadingBar != null && loadingBar.isShown()) return;
        if (show) (loadingBar = LoadingBar.make(coordinatorLayout, LENGTH_INDEFINITE)).show();
        else if (loadingBar != null && loadingBar.isShownOrQueued()) loadingBar.dismiss();
    }

    @Override
    public void toggleSystemUI(boolean show) {
        if (show) showSystemUI();
        else hideSystemUI();
    }

    @Override
    public void toggleLightNavBar(boolean isLight) {
        if (SDK_INT < O) return;

        View decorView = getDecorView();
        int visibility = decorView.getSystemUiVisibility();

        if (isLight) visibility = visibility | SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        else visibility &= ~SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;

        decorView.setSystemUiVisibility(visibility);
    }

    @Override
    public void setFabIcon(@DrawableRes int icon, @StringRes int title) {
        runOnUiThread(() -> {
            if (icon != 0 && title != 0 && fabInteractor != null) fabInteractor.update(icon, title);
        });
    }

    @Override
    public void setNavBarColor(int color) {
        getWindow().setNavigationBarColor(color);
    }

    @Override
    public void setFabExtended(boolean expanded) {
        if (fabInteractor != null) fabInteractor.setExtended(expanded);
    }

    @Override
    public void showSnackBar(CharSequence message) {
        Snackbar snackbar = Snackbar.make(coordinatorLayout, message, LENGTH_LONG);

        // Necessary to remove snackbar padding for keyboard on older versions of Android
        ViewCompat.setOnApplyWindowInsetsListener(snackbar.getView(), (view, insets) -> insets);
        snackbar.show();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void showSnackBar(Consumer<Snackbar> consumer) {
        Snackbar snackbar = Snackbar.make(coordinatorLayout, "", LENGTH_INDEFINITE).addCallback(callback);

        // Necessary to remove snackbar padding for keyboard on older versions of Android
        ViewCompat.setOnApplyWindowInsetsListener(snackbar.getView(), (view, insets) -> insets);
        consumer.accept(snackbar);
        transientBottomBars.add(snackbar);
        snackbar.show();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void showChoices(Consumer<ChoiceBar> consumer) {
        ChoiceBar bar = ChoiceBar.make(coordinatorLayout, LENGTH_INDEFINITE).addCallback(callback);
        consumer.accept(bar);
        transientBottomBars.add(bar);
        bar.show();
    }

    @Override
    public void setFabClickListener(@Nullable View.OnClickListener clickListener) {
        fabInteractor.setOnClickListener(clickListener);
    }

    public void onDialogDismissed() {
        TeammatesBaseFragment fragment = getCurrentFragment();
        boolean showFab = fragment != null && fragment.showsFab();

        if (showFab) toggleFab(true);
    }

    protected boolean isNotInMainFragmentContainer(View view) {
        View parent = (View) view.getParent();
        return parent == null || parent.getId() != R.id.main_fragment_container;
    }

    protected int adjustKeyboardPadding(int suggestion) {
        return suggestion - bottomInset;
    }

    protected void clearTransientBars() {
        for (BaseTransientBottomBar bar : transientBottomBars)
            if (bar instanceof ChoiceBar) ((ChoiceBar) bar).dismissAsTimeout();
            else bar.dismiss();
        transientBottomBars.clear();
    }

    protected void initTransition() {
        Transition transition = new AutoTransition();
        transition.setDuration(200);

        TeammatesBaseFragment view = getCurrentFragment();
        if (view != null) for (int id : view.staticViews()) transition.excludeTarget(id, true);
        transition.excludeTarget(RecyclerView.class, true);
        transition.excludeTarget(toolbar, true);

        TransitionManager.beginDelayedTransition((ViewGroup) toolbar.getParent(), transition);
    }

    private void updateUI(boolean force, UiState state) {
        uiState = uiState.diff(force,
                state,
                this::toggleFab,
                this::toggleToolbar,
                this::toggleAltToolbar,
                this::toggleBottombar,
                this::toggleSystemUI,
                this::toggleLightNavBar,
                this::setNavBarColor,
                insetFlag -> {},
                this::setFabIcon,
                this::updateMainToolBar,
                this::updateAltToolbar,
                this::setFabClickListener
        );
    }

    private WindowInsetsCompat consumeSystemInsets(WindowInsetsCompat insets) {
        if (insetsApplied) return insets;

        topInset = insets.getSystemWindowInsetTop();
        leftInset = insets.getSystemWindowInsetLeft();
        rightInset = insets.getSystemWindowInsetRight();
        bottomInset = insets.getSystemWindowInsetBottom();

        getLayoutParams(topInsetView).height = topInset;
        getLayoutParams(bottomInsetView).height = bottomInset;
        adjustSystemInsets(getCurrentFragment());

        insetsApplied = true;
        return insets;
    }

    private WindowInsetsCompat consumeFragmentInsets(WindowInsetsCompat insets) {
        int keyboardPadding = insets.getSystemWindowInsetBottom();
        setKeyboardPadding(keyboardPadding);

        TeammatesBaseFragment view = getCurrentFragment();
        if (view != null) view.onKeyBoardChanged(keyboardPadding != bottomInset);
        return insets;
    }

    private void setKeyboardPadding(int padding) {
        initTransition();
        padding = adjustKeyboardPadding(padding);
        padding = Math.max(padding, 0);

        fragmentContainer.setPadding(0, 0, 0, padding);
        getLayoutParams(this.padding).height = padding == 0 ? 1 : padding; // 0 breaks animations
    }

    private void adjustSystemInsets(Fragment fragment) {
        if (!(fragment instanceof TeammatesBaseFragment)) return;
        InsetFlags insetFlags = ((TeammatesBaseFragment) fragment).insetFlags();

        getLayoutParams(toolbar).topMargin = insetFlags.hasTopInset() ? 0 : topInset;
        bottomInsetView.setVisibility(insetFlags.hasBottomInset() ? VISIBLE : GONE);
        topInsetView.setVisibility(insetFlags.hasTopInset() ? VISIBLE : GONE);
        constraintLayout.setPadding(insetFlags.hasLeftInset() ? leftInset : 0, 0, insetFlags.hasRightInset() ? rightInset : 0, 0);
    }

    private void hideSystemUI() {
        View decorView = getDecorView();
        int visibility = decorView.getSystemUiVisibility()
                | SYSTEM_UI_FLAG_FULLSCREEN
                | SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(visibility);
    }

    private void showSystemUI() {
        View decorView = getDecorView();
        int visibility = decorView.getSystemUiVisibility();
        visibility &= ~SYSTEM_UI_FLAG_FULLSCREEN;
        visibility &= ~SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        visibility &= ~SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        decorView.setSystemUiVisibility(visibility);
    }

    private View getDecorView() {return getWindow().getDecorView();}
}
