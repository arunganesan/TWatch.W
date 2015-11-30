package edu.umich.eecs.twatchw;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.nio.ByteBuffer;

class PhoneSocketThread {
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

    public static byte START = 6;
    public static byte STOP = 7;

    public static byte FASTMODE = 9;
    public static byte SLOWMODE = 10;

    private byte [] sendBuffer = new byte [44100];

    public PhoneSocketThread(BluetoothSocket socket, MainActivity myactivity, TapBuffer tap) {
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
        //(new Thread(writer)).start();
    }

    private int primArray (ArrayList<Byte> array) {
        //byte [] prim = new byte [array.size()];
        for (int i = 0; i < array.size(); i++)
            sendBuffer[i] = array.get(i).byteValue();
        return array.size();
    }

    public byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(x);
        return buffer.array();
    }

    public long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.put(bytes);
        buffer.flip();//need flip
        return buffer.getLong();
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
                            myactivity.player.changeSound(myactivity.player.autotuneSound);
                            myactivity.player.setSoftwareVolume(0.2);
                            myactivity.player.chirp();
                        }
                        else if (command == STOP_AUTOTUNE) {
                            myactivity.player.stopChirp();
                        }
                        else if (command == START_NORMAL) {
                            myactivity.player.changeSound(myactivity.player.beepbeepSound);
                            myactivity.player.setSoftwareVolume(0.4);
                        }
                        else if (command == DO_TAP) myactivity.single.callOnClick();
                        else if (command == DO_DRAW) myactivity.draw.callOnClick();
                        else if (command == FASTMODE) myactivity.setSpeed("fast");
                        else if (command == SLOWMODE) myactivity.setSpeed("slow");
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }
    };


    /**
     * We tell commands to the phone on the same thread as the UI.
     * This takes a very short time so its OK
     *
     * @param COMMAND
     */
    public void tellPhone (byte COMMAND) {
        // The input loop above might be locked on the inputstream.read
        try {
            mmOutStream.write(COMMAND);
        } catch (Exception e) {}
    }

    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }
}