package com.ich.hw3;

import java.io.Serializable;

public class Song implements Serializable {
    public long songId;
    public String songTitle;
    public String songArtist;
    public String path;
    public int duration;
    public long albumId;
}
