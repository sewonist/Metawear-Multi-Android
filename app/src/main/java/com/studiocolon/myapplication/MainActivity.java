package com.studiocolon.myapplication;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.mbientlab.metawear.MetaWearBleService;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by studiocolon on 2016-09-27.
 */

public class MainActivity extends Activity implements ServiceConnection {

    final static public String TAG = "MainActivity";
    final static public String UNITY_GAME_OBJECT = "AndroidPlugin";
    private boolean serviceAble = false;
    private MetaWearBleService.LocalBinder binder;
    private Map<String, MetaBoard> map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        map = new HashMap<String, MetaBoard>();

        // Bind the service when the activity is created
        getApplicationContext().bindService(new Intent(this, MetaWearBleService.class),
                this, Context.BIND_AUTO_CREATE);

        Log.i(TAG, "onCreate!!!");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unbind the service when the activity is destroyed
        getApplicationContext().unbindService(this);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.i(TAG, "onServiceConnected");

        // Typecast the binder to the service's LocalBinder class
        binder = (MetaWearBleService.LocalBinder) service;
        serviceAble = true;

        addBoard("D4:EC:00:CC:8A:9F", new Callback() {
            @Override
            public void OnConnect() {
                Log.d(TAG, "D4:EC:00:CC:8A:9F connected.");
                connect("C2:D1:E0:41:85:90");
            }
        });
        addBoard("C2:D1:E0:41:85:90", new Callback() {
            @Override
            public void OnConnect() {
                Log.d(TAG, "C2:D1:E0:41:85:90 connected.");
            }
        });

        connect("D4:EC:00:CC:8A:9F");
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        serviceAble = false;
    }

    //--------------------------------------------------------------------------
    // call from unity
    //--------------------------------------------------------------------------
    public void initUnity(boolean init) {
        Log.i(TAG, "Unity inited");

    }

    public void addBoard(String address, Callback callback) {
        map.put(address, new MetaBoard(this, binder, address, callback));
    }

    public void connect(String address) {
        map.get(address).connect();
    }

    public void disconnect(String address) {
        map.get(address).disconnect();
    }

    public void readRssi(String address) {
        map.get(address).readRssi();
    }

    public void readBattery(String address) {
        map.get(address).readBattery();
    }

}
