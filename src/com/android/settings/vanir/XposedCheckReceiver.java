/*
 * Copyright 2015 EXODUS - Dave Kessler activethrasher00@gmail.com
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

package com.android.settings.vanir;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.android.settings.R;
import com.vanir.util.DeviceUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/*
 * Xposed exposer => a way to show developers when users have been submitting logcats for bugs that may
 * have been caused by using Xposed frameworks.
 */

public class XposedCheckReceiver extends BroadcastReceiver {
    private final static String TAG = "XPOSED_FRAMEWORKS";

    static final int MSG_XPOSED_INSTALLED = 100;
    static final int MSG_XPOSED_UNINSTALLED = 101;

    volatile boolean _initInstalled = false;

    private XposedHandler mHandler = new XposedHandler();

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (DeviceUtils.isPackageInstalled(ctx, "com.android.settings")) {
            if (!_initInstalled) {
                Toast.makeText(ctx, R.string.xposed_detected, Toast.LENGTH_SHORT).show();
                mHandler.sendEmptyMessageDelayed(MSG_XPOSED_INSTALLED, 1000);
                _initInstalled = true;
            }
        } else {
            mHandler.sendEmptyMessage(MSG_XPOSED_UNINSTALLED);
        }
    }

    private class XposedHandler extends Handler {
        private ScheduledExecutorService mExecutor;

        public XposedHandler() {
            mExecutor = Executors.newSingleThreadScheduledExecutor();
        }

        public void handleMessage(Message m) {
            switch (m.what) {
                case MSG_XPOSED_INSTALLED:
                    if (mExecutor != null) {
                        mExecutor.scheduleAtFixedRate(new Runnable() {
                            @Override
                            public void run() {
                                Log.e(TAG, "I SNIFF LITTLE BOYS' BICYCLE SEATS");
                            }
                        }, 0, 3, TimeUnit.SECONDS);
                    }
                    break;
                case MSG_XPOSED_UNINSTALLED:
                    if (_initInstalled && mExecutor != null) {
                        mExecutor.shutdown();
                        _initInstalled = false;
                    }
                    break;
            }
        }
    }
}
