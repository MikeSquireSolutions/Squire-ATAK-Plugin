
package com.atakmap.android.helloworld.plugin;

import com.atak.plugins.impl.AbstractPlugin;
import com.atak.plugins.impl.PluginContextProvider;
import gov.tak.api.plugin.IServiceController;
import com.atakmap.android.helloworld.SquireMapComponent;

public class SquireLifecycle extends AbstractPlugin {

    public SquireLifecycle(IServiceController serviceController) {
        super(serviceController, new SquireTool(serviceController.getService(PluginContextProvider.class).getPluginContext()), new SquireMapComponent());
    }
}
