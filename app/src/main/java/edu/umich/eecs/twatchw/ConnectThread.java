package edu.umich.eecs.twatchw;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by Arun on 11/17/2014.
 */
public class ConnectThread extends Thread {
    private BluetoothSocket mmSocket;
    private BluetoothAdapter myAdapter;
    private UUID DEFAULT_UUID = UUID.fromString("00001101-0000-1000-8000-10805F9B34FB");
    BluetoothDevice device;
    String TAG = "ConnectThread";
    MainActivity myactivity;

    public ConnectThread(String address, BluetoothAdapter myAdapter, MainActivity myactivity) {
        this.myactivity = myactivity;
        this.myAdapter = myAdapter;
        device = myAdapter.getRemoteDevice(address);
    }

    public void connect () {
        BluetoothSocket tmp = null;
        boolean notconnected = true;

        while (notconnected) {
            try {
                //myactivity.addInfo("BT Connect Thread");
                Log.v(TAG, "Device is: " + device.getAddress());
                tmp = device.createInsecureRfcommSocketToServiceRecord(DEFAULT_UUID);
                tmp.connect();
                notconnected = !tmp.isConnected();
            } catch (Exception e) {
                Log.e(TAG, "Received exception - " + e.getLocalizedMessage());
                //myactivity.addInfo("Failed connection: " + e.getLocalizedMessage());
                Log.v(TAG, "BT Connection failed, retrying in 1 second.");
                try { Thread.sleep(1000); } catch (Exception ee) {}
            }
        }
        mmSocket = tmp;
    }

    public void run () {
        connect();
        myAdapter.cancelDiscovery();
        myactivity.runOnUiThread(new Runnable () {
            public void run () {
                myactivity.setPCSocket(mmSocket);
            }
        });
    }

    public void cancel () {
        try {
            mmSocket.close();
        } catch (IOException e) {}
    }
}
