package com.example.playtime;

public class MatchNote {

    private final int id;
    private final String timestamp;
    private final String text;

    public MatchNote(int id, String timestamp, String text) {
        this.id = id;
        this.timestamp = timestamp;
        this.text = text;
    }

    public int getId() {
        return id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getText() {
        return text;
    }

    public String getDisplayText() {
        return timestamp + " : " + text;
    }

    public org.json.JSONObject toJson() throws org.json.JSONException {
        org.json.JSONObject json = new org.json.JSONObject();
        json.put("id", id);
        json.put("timestamp", timestamp);
        json.put("text", text);
        return json;
    }

    public static MatchNote fromJson(org.json.JSONObject json) throws org.json.JSONException {
        return new MatchNote(
                json.getInt("id"),
                json.getString("timestamp"),
                json.getString("text")
        );
    }
}
