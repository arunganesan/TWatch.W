package edu.umich.eecs.twatchw;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;


import android.animation.ValueAnimator;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.TextView;

import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;


public class MainActivity extends Activity {
    SharedPreferences sp;
    String nextMessage;
    ImageView bt, single, draw;
    FrameLayout parentView;
    enum Mode {CONNECTION, SINGLE, DRAW, TEXT}
    String TAG = "Main";


    SocketThread bsocket;
    BluetoothAdapter mBluetoothAdapter;

    Player player;
    TapBuffer tap;
    Recorder recorder;
    MainActivity mainActivity;
    FileSaver fsaver;

    TextView statusText;


    //String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        sp = getSharedPreferences("twatch", Context.MODE_PRIVATE);

        this.mainActivity = this;
        wireUI();


        /*
        1. If already paired before (in shared settings), then skip to 3
        2. If using for first time, turn on discoverability mode
        3. Start a BT socket and wait for connection
        4. If discoverability mode, then add the connected guy to the shared settings
        5. BT connection established

        -- Then turn on tap detection, and BT command listening
        -- Can turn on ChirpStream for some time (or just Chirp)
        -- Can turn on motion detection for some time
         */

        //fakeSetBTSocket();
        setupBluetooth();


        // To just chirp for auto tuning, we do:
        //player.chirp();
        // And then
        //player.stopChirp();

        // To trigger based on double or triple tap is simply a function inside the TapDetector
    }



    private void initializeTWatch () {
        player = new Player(this);
        tap = new SpiralBuffer("BTap", this);
        recorder = new Recorder(this, tap);
        recorder.startRecording();
        player.turnOffSound(true);
        fsaver = new FileSaver(this, tap);
        fsaver.start();


        // Defaults
        player.setSoftwareVolume(0.05);
        player.setSpace((int)(0.5*44100));
        player.startPlaying();

        // AGC Test

        //AGCTest agcTest = new AGCTest();
        //agcTest.test3_1AgcCreateAndRelease();
        //agcTest.test3_2AgcSetEnabledGetEnabled();
    }

    private void wireUI () {
        bt = (ImageView)findViewById(R.id.connectionImage);
        single = (ImageView)findViewById(R.id.tap);
        draw = (ImageView)findViewById(R.id.draw);
        single.setOnClickListener(chirpStreamListener);
        draw.setOnClickListener(chirpStreamListener);
        parentView = (FrameLayout)findViewById(R.id.parentView);

        statusText = (TextView)findViewById(R.id.statusText);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void masterShutdown () {
        if (player != null) {
            player.stopPlaying();
            if (player.audioTrack != null) player.audioTrack.release();
        }

        if (recorder != null) {
            recorder.stopRecording();
            if (recorder.recorder != null) recorder.recorder.release();
        }

        if (bsocket != null) bsocket.cancel();
        setMode(Mode.CONNECTION);
    }

    public void setMode (Mode mode) {
        //if (mode == C)
        parentView.removeAllViews();
        if (mode == Mode.CONNECTION) parentView.addView(bt);
        if (mode == Mode.SINGLE) parentView.addView(single);
        if (mode == Mode.DRAW) parentView.addView(draw);
        if (mode == Mode.TEXT) parentView.addView(statusText);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        System.out.println("OnDestroy");
        masterShutdown();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        //Map<Integer, short[]> map = new HashMap<Integer, short[]>();
        //map.put(R.id.chirpSound, Player.CHIRP);
        /*
        map.put(R.id.pnSound, Player.PN);
        map.put(R.id.goldSound, Player.GOLD);
        */

        /*
        map.put(R.id.whitenoiseSound, Player.WN);

        map.put(R.id.highWhitenoiseSound, Player.WNHIGH);
        map.put(R.id.highWhitenoiseHannSound, Player.WNHIGHHANN);
        map.put(R.id.highChirpSound, Player.CHIRPHIGH);
        map.put(R.id.highChirpHannSound, Player.CHIRPHIGHHANN);
        */


        switch (item.getItemId()) {
            case R.id.restartBluetooth:
                new Thread (new Runnable() {
                    @Override
                    public void run () {
                        masterShutdown();
                        setupBluetooth();
                    }
                }).start();
                break;
            case R.id.changeMode:
                if (parentView.findViewById(single.getId()) != null) setMode(Mode.DRAW);
                else setMode(Mode.SINGLE);
                break;
            case R.id.volumeLowest: player.setSoftwareVolume(0.2); break;
            case R.id.volumeLow: player.setSoftwareVolume(0.4); break;
            case R.id.volumeMedium: player.setSoftwareVolume(0.6); break;
            case R.id.volumeHigh: player.setSoftwareVolume(0.8); break;
            case R.id.volumeHighest: player.setSoftwareVolume(1); break;
            case R.id.volumeSilence: player.setSoftwareVolume(0); break;
            //case R.id.highChirpSound: player.changeSound(Player.CHIRPHIGH); break;
            //case R.id.highWhitenoiseSound: player.changeSound(Player.WNHIGH); break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void fakeSetBTSocket () {
        initializeTWatch();
    }

    public void setBTSocket (BluetoothSocket socket) {
        initializeTWatch();

        sp.edit().putString("phone address", socket.getRemoteDevice().getAddress()).commit();
        bsocket = new SocketThread(socket, this, tap);
        bsocket.start();
        setMode(Mode.TEXT);
    }

    void setupBluetooth () {
        setMode(Mode.CONNECTION);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        int REQUEST_ENABLE_BT = 1;
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        if (!sp.contains("phone address")) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }

        new BluetoothServer(mBluetoothAdapter, this).start();
    }

    public void setSpeed (String mode) {
        if (mode.equals("slow")) {
            player.autotuneSound = Player.LONGCHIRPFORWARD;
            player.beepbeepSound = Player.LONGCHIRP;
            player.setSpace((int)(0.5*44100));
        } else {
            player.autotuneSound = Player.SHORTCHIRPFORWARD;
            player.beepbeepSound = Player.SHORTCHIRP;
            player.setSpace((int)(0.05*44100));
        }

        say("Switch to mode - " + mode);
    }

    Runnable chirpStreamRunnerShort = new Runnable () {
      @Override
      public void run () {

          bsocket.tellPhone(SocketThread.START);
          fsaver.startNewFile();
          tap.openTap();

          try { Thread.sleep(500); } catch (Exception e) {}
          player.chirp();

          try { Thread.sleep(2000); } catch (Exception e) {
              Log.e(TAG, "Couldn't sleep.");
          }

          bsocket.tellPhone(SocketThread.STOP);
          try { Thread.sleep(500); } catch (Exception e) {}

          tap.closeTap();
          tap.emptyBuffer();

          String filename = fsaver.closeFile();
          player.stopChirp();
          bsocket.sendFile(filename);

      }
    };

    public void say (final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run () {
                Toast.makeText(mainActivity, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    View.OnClickListener chirpStreamListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.tap) {
                (new Thread(chirpStreamRunnerShort)).start();
            } else {
                if (player.isSoundOn()) {
                    bsocket.tellPhone(SocketThread.STOP);
                    tap.closeTap();
                    player.stopChirp();
                    String filename = fsaver.closeFile();
                    bsocket.sendFile(filename);
                } else {
                    bsocket.tellPhone(SocketThread.START);
                    fsaver.startNewFile();
                    tap.openTap();
                    player.chirp();
                    addInfo("Beeping...", 250 );
                }
            }
        }
    };


    public void addInfo (final String message, final int time) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ValueAnimator fadeAnim = ObjectAnimator.ofFloat(statusText, "alpha", 1f, 0f);
                fadeAnim.setDuration(time);
                fadeAnim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        statusText.setText(nextMessage);
                        ValueAnimator fadeAnim = ObjectAnimator.ofFloat(statusText, "alpha", 0f, 1f);
                        fadeAnim.setDuration(time);
                        fadeAnim.start();
                    }
                });
                fadeAnim.start();
                nextMessage = message;
            }
        });
    }


}
