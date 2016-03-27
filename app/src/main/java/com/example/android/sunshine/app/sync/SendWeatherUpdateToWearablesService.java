package com.example.android.sunshine.app.sync;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;

import com.example.android.sunshine.app.Utility;
import com.example.android.sunshine.app.data.WeatherContract;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;

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
        Cursor cursor = getContentResolver().query(WeatherContract.WeatherEntry.buildWeatherLocationWithDate(Utility.getPreferredLocation(getApplicationContext()), System.currentTimeMillis()), null, null, null, null);
        System.out.println("cursor = " + cursor);
        if(cursor != null){
            System.out.println("cursor.getCount() = " + cursor.getCount());

            String fmtHigh = "";
            String fmtLow = "";
            int weatherId = 0;
            while(cursor.moveToNext()){
                int highTextColumnIndex = cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP);
                int lowTempColumnIndex = cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP);
                double highTemp = cursor.getDouble(highTextColumnIndex);
                double lowTemp = cursor.getDouble(lowTempColumnIndex);
                fmtHigh = Utility.formatTemperature(getApplicationContext(), highTemp);
                fmtLow = Utility.formatTemperature(getApplicationContext(), lowTemp);
                int columnIndex = cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID);
                weatherId = cursor.getInt(columnIndex);
                System.out.println("fmtHigh = " + fmtHigh);
                System.out.println("fmtLow = " + fmtLow);
            }


            int iconResourceForWeatherCondition = Utility.getIconResourceForWeatherCondition(weatherId);
            Asset assetFromBitmap = createAssetFromBitmap(iconResourceForWeatherCondition);
//            PutDataRequest request = PutDataRequest.create("/image");
//            request.putAsset("weatherImage", assetFromBitmap);
//            Wearable.DataApi.putDataItem(googleApiClient, request);


            cursor.close();
            PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/weatherUpdate");
            putDataMapReq.getDataMap().putString(KEY_WEATHER_DATA, fmtHigh + ";" + fmtLow);
            putDataMapReq.getDataMap().putLong("Time", System.currentTimeMillis());
            putDataMapReq.getDataMap().putAsset("weatherImage", assetFromBitmap);

            PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
            putDataReq.setUrgent();
            PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(googleApiClient, putDataReq);

            pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                @Override
                public void onResult(DataApi.DataItemResult dataItemResult) {
                    System.out.println("SendWeatherUpdateToWearablesService.onResult");
                    System.out.println("dataItemResult = " + dataItemResult.getDataItem());
                }
            });

        }
    }

    private Asset createAssetFromBitmap(int iconResource) {
        Bitmap bm = BitmapFactory.decodeResource(getApplicationContext().getResources(), iconResource);
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
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
