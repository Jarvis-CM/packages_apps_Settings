/*
 * Copyright (C) 2014 Firtecy
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
package com.android.settings.extended;

import static android.provider.Settings.System.SCREEN_ANIMATION_STYLE;
import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;
import android.app.ActivityManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class VoiceControlSettings extends SettingsPreferenceFragment implements
    Preference.OnPreferenceChangeListener, OnPreferenceClickListener {
    
    private static final String KEY_DISABLE_VOICE = "disable_voice";
    
    private static final String KEY_SERVICE_APP_SELECTION = "service_app_selection";
    
    private static final String KEY_SHAKE_THRESHOLD = "shake_threshold";
    
    private static final String KEY_LISTEN_SCREEN_ON = "listen_screen_on";
    private static final String KEY_LISTEN_PLUGGED_IN = "listen_plugged_in";
    private static final String KEY_LISTEN_EVERYTIME = "listen_everytime";
    
    private CheckBoxPreference mVoiceEnabledPref;
    
    private Preference mServiceSelectionPref;
    
    private ShakeThresholdDialogPreference mShakeThresholdPref;
    
    private CheckBoxPreference mListenSOnPref;
    private CheckBoxPreference mListenPInPref;
    private CheckBoxPreference mListenETiPref;
    
    private int mCurrentListenValue;
    private boolean mVoiceEnabledValue;
    
    private boolean mInitBlock;
    
    public VoiceControlSettings() {
        super();
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.voice_control_settings);
        
        mInitBlock = true;
        
        //Now the listen modes
        mCurrentListenValue = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.JARVIS_SERVICE_LISTEN_WAKE_UP,
                3, ActivityManager.getCurrentUser());
        
        mVoiceEnabledValue = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.JARVIS_SERVICE_LISTEN_ENABLED,
                1, ActivityManager.getCurrentUser()) == 1;
        
        mVoiceEnabledPref = (CheckBoxPreference) findPreference(KEY_DISABLE_VOICE);
        mVoiceEnabledPref.setChecked(mVoiceEnabledValue);
        mVoiceEnabledPref.setOnPreferenceChangeListener(this);
        
        mServiceSelectionPref = findPreference(KEY_SERVICE_APP_SELECTION);
        
        mShakeThresholdPref = (ShakeThresholdDialogPreference) findPreference(KEY_SHAKE_THRESHOLD);
        mShakeThresholdPref.setOnPreferenceChangeListener(this);
        mShakeThresholdPref.setOnPreferenceClickListener(this);
        
        mListenSOnPref = (CheckBoxPreference) findPreference(KEY_LISTEN_SCREEN_ON);
        mListenSOnPref.setChecked((mCurrentListenValue & 1) > 0);
        mListenSOnPref.setOnPreferenceChangeListener(this);
        
        mListenPInPref = (CheckBoxPreference) findPreference(KEY_LISTEN_PLUGGED_IN);
        mListenPInPref.setChecked((mCurrentListenValue & 2) > 0);
        mListenPInPref.setOnPreferenceChangeListener(this);
        
        mListenETiPref = (CheckBoxPreference) findPreference(KEY_LISTEN_EVERYTIME);
        mListenETiPref.setChecked((mCurrentListenValue & 4) > 0);
        mListenETiPref.setOnPreferenceChangeListener(this);
        
        mInitBlock = false;
        
        resetListenModes();
    }
    
    private void resetListenModes() {
        mServiceSelectionPref.setEnabled(mVoiceEnabledValue);
        mShakeThresholdPref.setEnabled(mVoiceEnabledValue);
        mListenETiPref.setEnabled(mVoiceEnabledValue);
        
        boolean listenEverytime = (mCurrentListenValue & 4) > 0;
        
        mListenPInPref.setEnabled(!listenEverytime && mVoiceEnabledValue);
        mListenSOnPref.setEnabled(!listenEverytime && mVoiceEnabledValue);
    }
    
    private void saveListenSetting() {
        Settings.System.putIntForUser(getContentResolver(),
                Settings.System.JARVIS_SERVICE_LISTEN_WAKE_UP,
                mCurrentListenValue, ActivityManager.getCurrentUser());
    }
    
    private void saveEnabledState() {    
        Settings.System.putIntForUser(getContentResolver(),
                Settings.System.JARVIS_SERVICE_LISTEN_ENABLED,
                mVoiceEnabledValue ? 1 : 0, ActivityManager.getCurrentUser());
    }
    
    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if(mInitBlock)
            return true;
        
        final String key = preference.getKey();
        if (KEY_DISABLE_VOICE.equals(key)) {
            mVoiceEnabledValue = ((Boolean)objValue).booleanValue();
            
            saveEnabledState();
            resetListenModes();
        } else if(KEY_LISTEN_SCREEN_ON.equals(key)) {
            if(((Boolean)objValue).booleanValue())
                mCurrentListenValue |= 1;
            else mCurrentListenValue &= ~1;
            
            saveListenSetting();
        } else if(KEY_LISTEN_PLUGGED_IN.equals(key)) {
            if(((Boolean)objValue).booleanValue())
                mCurrentListenValue |= 2;
            else mCurrentListenValue &= ~2;
            
            saveListenSetting();
        } else if(KEY_LISTEN_EVERYTIME.equals(key)) {
            if(((Boolean)objValue).booleanValue())
                mCurrentListenValue |= 4;
            else mCurrentListenValue &= ~4;
            
            saveListenSetting();
            resetListenModes();
        }
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mShakeThresholdPref) {
            mShakeThresholdPref.click();
        }
        return false;
    }
}
