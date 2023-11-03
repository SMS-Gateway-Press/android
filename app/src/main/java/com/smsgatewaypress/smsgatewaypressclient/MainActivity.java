package com.smsgatewaypress.smsgatewaypressclient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.rxjava3.RxDataStore;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.smsgatewaypress.smsgatewaypressclient.R;

public class MainActivity extends AppCompatActivity {

    public static String LOG_EVENT = "com.smsgatewaypress.LOG_MESSAGE";
    public static String LOG_MESSAGE = "log_message";

    private Button btnConnect;
    private Button btnEditCredentials;
    private Button btnDisconnect;
    private TextView textViewLogs;

    private BroadcastReceiver connectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            App.connectionStatus = intent.getStringExtra("event");
            updateView();
        }
    };

    private BroadcastReceiver logReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String logMessage = intent.getStringExtra(LOG_MESSAGE);

            if (textViewLogs.getLineCount() > 20) {
                textViewLogs.setText("");
            }

            if (false == textViewLogs.getText().toString().contains(logMessage)) {
                textViewLogs.append(logMessage);
            }
        }
    };

    private ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        if (isGranted) {
            this.doConnect();
        } else {
            Toast.makeText(this, "Permission for send SMS is required.", Toast.LENGTH_SHORT).show();
        }
    });

    public void updateView() {
        if (App.connectionStatus.equals(MyService.CONNECTION_STATUS_CONNECTED)) {
            btnConnect.setVisibility(View.GONE);
            btnEditCredentials.setEnabled(false);
            btnDisconnect.setVisibility(View.VISIBLE);
        } else {
            btnConnect.setVisibility(View.VISIBLE);
            btnEditCredentials.setEnabled(true);
            btnDisconnect.setVisibility(View.GONE);
        }

        if (null != App.myService) {
            textViewLogs.setText("");

            for(String log : App.myService.logs) {
                textViewLogs.append(log);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.registerReceiver(connectionReceiver, new IntentFilter(MyService.CONNECTION_EVENT));
        localBroadcastManager.registerReceiver(logReceiver, new IntentFilter(LOG_EVENT));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.unregisterReceiver(connectionReceiver);
        localBroadcastManager.unregisterReceiver(logReceiver);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnConnect = findViewById(R.id.btnConnect);
        btnEditCredentials = findViewById(R.id.btnEditCredentials);
        btnDisconnect = findViewById(R.id.btnDisconnect);
        textViewLogs = findViewById(R.id.textViewLogs);

        updateView();
    }

    public void onClickBtnConnect(View view) {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            this.doConnect();
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.SEND_SMS);
        }
    }

    public void doConnect() {
        RxDataStore<Preferences> dataStore = App.getDataStore();

        boolean hasUrl = dataStore.data().map(preferences -> preferences.contains(App.SETTING_URL)).blockingFirst();
        boolean hasDeviceId = dataStore.data().map(preferences -> preferences.contains(App.SETTING_DEVICE_ID)).blockingFirst();
        boolean hasDeviceToken = dataStore.data().map(preferences -> preferences.contains(App.SETTING_DEVICE_TOKEN)).blockingFirst();
        boolean hasRequestTimeout = dataStore.data().map(preferences -> preferences.contains(App.SETTING_REQUEST_TIMEOUT)).blockingFirst();

        if (!hasUrl || !hasDeviceId || !hasDeviceToken || !hasRequestTimeout) {
            this.editCredentials();
            return;
        }

        String url = dataStore.data().map(preferences -> preferences.get(App.SETTING_URL)).blockingFirst();
        Integer deviceId = dataStore.data().map(preferences -> preferences.get(App.SETTING_DEVICE_ID)).blockingFirst();
        String deviceToken = dataStore.data().map(preferences -> preferences.get(App.SETTING_DEVICE_TOKEN)).blockingFirst();
        Integer requestTimeout = dataStore.data().map(preferences -> preferences.get(App.SETTING_REQUEST_TIMEOUT)).blockingFirst();

        if (null == App.intentService) {
            App.intentService = new Intent(MainActivity.this, MyService.class);
            App.intentService.putExtra("url", url);
            App.intentService.putExtra("deviceId", deviceId);
            App.intentService.putExtra("deviceToken", deviceToken);
            App.intentService.putExtra("requestTimeout", requestTimeout);

            startForegroundService(App.intentService);
        }
    }

    public void editCredentials() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    public void onClickBtnDisconnect(View view) {
        stopService(App.intentService);
    }

    public void onClickBtnEditCredentials(View view) {
        this.editCredentials();
    }
}