package com.example.android.sunshine.app;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.WearableListenerService;

public class MyWearableListenerService extends WearableListenerService {

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent dataEvent : dataEvents) {
            int type = dataEvent.getType();
            if(type == DataEvent.TYPE_CHANGED){
                DataItem dataItem = dataEvent.getDataItem();
                DataMap dataMap = DataMapItem.fromDataItem(dataItem).getDataMap();
                String key_weather_data = dataMap.get("KEY_WEATHER_DATA");
                String[] data = key_weather_data.split(";");
                String highTempFormatted = data[0];
                String lowTempFormatted = data[1];
                System.out.println("key_weather_data = " + key_weather_data);
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                prefs.edit().putString("KEY_WEARABLE_WEATHER_DATA", highTempFormatted + " - " + lowTempFormatted).commit();
            }
        }
        System.out.println("MyWearableListenerService.onDataChanged");
        super.onDataChanged(dataEvents);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        System.out.println("MyWearableListenerService.onMessageReceived");
        super.onMessageReceived(messageEvent);
    }

    @Override
    public void onPeerConnected(Node peer) {
        System.out.println("MyWearableListenerService.onPeerConnected");
        super.onPeerConnected(peer);
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        System.out.println("MyWearableListenerService.onPeerDisconnected");
        super.onPeerDisconnected(peer);
    }
}
