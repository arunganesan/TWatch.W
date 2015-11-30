package edu.umich.eecs.twatchw;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

class PCSocketThread {
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
    public static byte STARTFILE = 8;

    public static byte FASTMODE = 9;
    public static byte SLOWMODE = 10;

    private byte [] sendBuffer = new byte [44100];

    public PCSocketThread(BluetoothSocket socket, MainActivity myactivity, TapBuffer tap) {
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
}