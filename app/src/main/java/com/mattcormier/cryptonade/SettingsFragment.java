package com.mattcormier.cryptonade;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.mattcormier.cryptonade.lib.Crypto;

/**
 * Created by matt on 11/3/2017.
 */

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener , Preference.OnPreferenceClickListener{
    private static final String TAG = "SettingsFragment";

    private SharedPreferences prefs;
    private boolean masterPasswordEnable;
    private Preference prefMasterPass;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: start");
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        prefMasterPass = findPreference("pref_set_master_password");
        prefMasterPass.setOnPreferenceClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        Crypto.saveCurrentScreen(getActivity(), TAG);

        masterPasswordEnable = prefs.getBoolean("pref_set_master_password", false);
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    private void setDefaultLoginPreference(boolean masterPasswordEnable) {
        Preference defaultLogin = findPreference("pref_password_on_start");
        if (masterPasswordEnable) {
            defaultLogin.setEnabled(true);
        } else {
            defaultLogin.setEnabled(false);
        }
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals("pref_set_master_password")) {
            masterPasswordEnable = prefs.getBoolean(key, false);
        }
        this.setDefaultLoginPreference(masterPasswordEnable);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey() == prefMasterPass.getKey()) {
            Context context = getActivity();
            Crypto.setPassword(context, prefs);
        }
        return false;
    }
}
