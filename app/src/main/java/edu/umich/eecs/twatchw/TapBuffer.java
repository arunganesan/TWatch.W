package edu.umich.eecs.twatchw;

import java.util.ArrayList;

/**
 * Created by Arun on 8/14/2014.
 */
public abstract class TapBuffer {
    ArrayList<Byte> buffer = new ArrayList<Byte>();
    String TAG = "BufferManager";
    final static int FS = 44100;
    boolean open = false;
    boolean dirty = false;
    String name = "";
    MainActivity mainActivity;


    public TapBuffer(String name, MainActivity mainActivity) {
        this.name = name;
        this.mainActivity = mainActivity;
    }


    private int buffer_handle (String command, byte [] arr, int length) { return length; }
    public void openTap () { this.open = true; }
    public void closeTap () { this.open = false; }
    public boolean isTapOpen () { return this.open; }
    public int howMany () { return buffer_handle("size", null, 0); }
    public void addByteArray (byte[] array) { buffer_handle("add", array, array.length); }
    public void addByteArrayLen (byte [] array, int len) { buffer_handle("add", array, len); }
    abstract public void emptyBuffer ();
    abstract public int getSome (byte [] array, int length);
}
