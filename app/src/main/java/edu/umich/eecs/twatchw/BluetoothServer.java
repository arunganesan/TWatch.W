package edu.umich.eecs.twatchw;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by Arun on 11/17/2014.
 */
public class BluetoothServer extends Thread {
    private BluetoothServerSocket mmServerSocket = null;
    BluetoothAdapter mBluetoothAdapter = null;
    MainActivity myactivity;
    private UUID DEFAULT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public BluetoothServer(BluetoothAdapter mBluetoothAdapter, MainActivity myactivity) {
        BluetoothServerSocket tmp = null;
        this.myactivity = myactivity;
        this.mBluetoothAdapter = mBluetoothAdapter;
        try {
            tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("Soundprobe", DEFAULT_UUID);
         } catch (IOException e ) {

        }

        mmServerSocket = tmp;
    }

    public void run () {
        while (true) {
            try {
                final BluetoothSocket socket = mmServerSocket.accept();

                if (socket != null) {
                    myactivity.runOnUiThread(new Runnable () {
                        public void run () {
                            myactivity.setBTSocket(socket);
                        }
                    });

                    mmServerSocket.close();
                    break;
                }
            } catch (IOException e) {
            break;
        }

    }
    }
}
