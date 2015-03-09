/*
 * Copyright (C) 2015 Exodus
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

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.CmHardwareManager;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.R;

import java.util.Arrays;
import java.util.List;


public class KeyDisablerReceiver extends BroadcastReceiver {

    private static final String TAG = "KeyDisablerReceiver";
    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (intent.getAction().equals("vanir.android.settings.TOGGLE_NAVBAR_FOR_HARDKEYS")) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
            final ContentResolver resolver = ctx.getContentResolver();
            final int defaultBrightness = ctx.getResources().getInteger(
                com.android.internal.R.integer.config_buttonBrightnessSettingDefault);

            boolean forceDisabled = Settings.System.getIntForUser(resolver,
                    Settings.System.DEV_FORCE_DISABLE_HARDKEYS, 0, UserHandle.USER_CURRENT) == 1;
            boolean enabled = Settings.System.getInt(resolver,
                    Settings.System.DEV_FORCE_SHOW_NAVBAR, 0) == 1;
            final CmHardwareManager cmHardwareManager =
                        (CmHardwareManager) context.getSystemService(Context.CMHW_SERVICE);

            Settings.System.putInt(resolver, Settings.System.DEV_FORCE_SHOW_NAVBAR, enabled ? 1 : 0);
            cmHardwareManager.set(CmHardwareManager.FEATURE_KEY_DISABLE, forceDisabled && enabled);

            Editor editor = prefs.edit();
            if (enabled) {
				// save the current butten brightness timeout
                int currentBrightness = Settings.Secure.getInt(resolver,
                        Settings.Secure.BUTTON_BRIGHTNESS, defaultBrightness);
                if (!prefs.contains("pre_navbar_button_backlight")) {
                    editor.putInt("pre_navbar_button_backlight", currentBrightness);
                }
                // set current brightness timeout to zero hiding the buttons
                if (forceDisabled) {
                    Settings.Secure.putInt(resolver,
                            Settings.Secure.BUTTON_BRIGHTNESS, 0);
                } else {
					// reset the brightness timeout
					if (prefs.contains("pre_navbar_button_backlight")) {
						Settings.Secure.putInt(resolver,
								Settings.Secure.BUTTON_BRIGHTNESS,
								prefs.getInt("pre_navbar_button_backlight", defaultBrightness));
						editor.remove("pre_navbar_button_backlight");
					} else {
						Settings.Secure.putInt(resolver,
								Settings.Secure.BUTTON_BRIGHTNESS, defaultBrightness);
					}
				}
            } else {
				if (prefs.contains("pre_navbar_button_backlight")) {
                    Settings.Secure.putInt(resolver,
                            Settings.Secure.BUTTON_BRIGHTNESS,
                            prefs.getInt("pre_navbar_button_backlight", defaultBrightness));
                    editor.remove("pre_navbar_button_backlight");
                } else {
                    Settings.Secure.putInt(resolver,
                            Settings.Secure.BUTTON_BRIGHTNESS, defaultBrightness);
                }
            }
        }
    }
}
