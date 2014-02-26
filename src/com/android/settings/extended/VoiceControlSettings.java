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
    
    private static final String KEY_SHAKE_THRESHOLD = "shake_threshold";
    
    private static final String KEY_LISTEN_SCREEN_ON = "listen_screen_on";
    private static final String KEY_LISTEN_PLUGGED_IN = "listen_plugged_in";
    private static final String KEY_LISTEN_EVERYTIME = "listen_everytime";
    
    private CheckBoxPreference mDisableVoicePref;
    
    private ShakeThresholdDialogPreference mShakeThresholdPref;
    
    private CheckBoxPreference mListenSOnPref;
    private CheckBoxPreference mListenPInPref;
    private CheckBoxPreference mListenETiPref;
    
    private int mCurrentListenValue;
    private boolean mDisableVoiceValue;
    
    public VoiceControlSettings() {
        super();
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.voice_control_settings);
        
        mDisableVoicePref = (CheckBoxPreference) findPreference(KEY_DISABLE_VOICE);
        mDisableVoicePref.setOnPreferenceChangeListener(this);
        
        mShakeThresholdPref = (ShakeThresholdDialogPreference) findPreference(KEY_SHAKE_THRESHOLD);
        mShakeThresholdPref.setOnPreferenceChangeListener(this);
        mShakeThresholdPref.setOnPreferenceClickListener(this);
        
        //Now the listen modes
        mCurrentListenValue = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.JARVIS_SERVICE_LISTEN_WAKE_UP,
                3, ActivityManager.getCurrentUser());
        
        mDisableVoiceValue = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.JARVIS_SERVICE_LISTEN_LOCK,
                0, ActivityManager.getCurrentUser()) == 1;
        
        mListenSOnPref = (CheckBoxPreference) findPreference(KEY_LISTEN_SCREEN_ON);
        mListenSOnPref.setChecked((mCurrentListenValue & 1) > 0);
        mListenSOnPref.setOnPreferenceChangeListener(this);
        
        mListenPInPref = (CheckBoxPreference) findPreference(KEY_LISTEN_PLUGGED_IN);
        mListenPInPref.setChecked((mCurrentListenValue & 2) > 0);
        mListenPInPref.setOnPreferenceChangeListener(this);
        
        mListenETiPref = (CheckBoxPreference) findPreference(KEY_LISTEN_EVERYTIME);
        mListenETiPref.setChecked((mCurrentListenValue & 4) > 0);
        mListenETiPref.setOnPreferenceChangeListener(this);
        
        resetListenModes();
    }
    
    private void resetListenModes() {
        findPreference("service_app_selection").setEnabled(!mDisableVoiceValue);
        mShakeThresholdPref.setEnabled(!mDisableVoiceValue);
        mListenETiPref.setEnabled(!mDisableVoiceValue);
        
        boolean listenEverytime = (mCurrentListenValue & 4) > 0;
        
        mListenPInPref.setEnabled(!listenEverytime && !mDisableVoiceValue);
        mListenSOnPref.setEnabled(!listenEverytime && !mDisableVoiceValue);
    }
    
    private void saveListenSetting() {
        Settings.System.putIntForUser(getContentResolver(),
                Settings.System.JARVIS_SERVICE_LISTEN_WAKE_UP,
                mCurrentListenValue, ActivityManager.getCurrentUser());
    }
    
    private void saveDisabledState() {
        Settings.System.putIntForUser(getContentResolver(),
                Settings.System.JARVIS_SERVICE_LISTEN_LOCK,
                mDisableVoiceValue ? 1 : 0, ActivityManager.getCurrentUser());
    }
    
    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (KEY_DISABLE_VOICE.equals(key)) {
            mDisableVoiceValue = ((Boolean)objValue).booleanValue();
            
            saveDisabledState();
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
