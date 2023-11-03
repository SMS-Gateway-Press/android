package com.smsgatewaypress.smsgatewaypressclient;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.telephony.SmsManager;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.smsgatewaypress.smsgatewaypressclient.R;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MyService extends IntentService {

    public static String CONNECTION_EVENT = "com.smsgatewaypress.CONNECTION";
    public static String CONNECTION_STATUS_CONNECTED = "CONNECTED";
    public static String CONNECTION_STATUS_DISCONNECTED = "DISCONNECTED";

    public static String NOTIFICATION_CHANNEL_ID = "SMS_Gateway_Press";

    String SENT = "SMS_SENT";
    String DELIVERED = "SMS_DELIVERED";

    SMSGatewayPressService apiService;

    Integer deviceId;
    String deviceToken;
    Integer requestTimeout;

    Boolean stopped = false;

    OkHttpClient okHttpClient;

    ArrayList<String> logs = new ArrayList<>();

    public MyService() {
        super("MyService");
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onHandleIntent(Intent intent) {

        // solo se admite una instancia del servicio.
        if (null != App.myService) {
            stopSelf();
            return;
        } else {
            App.myService = this;
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        );

        NotificationChannel channel = new NotificationChannel
                (NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_ID, NotificationManager.IMPORTANCE_HIGH);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        Notification notification =
            new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("WP SMS Gateway")
                .setContentText("Service is running")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        BroadcastReceiver sentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent intent) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Integer deviceId = intent.getIntExtra("deviceId", 0);
                        String deviceToken = intent.getStringExtra("deviceToken");
                        Integer smsId = intent.getIntExtra("smsId", 0);
                        String smsToken = intent.getStringExtra("smsToken");

                        Call<SuccessApiResponse> request = apiService.updateSms(
                                deviceId, deviceToken, smsId, smsToken, false, true, false
                        );

                        request.enqueue(new Callback<SuccessApiResponse>() {
                            @Override
                            public void onResponse(Call<SuccessApiResponse> call, Response<SuccessApiResponse> response) {
                                if (200 == response.code() && true == response.body().success) {
                                    writeLog("The SMS(" + String.valueOf(smsId) + ") was sent.");
                                }
                            }

                            @Override
                            public void onFailure(Call<SuccessApiResponse> call, Throwable t) {
                            }
                        });
                        break;
                }
            }
        };

        BroadcastReceiver deliveredReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent intent) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Integer deviceId = intent.getIntExtra("deviceId", 0);
                        String deviceToken = intent.getStringExtra("deviceToken");
                        Integer smsId = intent.getIntExtra("smsId", 0);
                        String smsToken = intent.getStringExtra("smsToken");

                        Call<SuccessApiResponse> request = apiService.updateSms(
                                deviceId, deviceToken, smsId, smsToken, false, false, true
                        );

                        request.enqueue(new Callback<SuccessApiResponse>() {
                            @Override
                            public void onResponse(Call<SuccessApiResponse> call, Response<SuccessApiResponse> response) {
                                if (200 == response.code() && true == response.body().success) {
                                    writeLog("The SMS(" + String.valueOf(smsId) + ") was delivered.");
                                }
                            }

                            @Override
                            public void onFailure(Call<SuccessApiResponse> call, Throwable t) {
                            }
                        });
                        break;
                }
            }
        };

        //---when the SMS has been sent---
        registerReceiver(sentReceiver, new IntentFilter(SENT));
        //---when the SMS has been delivered---
        registerReceiver(deliveredReceiver, new IntentFilter(DELIVERED));

        String url = intent.getStringExtra("url");
        deviceId = intent.getIntExtra("deviceId", 0);
        deviceToken = intent.getStringExtra("deviceToken");
        requestTimeout = intent.getIntExtra("requestTimeout", 0);

        okHttpClient = new OkHttpClient.Builder()
            .readTimeout(requestTimeout, TimeUnit.SECONDS)
            .build();

        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build();

        apiService = retrofit.create(SMSGatewayPressService.class);

        Intent broadcastIntent = new Intent(CONNECTION_EVENT);
        broadcastIntent.putExtra("event", CONNECTION_STATUS_CONNECTED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);

        try {
            while (true) {
                if (true == stopped) {
                    break;
                }

                writeLog("Polling.");
                Call<List<Action>> request = apiService.getDeviceActions(deviceId, deviceToken);
                Response<List<Action>> response = request.execute();

                if (401 == response.code() || 403 == response.code()) {
                    writeLog("ERROR: Bad Credentials.");
                    stopSelf();
                    break;
                }

                List<Action> actions = response.body();

                if (null == actions) {
                    writeLog("End of polling without actions.");
                    continue;
                }

                for (Action action : actions) {
                    if (action.type.equals("send_sms")) {
                        Integer smsId = action.data.get("id").getAsInt();
                        String smsToken = action.data.get("token").getAsString();
                        String phoneNumber = action.data.get("phone_number").getAsString();
                        String smsText = action.data.get("text").getAsString();

                        writeLog("New SMS Action: " + "smsId='" + smsId + "' phoneNumber='" + phoneNumber + "', text='" + smsText + "'");

                        Call<SuccessApiResponse> confirmSmsRequest = apiService.updateSms(
                            deviceId, deviceToken, smsId, smsToken, true, false, false
                        );

                        Response<SuccessApiResponse> confirmSmsResponse = confirmSmsRequest.execute();

                        if (200 == confirmSmsResponse.code() && true == confirmSmsResponse.body().success) {
                            Intent sentIntent = new Intent(SENT);
                            sentIntent.putExtra("deviceId", deviceId);
                            sentIntent.putExtra("deviceToken", deviceToken);
                            sentIntent.putExtra("smsId", smsId);
                            sentIntent.putExtra("smsToken", smsToken);

                            Intent deliveredIntent = new Intent(DELIVERED);
                            deliveredIntent.putExtra("deviceId", deviceId);
                            deliveredIntent.putExtra("deviceToken", deviceToken);
                            deliveredIntent.putExtra("smsId", smsId);
                            deliveredIntent.putExtra("smsToken", smsToken);

                            Integer flag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ?
                                    PendingIntent.FLAG_IMMUTABLE :
                                    PendingIntent.FLAG_UPDATE_CURRENT;

                            PendingIntent sentPI = PendingIntent.getBroadcast(
                                this, smsId, sentIntent, flag
                            );
                            PendingIntent deliveredPI = PendingIntent.getBroadcast(
                                this, smsId, deliveredIntent, flag
                            );

                            SmsManager sms = SmsManager.getDefault();
                            sms.sendTextMessage(phoneNumber, null, smsText, sentPI, deliveredPI);
                        }
                    }
                }
            }
        } catch (IOException e) {
            Toast.makeText(this, "Service Unavailable", Toast.LENGTH_SHORT).show();
            stopService(intent);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopped = true;

        App.resetService();
        okHttpClient.dispatcher().cancelAll();

        Intent broadcastIntent = new Intent(CONNECTION_EVENT);
        broadcastIntent.putExtra("event", CONNECTION_STATUS_DISCONNECTED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);

        writeLog("Service Stopped");
    }

    public void writeLog(String log) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String logMessage = dtf.format(now) + ": " + log + "\n";

        logs.add(logMessage);

        if (logs.size() > 50) {
            logs.clear();
        }

        Intent broadcastIntent = new Intent(MainActivity.LOG_EVENT);
        broadcastIntent.putExtra(MainActivity.LOG_MESSAGE, logMessage);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
    }
}
