/*
 * Copyright (C) 2013 The CyanogenMod project
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

package com.android.settings;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;

import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import static com.android.internal.util.vanir.HardwareButtonConstants.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ButtonSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "SystemSettings";

    private static final String KEY_HOME_SWITCH = "key_home_enabled";
    private static final String KEY_HOME_ANSWER_CALL = "home_answer_call";
    private static final String KEY_HOME_SHORT_PRESS = "hardware_keys_home_press";
    private static final String KEY_HOME_LONG_PRESS = "hardware_keys_home_long_press";
    private static final String KEY_HOME_DOUBLE_TAP = "hardware_keys_home_double_tap";
    private static final String KEY_BACK_SWITCH = "key_back_enabled";
    private static final String KEY_BACK_PRESS = "hardware_keys_back_press";
    private static final String KEY_BACK_LONG_PRESS = "hardware_keys_back_long_press";
    private static final String KEY_MENU_SWITCH = "key_menu_enabled";
    private static final String KEY_MENU_PRESS = "hardware_keys_menu_press";
    private static final String KEY_MENU_LONG_PRESS = "hardware_keys_menu_long_press";
    private static final String KEY_ASSIST_SWITCH = "key_assist_enabled";
    private static final String KEY_ASSIST_PRESS = "hardware_keys_assist_press";
    private static final String KEY_ASSIST_LONG_PRESS = "hardware_keys_assist_long_press";
    private static final String KEY_APP_SWITCH_SWITCH = "key_appswitch_enabled";
    private static final String KEY_APP_SWITCH_PRESS = "hardware_keys_app_switch_press";
    private static final String KEY_APP_SWITCH_LONG_PRESS = "hardware_keys_app_switch_long_press";

    private static final String CATEGORY_HOME = "home_key";
    private static final String CATEGORY_BACK = "back_key";
    private static final String CATEGORY_MENU = "menu_key";
    private static final String CATEGORY_ASSIST = "assist_key";
    private static final String CATEGORY_APPSWITCH = "app_switch_key";

    // Masks for checking presence of hardware keys.
    // Must match values in frameworks/base/core/res/res/values/config.xml
    public static final int KEY_MASK_HOME = 0x01;
    public static final int KEY_MASK_BACK = 0x02;
    public static final int KEY_MASK_MENU = 0x04;
    public static final int KEY_MASK_ASSIST = 0x08;
    public static final int KEY_MASK_APP_SWITCH = 0x10;

    private ListPreference mHomePressAction;
    private ListPreference mHomeLongPressAction;
    private ListPreference mHomeDoubleTapAction;
    private ListPreference mBackPressAction;
    private ListPreference mBackLongPressAction;
    private ListPreference mMenuPressAction;
    private ListPreference mMenuLongPressAction;
    private ListPreference mAssistPressAction;
    private ListPreference mAssistLongPressAction;
    private ListPreference mAppSwitchPressAction;
    private ListPreference mAppSwitchLongPressAction;
    private CheckBoxPreference mHomeAnswerCall;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.button_settings);

        final PreferenceScreen prefScreen = getPreferenceScreen();

        // Home button answers calls.
        mHomeAnswerCall = (CheckBoxPreference) findPreference(KEY_HOME_ANSWER_CALL);

        HashMap<String, String> prefsToRemove = (HashMap<String, String>)
                getPreferencesToRemove(this, getActivity());
        for (String key : prefsToRemove.keySet()) {
            String category = prefsToRemove.get(key);
            Preference preference = findPreference(key);
            if (category != null) {
                // Parent is a category
                PreferenceCategory preferenceCategory =
                        (PreferenceCategory) findPreference(category);
                if (preferenceCategory != null) {
                    // Preference category might have already been removed
                    preferenceCategory.removePreference(preference);
                }
            } else {
                // Either parent is preference screen, or remove whole category
                removePreference(key);
            }
        }
    }

    private static Map<String, String> getPreferencesToRemove(ButtonSettings settings,
                   Context context) {
        HashMap<String, String> result = new HashMap<String, String>();

        final ContentResolver resolver = context.getContentResolver();
        final Resources res = context.getResources();

        final int deviceKeys = res.getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys);
        final int deviceWakeKeys = res.getInteger(
                com.android.internal.R.integer.config_deviceHardwareWakeKeys);

        final boolean hasHomeKey = (deviceKeys & KEY_MASK_HOME) != 0;
        final boolean hasBackKey = (deviceKeys & KEY_MASK_BACK) != 0;
        final boolean hasMenuKey = (deviceKeys & KEY_MASK_MENU) != 0;
        final boolean hasAssistKey = (deviceKeys & KEY_MASK_ASSIST) != 0;
        final boolean hasAppSwitchKey = (deviceKeys & KEY_MASK_APP_SWITCH) != 0;

        final boolean showHomeWake = (deviceWakeKeys & KEY_MASK_HOME) != 0;
        final boolean showBackWake = (deviceWakeKeys & KEY_MASK_BACK) != 0;
        final boolean showMenuWake = (deviceWakeKeys & KEY_MASK_MENU) != 0;
        final boolean showAssistWake = (deviceWakeKeys & KEY_MASK_ASSIST) != 0;
        final boolean showAppSwitchWake = (deviceWakeKeys & KEY_MASK_APP_SWITCH) != 0;

        boolean hasAnyBindableKey = false;

        if (hasHomeKey) {
            if (!showHomeWake) {
                result.put(Settings.System.HOME_WAKE_SCREEN, CATEGORY_HOME);
            }

            if (!Utils.isVoiceCapable(context)) {
                if (settings != null) {
                    settings.mHomeAnswerCall = null;
                }
                result.put(KEY_HOME_ANSWER_CALL, CATEGORY_HOME);
            }

            int defaultPressAction = KEY_ACTION_HOME;
            if (defaultPressAction < KEY_ACTION_NOTHING ||
                    defaultPressAction > KEY_ACTION_BACK) {
                defaultPressAction = KEY_ACTION_NOTHING;
            }

            int defaultLongPressAction = res.getInteger(
                    com.android.internal.R.integer.config_longPressOnHomeBehavior);
            if (defaultLongPressAction < KEY_ACTION_NOTHING ||
                    defaultLongPressAction > KEY_ACTION_BACK) {
                defaultLongPressAction = KEY_ACTION_NOTHING;
            }

            int defaultDoubleTapAction = res.getInteger(
                    com.android.internal.R.integer.config_doubleTapOnHomeBehavior);
            if (defaultDoubleTapAction < KEY_ACTION_NOTHING ||
                    defaultDoubleTapAction > KEY_ACTION_BACK) {
                defaultDoubleTapAction = KEY_ACTION_NOTHING;
            }

            if (settings != null) {
                int pressAction = Settings.System.getInt(resolver,
                        Settings.System.KEY_HOME_ACTION, defaultPressAction);
                settings.mHomePressAction = settings.initActionList(KEY_HOME_SHORT_PRESS, pressAction);

                int longPressAction = Settings.System.getInt(resolver,
                        Settings.System.KEY_HOME_LONG_PRESS_ACTION,
                        defaultLongPressAction);
                settings.mHomeLongPressAction = settings.initActionList(
                        KEY_HOME_LONG_PRESS, longPressAction);

                int doubleTapAction = Settings.System.getInt(resolver,
                        Settings.System.KEY_HOME_DOUBLE_TAP_ACTION,
                        defaultDoubleTapAction);
                settings.mHomeDoubleTapAction = settings.initActionList(
                        KEY_HOME_DOUBLE_TAP, doubleTapAction);
            }

        } else {
            result.put(CATEGORY_HOME, null);
        }

        if (hasBackKey) {
            if (!showBackWake) {
                result.put(Settings.System.BACK_WAKE_SCREEN, CATEGORY_BACK);
            }

            int defaultPressAction = KEY_ACTION_BACK;
            if (defaultPressAction < KEY_ACTION_NOTHING ||
                    defaultPressAction > KEY_ACTION_BACK) {
                defaultPressAction = KEY_ACTION_NOTHING;
            }

            int defaultLongPressAction = KEY_ACTION_KILL;
            if (defaultLongPressAction < KEY_ACTION_NOTHING ||
                    defaultLongPressAction > KEY_ACTION_BACK) {
                defaultLongPressAction = KEY_ACTION_NOTHING;
            }

            if (settings != null) {
                int pressAction = Settings.System.getInt(resolver,
                        Settings.System.KEY_BACK_ACTION, defaultPressAction);
                settings.mBackPressAction = settings.initActionList(KEY_BACK_PRESS, pressAction);

                int longPressAction = Settings.System.getInt(resolver,
                        Settings.System.KEY_BACK_LONG_PRESS_ACTION,
                        defaultLongPressAction);
                settings.mBackLongPressAction = settings.initActionList(
                        KEY_BACK_LONG_PRESS, longPressAction);
            }
        } else {
            result.put(CATEGORY_BACK, null);
        }

        if (hasMenuKey) {
            if (!showMenuWake) {
                result.put(Settings.System.MENU_WAKE_SCREEN, CATEGORY_MENU);
            }

            if (settings != null) {
                int pressAction = Settings.System.getInt(resolver,
                        Settings.System.KEY_MENU_ACTION, KEY_ACTION_MENU);
                settings.mMenuPressAction = settings.initActionList(
                        KEY_MENU_PRESS, pressAction);

                int longPressAction = Settings.System.getInt(resolver,
                        Settings.System.KEY_MENU_LONG_PRESS_ACTION,
                        hasAssistKey ? KEY_ACTION_NOTHING : KEY_ACTION_SEARCH);
                settings.mMenuLongPressAction = settings.initActionList(
                        KEY_MENU_LONG_PRESS, longPressAction);
            }
        } else {
            result.put(CATEGORY_MENU, null);
        }

        if (hasAssistKey) {
            if (!showAssistWake) {
                result.put(Settings.System.ASSIST_WAKE_SCREEN, CATEGORY_ASSIST);
            }

            if (settings != null) {
                int pressAction = Settings.System.getInt(resolver,
                        Settings.System.KEY_ASSIST_ACTION, KEY_ACTION_SEARCH);
                settings.mAssistPressAction = settings.initActionList(
                        KEY_ASSIST_PRESS, pressAction);

                int longPressAction = Settings.System.getInt(resolver,
                        Settings.System.KEY_ASSIST_LONG_PRESS_ACTION, KEY_ACTION_VOICE_SEARCH);
                settings.mAssistLongPressAction = settings.initActionList(
                        KEY_ASSIST_LONG_PRESS, longPressAction);
            }
        } else {
            result.put(CATEGORY_ASSIST, null);
        }

        if (hasAppSwitchKey) {
            if (!showAppSwitchWake) {
                result.put(Settings.System.APP_SWITCH_WAKE_SCREEN, CATEGORY_APPSWITCH);
            }

            if (settings != null) {
                int pressAction = Settings.System.getInt(resolver,
                        Settings.System.KEY_APP_SWITCH_ACTION, KEY_ACTION_APP_SWITCH);
                settings.mAppSwitchPressAction = settings.initActionList(
                        KEY_APP_SWITCH_PRESS, pressAction);

                int longPressAction = Settings.System.getInt(resolver,
                        Settings.System.KEY_APP_SWITCH_LONG_PRESS_ACTION, KEY_ACTION_NOTHING);
                settings.mAppSwitchLongPressAction = settings.initActionList(
                        KEY_APP_SWITCH_LONG_PRESS, longPressAction);
            }
        } else {
            result.put(CATEGORY_APPSWITCH, null);
        }
        return result;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Home button answers calls.
        if (mHomeAnswerCall != null) {
            final int incallHomeBehavior = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.RING_HOME_BUTTON_BEHAVIOR,
                    Settings.Secure.RING_HOME_BUTTON_BEHAVIOR_DEFAULT);
            final boolean homeButtonAnswersCall =
                (incallHomeBehavior == Settings.Secure.RING_HOME_BUTTON_BEHAVIOR_ANSWER);
            mHomeAnswerCall.setChecked(homeButtonAnswersCall);
        }

    }

    private ListPreference initActionList(String key, int value) {
        ListPreference list = (ListPreference) getPreferenceScreen().findPreference(key);
        list.setValue(Integer.toString(value));
        list.setSummary(list.getEntry());
        list.setOnPreferenceChangeListener(this);
        return list;
    }

    private void handleActionListChange(ListPreference pref, Object newValue, String setting) {
        String value = (String) newValue;
        int index = pref.findIndexOfValue(value);
        pref.setSummary(pref.getEntries()[index]);
        Settings.System.putInt(getContentResolver(), setting, Integer.valueOf(value));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mHomePressAction) {
            handleActionListChange(mHomePressAction, newValue,
                    Settings.System.KEY_HOME_ACTION);
            return true;
        } else if (preference == mHomeLongPressAction) {
            handleActionListChange(mHomeLongPressAction, newValue,
                    Settings.System.KEY_HOME_LONG_PRESS_ACTION);
            return true;
        } else if (preference == mHomeDoubleTapAction) {
            handleActionListChange(mHomeDoubleTapAction, newValue,
                    Settings.System.KEY_HOME_DOUBLE_TAP_ACTION);
            return true;
        } else if (preference == mBackPressAction) {
            handleActionListChange(mBackPressAction, newValue,
                    Settings.System.KEY_BACK_ACTION);
            return true;
        } else if (preference == mBackLongPressAction) {
            handleActionListChange(mBackLongPressAction, newValue,
                    Settings.System.KEY_BACK_LONG_PRESS_ACTION);
            return true;
        } else if (preference == mMenuPressAction) {
            handleActionListChange(mMenuPressAction, newValue,
                    Settings.System.KEY_MENU_ACTION);
            return true;
        } else if (preference == mMenuLongPressAction) {
            handleActionListChange(mMenuLongPressAction, newValue,
                    Settings.System.KEY_MENU_LONG_PRESS_ACTION);
            return true;
        } else if (preference == mAssistPressAction) {
            handleActionListChange(mAssistPressAction, newValue,
                    Settings.System.KEY_ASSIST_ACTION);
            return true;
        } else if (preference == mAssistLongPressAction) {
            handleActionListChange(mAssistLongPressAction, newValue,
                    Settings.System.KEY_ASSIST_LONG_PRESS_ACTION);
            return true;
        } else if (preference == mAppSwitchPressAction) {
            handleActionListChange(mAppSwitchPressAction, newValue,
                    Settings.System.KEY_APP_SWITCH_ACTION);
            return true;
        } else if (preference == mAppSwitchLongPressAction) {
            handleActionListChange(mAppSwitchLongPressAction, newValue,
                    Settings.System.KEY_APP_SWITCH_LONG_PRESS_ACTION);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mHomeAnswerCall) {
            handleToggleHomeButtonAnswersCallPreferenceClick();
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void handleToggleHomeButtonAnswersCallPreferenceClick() {
        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.RING_HOME_BUTTON_BEHAVIOR, (mHomeAnswerCall.isChecked()
                        ? Settings.Secure.RING_HOME_BUTTON_BEHAVIOR_ANSWER
                        : Settings.Secure.RING_HOME_BUTTON_BEHAVIOR_DO_NOTHING));
    }
}
