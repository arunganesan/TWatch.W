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

    public static byte START = 6;
    public static byte STOP = 7;
    public static byte STARTFILE = 8;

    public static byte FASTMODE = 9;
    public static byte SLOWMODE = 10;

    public static byte SILENCE = 11;
    public static byte UNSILENCE = 12;
    public static byte PLAY100 = 13;
    public static byte PLAYCONT = 14;

    public static byte SOUND_CHIRP = 15;
    public static byte SOUND_WN = 16;
    public static byte SOUND_HIGHCHIRP = 17;
    public static byte SOUND_CHIRPHANN = 18;


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

    public void sendFile (final String filename) {


        new Thread (new Runnable () {
            @Override
            public void run () {
                byte[] data = new byte[4096];
                FileInputStream in = null;
                int read = 0;
                int total_sent = 0;

                try {
                    in = new FileInputStream(filename);
                    long totalAudioLen = in.getChannel().size();

                    boolean error = false;
                    byte [] startfilecommand = new byte [9];
                    startfilecommand[0] = STARTFILE;
                    byte [] filesize = longToBytes(totalAudioLen);
                    for (int i = 0; i < filesize.length; i++) startfilecommand[i+1] = filesize[i];
                    Log.v(TAG, "Sending file of length: " + totalAudioLen);


                    mmOutStream.write(startfilecommand);
                    mmOutStream.flush();

                    try {
                        Thread.currentThread().sleep(150);
                    } catch (Exception e) {
                    }

                    total_sent = 0;

                    while (!error) {
                        read = in.read(data);
                        error = (read == -1);
                        if (error) {
                            Log.e(TAG, "Reached end of file");
                            break;
                        }

                        mmOutStream.write(data, 0, read);

                        total_sent += read;
                        myactivity.addInfo("Sending file - " + total_sent + "/" + totalAudioLen, 0);
                        //Log.v(TAG, "Total sent " + total_sent + " and remaining " + (totalAudioLen - total_sent));
                        if (totalAudioLen == total_sent) {
                            myactivity.addInfo("Done sending file! :D", 250);
                            Log.v(TAG, "Done sending file! :D");
                            break;
                        }
                    }
                } catch (Exception e) {
                    myactivity.addInfo("Error from sending loop", 0);
                }
            }
        }).start();

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
                        else if (command == DO_DRAW) {
                            myactivity.draw.callOnClick();
                            Log.v(TAG, "Got draw command");
                        }
                        else if (command == SILENCE) myactivity.player.setSoftwareVolume(0.0);
                        else if (command == UNSILENCE) myactivity.player.setSoftwareVolume(0.4);

                        else if (command == PLAY100) myactivity.player.countMode = true;
                        else if (command == PLAYCONT) myactivity.player.countMode = false;

                        else if (command == SOUND_CHIRP) myactivity.setSound(Player.SHORTCHIRP);
                        else if (command == SOUND_CHIRPHANN) myactivity.setSound(Player.CHIRPHANN);
                        else if (command == SOUND_HIGHCHIRP) myactivity.setSound(Player.HIGHCHIRP);
                        else if (command == SOUND_WN) myactivity.setSound(Player.WHITENOISE);
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }
    };



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