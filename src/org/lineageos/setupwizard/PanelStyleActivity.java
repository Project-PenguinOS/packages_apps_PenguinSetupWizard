/*
 * SPDX-FileCopyrightText: The PenguinOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard;

import android.os.Bundle;
import android.provider.Settings;
import android.widget.RadioGroup;

public class PanelStyleActivity extends BaseSetupWizardActivity {

    private static final String SETTING_VOLUME_PANEL_STYLE = "volume_panel_style";
    
    private static final String SETTING_POWER_MENU_STYLE = "power_menu_style";

    private static final int VOLUME_PANEL_STYLE_DEFAULT = 0;
    private static final int VOLUME_PANEL_STYLE_EXPANDABLE = 1;
    private static final int VOLUME_PANEL_STYLE_ONE_UI = 2;
    private static final int POWER_MENU_STYLE_PENGUIN = 0;
    private static final int POWER_MENU_STYLE_IOS = 1;

    private RadioGroup mVolumePanelStyle;
    private RadioGroup mPowerMenuStyle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setNextText(R.string.next);
        getGlifLayout().setDescriptionText(getString(R.string.panel_styles_description));

        mVolumePanelStyle = findViewById(R.id.volume_panel_style_radio_group);
        mPowerMenuStyle = findViewById(R.id.power_menu_style_radio_group);
    }

    @Override
    public void onResume() {
        super.onResume();
        final int volumeStyle =
                getSecureInt(SETTING_VOLUME_PANEL_STYLE, VOLUME_PANEL_STYLE_DEFAULT);
        final int volumeId;
        if (volumeStyle == VOLUME_PANEL_STYLE_ONE_UI) {
            volumeId = R.id.radio_volume_panel_oneui;
        } else if (volumeStyle == VOLUME_PANEL_STYLE_EXPANDABLE) {
            volumeId = R.id.radio_volume_panel_expandable;
        } else {
            volumeId = R.id.radio_volume_panel_default;
        }
        mVolumePanelStyle.check(volumeId);
        mPowerMenuStyle.check(
                getSecureInt(SETTING_POWER_MENU_STYLE, POWER_MENU_STYLE_PENGUIN)
                        == POWER_MENU_STYLE_IOS
                        ? R.id.radio_power_menu_ios : R.id.radio_power_menu_penguin);
    }

    @Override
    protected void onNextPressed() {
        final int checked = mVolumePanelStyle.getCheckedRadioButtonId();
        final int volumeStyle;
        if (checked == R.id.radio_volume_panel_oneui) {
            volumeStyle = VOLUME_PANEL_STYLE_ONE_UI;
        } else if (checked == R.id.radio_volume_panel_expandable) {
            volumeStyle = VOLUME_PANEL_STYLE_EXPANDABLE;
        } else {
            volumeStyle = VOLUME_PANEL_STYLE_DEFAULT;
        }
        putSecureInt(SETTING_VOLUME_PANEL_STYLE, volumeStyle);
        putSecureInt(SETTING_POWER_MENU_STYLE,
                mPowerMenuStyle.getCheckedRadioButtonId() == R.id.radio_power_menu_ios
                        ? POWER_MENU_STYLE_IOS : POWER_MENU_STYLE_PENGUIN);
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
        return R.layout.setup_panel_styles;
    }

    @Override
    protected int getTitleResId() {
        return R.string.setup_panel_styles;
    }

    @Override
    protected int getIconResId() {
        return R.drawable.ic_nos_icons;
    }
}
