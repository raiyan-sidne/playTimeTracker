package com.example.playtime;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.playtime.databinding.ActivitySetupBinding;

import java.util.ArrayList;

public class SetupActivity extends AppCompatActivity {

    public static final String EXTRA_TEAM1_NAME = "extra_team1_name";
    public static final String EXTRA_TEAM2_NAME = "extra_team2_name";
    public static final String EXTRA_TEAM1_PLAYERS = "extra_team1_players";
    public static final String EXTRA_TEAM2_PLAYERS = "extra_team2_players";

    private ActivitySetupBinding binding;
    private MatchStorage matchStorage;

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

        binding = ActivitySetupBinding.inflate(getLayoutInflater());
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

        binding.btnStartMatch.setOnClickListener(v -> startMatch());
        binding.btnResumeMatch.setOnClickListener(v -> resumeMatch());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (matchStorage.hasActiveMatch()) {
            MatchStorage.MatchState savedState = matchStorage.loadMatch();
            if (savedState != null) {
                binding.btnResumeMatch.setVisibility(android.view.View.VISIBLE);
                binding.btnResumeMatch.setText("Resume Ongoing Match\n" +
                        savedState.team1Name + " vs " + savedState.team2Name);
            } else {
                binding.btnResumeMatch.setVisibility(android.view.View.GONE);
            }
        } else {
            binding.btnResumeMatch.setVisibility(android.view.View.GONE);
        }
    }

    private void resumeMatch() {
        Intent intent = new Intent(this, MatchActivity.class);
        intent.putExtra(MatchActivity.EXTRA_RESUME, true);
        startActivity(intent);
    }

    private void startMatch() {
        String team1Name = binding.etTeam1Name.getText() != null ?
                binding.etTeam1Name.getText().toString().trim() : "";
        if (TextUtils.isEmpty(team1Name)) {
            team1Name = "Team 1";
        }

        String team2Name = binding.etTeam2Name.getText() != null ?
                binding.etTeam2Name.getText().toString().trim() : "";
        if (TextUtils.isEmpty(team2Name)) {
            team2Name = "Team 2";
        }

        String team1RawPlayers = binding.etTeam1Players.getText() != null ?
                binding.etTeam1Players.getText().toString() : "";
        String team2RawPlayers = binding.etTeam2Players.getText() != null ?
                binding.etTeam2Players.getText().toString() : "";

        ArrayList<String> team1Players = parsePlayerNames(team1RawPlayers);
        ArrayList<String> team2Players = parsePlayerNames(team2RawPlayers);

        if (team1Players.isEmpty()) {
            binding.etTeam1Players.setError("Please enter at least one player for Team 1");
            binding.etTeam1Players.requestFocus();
            Toast.makeText(this, "Please enter at least one player for Team 1", Toast.LENGTH_SHORT).show();
            return;
        }

        if (team2Players.isEmpty()) {
            binding.etTeam2Players.setError("Please enter at least one player for Team 2");
            binding.etTeam2Players.requestFocus();
            Toast.makeText(this, "Please enter at least one player for Team 2", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, MatchActivity.class);
        intent.putExtra(EXTRA_TEAM1_NAME, team1Name);
        intent.putExtra(EXTRA_TEAM2_NAME, team2Name);
        intent.putStringArrayListExtra(EXTRA_TEAM1_PLAYERS, team1Players);
        intent.putStringArrayListExtra(EXTRA_TEAM2_PLAYERS, team2Players);
        startActivity(intent);
    }

    private ArrayList<String> parsePlayerNames(String raw) {
        ArrayList<String> list = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) {
            return list;
        }
        String[] lines = raw.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }
        return list;
    }
}
