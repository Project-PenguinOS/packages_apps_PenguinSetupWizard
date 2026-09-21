/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.google.android.setupcompat.template.FooterButtonStyleUtils;

public class NavigationLayout extends RelativeLayout {
    /*
     * An interface to listen to events of the navigation bar,
     * namely when the user clicks on the back or next button.
     */
    public interface NavigationBarListener {
        void onNavigateNext();

        void onSkip();
    }

    private final Button mNextButton;
    private final Button mSkipButton;

    public NavigationLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        View.inflate(context, R.layout.navigation_layout, this);
        mNextButton = findViewById(R.id.navbar_next);
        mSkipButton = findViewById(R.id.navbar_skip);
        FooterButtonStyleUtils.applyPrimaryButtonPartnerResource(context, mNextButton, true);
        FooterButtonStyleUtils.applySecondaryButtonPartnerResource(context, mSkipButton, true);
        applyButtonRadius(mNextButton);
        applyButtonRadius(mSkipButton);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.NavigationLayout, 0, 0);
        final boolean showSkipButton;
        try {
            showSkipButton = a.getBoolean(
                    R.styleable.NavigationLayout_showSkipButton, false);
        } finally {
            a.recycle();
        }

        if (showSkipButton) {
            mSkipButton.setVisibility(View.VISIBLE);
        }
    }

    private void applyButtonRadius(Button button) {
        Drawable background = button.getBackground();
        if (background instanceof RippleDrawable ripple && ripple.getNumberOfLayers() > 0) {
            background = ripple.getDrawable(0);
        }
        if (background instanceof InsetDrawable inset) {
            background = inset.getDrawable();
        }
        if (background instanceof GradientDrawable gradient) {
            gradient.setCornerRadius(
                    getResources().getDimension(R.dimen.penguin_button_corner_radius));
        }
    }

    public Button getSkipButton() {
        return mSkipButton;
    }

    public Button getNextButton() {
        return mNextButton;
    }

    public void setNavigationBarListener(NavigationBarListener listener) {
        mSkipButton.setOnClickListener(view -> listener.onSkip());
        mNextButton.setOnClickListener(view -> listener.onNavigateNext());
    }
}
