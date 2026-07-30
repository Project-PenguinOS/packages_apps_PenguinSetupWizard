/*
 * SPDX-FileCopyrightText: The PenguinOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard;

import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.CheckBox;

public class IconStyleActivity extends BaseSetupWizardActivity {

    private static final String SETTING_NOS_THEMED_ICONS = "nos_themed_icons";

    private CheckBox mEnableNosIcons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setNextText(R.string.next);
        getGlifLayout().setDescriptionText(getString(R.string.nos_icons_description));

        mEnableNosIcons = findViewById(R.id.nos_icons_checkbox);
        final View row = findViewById(R.id.nos_icons);
        row.setOnClickListener(v -> mEnableNosIcons.setChecked(!mEnableNosIcons.isChecked()));
    }

    @Override
    public void onResume() {
        super.onResume();
        final boolean enabled = Settings.Secure.getInt(getContentResolver(),
                SETTING_NOS_THEMED_ICONS, 0) != 0;
        mEnableNosIcons.setChecked(enabled);
    }

    @Override
    protected void onNextPressed() {
        Settings.Secure.putInt(getContentResolver(), SETTING_NOS_THEMED_ICONS,
                mEnableNosIcons.isChecked() ? 1 : 0);
        super.onNextPressed();
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.setup_nos_icons;
    }

    @Override
    protected int getTitleResId() {
        return R.string.setup_nos_icons;
    }

    @Override
    protected int getIconResId() {
        return R.drawable.ic_nos_icons;
    }
}
