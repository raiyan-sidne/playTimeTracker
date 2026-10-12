package com.example.playtime;

public interface MatchListItem {
    int TYPE_HEADER = 0;
    int TYPE_PLAYER = 1;

    int getItemType();
}
