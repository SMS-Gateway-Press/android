package com.smsgatewaypress.smsgatewaypressclient;

import android.app.Application;
import android.content.Intent;

import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder;
import androidx.datastore.rxjava3.RxDataStore;

public class App extends Application {

    private static RxDataStore<Preferences> dataStore;
    public static final String DATASTORE_NAME = "settings";

    public static final Preferences.Key<String> SETTING_URL = PreferencesKeys.stringKey("url");
    public static final Preferences.Key<Integer> SETTING_DEVICE_ID = PreferencesKeys.intKey("device_id");
    public static final Preferences.Key<String> SETTING_DEVICE_TOKEN = PreferencesKeys.stringKey("device_token");
    public static final Preferences.Key<Integer> SETTING_REQUEST_TIMEOUT = PreferencesKeys.intKey("request_timeout");

    public static MyService myService;
    public static Intent intentService;
    public static String connectionStatus;

    public void onCreate() {
        super.onCreate();
        dataStore = new RxPreferenceDataStoreBuilder(getApplicationContext(), DATASTORE_NAME).build();
        resetService();
    }

    public static void resetService() {
        myService = null;
        intentService = null;
        connectionStatus = MyService.CONNECTION_STATUS_DISCONNECTED;
    }

    public static RxDataStore<Preferences> getDataStore() {
        return dataStore;
    }

    public static void setDataStore(RxDataStore<Preferences> dataStore) {
        App.dataStore = dataStore;
    }
}
