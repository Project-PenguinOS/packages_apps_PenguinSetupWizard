/*
 * SPDX-FileCopyrightText: The PenguinOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard;

import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioGroup;

public class IconStyleActivity extends BaseSetupWizardActivity {

    private static final String SETTING_NOS_THEMED_ICONS = "nos_themed_icons";
    
    private static final String SETTING_QS_PANEL_STYLE = "qs_panel_style";
    
    private static final String SETTING_DUAL_SHADE = "dual_shade";

    private static final int QS_STYLE_DEFAULT = 0;
    private static final int QS_STYLE_PENGUIN = 1;

    private CheckBox mEnableNosIcons;
    private RadioGroup mQsStyle;
    private RadioGroup mQsLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setNextText(R.string.next);
        getGlifLayout().setDescriptionText(getString(R.string.nos_icons_description));

        mEnableNosIcons = findViewById(R.id.nos_icons_checkbox);
        final View row = findViewById(R.id.nos_icons);
        row.setOnClickListener(v -> mEnableNosIcons.setChecked(!mEnableNosIcons.isChecked()));

        mQsStyle = findViewById(R.id.qs_style_radio_group);
        mQsLayout = findViewById(R.id.qs_layout_radio_group);
    }

    @Override
    public void onResume() {
        super.onResume();
        mEnableNosIcons.setChecked(getSecureInt(SETTING_NOS_THEMED_ICONS, 0) != 0);
        mQsStyle.check(getSecureInt(SETTING_QS_PANEL_STYLE, QS_STYLE_PENGUIN) == QS_STYLE_DEFAULT
                ? R.id.radio_qs_style_default : R.id.radio_qs_style_penguin);
        mQsLayout.check(getSecureInt(SETTING_DUAL_SHADE, 0) != 0
                ? R.id.radio_qs_layout_split : R.id.radio_qs_layout_combined);
    }

    @Override
    protected void onNextPressed() {
        putSecureInt(SETTING_NOS_THEMED_ICONS, mEnableNosIcons.isChecked() ? 1 : 0);
        putSecureInt(SETTING_QS_PANEL_STYLE,
                mQsStyle.getCheckedRadioButtonId() == R.id.radio_qs_style_default
                        ? QS_STYLE_DEFAULT : QS_STYLE_PENGUIN);
        putSecureInt(SETTING_DUAL_SHADE,
                mQsLayout.getCheckedRadioButtonId() == R.id.radio_qs_layout_split ? 1 : 0);
        super.onNextPressed();
    }

    private int getSecureInt(String key, int defaultValue) {
        return Settings.Secure.getInt(getContentResolver(), key, defaultValue);
    }

    private void putSecureInt(String key, int value) {
        Settings.Secure.putInt(getContentResolver(), key, value);
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
