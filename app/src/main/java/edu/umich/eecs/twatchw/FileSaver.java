package edu.umich.eecs.twatchw;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Created by Arun on 2/22/2015.
 */
public class FileSaver extends Thread {
    TapBuffer  tap;
    MainActivity mainActivity;
    //ArrayList<Byte> btData, recData;
    boolean running = true;
    byte [] tmpBuffer = new byte [44100];

    int saved_count = 0;
    FileOutputStream watch_tmp;

    String TAG = "FileSaver";

    public static String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    public static String AUDIO_RECORDER_FOLDER = "twatch";
    String AUDIO_RECORDER_WATCH_TMP;

    public FileSaver(MainActivity mainActivity, TapBuffer tap) {
        this.mainActivity = mainActivity;
        this.tap = tap;

        //btData = new ArrayList<Byte>();
        //recData = new ArrayList<Byte>();

        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, AUDIO_RECORDER_FOLDER);
        File tmpWatch = new File(filepath, "watch_temp.raw");
        AUDIO_RECORDER_WATCH_TMP = tmpWatch.getAbsolutePath();
    }

    public void run () {
        Log.v(TAG, "Running filesaver in thread " + currentThread().getName());

        while (running) {
            if (!tap.isTapOpen()) try {
                this.sleep(150);
            } catch (Exception e) {
            }
            else {
                if (tap.howMany() != 0) {
                    int got = tap.getSome(tmpBuffer, tmpBuffer.length);
                    //for (int i = 0; i < got; i++) btData.add(tmpBuffer[i]);
                    try {
                        watch_tmp.write(tmpBuffer, 0, got);
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    public String closeFile () {
        try {
            watch_tmp.close();
        } catch (Exception e) {}
        return AUDIO_RECORDER_WATCH_TMP;
    }


    public void startNewFile () {
        File tmpWatch = new File(AUDIO_RECORDER_WATCH_TMP);
        if (tmpWatch.exists()) tmpWatch.delete();
        try {
           watch_tmp = new FileOutputStream(tmpWatch);
        } catch (FileNotFoundException e) { }
    }

    public void shutdown () {
        running = false;
    }
}
