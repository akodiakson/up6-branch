package com.example.android.sunshine.app.sync;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

public class SendWeatherUpdateToWearablesService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final String ACTION_UPDATE_WEARABLE_WEATHER_DATA = "ACTION_UPDATE_WEARABLE_WEATHER_DATA";
    private static final String KEY_WEATHER_DATA = "KEY_WEATHER_DATA";
    private GoogleApiClient googleApiClient;

    public SendWeatherUpdateToWearablesService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("SendWeatherUpdateToWearablesService.onStartCommand");
        if (intent == null) {
            return super.onStartCommand(intent, flags, startId);
        }

        if (ACTION_UPDATE_WEARABLE_WEATHER_DATA.equals(intent.getAction())) {
            initGooglePlayServices();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void initGooglePlayServices() {
        System.out.println("SendWeatherUpdateToWearablesService.initGooglePlayServices");
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApiIfAvailable(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        googleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        System.out.println("SendWeatherUpdateToWearablesService.onConnected");
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/weatherUpdate");
        putDataMapReq.getDataMap().putString(KEY_WEATHER_DATA, "some generic weather data");
        putDataMapReq.getDataMap().putLong("Time", System.currentTimeMillis());
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        putDataReq.setUrgent();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(googleApiClient, putDataReq);

        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(DataApi.DataItemResult dataItemResult) {
                System.out.println("SendWeatherUpdateToWearablesService.onResult");
                System.out.println("dataItemResult = " + dataItemResult);
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
        System.out.println("SendWeatherUpdateToWearablesService.onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        System.out.println("SendWeatherUpdateToWearablesService.onConnectionFailed");
    }
}
