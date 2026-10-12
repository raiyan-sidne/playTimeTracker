package com.example.playtime;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MatchStorage {

    private static final String PREF_NAME = "playtime_match_storage";
    private static final String KEY_HAS_ACTIVE_MATCH = "has_active_match";
    private static final String KEY_TEAM1_NAME = "team1_name";
    private static final String KEY_TEAM2_NAME = "team2_name";
    private static final String KEY_PLAYERS = "players";
    private static final String KEY_NOTES = "notes";
    private static final String KEY_LOGS = "logs";

    private final SharedPreferences prefs;

    public static class MatchState {
        public final String team1Name;
        public final String team2Name;
        public final List<Player> players;
        public final List<MatchNote> notes;
        public final List<LogEntry> logs;

        public MatchState(String team1Name, String team2Name,
                          List<Player> players, List<MatchNote> notes, List<LogEntry> logs) {
            this.team1Name = team1Name;
            this.team2Name = team2Name;
            this.players = players;
            this.notes = notes;
            this.logs = logs;
        }
    }

    public MatchStorage(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean hasActiveMatch() {
        return prefs.getBoolean(KEY_HAS_ACTIVE_MATCH, false);
    }

    public void saveMatch(String team1Name, String team2Name,
                          List<Player> players, List<MatchNote> notes, List<LogEntry> logs) {
        try {
            JSONArray playersArray = new JSONArray();
            for (Player p : players) {
                playersArray.put(p.toJson());
            }

            JSONArray notesArray = new JSONArray();
            for (MatchNote n : notes) {
                notesArray.put(n.toJson());
            }

            JSONArray logsArray = new JSONArray();
            for (LogEntry l : logs) {
                logsArray.put(l.toJson());
            }

            prefs.edit()
                    .putBoolean(KEY_HAS_ACTIVE_MATCH, true)
                    .putString(KEY_TEAM1_NAME, team1Name)
                    .putString(KEY_TEAM2_NAME, team2Name)
                    .putString(KEY_PLAYERS, playersArray.toString())
                    .putString(KEY_NOTES, notesArray.toString())
                    .putString(KEY_LOGS, logsArray.toString())
                    .apply();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public MatchState loadMatch() {
        if (!hasActiveMatch()) {
            return null;
        }

        String team1Name = prefs.getString(KEY_TEAM1_NAME, "Team 1");
        String team2Name = prefs.getString(KEY_TEAM2_NAME, "Team 2");
        List<Player> players = new ArrayList<>();
        List<MatchNote> notes = new ArrayList<>();
        List<LogEntry> logs = new ArrayList<>();

        try {
            String playersJson = prefs.getString(KEY_PLAYERS, "[]");
            JSONArray playersArray = new JSONArray(playersJson);
            for (int i = 0; i < playersArray.length(); i++) {
                players.add(Player.fromJson(playersArray.getJSONObject(i)));
            }

            String notesJson = prefs.getString(KEY_NOTES, "[]");
            JSONArray notesArray = new JSONArray(notesJson);
            for (int i = 0; i < notesArray.length(); i++) {
                notes.add(MatchNote.fromJson(notesArray.getJSONObject(i)));
            }

            String logsJson = prefs.getString(KEY_LOGS, "[]");
            JSONArray logsArray = new JSONArray(logsJson);
            for (int i = 0; i < logsArray.length(); i++) {
                logs.add(LogEntry.fromJson(logsArray.getJSONObject(i)));
            }

            return new MatchState(team1Name, team2Name, players, notes, logs);

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void clearMatch() {
        prefs.edit().clear().apply();
    }
}
