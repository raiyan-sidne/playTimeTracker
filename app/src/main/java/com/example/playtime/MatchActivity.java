package com.example.playtime;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.playtime.databinding.ActivityMatchBinding;
import com.example.playtime.databinding.DialogLogsBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class MatchActivity extends AppCompatActivity implements PlayerAdapter.PlayerActionListener, NoteAdapter.OnNoteDeleteListener {

    public static final String EXTRA_RESUME = "extra_resume";

    private ActivityMatchBinding binding;
    private MatchStorage matchStorage;

    private String team1Name = "Team 1";
    private String team2Name = "Team 2";

    private final List<MatchListItem> listItems = new ArrayList<>();
    private final List<Player> allPlayers = new ArrayList<>();
    private PlayerAdapter playerAdapter;

    private final List<MatchNote> notes = new ArrayList<>();
    private NoteAdapter noteAdapter;

    private final List<LogEntry> logEntries = new ArrayList<>();
    private int logIdCounter = 1;
    private int noteIdCounter = 1;

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.US);

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            for (int i = 0; i < listItems.size(); i++) {
                MatchListItem item = listItems.get(i);
                if (item instanceof PlayerItem) {
                    Player p = ((PlayerItem) item).getPlayer();
                    if (p.isPlaying()) {
                        playerAdapter.notifyItemChanged(i, PlayerAdapter.PAYLOAD_TIMER);
                    }
                }
            }
            timerHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Ensure status bar is clean white with clear, dark battery/network icons
        androidx.core.view.WindowInsetsControllerCompat controller =
                androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(true);
        }
        getWindow().setStatusBarColor(androidx.core.content.ContextCompat.getColor(this, R.color.white));

        binding = ActivityMatchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Apply system status bar and cutout insets as top padding so app starts strictly below system area
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.statusBars() |
                    androidx.core.view.WindowInsetsCompat.Type.displayCutout());
            v.setPadding(0, insets.top, 0, 0);
            return windowInsets;
        });

        matchStorage = new MatchStorage(this);

        extractIntentData();
        setupPlayerList();
        setupNotesList();
        setupTopButtons();
    }

    @Override
    protected void onResume() {
        super.onResume();
        timerHandler.post(timerRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
        persistState();
    }

    private void extractIntentData() {
        boolean resume = getIntent().getBooleanExtra(EXTRA_RESUME, false);

        if (resume && matchStorage.hasActiveMatch()) {
            MatchStorage.MatchState savedState = matchStorage.loadMatch();
            if (savedState != null) {
                team1Name = savedState.team1Name;
                team2Name = savedState.team2Name;

                // Separate players by team index
                listItems.add(new HeaderItem(team1Name));
                for (Player p : savedState.players) {
                    if (p.getTeamIndex() == 1) {
                        allPlayers.add(p);
                        listItems.add(new PlayerItem(p));
                    }
                }

                listItems.add(new HeaderItem(team2Name));
                for (Player p : savedState.players) {
                    if (p.getTeamIndex() == 2) {
                        allPlayers.add(p);
                        listItems.add(new PlayerItem(p));
                    }
                }

                notes.addAll(savedState.notes);
                logEntries.addAll(savedState.logs);

                // Re-calculate ID counters
                for (MatchNote n : notes) {
                    if (n.getId() >= noteIdCounter) {
                        noteIdCounter = n.getId() + 1;
                    }
                }
                for (LogEntry l : logEntries) {
                    if (l.getId() >= logIdCounter) {
                        logIdCounter = l.getId() + 1;
                    }
                }
                return;
            }
        }

        // New Match from Intent extras
        team1Name = getIntent().getStringExtra(SetupActivity.EXTRA_TEAM1_NAME);
        team2Name = getIntent().getStringExtra(SetupActivity.EXTRA_TEAM2_NAME);
        ArrayList<String> team1Players = getIntent().getStringArrayListExtra(SetupActivity.EXTRA_TEAM1_PLAYERS);
        ArrayList<String> team2Players = getIntent().getStringArrayListExtra(SetupActivity.EXTRA_TEAM2_PLAYERS);

        if (TextUtils.isEmpty(team1Name)) team1Name = "Team 1";
        if (TextUtils.isEmpty(team2Name)) team2Name = "Team 2";

        int idCounter = 1;

        // Team 1 Header & Players
        listItems.add(new HeaderItem(team1Name));
        if (team1Players != null) {
            for (String name : team1Players) {
                Player player = new Player(idCounter++, name, team1Name, 1);
                allPlayers.add(player);
                listItems.add(new PlayerItem(player));
            }
        }

        // Team 2 Header & Players
        listItems.add(new HeaderItem(team2Name));
        if (team2Players != null) {
            for (String name : team2Players) {
                Player player = new Player(idCounter++, name, team2Name, 2);
                allPlayers.add(player);
                listItems.add(new PlayerItem(player));
            }
        }

        // Save initial state
        persistState();
    }

    private void persistState() {
        if (matchStorage != null) {
            matchStorage.saveMatch(team1Name, team2Name, allPlayers, notes, logEntries);
        }
    }

    private void setupPlayerList() {
        playerAdapter = new PlayerAdapter(listItems, this);
        binding.rvPlayers.setLayoutManager(new LinearLayoutManager(this));
        binding.rvPlayers.setAdapter(playerAdapter);
        binding.rvPlayers.setItemAnimator(null); // Prevents item blink during fast updates
    }

    private void setupNotesList() {
        noteAdapter = new NoteAdapter(notes, this);
        binding.rvNotes.setLayoutManager(new LinearLayoutManager(this));
        binding.rvNotes.setAdapter(noteAdapter);

        binding.btnSubmitNote.setOnClickListener(v -> submitNote());
    }

    private void setupTopButtons() {
        // Pause all selected (acts on checked players only)
        binding.btnPauseAll.setOnClickListener(v -> pauseAllSelected());

        // Play all selected (acts on checked players only)
        binding.btnPlayAllSelected.setOnClickListener(v -> playAllSelected());

        // Display logs dialog
        binding.btnDisplayLogs.setOnClickListener(v -> showLogsDialog());
    }

    private String getCurrentFormattedTime() {
        return timeFormat.format(new Date()).toLowerCase(Locale.US);
    }

    private LogEntry addLog(String message, Integer noteId) {
        String timestamp = getCurrentFormattedTime();
        LogEntry entry = new LogEntry(logIdCounter++, timestamp, message, noteId);
        logEntries.add(entry);
        return entry;
    }

    // --- Player Actions ---

    @Override
    public void onPlayPauseClicked(Player player) {
        long now = System.currentTimeMillis();
        player.setChecked(false); // Uncheck row after action
        if (player.isPlaying()) {
            player.pause(now);
            addLog(player.getName() + " (Out)", null);
        } else {
            if (player.canPlay()) {
                player.play(now);
                addLog(player.getName() + " (in)", null);
            }
        }
        notifyPlayerChanged(player);
        persistState();
    }

    @Override
    public void onYellowCardClicked(Player player) {
        long now = System.currentTimeMillis();
        player.setChecked(false); // Uncheck row after action
        LogEntry cardLog = addLog(player.getName() + " (Yellow Card)", null);
        player.addYellowCard(now, cardLog.getId());

        notifyPlayerChanged(player);
        persistState();
    }

    @Override
    public void onRedCardClicked(Player player) {
        long now = System.currentTimeMillis();
        player.setChecked(false); // Uncheck row after action
        LogEntry cardLog = addLog(player.getName() + " (Red Card)", null);
        player.addRedCard(now, cardLog.getId());

        notifyPlayerChanged(player);
        persistState();
    }

    @Override
    public void onUndoCardClicked(Player player) {
        player.setChecked(false); // Uncheck row after action
        Player.CardRecord undoneRecord = player.undoLastCard();
        if (undoneRecord != null) {
            removeCardLog(player, undoneRecord);
            notifyPlayerChanged(player);
            persistState();
        }
    }

    private void removeCardLog(Player player, Player.CardRecord record) {
        if (record != null && record.getLogId() > 0) {
            for (int i = logEntries.size() - 1; i >= 0; i--) {
                if (logEntries.get(i).getId() == record.getLogId()) {
                    logEntries.remove(i);
                    return;
                }
            }
        }
        // Fallback: search backwards for the last matching card entry for this player
        String cardSuffix = (record != null && record.getAction() == Player.CardAction.RED) ?
                " (Red Card)" : " (Yellow Card)";
        String targetMsg = player.getName() + cardSuffix;
        for (int i = logEntries.size() - 1; i >= 0; i--) {
            if (targetMsg.equals(logEntries.get(i).getMessage())) {
                logEntries.remove(i);
                return;
            }
        }
    }

    @Override
    public void onPlayerCheckChanged(Player player, boolean isChecked) {
        player.setChecked(isChecked);
        persistState();
    }

    private void notifyPlayerChanged(Player player) {
        for (int i = 0; i < listItems.size(); i++) {
            MatchListItem item = listItems.get(i);
            if (item instanceof PlayerItem && ((PlayerItem) item).getPlayer().getId() == player.getId()) {
                playerAdapter.notifyItemChanged(i);
                break;
            }
        }
    }

    // --- Batch Actions (Acting on checked players only) ---

    private void pauseAllSelected() {
        long now = System.currentTimeMillis();
        boolean anyChecked = false;
        boolean anyPaused = false;
        for (int i = 0; i < listItems.size(); i++) {
            MatchListItem item = listItems.get(i);
            if (item instanceof PlayerItem) {
                Player p = ((PlayerItem) item).getPlayer();
                if (p.isChecked()) {
                    anyChecked = true;
                    if (p.isPlaying()) {
                        p.pause(now);
                        addLog(p.getName() + " (Out)", null);
                        anyPaused = true;
                    }
                    p.setChecked(false); // Always uncheck selected rows
                    playerAdapter.notifyItemChanged(i);
                }
            }
        }
        if (!anyChecked) {
            Toast.makeText(this, "No players selected", Toast.LENGTH_SHORT).show();
        } else {
            persistState();
        }
    }

    private void playAllSelected() {
        long now = System.currentTimeMillis();
        boolean anyChecked = false;
        boolean anyPlayed = false;
        for (int i = 0; i < listItems.size(); i++) {
            MatchListItem item = listItems.get(i);
            if (item instanceof PlayerItem) {
                Player p = ((PlayerItem) item).getPlayer();
                if (p.isChecked()) {
                    anyChecked = true;
                    if (!p.isPlaying() && p.canPlay()) {
                        p.play(now);
                        addLog(p.getName() + " (in)", null);
                        anyPlayed = true;
                    }
                    p.setChecked(false); // Always uncheck selected rows
                    playerAdapter.notifyItemChanged(i);
                }
            }
        }
        if (!anyChecked) {
            Toast.makeText(this, "No players selected", Toast.LENGTH_SHORT).show();
        } else {
            persistState();
        }
    }

    // --- Notes Management ---

    private void submitNote() {
        String noteText = binding.etNote.getText() != null ?
                binding.etNote.getText().toString().trim() : "";
        if (TextUtils.isEmpty(noteText)) {
            Toast.makeText(this, "Please enter a note first", Toast.LENGTH_SHORT).show();
            return;
        }

        String timestamp = getCurrentFormattedTime();
        int noteId = noteIdCounter++;
        MatchNote matchNote = new MatchNote(noteId, timestamp, noteText);
        notes.add(matchNote);
        noteAdapter.notifyItemInserted(notes.size() - 1);

        // Also add note to log string
        addLog(noteText, noteId);

        binding.etNote.setText("");
        persistState();
    }

    @Override
    public void onNoteDelete(MatchNote note) {
        int noteId = note.getId();

        // 1. Remove from notes list on screen
        int index = -1;
        for (int i = 0; i < notes.size(); i++) {
            if (notes.get(i).getId() == noteId) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            notes.remove(index);
            noteAdapter.notifyItemRemoved(index);
        }

        // 2. Remove from log string popup
        Iterator<LogEntry> iterator = logEntries.iterator();
        while (iterator.hasNext()) {
            LogEntry entry = iterator.next();
            if (entry.getNoteId() != null && entry.getNoteId() == noteId) {
                iterator.remove();
            }
        }

        Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();
        persistState();
    }

    // --- Display Logs Dialog ---

    private void showLogsDialog() {
        Dialog dialog = new Dialog(this);
        DialogLogsBinding dialogBinding = DialogLogsBinding.inflate(LayoutInflater.from(this));
        dialog.setContentView(dialogBinding.getRoot());

        // Build log string
        final StringBuilder sb = new StringBuilder();
        if (logEntries.isEmpty()) {
            sb.append(getString(R.string.no_logs));
        } else {
            for (int i = 0; i < logEntries.size(); i++) {
                sb.append(logEntries.get(i).getFormattedLogLine());
                if (i < logEntries.size() - 1) {
                    sb.append("\n");
                }
            }
        }

        final String fullLogsText = sb.toString();
        dialogBinding.tvLogsText.setText(fullLogsText);

        // Copy button
        dialogBinding.btnCopyLogs.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Match Logs", fullLogsText);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, R.string.logs_copied, Toast.LENGTH_SHORT).show();
            }
        });

        // Close button
        dialogBinding.btnCloseLogs.setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        // Adjust dialog width to match parent nicely
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.92),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }
}
