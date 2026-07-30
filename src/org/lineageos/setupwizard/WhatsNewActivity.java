/*
 * SPDX-FileCopyrightText: The PenguinOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard;

import android.content.res.ColorStateList;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.lineageos.setupwizard.util.SetupWizardUtils;
import org.lineageos.setupwizard.util.WhatsNewContent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WhatsNewActivity extends AppCompatActivity {

    private static final int PAGE_HELLO = 0;
    private static final int PAGE_CHANGELOG = 1;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private ViewFlipper mFlipper;
    private Button mPrimaryButton;
    private TextView mTitle;
    private TextView mHelloSubtitle;
    private LinearLayout mChangelogContainer;
    private View mDotHello;
    private View mDotChangelog;

    private ExecutorService mFetchContentTask;
    private ConnectivityManager mConnectivityManager;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private boolean mContentFetched;

    private boolean mBuildRecorded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setShowWhenLocked(true);
        setTurnScreenOn(true);

        setContentView(R.layout.whats_new_activity);

        // The window draws edge to edge, so without this the title ran under the status bar.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (view, windowInsets) -> {
            final Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        mFlipper = findViewById(R.id.flipper);
        mPrimaryButton = findViewById(R.id.primary_button);
        mTitle = findViewById(R.id.whats_new_title);
        mHelloSubtitle = findViewById(R.id.whats_new_hello_subtitle);
        mChangelogContainer = findViewById(R.id.changelog_container);

        final String osName = getString(R.string.os_name);
        ((TextView) findViewById(R.id.whats_new_hello_title))
                .setText(getString(R.string.setup_welcome_message, osName));

        String buildId = Build.VERSION.INCREMENTAL;
        if (TextUtils.isEmpty(buildId)) {
            buildId = Build.ID;
        }
        ((TextView) findViewById(R.id.whats_new_subtitle)).setText(
                getString(R.string.whats_new_build_subtitle,
                        getString(R.string.welcome_codename), buildId));

        buildDots();
        applyContent(WhatsNewContent.cached(this));
        fetchContent();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mFlipper.getDisplayedChild() == PAGE_CHANGELOG) {
                    showPage(PAGE_HELLO);
                } else {
                    finish();
                }
            }
        });

        showPage(PAGE_HELLO);
    }

    @Override
    protected void onDestroy() {
        stopWatchingNetwork();
        if (mFetchContentTask != null) {
            mFetchContentTask.shutdownNow();
            mFetchContentTask = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!mBuildRecorded) {
            mBuildRecorded = true;
            SetupWizardUtils.recordCurrentBuild(this);
        }
    }

    private void buildDots() {
        final LinearLayout dots = findViewById(R.id.dots);
        mDotHello = makeDot(dots);
        mDotChangelog = makeDot(dots);
    }

    private View makeDot(LinearLayout parent) {
        final View dot = new View(this);
        dot.setBackgroundResource(R.drawable.dot_indicator);
        final int h = dpToPx(7);
        final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dpToPx(7), h);
        lp.setMarginEnd(dpToPx(7));
        dot.setLayoutParams(lp);
        parent.addView(dot);
        return dot;
    }

    private void setDotActive(View dot, boolean active) {
        final MarginLayoutParams lp = (MarginLayoutParams) dot.getLayoutParams();
        lp.width = dpToPx(active ? 20 : 7);
        dot.setLayoutParams(lp);
        dot.setBackgroundTintList(ColorStateList.valueOf(active
                ? getColor(R.color.lineage_accent)
                : 0x59FFFFFF ));
    }

    /**
     * Fetches whenever a network becomes usable, until one fetch succeeds.
     *
     * After an update this screen opens on the lock screen at LOCKED_BOOT_COMPLETED. Saved Wi-Fi
     * networks live in credential encrypted storage and are only loaded once the user unlocks, so
     * at that point there is usually no network at all; a single fetch from onCreate failed and
     * was never tried again, leaving the page on its "unavailable" text.
     */
    private void fetchContent() {
        mFetchContentTask = Executors.newSingleThreadExecutor();
        mConnectivityManager = getSystemService(ConnectivityManager.class);
        mNetworkCallback = new ConnectivityManager.NetworkCallback() {
            // Not onAvailable: a network is announced before it is validated, and a fetch made
            // then can still fail with nothing left to trigger another.
            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities caps) {
                if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                    fetchContentOnce();
                }
            }
        };
        // Also calls back straight away when a default network already exists.
        mConnectivityManager.registerDefaultNetworkCallback(mNetworkCallback, mHandler);
    }

    private void fetchContentOnce() {
        if (mContentFetched || mFetchContentTask == null || mFetchContentTask.isShutdown()) {
            return;
        }
        mFetchContentTask.execute(() -> {
            final WhatsNewContent content = WhatsNewContent.fetch(this);
            if (content == null) {
                return;
            }
            mHandler.post(() -> {
                if (mContentFetched || isFinishing() || isDestroyed()) {
                    return;
                }
                mContentFetched = true;
                stopWatchingNetwork();
                applyContent(content);
            });
        });
    }

    private void stopWatchingNetwork() {
        if (mNetworkCallback == null) {
            return;
        }
        mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
        mNetworkCallback = null;
    }

    /** Renders {@code content}, falling back to the built-in copy for anything it omits. */
    private void applyContent(WhatsNewContent content) {
        mTitle.setText(content != null && !TextUtils.isEmpty(content.title)
                ? content.title
                : getString(R.string.whats_new_title));
        mHelloSubtitle.setText(content != null && !TextUtils.isEmpty(content.helloSubtitle)
                ? content.helloSubtitle
                : getString(R.string.whats_new_hello_subtitle));
        populateChangelog(content);
    }

    private void populateChangelog(WhatsNewContent content) {
        mChangelogContainer.removeAllViews();

        if (content == null || content.highlights.isEmpty()) {
            final TextView empty = (TextView) LayoutInflater.from(this)
                    .inflate(R.layout.whats_new_empty, mChangelogContainer, false);
            empty.setText(R.string.whats_new_unavailable);
            mChangelogContainer.addView(empty);
            return;
        }

        final LayoutInflater inflater = LayoutInflater.from(this);
        for (WhatsNewContent.Highlight highlight : content.highlights) {
            final View row = inflater.inflate(R.layout.whats_new_row, mChangelogContainer, false);
            ((TextView) row.findViewById(R.id.whats_new_row_icon)).setText(highlight.icon);
            ((TextView) row.findViewById(R.id.whats_new_row_title)).setText(highlight.title);
            ((TextView) row.findViewById(R.id.whats_new_row_summary)).setText(highlight.summary);
            mChangelogContainer.addView(row);
        }
    }

    private void showPage(int page) {
        mFlipper.setDisplayedChild(page);
        setDotActive(mDotHello, page == PAGE_HELLO);
        setDotActive(mDotChangelog, page == PAGE_CHANGELOG);

        if (page == PAGE_HELLO) {
            mPrimaryButton.setText(R.string.whats_new_hello_button);
            mPrimaryButton.setOnClickListener(v -> showPage(PAGE_CHANGELOG));
        } else {
            mPrimaryButton.setText(getString(R.string.whats_new_enter_button,
                    getString(R.string.os_name)));
            mPrimaryButton.setOnClickListener(v -> finish());
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
