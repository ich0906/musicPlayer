package com.ich.hw3;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;

public class MusicAdapter extends BaseAdapter {

    Context context;
    ArrayList<Song> songs;
    LayoutInflater inflater;

    public MusicAdapter(Context context, ArrayList<Song> songs){
        this.context = context;
        inflater = LayoutInflater.from(context);
        this.songs = songs;
    }

    @Override
    public int getCount() {
        return songs.size();
    }

    @Override
    public Object getItem(int i) {
        return songs.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View v = inflater.inflate(R.layout.list_view_main_item,null);

        ImageView imageView = (ImageView)v.findViewById(R.id.imageViewListItem);
        TextView textView = (TextView)v.findViewById(R.id.textViewListItem);

        textView.setText(songs.get(i).songTitle);

        Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
        Uri albumUri = ContentUris.withAppendedId(sArtworkUri, songs.get(i).albumId);

        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(),albumUri);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(bitmap != null)
            imageView.setImageBitmap(bitmap);

        return v;
    }
}
