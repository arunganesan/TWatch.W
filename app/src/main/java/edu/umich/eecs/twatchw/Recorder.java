package edu.umich.eecs.twatchw;

/**
 * Created by Arun on 10/24/2014.
 */

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.BassBoost;
import android.media.audiofx.EnvironmentalReverb;
import android.media.audiofx.Equalizer;
import android.media.audiofx.LoudnessEnhancer;
import android.media.audiofx.NoiseSuppressor;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


// Code heavily borrowed from
// http://krvarma-android-samples.googlecode.com/svn/trunk/AudioRecorder.2/src/com/varma/samples/audiorecorder/RecorderActivity.java

public class Recorder {
    static final String TAG = "Recorder";

    public static int RECORDER_SAMPLERATE = 44100;
    public static int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
    public static int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    public static int RECORDER_SOURCE = MediaRecorder.AudioSource.CAMCORDER;

    public AudioRecord recorder = null;
    private AudioManager aManager = null;
    public int bufferSize = 0;
    public boolean isRecording = false;

    MainActivity context;
    TapBuffer  tap;

    public Recorder(MainActivity context, TapBuffer tap) {
        this.context = context;
        this.tap = tap;
        this.aManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING);
    }

    public void turnOffAllEffects (AudioRecord recorder) {
        if (recorder == null) {
            Log.e(TAG, "Recorder not initialized");
            return;
        }

        int rid = recorder.getAudioSessionId();

        AutomaticGainControl.create(rid).setEnabled(false);
        AcousticEchoCanceler.create(rid).setEnabled(false);
        new BassBoost(1000, rid).setEnabled(false);//BassBoost.create?
        new EnvironmentalReverb(1000, rid).setEnabled(false);//EnvironmentalReverb?
        new Equalizer(1000, rid).setEnabled(false);
        new LoudnessEnhancer(rid).setEnabled(false);
        NoiseSuppressor.create(rid).setEnabled(false);
    }

    public void startRecording(){


        recorder = new AudioRecord(RECORDER_SOURCE, RECORDER_SAMPLERATE, RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING, bufferSize);

        turnOffAllEffects(recorder);

        recorder.startRecording();
        isRecording = true;
        new Thread(new Runnable() {

            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                writeAudioDataToFile();
            }
        },"AudioRecorder Thread").start();
    }


    private void writeAudioDataToFile(){
        byte data[] = new byte[bufferSize];
        int read = 0;
        Log.v(TAG, "Buffer size is " + bufferSize);
        while(isRecording){
            read = recorder.read(data, 0, bufferSize);
            if(read != AudioRecord.ERROR_INVALID_OPERATION){
                if (tap.isTapOpen()) tap.addByteArray(data);
            }
        }
    }

    /**
    public Runnable annotateRunner  = new Runnable () {
        @Override
        public void run () {
            collectRecording();
            try { Thread.currentThread().sleep(5000); } catch (InterruptedException e) {}
            saveRecording();
            context.doneExperiment();
        }
    };
     **/

    public void stopRecording(){
        Log.v(TAG, "Stopping recorder");
        Log.v(TAG, "State on complete: " + recorder.getState());
        if(recorder != null){
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
        }
        Log.v(TAG, "Stopped recording.");
    }

}