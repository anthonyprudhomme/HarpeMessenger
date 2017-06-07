package com.harpe.harpemessenger.other;

import android.app.Application;
import android.content.Context;

/**
 * Created by Harpe-e on 06/06/2017.
 */

public class HEMessengerApplication extends Application {

    private static Context context;

    public static Context getAppContext() {
        return HEMessengerApplication.context;
    }

    public void onCreate() {
        super.onCreate();
        HEMessengerApplication.context = getApplicationContext();
    }
}
