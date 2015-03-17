/*
 * Copyright (C) 2014 Dirty Unicorns
 * Copyright (C) 2014 Android Ice Cold Project
 * Copyright (C) 2015 EXODUS
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

package com.android.settings.vanir.hfm;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Set;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.os.StrictMode;
import android.provider.Settings;
import android.widget.TextView;

import com.android.settings.R;

public final class HfmHelpers {
    private static final String TAG = "HfmHelpers";

    private HfmHelpers() {
        throw new AssertionError();
    }

    public static int i = 0;

    public static void checkStatus(Context context) {
        checkStatus(context, false);
    }

    public static void checkStatus(Context context, boolean applyWhitelist) {
        File defHosts = new File("/etc/hosts.og");
        File altHosts = new File("/etc/hosts.alt");
        File altOrigHosts = new File("/etc/hosts.alt_orig");
        File hosts = new File("/etc/hosts");
        try {
            boolean adsDisabled = Settings.System.getInt(context.getContentResolver(), Settings.System.HFM_DISABLE_ADS, 0) == 1;
            if (adsDisabled && areFilesDifferent(hosts, altHosts)) {
                copyFiles(altHosts, hosts);
            } else if ( ! adsDisabled && areFilesDifferent(hosts, defHosts)) {
                copyFiles(defHosts, hosts);
            }
        } catch(IOException e) {
        }
    }

    public static boolean areFilesDifferent(File file1, File file2) throws IOException {
        String cr1, cr2;
        BufferedReader br1 = getBufferedReader(file1);
        BufferedReader br2 = getBufferedReader(file2);
        while ((cr1 = br1.readLine()) != null) {
                if((cr2 = br2.readLine()) != null) {
                        if(cr1.equals(cr2)) {
                            continue;
                        }
                }
                return true;
        }
        return br2.readLine() != null;
    }

    private static BufferedReader getBufferedReader(File file) throws IOException {
            return new BufferedReader(new FileReader(file));
    }

    public static void RunAsRoot(String string) throws IOException {
        Process P = Runtime.getRuntime().exec("su");
        DataOutputStream os = new DataOutputStream(P.getOutputStream());
        os.writeBytes(string + "\n");
        os.writeBytes("exit\n");
        os.flush();
    }

    public static void copyFiles(File srcFile, File dstFile) throws IOException {
        if (srcFile.exists() && dstFile.exists()) {
            String cmd = "mount -o rw,remount /system"
                       + " && rm -f " + dstFile.getAbsolutePath()
                       + " && cp -f " + srcFile.getAbsolutePath() + " " + dstFile.getAbsolutePath()
                       + " && chmod 644 " + dstFile.getAbsolutePath()
                       + " ; mount -o ro,remount /system";
            RunAsRoot(cmd);
        }
    }
}
