package com.ich.hw3;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class PlayMusicActivity extends AppCompatActivity {

    ImageView titleImage;
    TextView musicName;
    TextView musicTimeStart;
    TextView musicTimeEnd;
    SeekBar seekBar;
    ImageButton previous;
    ImageButton next;
    ImageButton play_pause;

    ArrayList<Song> songs;
    int curPos = 0;

    IMusicService musicService;
    boolean isBound = false;
    boolean isPlaying = true;
    boolean isSliding = false;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            musicService = IMusicService.Stub.asInterface(iBinder);
            Log.d("Homework","서비스 연결됨");
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isBound = false;
        }
    };

    BroadcastReceiver receiver;
    Thread updateThread;
    Handler updateHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_music);

        initialize();
        setListeners();
        registerMusicReceiver();
        setSeekbarUpdateThread();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    private void initialize(){
        titleImage = findViewById(R.id.imageViewPlayMusicCover);
        musicName = findViewById(R.id.textViewPlayMusicName);
        musicTimeStart = findViewById(R.id.textViewPlayMusicTimeStart);
        musicTimeEnd = findViewById(R.id.textViewPlayMusicTimeEnd);
        seekBar = findViewById(R.id.seekbarPlayMusic);
        previous = findViewById(R.id.buttonPrevious);
        next = findViewById(R.id.buttonNext);
        play_pause = findViewById(R.id.buttonPlay);

        Intent intent = getIntent();
        songs = (ArrayList<Song>)intent.getSerializableExtra("songs");
        curPos = intent.getIntExtra("pos",-1);
        setMusic();

        Intent serviceIntent = new Intent(this,MusicService.class);
        serviceIntent.putExtra("songs",songs);
        serviceIntent.putExtra("pos",curPos);

        int seek = intent.getIntExtra("seek",-1);
        if(seek == -1){
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            }else{
                startService(serviceIntent);
            }
        }else{
            seekBar.setProgress(seek);
        }
        bindService(serviceIntent,connection,BIND_AUTO_CREATE);
    }

    private void setListeners(){
        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendPrevBroadcast();
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendNextBroadcast();
            }
        });

        play_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isBound){
                    try {
                        if (isPlaying) {
                            play_pause.setImageResource(R.drawable.play);
                            musicService.pauseMusic();
                            musicService.changeNotification();
                            isPlaying = false;
                        } else {
                            play_pause.setImageResource(R.drawable.pause);
                            musicService.playMusic();
                            musicService.changeNotification();
                            isPlaying = true;
                        }
                    }catch (RemoteException e){
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void setSeekbarUpdateThread(){
        updateThread = new Thread(){
            @Override
            public void run() {
                int dur = songs.get(curPos).duration;
                int currentPosition = 0;
                while(currentPosition < dur){
                    try{
                        sleep(500);
                        currentPosition = musicService.getPosition();
                        if(!isSliding)seekBar.setProgress(currentPosition);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        };

        seekBar.setMax(songs.get(curPos).duration);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isSliding = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                try {
                    musicService.seek(progress);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                musicTimeStart.setText(durationToString(progress));
                isSliding = false;
            }
        });

        updateThread.start();

        updateHandler = new Handler(Looper.getMainLooper());
        updateHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    musicTimeStart.setText(durationToString(musicService.getPosition()));
                    updateHandler.postDelayed(this,1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        },1000);
    }

    private void sendNextBroadcast(){
        Intent intent = new Intent();
        intent.setAction(ActionNames.ACTION_NEXT_CLICKED);
        sendBroadcast(intent);
    }

    private void sendPrevBroadcast(){
        Intent intent = new Intent();
        intent.setAction(ActionNames.ACTION_PREVIOUS_CLICKED);
        sendBroadcast(intent);
    }

    private void setMusic(){
        setSeekbarUpdateThread();

        musicName.setText(songs.get(curPos).songTitle);

        musicTimeStart.setText("00:00");
        musicTimeEnd.setText(durationToString(songs.get(curPos).duration));

        Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
        Uri albumUri = ContentUris.withAppendedId(sArtworkUri, songs.get(curPos).albumId);

        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),albumUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(bitmap != null)
            titleImage.setImageBitmap(bitmap);
        else
            titleImage.setImageResource(R.drawable.music_note);
    }

    private void registerMusicReceiver(){
        IntentFilter musicEndFilter = new IntentFilter();
        musicEndFilter.addAction(ActionNames.ACTION_END_MUSIC);

        IntentFilter nextMusicFilter = new IntentFilter();
        nextMusicFilter.addAction(ActionNames.ACTION_NEXT_CLICKED);

        IntentFilter prevMusicFilter = new IntentFilter();
        prevMusicFilter.addAction(ActionNames.ACTION_PREVIOUS_CLICKED);

        IntentFilter playMusicFilter = new IntentFilter();
        playMusicFilter.addAction(ActionNames.ACTION_PLAY_CLICKED);

        IntentFilter cancelFilter = new IntentFilter();
        cancelFilter.addAction(ActionNames.ACTION_NOTIFICATION_CANCEL_CLICKER);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch (action){
                    case ActionNames.ACTION_END_MUSIC:
                    case ActionNames.ACTION_NEXT_CLICKED: {
                        curPos = (curPos + 1) % songs.size();
                        setMusic();
                        break;
                    }
                    case ActionNames.ACTION_PREVIOUS_CLICKED:{
                        if(curPos == 0)curPos = songs.size() - 1;
                        else curPos--;
                        setMusic();
                        break;
                    }
                    case ActionNames.ACTION_PLAY_CLICKED:{
                        if (isPlaying) {
                            play_pause.setImageResource(R.drawable.play);
                            isPlaying = false;
                        } else {
                            play_pause.setImageResource(R.drawable.pause);
                            isPlaying = true;
                        }
                        break;
                    }
                    case ActionNames.ACTION_NOTIFICATION_CANCEL_CLICKER:{
                        updateThread.interrupt();
                        updateHandler.removeCallbacksAndMessages(null);
                        finish();
                        break;
                    }
                }
            }
        };

        registerReceiver(receiver,musicEndFilter);
        registerReceiver(receiver,nextMusicFilter);
        registerReceiver(receiver,prevMusicFilter);
        registerReceiver(receiver,playMusicFilter);
        registerReceiver(receiver,cancelFilter);
    }

    private String durationToString(int duration){
        String minutes = String.format("%02d", TimeUnit.MILLISECONDS.toMinutes(duration) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(duration)));
        String seconds = String.format("%02d", TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration)));

        return minutes+":"+seconds;
    }
}