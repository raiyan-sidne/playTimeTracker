package com.example.playtime;

public class LogEntry {

    private final int id;
    private final String timestamp;
    private final String message;
    private final Integer noteId; // nullable, non-null if associated with a custom note

    public LogEntry(int id, String timestamp, String message, Integer noteId) {
        this.id = id;
        this.timestamp = timestamp;
        this.message = message;
        this.noteId = noteId;
    }

    public int getId() {
        return id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }

    public Integer getNoteId() {
        return noteId;
    }

    public String getFormattedLogLine() {
        return timestamp + " -> " + message;
    }

    public org.json.JSONObject toJson() throws org.json.JSONException {
        org.json.JSONObject json = new org.json.JSONObject();
        json.put("id", id);
        json.put("timestamp", timestamp);
        json.put("message", message);
        if (noteId != null) {
            json.put("noteId", noteId.intValue());
        }
        return json;
    }

    public static LogEntry fromJson(org.json.JSONObject json) throws org.json.JSONException {
        Integer noteId = json.has("noteId") && !json.isNull("noteId") ?
                json.getInt("noteId") : null;
        return new LogEntry(
                json.getInt("id"),
                json.getString("timestamp"),
                json.getString("message"),
                noteId
        );
    }
}
