package com.uroflowmetry;

import android.content.Context;

import android.content.SharedPreferences;
import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;
import com.orm.SugarContext;

public class App extends MultiDexApplication {

    static SharedPreferences preferences;
    static SharedPreferences.Editor prefEditor;

    public static String PREF_KEY_USER_ID = "USER_ID";
    public static String PREF_KEY_PASSWORD = "PASSWORD";

    private static App mInstance;

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(context);
        MultiDex.install(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        SugarContext.terminate();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        SugarContext.init(this);

        mInstance = this;
        preferences = getSharedPreferences("uroflowmetry", Context.MODE_PRIVATE);
        prefEditor = preferences.edit();
    }

    public static void setUserId(String UserId) {
        prefEditor.putString(PREF_KEY_USER_ID, UserId);
        prefEditor.commit();
    }

    public static String getUserId() {
        return preferences.getString(PREF_KEY_USER_ID, "");
    }

    public static void setPassword(String password)  {
        prefEditor.putString(PREF_KEY_PASSWORD,password);
        prefEditor.commit();
    }
    public static String getPassword(){
        return preferences.getString(PREF_KEY_PASSWORD,"");
    }

}
