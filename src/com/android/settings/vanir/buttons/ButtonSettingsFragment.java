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

package com.android.settings.vanir.buttons;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.settings.R;

import static com.android.internal.util.vanir.HardwareButtonConstants.*;

public class ButtonSettingsFragment extends Fragment {
    private static final String TAG = ButtonSettingsFragment.class.getSimpleName();
    private static final boolean DEBUG = false;

    private int mButtonMask = 0;

    String SINGLE_PRESS_CONSTANT;
    String DOUBLE_PRESS_CONSTANT;
    String LONG_PRESS_CONSTANT;
    String ENABLED_CONSTANT;
    int[] DEFAULTS;

    TextView mSingleActionSummary;
    TextView mLongActionSummary;
    TextView mDoubleActionSummary;

    public ButtonSettingsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mButtonMask = Integer.parseInt(getTag());
        setButtonClass();

        if (DEBUG) Log.e(TAG, "Added a new fragment " + ButtonsTabHost.getStringFromMask(mButtonMask));
    }

    @Override
    public void onResume() {
        super.onResume();
        final Activity activity = getActivity();
        final Resources res = activity.getResources();
        final ContentResolver resolver = activity.getContentResolver();
        final CharSequence[] items = res.getStringArray(R.array.hardware_keys_action_entries);
        final String[] values = res.getStringArray(R.array.hardware_keys_action_values);
        int pressAction;

        if (mSingleActionSummary != null) {
            pressAction = Settings.System.getInt(resolver, SINGLE_PRESS_CONSTANT, DEFAULTS[0]);
            mSingleActionSummary.setText(items[getArrayInt(values, pressAction)]);
        }
        if (mLongActionSummary != null) {
            pressAction = Settings.System.getInt(resolver, LONG_PRESS_CONSTANT, DEFAULTS[1]);
            mLongActionSummary.setText(items[getArrayInt(values, pressAction)]);
        }
        if (mDoubleActionSummary != null) {
            pressAction = Settings.System.getInt(resolver, DOUBLE_PRESS_CONSTANT, DEFAULTS[1]);
            mDoubleActionSummary.setText(items[getArrayInt(values, pressAction)]);
        }
    }

    private int getArrayInt(String[] values, int action) {
        int i = 0;
        for (String value : values) {
            if (Integer.parseInt(value) == action) return i;
            i = i + 1;
        }
        return 0;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.button_settings, container, false);
        final View view = v;

        mSingleActionSummary = (TextView) v.findViewById(R.id.single_press_summary);
        mLongActionSummary = (TextView) v.findViewById(R.id.long_press_summary);
        mDoubleActionSummary = (TextView) v.findViewById(R.id.double_press_summary);

        // single press
        final LinearLayout singlePressLayout = (LinearLayout) v.findViewById(R.id.single_press_layout);
        singlePressLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setAction(mSingleActionSummary, SINGLE_PRESS_CONSTANT);
            }
        });

        // long press
        final LinearLayout longPressLayout = (LinearLayout) v.findViewById(R.id.long_press_layout);
        longPressLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setAction(mLongActionSummary, LONG_PRESS_CONSTANT);
            }
        });

        // double press
        final LinearLayout doublePressLayout = (LinearLayout) v.findViewById(R.id.double_tap_layout);
        doublePressLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setAction(mDoubleActionSummary, DOUBLE_PRESS_CONSTANT);
            }
        });

        // enable switch
        final Switch enabledSwitch = (Switch) v.findViewById(R.id.enable_button_switch);
        TextView switchText = (TextView) v.findViewById(R.id.enable_button_text);
        switchText.setText(getSwitchTitle());
        enabledSwitch.setChecked((Settings.System.getInt(getActivity().getContentResolver(),
                ENABLED_CONSTANT, 1) == 1));
        enabledSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                boolean enabled = enabledSwitch.isChecked();
                Settings.System.putInt(getActivity().getContentResolver(),
                        ENABLED_CONSTANT, enabled ? 1 : 0);
                singlePressLayout.setEnabled(enabled);
            }
        });
        return v;
    }

    void setAction(final TextView v, final String PRESS_CONSTANT) {
        final Activity activity = getActivity();
        final Resources res = activity.getResources();
        final ContentResolver resolver = activity.getContentResolver();
        final CharSequence[] items = res.getStringArray(R.array.hardware_keys_action_entries);
        final String[] values = res.getStringArray(R.array.hardware_keys_action_values);

        int defaultValue = DEFAULTS[0];
        if (PRESS_CONSTANT.equals(SINGLE_PRESS_CONSTANT)) {
            defaultValue = DEFAULTS[0];
        } else if (PRESS_CONSTANT.equals(LONG_PRESS_CONSTANT)) {
            defaultValue = DEFAULTS[1];
        } else if (PRESS_CONSTANT.equals(DOUBLE_PRESS_CONSTANT)) {
            defaultValue = DEFAULTS[2];
        }

        int pressAction = Settings.System.getInt(resolver, PRESS_CONSTANT, defaultValue);

        AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.set_button_actions);
        builder.setSingleChoiceItems(items, getArrayInt(values, pressAction), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    int newAction = Integer.parseInt(values[item]);
                    Settings.System.putInt(resolver, PRESS_CONSTANT, newAction);
                    dialog.dismiss();
                    sendUpdateBroadcast();
                    v.setText(items[getArrayInt(values, newAction)]);
                }
        });
        dialog = builder.create();
        dialog.show();
    }

    private void sendUpdateBroadcast() {
        Intent u = new Intent();
        u.setAction(Intent.ACTION_UPDATE_KEYS);
        getActivity().sendBroadcastAsUser(u, UserHandle.ALL);
    }

    private String getSwitchTitle() {
        Resources res = getActivity().getResources();
        switch (mButtonMask) {
            case KEY_MASK_HOME:
                return res.getString(R.string.enable_home_button);
            case KEY_MASK_BACK:
                return res.getString(R.string.enable_back_button);
            case KEY_MASK_MENU:
                return res.getString(R.string.enable_menu_button);
            case KEY_MASK_APP_SWITCH:
                return res.getString(R.string.enable_app_switch_button);
            case KEY_MASK_ASSIST:
                return res.getString(R.string.enable_assist_button);
        }
        return null;
    }

    private void setButtonClass() {
        switch (mButtonMask) {
            case KEY_MASK_HOME:
                SINGLE_PRESS_CONSTANT = Settings.System.KEY_HOME_ACTION;
                DOUBLE_PRESS_CONSTANT = Settings.System.KEY_HOME_DOUBLE_TAP_ACTION;
                LONG_PRESS_CONSTANT   = Settings.System.KEY_HOME_LONG_PRESS_ACTION;
                ENABLED_CONSTANT      = Settings.System.KEY_HOME_ENABLED;
                DEFAULTS = HOME_DEFAULTS;
                break;

            case KEY_MASK_BACK:
                SINGLE_PRESS_CONSTANT = Settings.System.KEY_BACK_ACTION;
                DOUBLE_PRESS_CONSTANT = Settings.System.KEY_BACK_DOUBLE_TAP_ACTION;
                LONG_PRESS_CONSTANT   = Settings.System.KEY_BACK_LONG_PRESS_ACTION;
                ENABLED_CONSTANT      = Settings.System.KEY_BACK_ENABLED;
                DEFAULTS = BACK_DEFAULTS;
                break;

            case KEY_MASK_ASSIST:
                SINGLE_PRESS_CONSTANT = Settings.System.KEY_ASSIST_ACTION;
                DOUBLE_PRESS_CONSTANT = Settings.System.KEY_ASSIST_DOUBLE_TAP_ACTION;
                LONG_PRESS_CONSTANT   = Settings.System.KEY_ASSIST_LONG_PRESS_ACTION;
                ENABLED_CONSTANT      = Settings.System.KEY_ASSIST_ENABLED;
                DEFAULTS = ASSIST_DEFAULTS;
                break;

            case KEY_MASK_APP_SWITCH:
                SINGLE_PRESS_CONSTANT = Settings.System.KEY_APP_SWITCH_ACTION;
                DOUBLE_PRESS_CONSTANT = Settings.System.KEY_APP_SWITCH_DOUBLE_TAP_ACTION;
                LONG_PRESS_CONSTANT   = Settings.System.KEY_APP_SWITCH_LONG_PRESS_ACTION;
                ENABLED_CONSTANT      = Settings.System.KEY_APPSWITCH_ENABLED;
                DEFAULTS = APPSWITCH_DEFAULTS;
                break;

            case KEY_MASK_MENU:
                SINGLE_PRESS_CONSTANT = Settings.System.KEY_MENU_ACTION;
                DOUBLE_PRESS_CONSTANT = Settings.System.KEY_MENU_DOUBLE_TAP_ACTION;
                LONG_PRESS_CONSTANT   = Settings.System.KEY_MENU_LONG_PRESS_ACTION;
                ENABLED_CONSTANT      = Settings.System.KEY_MENU_ENABLED;
                DEFAULTS = MENU_DEFAULTS;
                break;
         }
     }
}
