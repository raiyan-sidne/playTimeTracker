package com.example.playtime;

public class PlayerItem implements MatchListItem {

    private final Player player;

    public PlayerItem(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public int getItemType() {
        return TYPE_PLAYER;
    }
}
