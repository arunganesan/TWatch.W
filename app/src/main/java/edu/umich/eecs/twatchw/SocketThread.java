package edu.umich.eecs.twatchw;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

class SocketThread  {
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private MainActivity myactivity;
    private final String TAG = "ListenerClient";
    private TapBuffer  tap;

    byte START_AUTOTUNE = 0;
    byte STOP_AUTOTUNE = 1;
    byte START_BORDER= 2;
    byte START_NORMAL = 3;
    byte DO_TAP = 4;
    byte DO_DRAW = 5;

    private byte [] sendBuffer = new byte [44100];

    public SocketThread(BluetoothSocket socket, MainActivity myactivity, TapBuffer  tap) {
        mmSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        this.myactivity = myactivity;
        this.tap = tap;


        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) { }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;

        //try {
            //mmOutStream.write("PING".getBytes());
        //} catch (IOException e) {}
    }

    public void start () {
        Log.v(TAG, "Starting BT thread");
        (new Thread(listener)).start();
        (new Thread(writer)).start();
    }

    private int primArray (ArrayList<Byte> array) {
        //byte [] prim = new byte [array.size()];
        for (int i = 0; i < array.size(); i++)
            sendBuffer[i] = array.get(i).byteValue();
        return array.size();
    }

    Runnable listener = new Runnable () {
        public void run() {
            int buflen = 1024;
            final byte[] buffer = new byte[buflen];  // buffer store for the stream
            int bytes; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    Log.v(TAG, "Received " + bytes);
                    for (int i = 0; i < bytes; i++) {
                        int command = buffer[i];
                        if (command == START_AUTOTUNE) {
                            myactivity.player.changeSound(Player.CHIRPFORWARD);
                            myactivity.player.chirp();
                            myactivity.player.setSoftwareVolume(0.2);
                        }
                        else if (command == STOP_AUTOTUNE) {
                            myactivity.player.stopChirp();
                        }
                        else if (command == START_NORMAL) {
                            myactivity.player.changeSound(Player.CHIRP);
                            myactivity.player.setSoftwareVolume(0.0);
                            myactivity.driftDetect();
                        }
                        else if (command == DO_TAP) myactivity.single.callOnClick();
                        else if (command == DO_DRAW) myactivity.draw.callOnClick();
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }
    };


    Runnable writer = new Runnable () {
        public void run() {
            Log.v(TAG, "Running writer in thread: " + Thread.currentThread().getName());

            while (true) {
                try {
                    if (tap.howMany() != 0) {
                        int len = tap.getSome(sendBuffer, sendBuffer.length);
                        Log.v(TAG, "Sending " + len + " samples");
                        long start = System.currentTimeMillis();
                        mmOutStream.write(sendBuffer, 0, len);
                        long end = System.currentTimeMillis();

                        Log.v(TAG, "Bluetooth took " + (end - start) + "ms to send " + len + " values.");
                        //Log.v(TAG, "Writing unblocked");
                    } else {
                        if (tap.howMany() == 0 && tap.isTapOpen() == false) tap.emptyBuffer();
                        //Log.v(TAG, "Writing nothing, sleeping.");
                        //Thread.sleep(500);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Got exception - " + e.getLocalizedMessage());
                    break;
                }
                // This needs to happen very fast
                // But we need to be careful that its not killing the CPU... right now its literally busy cycling
            }

            Log.e(TAG, "Exiting writer thread for some reason");
        }
    };



    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }
}