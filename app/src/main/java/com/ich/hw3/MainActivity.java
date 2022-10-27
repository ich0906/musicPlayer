package com.ich.hw3;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity{
    MediaPlayer player;
    ListView listView;
    ArrayList<Song> mList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        scanMedia();
        requestPermission();
    }

    private void requestPermission(){
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

        if(permission == PackageManager.PERMISSION_GRANTED){
            initialize();
        }else{
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);
        }
    }

    private void initialize(){
        listView = findViewById(R.id.listViewMain);
        player = new MediaPlayer();
        mList = getSongs(this);

        listView.setAdapter(new MusicAdapter(this,mList));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                startPlayMusicActivity(i);
            }
        });
    }

    private void startPlayMusicActivity(int songPos){
        Intent intent = new Intent(this,PlayMusicActivity.class);
        intent.putExtra("songs",mList);
        intent.putExtra("pos",songPos);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initialize();
            } else {
                finish();
            }
        }
    }

    private void scanMedia(){
        String root = Environment.getExternalStorageDirectory().getAbsolutePath();
        MediaScannerConnection.scanFile(this,
                new String[] { root }, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                    }
                });
    }

    private ArrayList<Song> getSongs(Context context){
        ArrayList<Song> songs = new ArrayList<>();

        Cursor c = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,null,
                MediaStore.Audio.Media.IS_MUSIC + "!=0",null,null);

        c.moveToFirst();

        if(c != null && c.getCount() > 0){
            do{
                Song song = new Song();
                song.songId = c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                song.path = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                song.songTitle = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                song.songArtist = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                song.albumId = c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
                song.duration = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                songs.add(song);
            }while(c.moveToNext());
        }
        c.close();

        return songs;
    }
}