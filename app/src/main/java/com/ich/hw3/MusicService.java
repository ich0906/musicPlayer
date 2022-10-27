package com.ich.hw3;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.util.ArrayList;

public class MusicService extends Service implements MediaPlayer.OnPreparedListener{
    MediaPlayer player;

    static String CHANNEL_ID = "Music Service Channel";
    static String CHANNEL_NAME = "Music";
    static int NOTIFICATION_ID = 1234;

    IMusicService.Stub binder = new IMusicService.Stub() {
        @Override
        public int getPosition(){
            return player.getCurrentPosition();
        }

        @Override
        public void changeNotification(){
            refreshNotification();
        }

        @Override
        public void seek(int pos){
            if(player != null)
                player.seekTo(pos);
        }

        @Override
        public void playMusic(){
            if(player != null)
                player.start();
        }

        @Override
        public void pauseMusic(){
            if(player != null)
                player.pause();
        }
    };

    ArrayList<Song> songs;
    int curPos = 0;

    BroadcastReceiver receiver;
    boolean isMusicChanging = false;

    @Override
    public void onCreate() {
        super.onCreate();
        registerMusicControlReceivers();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null) {
            songs = (ArrayList<Song>) intent.getSerializableExtra("songs");
            curPos = intent.getIntExtra("pos", 0);

            if (player != null) {
                player.stop();
                player.release();
            }

            player = new MediaPlayer();
            try {
                player.setDataSource(songs.get(curPos).path);
                player.setOnPreparedListener(this);
                player.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        showNotification();

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                Intent intent = new Intent();
                intent.setAction(ActionNames.ACTION_END_MUSIC);
                sendBroadcast(intent);
            }
        });
        mediaPlayer.start();
    }

    @Override
    public void onDestroy() {
        if(player != null)player.release();
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    private void changeMusic(){
        try {
            player.stop();
            player.release();
            player = new MediaPlayer();
            player.setDataSource(songs.get(curPos).path);
            player.setOnPreparedListener(MusicService.this);
            player.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showNotification(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this,CHANNEL_ID);

            builder.setSmallIcon(R.drawable.play);
            builder.setContent(getRemoteViews(true));

            NotificationManager manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);

            Notification notification = builder.build();

            startForeground(NOTIFICATION_ID,notification);
        }else{
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

            builder.setSmallIcon(R.drawable.play);
            builder.setContent(getRemoteViews(true));

            NotificationManager manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

            Notification notification = builder.build();
            manager.notify(NOTIFICATION_ID,notification);

            startForeground(NOTIFICATION_ID,notification);
        }
    }

    private RemoteViews getRemoteViews(boolean isPlaying){
        RemoteViews remoteViews = new RemoteViews(getPackageName(),R.layout.layout_notification);

        Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
        Uri albumUri = ContentUris.withAppendedId(sArtworkUri, songs.get(curPos).albumId);

        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),albumUri);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Set Image View
        Intent imageViewIntent = new Intent();
        imageViewIntent.setAction(ActionNames.ACTION_NOTIFICATION_IMAGE_CLICKED);
        PendingIntent imageViewPendingIntent = PendingIntent.getBroadcast(this,0,imageViewIntent,0);

        if(bitmap != null) remoteViews.setImageViewBitmap(R.id.imageViewNotification,bitmap);
        else remoteViews.setImageViewResource(R.id.imageViewNotification,R.drawable.music_note);
        remoteViews.setOnClickPendingIntent(R.id.imageViewNotification,imageViewPendingIntent);

        // Set Song Name Text
        remoteViews.setTextViewText(R.id.textViewMusicNameNotification,songs.get(curPos).songTitle);

        // Set Previous Button
        Intent previousButtonIntent = new Intent();
        previousButtonIntent.setAction(ActionNames.ACTION_PREVIOUS_CLICKED);
        PendingIntent previousButtonPendingIntent = PendingIntent.getBroadcast(this,0,previousButtonIntent,0);
        remoteViews.setOnClickPendingIntent(R.id.buttonPreviousNotification,previousButtonPendingIntent);

        // Set Next Button
        Intent nextButtonIntent = new Intent();
        nextButtonIntent.setAction(ActionNames.ACTION_NEXT_CLICKED);
        PendingIntent nextButtonPendingIntent = PendingIntent.getBroadcast(this,0,nextButtonIntent,0);
        remoteViews.setOnClickPendingIntent(R.id.buttonNextNotification,nextButtonPendingIntent);

        // Set Play Button
        Intent playButtonIntent = new Intent();
        playButtonIntent.setAction(ActionNames.ACTION_PLAY_CLICKED);
        PendingIntent playButtonPendingIntent = PendingIntent.getBroadcast(this,0,playButtonIntent,0);
        remoteViews.setOnClickPendingIntent(R.id.buttonPlayNotification,playButtonPendingIntent);
        if(isPlaying) remoteViews.setImageViewResource(R.id.buttonPlayNotification,R.drawable.pause);
        else remoteViews.setImageViewResource(R.id.buttonPlayNotification,R.drawable.play);

        // Set Cancel Button
        Intent cancelIntent = new Intent();
        cancelIntent.setAction(ActionNames.ACTION_NOTIFICATION_CANCEL_CLICKER);
        PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(this,0,cancelIntent,0);
        remoteViews.setOnClickPendingIntent(R.id.buttonCancelNotification,cancelPendingIntent);

        return remoteViews;
    }

    private void registerMusicControlReceivers(){
        IntentFilter prevFilter = new IntentFilter();
        prevFilter.addAction(ActionNames.ACTION_PREVIOUS_CLICKED);

        IntentFilter nextFilter = new IntentFilter();
        nextFilter.addAction(ActionNames.ACTION_NEXT_CLICKED);

        IntentFilter playFilter = new IntentFilter();
        playFilter.addAction(ActionNames.ACTION_PLAY_CLICKED);

        IntentFilter imageFilter = new IntentFilter();
        imageFilter.addAction(ActionNames.ACTION_NOTIFICATION_IMAGE_CLICKED);

        IntentFilter cancelFilter = new IntentFilter();
        cancelFilter.addAction(ActionNames.ACTION_NOTIFICATION_CANCEL_CLICKER);

        IntentFilter endFilter = new IntentFilter();
        endFilter.addAction(ActionNames.ACTION_END_MUSIC);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch (action) {
                    case ActionNames.ACTION_END_MUSIC:
                    case ActionNames.ACTION_NEXT_CLICKED:
                        if(player == null)return;

                        curPos = (curPos + 1) % songs.size();

                        isMusicChanging = true;
                        refreshNotification();
                        changeMusic();
                        isMusicChanging = false;
                        break;
                    case ActionNames.ACTION_PREVIOUS_CLICKED:
                        if(player == null)return;

                        if(curPos == 0)curPos = songs.size() - 1;
                        else curPos--;

                        isMusicChanging = true;
                        refreshNotification();
                        changeMusic();
                        isMusicChanging = false;
                        break;
                    case ActionNames.ACTION_PLAY_CLICKED:
                        if (player.isPlaying()) {
                            player.pause();
                        } else {
                            player.start();
                        }
                        refreshNotification();
                        break;
                    case ActionNames.ACTION_NOTIFICATION_IMAGE_CLICKED:
                        Intent imageViewIntent = new Intent(MusicService.this,PlayMusicActivity.class);
                        imageViewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        imageViewIntent.putExtra("songs",songs);
                        imageViewIntent.putExtra("pos",curPos);
                        imageViewIntent.putExtra("seek",player.getCurrentPosition());
                        startActivity(imageViewIntent);
                        break;
                    case ActionNames.ACTION_NOTIFICATION_CANCEL_CLICKER:{
                        stopForeground(true);
                        stopSelf();
                        break;
                    }
                }
            }
        };

        registerReceiver(receiver, prevFilter);
        registerReceiver(receiver, nextFilter);
        registerReceiver(receiver, playFilter);
        registerReceiver(receiver, imageFilter);
        registerReceiver(receiver, cancelFilter);
        registerReceiver(receiver, endFilter);
    }

    private void refreshNotification(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this,CHANNEL_ID);

            if(player.isPlaying() || isMusicChanging)
                builder.setSmallIcon(R.drawable.play);
            else
                builder.setSmallIcon(R.drawable.pause);
            builder.setContent(getRemoteViews(player.isPlaying() || isMusicChanging));
            Notification notification = builder.build();

            NotificationManager manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            manager.notify(NOTIFICATION_ID,notification);
        }
    }
}