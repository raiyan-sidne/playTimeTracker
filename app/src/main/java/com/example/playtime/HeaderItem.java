package com.example.playtime;

public class HeaderItem implements MatchListItem {

    private final String title;

    public HeaderItem(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public int getItemType() {
        return TYPE_HEADER;
    }
}
