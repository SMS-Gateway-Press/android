package com.smsgatewaypress.smsgatewaypressclient;

import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;

class SuccessApiResponse {
    boolean success;
}

class Action {
    String type;
    JsonObject data;
}

public interface SMSGatewayPressService {

    @POST("wp-json/sms-gateway-press/v1/device-auth")
    Call<SuccessApiResponse> deviceAuth(
        @Header("X-Device-Id") int deviceId,
        @Header("X-Device-Token") String deviceToken
    );

    @GET("wp-json/sms-gateway-press/v1/device-actions")
    Call<List<Action>> getDeviceActions(
        @Header("X-Device-Id") int deviceId,
        @Header("X-Device-Token") String deviceToken
    );

    @PUT("wp-json/sms-gateway-press/v1/update-sms")
    Call<SuccessApiResponse> updateSms(
        @Header("X-Device-Id") int deviceId,
        @Header("X-Device-Token") String deviceToken,
        @Query("sms_id") Integer smsId,
        @Query("sms_token") String smsToken,
        @Query("confirmed") boolean confirmed,
        @Query("sent") boolean sent,
        @Query("delivered") boolean delivered
    );
}
