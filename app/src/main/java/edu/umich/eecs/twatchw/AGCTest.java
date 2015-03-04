package edu.umich.eecs.twatchw;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.AutomaticGainControl;
import android.util.Log;

/**
 * Created by Arun on 3/4/2015.
 */
public class AGCTest {
    // from https://android.googlesource.com/platform/cts/+/7e8e2bddfde25f479fdff76f4f2111550039cf3f/tests/tests/media/src/android/media/cts/AudioPreProcessingTest.java
    String TAG = "AGCTest";

    public AGCTest() {};

    //Test case 3.1: test AGC creation and release
    public void test3_1AgcCreateAndRelease()  {
        AudioRecord ar = getAudioRecord();
        if (ar == null) Log.e(TAG, "could not create AudioRecord");
        boolean isAvailable = AutomaticGainControl.isAvailable();
        AutomaticGainControl agc = AutomaticGainControl.create(ar.getAudioSessionId());
        if ((isAvailable == (agc != null)) == false) Log.e(TAG, "AGC not available but created or available and not created");

        if (agc != null) {
            agc.release();
        }

        ar.release();
    }


    //Test case 3.2: test AGC setEnabled() and getEnabled()
    public void test3_2AgcSetEnabledGetEnabled()  {
        if (!AutomaticGainControl.isAvailable()) {
            return;
        }

        AudioRecord ar = getAudioRecord();
        if (ar == null) Log.e(TAG, "could not create AudioRecord");
        AutomaticGainControl agc = AutomaticGainControl.create(ar.getAudioSessionId());
        if (agc == null) Log.e(TAG, "could not create AutomaticGainControl");
        try {
            agc.setEnabled(true);
            if (agc.getEnabled() == false) Log.e(TAG, "invalid state from getEnabled");
            agc.setEnabled(false);
            if (agc.getEnabled() != false) Log.e(TAG, "invalid state to getEnabled");
            else Log.v(TAG, "we... disabled it....");


            //Log.e(TAG, "Audio effects libraryhas control? " + agc.hasControl());
            AudioEffect.Descriptor[] effects = agc.queryEffects();
            for (int i = 0; i < effects.length; i++)
                Log.e(TAG, "Effect: " + effects[i].name + " and implementor is " + effects[i].implementor);
// test passed
        } catch (IllegalStateException e) {
            Log.e(TAG, "setEnabled() in wrong state");
        } finally {
            agc.release();
            ar.release();
        }
    }

    private AudioRecord getAudioRecord() {
        AudioRecord ar = null;
        try {
            ar = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                    44100,
                    AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    AudioRecord.getMinBufferSize(44100,
                            AudioFormat.CHANNEL_CONFIGURATION_MONO,
                            AudioFormat.ENCODING_PCM_16BIT) * 10);
            if (ar == null) Log.e(TAG, "Could not create AudioRecord");
            if (ar.getState() != AudioRecord.STATE_INITIALIZED)
                Log.e(TAG, "AudioRecord not initialized");
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Illegal argument exception");//fail("AudioRecord invalid parameter");
        }
        return ar;
    }
}
