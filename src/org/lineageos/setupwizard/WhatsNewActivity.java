/*
 * SPDX-FileCopyrightText: The PenguinOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard;

import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
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

import org.lineageos.setupwizard.util.SetupWizardUtils;

public class WhatsNewActivity extends AppCompatActivity {

    private static final int PAGE_HELLO = 0;
    private static final int PAGE_CHANGELOG = 1;

    private ViewFlipper mFlipper;
    private Button mPrimaryButton;
    private View mDotHello;
    private View mDotChangelog;

    private boolean mBuildRecorded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setShowWhenLocked(true);
        setTurnScreenOn(true);

        setContentView(R.layout.whats_new_activity);

        mFlipper = findViewById(R.id.flipper);
        mPrimaryButton = findViewById(R.id.primary_button);

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
        populateChangelog();

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

    private void populateChangelog() {
        final LinearLayout container = findViewById(R.id.changelog_container);
        final String[] icons = getResources().getStringArray(R.array.whats_new_icons);
        final String[] titles = getResources().getStringArray(R.array.whats_new_titles);
        final String[] summaries = getResources().getStringArray(R.array.whats_new_summaries);
        final LayoutInflater inflater = LayoutInflater.from(this);

        final int count = Math.min(icons.length, Math.min(titles.length, summaries.length));
        for (int i = 0; i < count; i++) {
            final View row = inflater.inflate(R.layout.whats_new_row, container, false);
            ((TextView) row.findViewById(R.id.whats_new_row_icon)).setText(icons[i]);
            ((TextView) row.findViewById(R.id.whats_new_row_title)).setText(titles[i]);
            ((TextView) row.findViewById(R.id.whats_new_row_summary)).setText(summaries[i]);
            container.addView(row);
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
