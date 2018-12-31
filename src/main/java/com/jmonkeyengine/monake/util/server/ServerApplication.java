package com.jmonkeyengine.monake.util.server;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.system.JmeContext;
import com.jmonkeyengine.monake.Main;

public class ServerApplication extends SimpleApplication {

    public static Application self;

    public static ServerApplication createApplication() {
        ServerApplication sa = new ServerApplication();
        sa.start(JmeContext.Type.Headless);
        self = sa;
        return sa;
    }

    @Override
    public void simpleInitApp() {
    }

    protected boolean isSelfHosted() {
        return self instanceof Main;
    }
}
