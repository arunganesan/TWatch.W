package edu.umich.eecs.twatchw;

import android.bluetooth.BluetoothSocket;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Data streamer thread. Simply streams the tap data, if there is any, to the bluetooth socket..
 */
public class DataStreamer extends Thread {
    TapBuffer  tap;
    MainActivity mainActivity;
    //ArrayList<Byte> btData, recData;
    boolean running = true;
    byte [] tmpBuffer = new byte [44100];

    OutputStream mmOutStream;

    String TAG = "DataStreamer";



    public DataStreamer(MainActivity mainActivity, TapBuffer tap, BluetoothSocket socket) {
        this.mainActivity = mainActivity;
        this.tap = tap;


        OutputStream tmpOut = null;
        try {
            tmpOut = socket.getOutputStream();
        } catch (IOException e) { }

        mmOutStream = tmpOut;
    }

    public void run () {
        Log.v(TAG, "Running audio streamer in thread " + currentThread().getName());

        while (running) {
            if (tap.howMany() != 0) {
                int got = tap.getSome(tmpBuffer, tmpBuffer.length);
                //for (int i = 0; i < got; i++) btData.add(tmpBuffer[i]);
                try {
                    mmOutStream.write(tmpBuffer, 0, got);
                    //watch_tmp.write(tmpBuffer, 0, got);
                } catch (Exception e) {
                }
            } else {
                try { Thread.sleep(150); } catch (Exception e) {}
            }
        }
    }

    public void shutdown () {
        running = false;
    }
}
