/*
 * Copyright 2015 Team Exodus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.exodus;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.exodus.AntiPiracyConstants;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

/*
 *  Help protect developers by blacklisting known piracy apps.  To add support for an app
 *  simply add the package name to the AntiPiracyConstants.PACKAGES and update the description.
 *  For help or to report a new app please contact primedirective@exodus-developers.net
 */

public class AntiPiracyInstallReceiver extends BroadcastReceiver {
    private static final String TAG = "PACKAGE_INSTALL_RECEIVER";
    private static final boolean DEBUG = AntiPiracyConstants.DEBUG;

    private Handler mHandler = new Handler();

    @Override
    public void onReceive(Context ctx, Intent intent) {
        Intent notifyService = new Intent(ctx, AntiPiracyNotifyService.class);
        if (DEBUG) Log.i(TAG, "install check event");

        for (String app : AntiPiracyConstants.PACKAGES) {
            if (DEBUG) Log.e(TAG, "PACKAGE " + app + " testing for install");
            if (isInstalled(ctx, app)) {
                Log.i(TAG, "Blacklisted packages found " + app);
                if (!isServiceRunning(UninstallNotifyService.class, ctx)) {
					ctx.startService(notifyService);
                    Toast.makeText(ctx, "Anti-piracy software activated", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    boolean isServiceRunning(Class<?> serviceClass, Context ctx) {
        ActivityManager manager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i(TAG, "Check service already running");
                return true;
            }
        }
        Log.i(TAG, "Check service not running");
        return false;
    }

    boolean isInstalled(Context ctx, final String packageName) {
        final PackageManager pm = ctx.getPackageManager();
        String mVersion;
        try {
            mVersion = pm.getPackageInfo(packageName, 0).versionName;
            if (mVersion.equals(null)) {
                return false;
            }
        } catch (NameNotFoundException e) {
            if (DEBUG) Log.e(TAG, "Package " + packageName + " NameNotFoundException" + e);
            return false;
        }
        return true;
    }
}
