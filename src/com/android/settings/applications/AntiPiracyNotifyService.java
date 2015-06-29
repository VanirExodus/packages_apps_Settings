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
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.security.GeneralSecurityException;

/*
 *  Service to uninstall blacklisted apps
 */

public class AntiPiracyNotifyService extends Service {
    static final String TAG = "ANTI-PIRACY SERVICE";
    static final boolean DEBUG = AntiPiracyConstants.DEBUG;

    // Notify service handler
    EventHandler mHandler = new EventHandler();
    static final int MSG_UNINSTALL = 100;

    Context mContext;

    PackageInstallObserver mObserver;
    PackageDeleteObserver mObserverDelete;
    Method mMethod;
    Method mUninstallMethod;
    PackageManager mPm;

    volatile boolean _initInstalled = false;

    List<String> mInstalledList = new ArrayList<String>();

    static Class<?>[] TYPES = new Class[] {
        Uri.class, IPackageInstallObserver.class, int.class, String.class
    };
    static Class<?>[] UNINSTALLTYPES = new Class[] {
        String.class, IPackageDeleteObserver.class, int.class
    };

    public AntiPiracyNotifyService() {
        mContext = this;
    }

    class PackageDeleteObserver extends IPackageDeleteObserver.Stub {
        public void packageDeleted(String packageName, int returnCode) throws RemoteException {
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!_initInstalled) {
            mPm = mContext.getPackageManager();
            mObserver = new PackageInstallObserver();
            mObserverDelete = new PackageDeleteObserver();

            try {
                mMethod = mPm.getClass().getMethod("installPackage", TYPES);
                mUninstallMethod = mPm.getClass().getMethod("deletePackage", UNINSTALLTYPES);
            } catch (NoSuchMethodException WTF) {
                Log.e(TAG, "NoSuchMethodException" + WTF);
            }

            String[] packageNames = AntiPiracyConstants.PACKAGES;
            for (String packagename : packageNames) {
                if (isInstalled(mContext, packagename)) {
                    mInstalledList.add(packagename);
                }
            }

            mHandler.sendEmptyMessage(MSG_UNINSTALL);
            _initInstalled = true;
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
        this.stopSelf();
    }

    private boolean isInstalled(Context ctx, final String packageName) {
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
            }
        }

        private synchronized void uninstallPackages() throws
                IllegalArgumentException, IllegalAccessException, InvocationTargetException {
            String[] packageNames = new String[mInstalledList.size()];
            packageNames = mInstalledList.toArray(packageNames);

            int pid;
            for (String packagename : packageNames) {
                mPm.setApplicationEnabledSetting(packagename, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);

                mUninstallMethod.invoke(mPm, new Object[] {
                    packagename, mObserverDelete, 0
                });

                // Take a pause before attempting the next package.  This should eventually be handled with an AsyncTask class.
                try {
                   Thread.sleep(500);
                } catch (InterruptedException WTF) {
                    Log.e(TAG, "InterruptedException" + WTF);
                }
            }
            stopSelf();
        }
    }
}

