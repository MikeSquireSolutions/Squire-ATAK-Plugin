
package com.atakmap.android.helloworld.plugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.atak.plugins.impl.AbstractPluginTool;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.navigation.NavButtonManager;
import com.atakmap.android.navigation.models.NavButtonModel;

import gov.tak.api.util.Disposable;

public class SquireTool extends AbstractPluginTool implements Disposable {

    private final static String TAG = "HelloworldTool";

    public SquireTool(final Context context) {
        super(context,
                context.getString(R.string.app_name),
                context.getString(R.string.app_name),
                context.getResources().getDrawable(R.drawable.ic_launcher),
                "com.atakmap.android.helloworld.SHOW_HELLO_WORLD");

        AtakBroadcast.getInstance().registerReceiver(br,
                new AtakBroadcast.DocumentedIntentFilter(
                        "com.atakmap.android.helloworld.plugin.iconcount"));

    }

    private final BroadcastReceiver br = new BroadcastReceiver() {
        private int count = 0;

        @Override
        public void onReceive(Context c, Intent intent) {
            // Get the button model used by this plugin
            NavButtonModel mdl = NavButtonManager.getInstance()
                    .getModelByPlugin(SquireTool.this);
            if (mdl != null) {
                // Increment the badge count and refresh
                mdl.setBadgeCount(++count);
                NavButtonManager.getInstance().notifyModelChanged(mdl);
                Log.d(TAG, "increment visual count to: " + count);
            }
        }
    };

    @Override
    public void dispose() {
        AtakBroadcast.getInstance().unregisterReceiver(br);
    }

}
