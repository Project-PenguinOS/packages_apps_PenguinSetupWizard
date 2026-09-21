/*
 * SPDX-FileCopyrightText: 2019-2020 The Calyx Institute
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard.backup;

import static com.google.android.setupcompat.util.ResultCodes.RESULT_ACTIVITY_NOT_FOUND;

import static org.lineageos.setupwizard.SetupWizardApp.ACTION_GOOGLE_CLOUD_RESTORE;
import static org.lineageos.setupwizard.SetupWizardApp.ACTION_GOOGLE_RESTORE;
import static org.lineageos.setupwizard.SetupWizardApp.ACTION_RESTORE_FROM_BACKUP;

import android.accounts.AccountManager;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.provider.Settings;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.result.ActivityResult;

import org.lineageos.setupwizard.R;
import org.lineageos.setupwizard.SubBaseActivity;
import org.lineageos.setupwizard.util.SetupWizardUtils;

public class RestoreIntroActivity extends SubBaseActivity {

    private static final String GOOGLE_ACCOUNT_TYPE = "com.google";
    private static final long NETWORK_WAIT_MS = 20000L;

    private final Handler mNetworkHandler = new Handler(Looper.getMainLooper());
    private final Runnable mNetworkTimeout = this::onNetworkSettled;

    private ConnectivityManager mConnectivityManager;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private boolean mAwaitingAccount = false;
    private boolean mAwaitingNetwork = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getGlifLayout().setDescriptionText(getString(R.string.intro_restore_subtitle,
                getString(R.string.os_name)));
    }

    @Override
    protected void onSubactivityResult(ActivityResult activityResult) {
        int resultCode = activityResult.getResultCode();
        Intent data = activityResult.getData();
        if (mAwaitingAccount) {
            mAwaitingAccount = false;
            launchRestoreFlow();
            return;
        }
        if (resultCode != RESULT_CANCELED) {
            nextAction(resultCode, data);
        } else if (mIsSubactivityNotFound) {
            finishAction(RESULT_ACTIVITY_NOT_FOUND);
        } else if (data != null && data.getBooleanExtra("onBackPressed", false)) {
            onStartSubactivity();
        }
    }

    @Override
    protected void onStartSubactivity() {
        setNextAllowed(true);
    }

    @Override
    protected void onNextPressed() {
        launchRestore();
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.intro_restore_activity;
    }

    @Override
    protected int getTitleResId() {
        return R.string.intro_restore_title;
    }

    @Override
    protected int getIconResId() {
        return R.drawable.ic_restore;
    }

    private void launchRestore() {
        SetupWizardUtils.enableMobileData(this);
        if (SetupWizardUtils.hasValidatedInternet(this)) {
            continueRestore();
            return;
        }
        awaitNetwork();
    }

    private void awaitNetwork() {
        mAwaitingNetwork = true;
        setNextAllowed(false);
        getGlifLayout().setProgressBarShown(true);
        mConnectivityManager = getSystemService(ConnectivityManager.class);
        mNetworkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities caps) {
                if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                    onNetworkSettled();
                }
            }
        };
        mConnectivityManager.registerDefaultNetworkCallback(mNetworkCallback, mNetworkHandler);
        mNetworkHandler.postDelayed(mNetworkTimeout, NETWORK_WAIT_MS);
    }

    private void onNetworkSettled() {
        if (!mAwaitingNetwork || isFinishing() || isDestroyed()) {
            return;
        }
        mAwaitingNetwork = false;
        stopWatchingNetwork();
        getGlifLayout().setProgressBarShown(false);
        setNextAllowed(true);
        continueRestore();
    }

    private void stopWatchingNetwork() {
        mNetworkHandler.removeCallbacks(mNetworkTimeout);
        if (mNetworkCallback != null) {
            mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
            mNetworkCallback = null;
        }
    }

    private void continueRestore() {
        if (!hasGoogleAccount() && startAccountSetup()) {
            return;
        }
        launchRestoreFlow();
    }

    @Override
    protected void onDestroy() {
        stopWatchingNetwork();
        super.onDestroy();
    }

    private boolean hasGoogleAccount() {
        AccountManager accounts = getSystemService(AccountManager.class);
        return accounts != null && accounts.getAccountsByType(GOOGLE_ACCOUNT_TYPE).length > 0;
    }

    private boolean startAccountSetup() {
        Intent intent = new Intent(Settings.ACTION_ADD_ACCOUNT);
        intent.putExtra(Settings.EXTRA_ACCOUNT_TYPES, new String[] {GOOGLE_ACCOUNT_TYPE});
        if (intent.resolveActivity(getPackageManager()) == null) {
            return false;
        }
        mAwaitingAccount = true;
        startSubactivity(intent);
        return true;
    }

    private void launchRestoreFlow() {
        for (String action : new String[] {
                ACTION_GOOGLE_RESTORE, ACTION_GOOGLE_CLOUD_RESTORE, ACTION_RESTORE_FROM_BACKUP}) {
            Intent intent = new Intent(action);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startSubactivity(intent);
                return;
            }
        }
        finishAction(RESULT_ACTIVITY_NOT_FOUND);
    }

}
