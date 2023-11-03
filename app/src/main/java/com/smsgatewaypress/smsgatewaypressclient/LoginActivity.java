package com.smsgatewaypress.smsgatewaypressclient;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.datastore.preferences.core.MutablePreferences;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.rxjava3.RxDataStore;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.smsgatewaypress.smsgatewaypressclient.R;

import java.io.IOException;

import io.reactivex.rxjava3.core.Single;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LoginActivity extends AppCompatActivity {

    EditText editTextUrl;
    EditText editTextDeviceId;
    EditText editTextDeviceToken;
    EditText editTextRequestTimeout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        this.editTextUrl = findViewById(R.id.editTextUrl);
        this.editTextDeviceId = findViewById(R.id.editTextDeviceId);
        this.editTextDeviceToken = findViewById(R.id.editTextDeviceToken);
        this.editTextRequestTimeout = findViewById(R.id.editTextRequestTimeout);

        RxDataStore<Preferences> dataStore = App.getDataStore();

        boolean hasUrl = dataStore.data().map(preferences -> preferences.contains(App.SETTING_URL)).blockingFirst();
        boolean hasDeviceId = dataStore.data().map(preferences -> preferences.contains(App.SETTING_DEVICE_ID)).blockingFirst();
        boolean hasDeviceToken = dataStore.data().map(preferences -> preferences.contains(App.SETTING_DEVICE_TOKEN)).blockingFirst();
        boolean hasRequestTimeout = dataStore.data().map(preferences -> preferences.contains(App.SETTING_REQUEST_TIMEOUT)).blockingFirst();

        if (hasUrl) {
            String url = dataStore.data().map(preferences -> preferences.get(App.SETTING_URL)).blockingFirst();
            this.editTextUrl.setText(url);
        }

        if (hasDeviceId) {
            Integer deviceId = dataStore.data().map(preferences -> preferences.get(App.SETTING_DEVICE_ID)).blockingFirst();
            this.editTextDeviceId.setText(String.valueOf(deviceId));
        }

        if (hasDeviceToken) {
            String deviceToken = dataStore.data().map(preferences -> preferences.get(App.SETTING_DEVICE_TOKEN)).blockingFirst();
            this.editTextDeviceToken.setText(deviceToken);
        }

        if (hasRequestTimeout) {
            Integer requestTimeout = dataStore.data().map(preferences -> preferences.get(App.SETTING_REQUEST_TIMEOUT)).blockingFirst();
            this.editTextRequestTimeout.setText(String.valueOf(requestTimeout));
        }
    }

    public void onClickBtnSave(View view) {
        App.getDataStore().updateDataAsync(prefsIn -> {
            MutablePreferences mutablePreferences = prefsIn.toMutablePreferences();

            mutablePreferences.set(App.SETTING_URL, this.editTextUrl.getText().toString());
            mutablePreferences.set(App.SETTING_DEVICE_ID, Integer.valueOf(this.editTextDeviceId.getText().toString()));
            mutablePreferences.set(App.SETTING_DEVICE_TOKEN, this.editTextDeviceToken.getText().toString());
            mutablePreferences.set(App.SETTING_REQUEST_TIMEOUT, Integer.valueOf(this.editTextRequestTimeout.getText().toString()));

            return Single.just(mutablePreferences);
        }).blockingGet();

        finish();
    }

    public void onClickTestConnection(View view) throws IOException {
        String url = this.editTextUrl.getText().toString();
        Integer deviceId = Integer.valueOf(this.editTextDeviceId.getText().toString());
        String deviceToken = this.editTextDeviceToken.getText().toString();

        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

        SMSGatewayPressService service = retrofit.create(SMSGatewayPressService.class);
        Call<SuccessApiResponse> response = service.deviceAuth(deviceId, deviceToken);

        response.enqueue(new Callback<SuccessApiResponse>() {
            @Override
            public void onResponse(Call<SuccessApiResponse> call, Response<SuccessApiResponse> response) {
                if (200 != response.code()) {
                    Toast.makeText(LoginActivity.this, "ERROR", Toast.LENGTH_SHORT).show();
                    return;
                }

                SuccessApiResponse SuccessApiResponse = response.body();

                if (true == SuccessApiResponse.success) {
                    Toast.makeText(LoginActivity.this, "OK", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LoginActivity.this, "ERROR", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SuccessApiResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "FAILURE", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void onClickScanQr(View view) {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Scan a QR Code");
        integrator.setCameraId(0);  // Use the back camera
        integrator.setBeepEnabled(false);
        integrator.setBarcodeImageEnabled(true);
        integrator.setOrientationLocked(false);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Toast.makeText(this, "Scan Cancellaed", Toast.LENGTH_LONG).show();
            } else {
                JsonParser jsonParser = new JsonParser();
                JsonObject jsonObject = jsonParser.parse(result.getContents()).getAsJsonObject();

                if (jsonObject.isJsonObject() &&
                    jsonObject.has("url") &&
                    jsonObject.has("device_id") &&
                    jsonObject.has("device_token") &&
                    jsonObject.has("request_timeout")
                ) {
                    String url = jsonObject.get("url").getAsString();
                    Integer deviceId = jsonObject.get("device_id").getAsInt();
                    String deviceToken = jsonObject.get("device_token").getAsString();
                    Integer requestTimeout = jsonObject.get("request_timeout").getAsInt();

                    editTextUrl.setText(url);
                    editTextDeviceId.setText(String.valueOf(deviceId));
                    editTextDeviceToken.setText(deviceToken);
                    editTextRequestTimeout.setText(String.valueOf(requestTimeout));

                    Toast.makeText(this, "Device QR Scanned", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Bad QR code", Toast.LENGTH_LONG).show();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}