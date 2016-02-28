package com.example.android.sunshine.app;

import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.WearableListenerService;

public class MyWearableListenerService extends WearableListenerService {

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
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
