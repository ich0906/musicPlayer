package com.ich.hw3;

interface IMusicService {
    void playMusic();
    void pauseMusic();
    void seek(int pos);
    int getPosition();
    void changeNotification();
}