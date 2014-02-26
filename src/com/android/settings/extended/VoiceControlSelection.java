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

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RadioButton;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

import com.android.settings.SettingsPreferenceFragment;

public class VoiceControlSelection extends SettingsPreferenceFragment {
    private static final String TAG = "VoiceControlSettings";

    private static final int REQUESTING_UNINSTALL = 10;

    private PreferenceGroup mPrefGroup;

    private PackageManager mPm;
    private ComponentName[] mServiceComponentSet;
    private ArrayList<ServicePreference> mPrefs;
    private ServicePreference mCurrentService = null;

    public VoiceControlSelection() {
    }

    private OnClickListener mServiceClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            int index = (Integer)v.getTag();
            ServicePreference pref = mPrefs.get(index);
            if (!pref.isChecked) {
                makeCurrentToService(pref);
            }
        }
    };

    private OnClickListener mDeleteClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            int index = (Integer)v.getTag();
            uninstallApp(mPrefs.get(index));
        }
    };

    private void makeCurrentToService(ServicePreference newService) {
        if (mCurrentService != null) {
            mCurrentService.setChecked(false);
        }
        newService.setChecked(true);
        mCurrentService = newService;

        Settings.System.putStringForUser(
                getContentResolver(), 
                Settings.System.JARVIS_SERVICE_KEYS, 
                newService.serviceName.getPackageName() + ";" 
                + newService.serviceName.getClassName(), 
                ActivityManager.getCurrentUser());
    }

    private void uninstallApp(ServicePreference pref) {
        // Uninstallation is done by asking the OS to do it
       Uri packageURI = Uri.parse("package:" + pref.serviceName.getPackageName());
       Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageURI);
       uninstallIntent.putExtra(Intent.EXTRA_UNINSTALL_ALL_USERS, false);
       int requestCode = REQUESTING_UNINSTALL + (pref.isChecked ? 1 : 0);
       startActivityForResult(uninstallIntent, requestCode);
   }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Rebuild the list now that we might have nuked something
        buildServiceList();

        // if the previous home app is now gone, fall back to the system one
        if (requestCode > REQUESTING_UNINSTALL) {
            // if mCurrentService has gone null, it means we didn't find the previously-
            // default service app when rebuilding the list, i.e. it was the one we
            // just uninstalled.  When that happens we make the system-bundled
            // system app the active default.
            if (mCurrentService == null) {
                makeSystemAppService();
            }
        }
    }
    
    private void makeSystemAppService() {
        for (int i = 0; i < mPrefs.size(); i++) {
            ServicePreference pref = mPrefs.get(i);
            if (pref.isSystem) {
                makeCurrentToService(pref);
                break;
            }
        }
    }

    private void buildServiceList() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_JARVIS_VOICE_CONTROL);
        List<ResolveInfo> serviceActivities = mPm.queryIntentServices(intent, 0);
        
        Context context = getActivity();
        
        //Get the current available service package
        String service =
                Settings.System.getString(context.getContentResolver(), Settings.System.JARVIS_SERVICE_KEYS);
        ComponentName currentService = null;
        if(service != null && service.indexOf(';') > 0) {
            String[]parts = service.split(";");
            currentService = new ComponentName(parts[0], parts[1]);
        }

        mCurrentService = null;
        mPrefGroup.removeAll();
        mPrefs = new ArrayList<ServicePreference>();
        
        //If we have no package available, we should take a system one
        if(currentService == null) {
            makeSystemAppService();
        }
        
        mServiceComponentSet = new ComponentName[serviceActivities.size()];
        int prefIndex = 0;
        for (int i = 0; i < serviceActivities.size(); i++) {
            ServiceInfo info = serviceActivities.get(i).serviceInfo;
            
            ComponentName serviceName = new ComponentName(info.packageName, info.name);
            mServiceComponentSet[i] = serviceName;
            
            try {
                Drawable icon = info.loadIcon(mPm);
                CharSequence name = info.loadLabel(mPm);
                boolean isSystem = (info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                ServicePreference pref = new ServicePreference(context, serviceName, prefIndex,
                        icon, name, this, isSystem);
                mPrefs.add(pref);
                mPrefGroup.addPreference(pref);
                pref.setEnabled(true);
                
                if (serviceName.equals(currentService)) {
                    mCurrentService = pref;
                }
                prefIndex++;
            } catch (Exception e) {
                Log.v(TAG, "Problem dealing with service " + serviceName, e);
            }
        }

        if (mCurrentService != null) {
            new Handler().post(new Runnable() {
               public void run() {
                   mCurrentService.setChecked(true);
               }
            });
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.voice_control_selection);

        mPm = getPackageManager();
        mPrefGroup = (PreferenceGroup) findPreference("voice_control");
    }

    @Override
    public void onResume() {
        super.onResume();
        buildServiceList();
    }

    class ServicePreference extends Preference {
        ComponentName serviceName;
        int index;
        boolean isSystem;
        VoiceControlSelection fragment;
        final ColorFilter grayscaleFilter;
        boolean isChecked;

        public ServicePreference(Context context, ComponentName activity,
                int i, Drawable icon, CharSequence title,
                VoiceControlSelection parent, boolean sys) {
            super(context);
            setLayoutResource(R.layout.preference_listen_app);
            setIcon(icon);
            setTitle(title);
            serviceName = activity;
            fragment = parent;
            index = i;
            isSystem = sys;

            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(0f);
            float[] matrix = colorMatrix.getArray();
            matrix[18] = 0.5f;
            grayscaleFilter = new ColorMatrixColorFilter(colorMatrix);
        }

        @Override
        protected void onBindView(View view) {
            super.onBindView(view);

            RadioButton radio = (RadioButton) view.findViewById(R.id.home_radio);
            radio.setChecked(isChecked);

            Integer indexObj = new Integer(index);

            ImageView icon = (ImageView) view.findViewById(R.id.app_uninstall);
            if (isSystem) {
                icon.setEnabled(false);
                icon.setColorFilter(grayscaleFilter);
            } else {
                icon.setOnClickListener(mDeleteClickListener);
                icon.setTag(indexObj);
            }

            View v = view.findViewById(R.id.voice_control_app_pref);
            v.setOnClickListener(mServiceClickListener);
            v.setTag(indexObj);
        }

        private void setChecked(boolean state) {
            if (state != isChecked) {
                isChecked = state;
                notifyChanged();
            }
        }
    }
}