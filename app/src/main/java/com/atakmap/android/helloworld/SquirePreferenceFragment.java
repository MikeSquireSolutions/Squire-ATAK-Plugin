
package com.atakmap.android.helloworld;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.atakmap.android.gui.PanPreference;
import com.atakmap.android.gui.PanSwitchPreference;
import com.atakmap.android.helloworld.plugin.R;
import com.atakmap.android.preference.PluginPreferenceFragment;

public class SquirePreferenceFragment extends PluginPreferenceFragment {

    private static Context staticPluginContext;
    public static final String TAG = "HellWorldPreferenceFragment";

    /**
     * Only will be called after this has been instantiated with the 1-arg constructor.
     * Fragments must has a zero arg constructor.
     */
    public SquirePreferenceFragment() {
        super(staticPluginContext, R.xml.preferences);
    }

    @SuppressLint("ValidFragment")
    public SquirePreferenceFragment(final Context pluginContext) {
        super(pluginContext, R.xml.preferences);
        staticPluginContext = pluginContext;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            PanSwitchPreference offlineSwitch = (PanSwitchPreference) findPreference("squire_setting_offline");
            PanSwitchPreference readbackSwitch = (PanSwitchPreference) findPreference("squire_setting_readback");
            PanPreference disconnectSensors = (PanPreference) findPreference("squire_setting_dc_bt");

            offlineSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
                SharedPreferences medevacPrefs = getActivity().getSharedPreferences("squire_medevac", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = medevacPrefs.edit();
                editor.putBoolean("offline", (boolean) newValue);
                editor.apply();
                return true;
            });
            readbackSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
                SharedPreferences medevacPrefs = getActivity().getSharedPreferences("squire_medevac", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = medevacPrefs.edit();
                editor.putBoolean("readback", (boolean) newValue);
                editor.apply();
                return true;
            });
            /*
            disconnectSensors.setOnPreferenceClickListener(preference -> {
                Log.d(TAG, "Supposed to be doing popup");
                new AlertDialog.Builder(getContext())
                        .setTitle("Confirm")
                        .setMessage("This will stop gathering data from wearable sensors. " +
                                "Are you sure you want to disconnect them?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            Log.d(TAG, "Disconnecting all sensors");
                            Log.d(TAG, "Disconnected " + HelloWorldLifecycle.disconnectAllSensors());
                        })
                        .setNegativeButton("No", null)
                        .setIcon(R.drawable.squire_icon)
                        .show();


                return true;
            });
            */
        } catch (Exception ignored) {
        }
    }

    @Override
    public String getSubTitle() {
        return getSubTitle("Tool Preferences", "Hello World Preferences");
    }
}
