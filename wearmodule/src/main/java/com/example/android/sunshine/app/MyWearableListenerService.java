package com.example.android.sunshine.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemAsset;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MyWearableListenerService extends WearableListenerService {
    GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Bitmap bitmap = null;
        String weatherData = null;

        for (DataEvent dataEvent : dataEvents) {
            int type = dataEvent.getType();
            if(type == DataEvent.TYPE_CHANGED){
                DataItem dataItem = dataEvent.getDataItem();
                Map<String, DataItemAsset> assets = dataItem.getAssets();
                DataMap dataMap = DataMapItem.fromDataItem(dataItem).getDataMap();

                if(assets != null){
                    Asset weatherImage = dataMap.getAsset("weatherImage");
                    bitmap = loadBitmapFromAsset(weatherImage);
                }
                String key_weather_data = dataMap.get("KEY_WEATHER_DATA");
                String[] data = key_weather_data.split(";");
                String highTempFormatted = data[0];
                String lowTempFormatted = data[1];
                weatherData =  highTempFormatted + " - " + lowTempFormatted;
            }
            if(weatherData != null){
                Intent intent = new Intent(this, MyWatchFace.class);
                intent.setAction(MyWatchFace.ACTION_NOTIFY_WEATHER_BITMAP_AVAILABLE);
                intent.putExtra(MyWatchFace.EXTRA_WEATHER_TEXT, weatherData);
                startService(intent);
            }
            if(bitmap != null){
                Intent intent = new Intent(this, MyWatchFace.class);
                intent.setAction(MyWatchFace.ACTION_NOTIFY_WEATHER_BITMAP_AVAILABLE);
                intent.putExtra(MyWatchFace.EXTRA_WEATHER_BITMAP, bitmap);
                startService(intent);
            }
        }

        super.onDataChanged(dataEvents);
    }

    private Bitmap loadBitmapFromAsset(Asset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }
        ConnectionResult result =
                mGoogleApiClient.blockingConnect(5000, TimeUnit.MILLISECONDS);
        if (!result.isSuccess()) {
            return null;
        }
        // convert asset into a file descriptor and block until it's ready
        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                mGoogleApiClient, asset).await().getInputStream();
        mGoogleApiClient.disconnect();

        if (assetInputStream == null) {
            return null;
        }
        // decode the stream into a bitmap
        return BitmapFactory.decodeStream(assetInputStream);
    }
}
