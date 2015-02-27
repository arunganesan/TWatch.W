package edu.umich.eecs.twatchw;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class MainActivity extends Activity {
    TextView statusText;
    SharedPreferences sp;

    SocketThread bsocket;
    BluetoothAdapter mBluetoothAdapter;

    Player player;
    TapBuffer tap;
    Recorder recorder;
    MainActivity mainActivity;

    String TAG = "MainActivity";

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

        // Defaults
        player.setSoftwareVolume(0.1);
        player.setSpace((int)(0.05*44100));
        player.startPlaying();
    }

    private void wireUI () {
        statusText = (TextView)findViewById(R.id.statusText);
        ((ImageView)findViewById(R.id.chirpStream)).setOnClickListener(chirpStreamListener);
        ((ImageView)findViewById(R.id.resyncBT)).setOnClickListener(restartBT);
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

        bsocket.cancel();
        statusText.setText("Disconnected");
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
        Map<Integer, short[]> map = new HashMap<Integer, short[]>();
        map.put(R.id.chirpSound, Player.CHIRP);
        /*
        map.put(R.id.pnSound, Player.PN);
        map.put(R.id.goldSound, Player.GOLD);
        */

        map.put(R.id.whitenoiseSound, Player.WN);
        map.put(R.id.highWhitenoiseSound, Player.WNHIGH);
        map.put(R.id.highWhitenoiseHannSound, Player.WNHIGHHANN);
        map.put(R.id.highChirpSound, Player.CHIRPHIGH);
        map.put(R.id.highChirpHannSound, Player.CHIRPHIGHHANN);


        int id = item.getItemId();
        player.changeSound(map.get(id));

        return super.onOptionsItemSelected(item);
    }

    public void setBTSocket (BluetoothSocket socket) {
        initializeTWatch();

        sp.edit().putString("phone address", socket.getRemoteDevice().getAddress()).commit();
        bsocket = new SocketThread(socket, this, tap);
        bsocket.start();
        statusText.setText("Connected");
    }

    void setupBluetooth () {
        statusText.setText("Connecting...");
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

    Runnable chirpStreamRunner = new Runnable () {
      @Override
      public void run () {
          //player.flipSound();
          player.chirp();
          tap.openTap();
          try { Thread.sleep(5000); } catch (Exception e) {}
          player.stopChirp();
          tap.closeTap();
          //player.flipSound();
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
            (new Thread(chirpStreamRunner)).start();
        }
    };

    View.OnClickListener restartBT = new View.OnClickListener() {
        @Override
        public void onClick (View v) {
            masterShutdown();
            setupBluetooth();
        }
    };

}