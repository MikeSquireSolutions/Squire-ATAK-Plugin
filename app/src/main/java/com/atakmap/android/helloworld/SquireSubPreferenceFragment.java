
package com.atakmap.android.helloworld;

import android.annotation.SuppressLint;
import android.content.Context;

import com.atakmap.android.helloworld.plugin.R;
import com.atakmap.android.preference.PluginPreferenceFragment;

public class SquireSubPreferenceFragment extends PluginPreferenceFragment {

    private static Context staticPluginContext;
    public static final String TAG = "HellWorldPreferenceFragment";

    /**
     * Only will be called after this has been instantiated with the 1-arg constructor.
     * Fragments must have a zero arg constructor.
     */
    public SquireSubPreferenceFragment() {
        this(staticPluginContext);
    }

    @SuppressLint("ValidFragment")
    public SquireSubPreferenceFragment(final Context pluginContext) {
        super(pluginContext, R.xml.subpreferences);
        staticPluginContext = pluginContext;
    }

    @Override
    public String getSubTitle() {
        return getSubTitle("Sqiure Preferences", "Squire Sub-Preferences");
    }
}
