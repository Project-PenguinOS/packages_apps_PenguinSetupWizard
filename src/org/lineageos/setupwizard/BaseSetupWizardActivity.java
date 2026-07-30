/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard;

import static android.view.View.INVISIBLE;

import static androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;

import static com.google.android.setupcompat.util.ResultCodes.RESULT_SKIP;

import static org.lineageos.setupwizard.SetupWizardApp.LOGV;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.appcompat.app.AppCompatActivity;

import com.android.settingslib.Utils;

import com.google.android.setupcompat.template.StatusBarMixin;
import com.google.android.setupcompat.util.WizardManagerHelper;
import com.google.android.setupdesign.GlifLayout;
import com.google.android.setupdesign.transition.TransitionHelper;
import com.google.android.setupdesign.util.ThemeHelper;

import java.util.List;

import org.lineageos.setupwizard.NavigationLayout.NavigationBarListener;
import org.lineageos.setupwizard.util.SetupWizardUtils;

public abstract class BaseSetupWizardActivity extends AppCompatActivity implements
        NavigationBarListener {

    public static final String TAG = BaseSetupWizardActivity.class.getSimpleName();
    public static final int DEFAULT_TRANSITION = TransitionHelper.TRANSITION_FADE_THROUGH;

    private static final List<String> STEP_ORDER = List.of(
            "WelcomeActivity", "LocaleActivity", "SimMissingActivity",
            "NetworkSetupActivity", "DateTimeActivity", "LocationSettingsActivity",
            "NavigationSettingsActivity", "ThemeSettingsActivity", "IconStyleActivity");

    private NavigationLayout mNavigationBar;

    private ActivityResultLauncher<Intent> mNextIntentResultLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (LOGV) {
            logActivityState("onCreate savedInstanceState=" + savedInstanceState);
        }
        super.onCreate(savedInstanceState);
        mNextIntentResultLauncher = registerForActivityResult(
                new StartDecoratedActivityForResult(),
                BaseSetupWizardActivity.this::onNextIntentResult);
        initLayout();
        mNavigationBar = getNavigationBar();
        if (mNavigationBar != null) {
            mNavigationBar.setNavigationBarListener(this);
        }
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (LOGV) {
                    Log.v(TAG, "handleOnBackPressed()");
                }
                finishAction(RESULT_CANCELED, new Intent().putExtra("onBackPressed", true));
            }
        });
        // Apply default transition, to take effect whenever leaving this activity.
        applyForwardTransition();
    }

    @Override
    protected void onStart() {
        if (LOGV) {
            logActivityState("onStart");
        }
        super.onStart();
    }

    @Override
    protected void onRestart() {
        if (LOGV) {
            logActivityState("onRestart");
        }
        super.onRestart();
    }

    @Override
    protected void onResume() {
        if (LOGV) {
            logActivityState("onResume");
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (LOGV) {
            logActivityState("onPause");
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (LOGV) {
            logActivityState("onStop");
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (LOGV) {
            logActivityState("onDestroy");
        }
        super.onDestroy();
        mNextIntentResultLauncher.unregister();
    }

    @Override
    public void onAttachedToWindow() {
        if (LOGV) {
            logActivityState("onAttachedToWindow");
        }
        super.onAttachedToWindow();
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        if (LOGV) {
            Log.v(TAG, "onRestoreInstanceState(" + savedInstanceState + ")");
        }
        super.onRestoreInstanceState(savedInstanceState);
        int currentId = savedInstanceState.getInt("currentFocus", -1);
        if (currentId != -1) {
            View view = findViewById(currentId);
            if (view != null) {
                view.requestFocus();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        View current = getCurrentFocus();
        outState.putInt("currentFocus", current != null ? current.getId() : -1);
        if (LOGV) {
            Log.v(TAG, "onSaveInstanceState(" + outState + ")");
        }
    }

    /**
     * @return The navigation bar instance in the layout, or null if the layout does not have a
     * navigation bar.
     */
    public NavigationLayout getNavigationBar() {
        final View view = findViewById(R.id.navigation_bar);
        return view instanceof NavigationLayout ? (NavigationLayout) view : null;
    }

    public final void setNextAllowed(boolean allowed) {
        if (mNavigationBar != null) {
            mNavigationBar.getNextButton().setEnabled(allowed);
        }
    }

    protected void onNextPressed() {
        nextAction(RESULT_OK);
    }

    protected void onSkipPressed() {
        nextAction(RESULT_SKIP);
    }

    protected final void setNextText(int resId) {
        if (mNavigationBar != null) {
            mNavigationBar.getNextButton().setText(resId);
        }
    }

    public Button getNextButton() {
        return mNavigationBar.getNextButton();
    }

    protected final void setSkipText(int resId) {
        if (mNavigationBar != null) {
            mNavigationBar.getSkipButton().setText(resId);
        }
    }

    protected final void hideNextButton() {
        if (mNavigationBar != null) {
            final Button next = mNavigationBar.getNextButton();
            next.setVisibility(INVISIBLE);
        }
    }

    public void onNavigateNext() {
        onNextPressed();
    }

    public void onSkip() {
        onSkipPressed();
    }

    protected final void onSetupStart() {
        if (SetupWizardUtils.isOwner()) {
            tryEnablingWifi();
        }
    }

    public void finish() {
        if (LOGV) {
            Log.v(TAG, "finish");
        }
        super.finish();
    }

    protected final void finishAction(int resultCode) {
        finishAction(resultCode, null);
    }

    protected final void finishAction(int resultCode, Intent data) {
        if (resultCode != RESULT_CANCELED) {
            nextAction(resultCode, data);
            finish();
        } else {
            setResult(resultCode, data);
            finish();
            applyBackwardTransition();
        }
    }

    public final void nextAction(int resultCode) {
        nextAction(resultCode, null);
    }

    protected final void nextAction(int resultCode, Intent data) {
        if (LOGV) {
            Log.v(TAG, "nextAction resultCode=" + resultCode +
                    " data=" + data + " this=" + this);
        }
        if (resultCode == RESULT_CANCELED) {
            throw new IllegalArgumentException("Cannot call nextAction with RESULT_CANCELED");
        }
        setResult(resultCode, data);
        Intent intent = WizardManagerHelper.getNextIntent(getIntent(), resultCode, data);
        mNextIntentResultLauncher.launch(intent);
    }

    /** Adorn the Intent with Setup Wizard-related extras. */
    protected Intent decorateIntent(Intent intent) {
        return intent
                .putExtra(WizardManagerHelper.EXTRA_IS_FIRST_RUN, isFirstRun())
                .putExtra(WizardManagerHelper.EXTRA_IS_SETUP_FLOW, true)
                .putExtra(WizardManagerHelper.EXTRA_THEME, ThemeHelper.THEME_GLIF_V4);
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(decorateIntent(intent));
    }

    protected void onNextIntentResult(@NonNull ActivityResult activityResult) {
        int resultCode = activityResult.getResultCode();
        Intent data = activityResult.getData();
        if (LOGV) {
            StringBuilder append = new StringBuilder().append("onNextIntentResult(")
                    .append(resultCode).append(", ");
            Bundle extras = null;
            if (data != null) {
                extras = data.getExtras();
            }
            Log.v(TAG, append.append(extras).append(")").toString());
        }
    }

    protected final boolean tryEnablingWifi() {
        WifiManager wifiManager = getSystemService(WifiManager.class);
        return wifiManager != null && wifiManager.setWifiEnabled(true);
    }

    private boolean isFirstRun() {
        return true;
    }

    protected final void logActivityState(String prefix) {
        Log.v(TAG, prefix + " isResumed=" + isResumed() + " isFinishing=" +
                isFinishing() + " isDestroyed=" + isDestroyed());
    }

    private void initLayout() {
        if (getLayoutResId() != -1) {
            setContentView(getLayoutResId());
            final View glif = findViewById(R.id.setup_wizard_layout);
            if (glif instanceof GlifLayout) {

                ((GlifLayout) glif).getMixin(StatusBarMixin.class)
                        .setStatusBarBackground(Color.TRANSPARENT);
            }
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
        }
        if (getTitleResId() != -1) {
            final CharSequence headerText = TextUtils.expandTemplate(getText(getTitleResId()));
            getGlifLayout().setHeaderText(headerText);
        }
        if (getIconResId() != -1) {
            final GlifLayout layout = getGlifLayout();
            final Drawable icon = getDrawable(getIconResId()).mutate();
            icon.setTintList(Utils.getColorAccent(layout.getContext()));
            layout.setIcon(icon);
        }
        addProgressIndicator();
    }

    private void addProgressIndicator() {
        final int index = STEP_ORDER.indexOf(getClass().getSimpleName());
        if (index < 0) {
            return;
        }
        final View navBar = findViewById(R.id.navigation_bar);
        if (navBar == null || !(navBar.getParent() instanceof LinearLayout parent)) {
            return;
        }
        final boolean night = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        final int accent = getColor(R.color.lineage_accent);
        final int idle = night ? 0x59FFFFFF : 0x40000000;

        final LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(0, dp(16), 0, dp(16));

        final int h = dp(7);
        for (int i = 0; i < STEP_ORDER.size(); i++) {
            final View dot = new View(this);
            final LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(i == index ? dp(20) : dp(7), h);
            lp.setMarginStart(dp(2));
            lp.setMarginEnd(dp(2));
            dot.setLayoutParams(lp);
            final GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.RECTANGLE);
            g.setCornerRadius(h / 2f);
            g.setColor(i == index ? accent : idle);
            dot.setBackground(g);
            row.addView(dot);
        }
        parent.addView(row, parent.indexOfChild(navBar));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    protected GlifLayout getGlifLayout() {
        return requireViewById(R.id.setup_wizard_layout);
    }

    protected int getLayoutResId() {
        return -1;
    }

    protected int getTitleResId() {
        return -1;
    }

    protected int getIconResId() {
        return -1;
    }

    protected void applyForwardTransition() {
        TransitionHelper.applyForwardTransition(this, DEFAULT_TRANSITION, true);
    }

    protected void applyBackwardTransition() {
        TransitionHelper.applyBackwardTransition(BaseSetupWizardActivity.this,
                DEFAULT_TRANSITION, true);
    }

    protected final class StartDecoratedActivityForResult
            extends ActivityResultContract<Intent, ActivityResult> {

        private final StartActivityForResult mWrappedContract = new StartActivityForResult();

        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, @NonNull Intent intent) {
            return decorateIntent(mWrappedContract.createIntent(context, intent));
        }

        @Override
        public ActivityResult parseResult(int resultCode, @Nullable Intent result) {
            return mWrappedContract.parseResult(resultCode, result);
        }
    }
}
