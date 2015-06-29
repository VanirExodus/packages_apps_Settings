/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.applications;

import android.app.PackageDeleteObserver;
import android.app.PackageInstallObserver;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static com.android.settings.applications.AntiPiracyConstants.*;

/*
 *  Service to disable and uninstall blacklisted piracy apps.  Please report new piracy apps to ROM developers
 *  deploying this code.
 */

public class AntiPiracyNotifyService extends Service {
    static final String TAG = "ANTI-PIRACY SERVICE";

    // Notify service handler
    EventHandler mHandler = new EventHandler();
    static final int MSG_UNINSTALL = 100;
    static final int MSG_FINISH = 101;

    PackageInstallObserver mObserver;
    PackageDeleteObserver mObserverDelete;
    Method mMethod;
    Method mUninstallMethod;
    PackageManager mPm;

    private volatile boolean _init = false;

    List<String> mInstalledList = new ArrayList<String>();

    private static Class<?>[] TYPES = new Class[] {
        Uri.class, IPackageInstallObserver.class, int.class, String.class
    };
    private static Class<?>[] UNINSTALLTYPES = new Class[] {
        String.class, IPackageDeleteObserver.class, int.class
    };

    class PackageDeleteObserver extends IPackageDeleteObserver.Stub {
        public void packageDeleted(String packageName, int returnCode) throws RemoteException {
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mPm = this.getPackageManager();
        mObserver = new PackageInstallObserver();
        mObserverDelete = new PackageDeleteObserver();

        try {
            mMethod = mPm.getClass().getMethod("installPackage", TYPES);
            mUninstallMethod = mPm.getClass().getMethod("deletePackage", UNINSTALLTYPES);
        } catch (NoSuchMethodException WTF) {
            Log.e(TAG, "NoSuchMethodException" + WTF);
        }

        String[] packageNames = PACKAGES;
        for (String app : packageNames) {
            if (isInstalled(app)) {
                mInstalledList.add(app);
            }
        }

        if (!_init) {
            mHandler.sendEmptyMessage(MSG_UNINSTALL);
            _init = true;
        }
        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mHandler != null) {
			mHandler.sendEmptyMessage(MSG_FINISH);
            mHandler = null;
        }
        this.stopSelf();
    }

    void shutdown() {
		if (mHandler != null) {
			mHandler.sendEmptyMessage(MSG_FINISH);
		    mHandler = null;
		}
        this.stopSelf();
    }

    private boolean isInstalled(final String packageName) {
        String mVersion;
        try {
            mVersion = mPm.getPackageInfo(packageName, 0).versionName;
            if (mVersion.equals(null)) {
                return false;
            }
        } catch (NameNotFoundException e) {
            if (DEBUG) Log.e(TAG, "Package " + packageName + " NameNotFoundException" + e);
            return false;
        }
        return true;
    }

    private class EventHandler extends Handler {
        public void handleMessage(Message m) {
            switch (m.what) {
                case MSG_UNINSTALL:
                    // uninstall
                    try {
                        uninstallPackages();
                    } catch (IllegalAccessException WTF) {
                        Log.e(TAG, "IllegalAccessException" + WTF);
                    } catch (InvocationTargetException BBQ) {
                        Log.e(TAG, "InvocationTargetException" + BBQ);
                    }
                    break;
                case MSG_FINISH:
                    this.removeMessages(0);
                    break;
                default:
                    break;
            }
        }

        private synchronized void uninstallPackages() throws
                IllegalArgumentException, IllegalAccessException, InvocationTargetException {
            String[] packageNames = new String[mInstalledList.size()];
            packageNames = mInstalledList.toArray(packageNames);

            int pid;
            for (String app : packageNames) {
                mPm.setApplicationEnabledSetting(app, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);

                mUninstallMethod.invoke(mPm, new Object[] {
                    app, mObserverDelete, 0
                });

                // Take a pause before attempting the next package.
                try {
                   Thread.sleep(500);
                } catch (InterruptedException WTF) {
                    Log.e(TAG, "InterruptedException" + WTF);
                }
            }
            shutdown();
        }
    }
}

