package com.example.playtime;

import java.util.Stack;

public class Player {

    public enum CardAction {
        YELLOW,
        RED
    }

    public static class CardRecord {
        private final CardAction action;
        private final int logId;

        public CardRecord(CardAction action, int logId) {
            this.action = action;
            this.logId = logId;
        }

        public CardAction getAction() {
            return action;
        }

        public int getLogId() {
            return logId;
        }
    }

    private final int id;
    private final String name;
    private final String teamName;
    private final int teamIndex; // 1 or 2

    private boolean isChecked;
    private boolean isPlaying;
    private long playStartTime;
    private long accumulatedTimeMillis;

    private int yellowCards;
    private boolean hasDirectRedCard;
    private final Stack<CardRecord> cardHistory;

    public Player(int id, String name, String teamName, int teamIndex) {
        this.id = id;
        this.name = name;
        this.teamName = teamName;
        this.teamIndex = teamIndex;
        this.isChecked = false;
        this.isPlaying = false;
        this.playStartTime = 0;
        this.accumulatedTimeMillis = 0;
        this.yellowCards = 0;
        this.hasDirectRedCard = false;
        this.cardHistory = new Stack<>();
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTeamName() {
        return teamName;
    }

    public int getTeamIndex() {
        return teamIndex;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public boolean isSentOff() {
        return hasDirectRedCard || yellowCards >= 2;
    }

    public int getYellowCards() {
        return yellowCards;
    }

    public boolean hasDirectRedCard() {
        return hasDirectRedCard;
    }

    public boolean canPlay() {
        return !isSentOff();
    }

    public boolean canUndoCard() {
        return !cardHistory.isEmpty();
    }

    public boolean play(long now) {
        if (!canPlay() || isPlaying) {
            return false;
        }
        isPlaying = true;
        playStartTime = now;
        return true;
    }

    public boolean pause(long now) {
        if (!isPlaying) {
            return false;
        }
        accumulatedTimeMillis += Math.max(0, now - playStartTime);
        isPlaying = false;
        return true;
    }

    public void addYellowCard(long now) {
        addYellowCard(now, 0);
    }

    public void addYellowCard(long now, int logId) {
        cardHistory.push(new CardRecord(CardAction.YELLOW, logId));
        yellowCards++;
        if (yellowCards >= 2 && isPlaying) {
            pause(now);
        }
    }

    public void addRedCard(long now) {
        addRedCard(now, 0);
    }

    public void addRedCard(long now, int logId) {
        cardHistory.push(new CardRecord(CardAction.RED, logId));
        hasDirectRedCard = true;
        if (isPlaying) {
            pause(now);
        }
    }

    /**
     * Undoes the last card action for this player and returns the undone CardRecord.
     * @return the undone CardRecord or null if no card action was present.
     */
    public CardRecord undoLastCard() {
        if (cardHistory.isEmpty()) {
            return null;
        }
        CardRecord lastRecord = cardHistory.pop();
        if (lastRecord.getAction() == CardAction.YELLOW) {
            yellowCards = Math.max(0, yellowCards - 1);
        } else if (lastRecord.getAction() == CardAction.RED) {
            hasDirectRedCard = false;
        }
        return lastRecord;
    }

    public long getTotalPlayedTimeMillis(long now) {
        if (isPlaying) {
            return accumulatedTimeMillis + Math.max(0, now - playStartTime);
        }
        return accumulatedTimeMillis;
    }

    public String getFormattedPlayTime(long now) {
        long totalSecs = getTotalPlayedTimeMillis(now) / 1000;
        long minutes = totalSecs / 60;
        long seconds = totalSecs % 60;
        return String.format(java.util.Locale.US, "(%02d:%02d)", minutes, seconds);
    }

    public org.json.JSONObject toJson() throws org.json.JSONException {
        org.json.JSONObject json = new org.json.JSONObject();
        json.put("id", id);
        json.put("name", name);
        json.put("teamName", teamName);
        json.put("teamIndex", teamIndex);
        json.put("isChecked", isChecked);
        json.put("isPlaying", isPlaying);
        json.put("playStartTime", playStartTime);
        json.put("accumulatedTimeMillis", accumulatedTimeMillis);
        json.put("yellowCards", yellowCards);
        json.put("hasDirectRedCard", hasDirectRedCard);

        org.json.JSONArray historyArray = new org.json.JSONArray();
        for (CardRecord record : cardHistory) {
            org.json.JSONObject item = new org.json.JSONObject();
            item.put("action", record.getAction().name());
            item.put("logId", record.getLogId());
            historyArray.put(item);
        }
        json.put("cardHistory", historyArray);
        return json;
    }

    public static Player fromJson(org.json.JSONObject json) throws org.json.JSONException {
        int id = json.getInt("id");
        String name = json.getString("name");
        String teamName = json.getString("teamName");
        int teamIndex = json.getInt("teamIndex");

        Player player = new Player(id, name, teamName, teamIndex);
        player.isChecked = json.optBoolean("isChecked", false);
        player.isPlaying = json.optBoolean("isPlaying", false);
        player.playStartTime = json.optLong("playStartTime", 0);
        player.accumulatedTimeMillis = json.optLong("accumulatedTimeMillis", 0);
        player.yellowCards = json.optInt("yellowCards", 0);
        player.hasDirectRedCard = json.optBoolean("hasDirectRedCard", false);

        org.json.JSONArray historyArray = json.optJSONArray("cardHistory");
        if (historyArray != null) {
            for (int i = 0; i < historyArray.length(); i++) {
                Object obj = historyArray.get(i);
                if (obj instanceof org.json.JSONObject) {
                    org.json.JSONObject rec = (org.json.JSONObject) obj;
                    try {
                        player.cardHistory.push(new CardRecord(
                                CardAction.valueOf(rec.getString("action")),
                                rec.optInt("logId", 0)));
                    } catch (IllegalArgumentException ignored) {}
                } else if (obj != null) {
                    try {
                        player.cardHistory.push(new CardRecord(
                                CardAction.valueOf(obj.toString()), 0));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }
        return player;
    }
}
