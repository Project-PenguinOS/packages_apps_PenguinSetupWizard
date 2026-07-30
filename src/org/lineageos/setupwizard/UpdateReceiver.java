/*
 * SPDX-FileCopyrightText: The PenguinOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.lineageos.setupwizard.util.SetupWizardUtils;

public class UpdateReceiver extends BroadcastReceiver {

    private static final String TAG = UpdateReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        final boolean shouldShow;
        if (Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)) {

            shouldShow = SetupWizardUtils.shouldShowWhatsNewLocked(context);
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            shouldShow = SetupWizardUtils.shouldShowWhatsNew(context);
        } else {
            return;
        }
        if (!shouldShow) {
            return;
        }
        Log.i(TAG, "Build changed on an already set-up device; showing What's new (" + action + ")");
        try {
            final Intent whatsNew = new Intent(context, WhatsNewActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(whatsNew);
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch What's new", e);
        }
    }
}
