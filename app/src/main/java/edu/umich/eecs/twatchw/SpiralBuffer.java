package edu.umich.eecs.twatchw;

import android.util.Log;

/**
 * Created by Arun on 8/14/2014.
 */
public class SpiralBuffer extends TapBuffer {
    private byte [] buffer = new byte[10*44100];
    private String TAG = "BufferManager";
    private final static int FS = 44100;
    private boolean open = false;
    int head, tail;
    boolean dirty = false;

    int max_size = 0;
    int max_added = 0;
    int num_actions = 0;


    public SpiralBuffer (String name, MainActivity mainActivity) {
        super(name, mainActivity);
    }

    public synchronized int buffer_handle (String action, byte[] array, int length) {
        //Log.e(TAG, "Buffer handling with " + action + " current length is " + buffer.size());
        int saved = 0;

        if (action.equals("add")) {
            dirty = true;
            if (length < (buffer.length - (tail - head))) saved = length;
            else saved = (buffer.length - (tail - head));
            //if (tail + length < buffer.length) saved = length;
            //else saved = (tail+length)-buffer.length;

            if (saved != length) {
                Log.w(TAG, "Skipping data.");
                mainActivity.say("Skipping!");
            }

            for (int i = 0; i < saved; i++)  buffer[(tail + i) % buffer.length] = array[i];

            if ((tail-head) > max_size) max_size = tail-head;
            if (array.length > max_added) max_added = array.length;

            if ((tail-head) > buffer.length) {
                Log.e(TAG, name + " is violating circular buffer assumptions");
            }

            tail += saved;
        } else if (action.equals("get")) {
            if (length > buffer.length) length = buffer.length;
            if (length > (tail - head)) length = (tail - head);

            for (int i = 0; i < length; i++) array[i] = buffer[(head + i) % buffer.length];

            head += length;
            saved = length;
        } else if (action.equals("size")) {
            return tail - head;
        } else if (action.equals("reset")) {
            dirty = false;
            Log.v(TAG, "Cleaning " + name + ". Head is " + head + " tail is " + tail + " and size is " + (tail - head) + " max size was " + max_size + " and max added was " + max_added);
            head = 0;
            tail = 0;
            max_size = 0;
            max_added = 0;
            num_actions = 0;
        }

        if (action.equals("add") || action.equals("get")) {
            num_actions++;
            if (num_actions % 10 == 0) {
                Log.v(TAG, name + ": Head is " + head + " tail is " + tail + " and size is " + (tail - head));
                num_actions = 0;
            }
        }

        return saved;
    }
    
    // Open tap mode
        // - Just store
        // - Get Some
    // Count down mode
        // - Store for X samples
        // - Get all samples when done

    public void openTap () { this.open = true; }
    public void closeTap () { this.open = false; }
    public boolean isTapOpen () { return this.open; }
    public int howMany () { return buffer_handle("size", null, 0); }
    public void emptyBuffer () {
        if (dirty) { buffer_handle("reset", null, buffer.length); }
    }

    public int getSome (byte [] array, int length) {
        if (array.length < length) Log.e(TAG, "Array is not large enough to hold values");
        return buffer_handle ("get", array, length);
    }

    public void addByteArray (byte[] array) {
        buffer_handle("add", array, array.length);
    }

    public void addByteArrayLen (byte [] array, int len) {
        buffer_handle("add", array, len);
    }
}
