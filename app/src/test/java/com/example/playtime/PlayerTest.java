package com.example.playtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class PlayerTest {

    private Player player;

    @Before
    public void setUp() {
        player = new Player(1, "Bob", "Team 1", 1);
    }

    @Test
    public void testInitialState() {
        assertEquals("Bob", player.getName());
        assertEquals("Team 1", player.getTeamName());
        assertEquals(1, player.getTeamIndex());
        assertFalse(player.isChecked());
        assertFalse(player.isPlaying());
        assertFalse(player.isSentOff());
        assertEquals(0, player.getYellowCards());
        assertFalse(player.hasDirectRedCard());
        assertTrue(player.canPlay());
        assertFalse(player.canUndoCard());
        assertEquals("(00:00)", player.getFormattedPlayTime(100000L));
    }

    @Test
    public void testPlayPauseAccumulation() {
        long t0 = 1000L;
        assertTrue(player.play(t0));
        assertTrue(player.isPlaying());

        // 5 seconds later
        long t1 = t0 + 5000L;
        assertEquals(5000L, player.getTotalPlayedTimeMillis(t1));
        assertEquals("(00:05)", player.getFormattedPlayTime(t1));

        // Pause at 5 seconds
        assertTrue(player.pause(t1));
        assertFalse(player.isPlaying());
        assertEquals(5000L, player.getTotalPlayedTimeMillis(t1 + 10000L)); // Static while paused

        // Play again at 20 seconds
        long t2 = t1 + 15000L;
        assertTrue(player.play(t2));
        assertTrue(player.isPlaying());

        // 70 seconds later -> total 75 seconds = 01:15
        long t3 = t2 + 70000L;
        assertEquals(75000L, player.getTotalPlayedTimeMillis(t3));
        assertEquals("(01:15)", player.getFormattedPlayTime(t3));

        assertTrue(player.pause(t3));
        assertFalse(player.isPlaying());
        assertEquals("(01:15)", player.getFormattedPlayTime(t3 + 5000L));
    }

    @Test
    public void testYellowCardProgressionAndAutoPause() {
        long t0 = 1000L;
        player.play(t0);
        assertTrue(player.isPlaying());

        // 1st yellow card -> remains playing, not sent off
        long t1 = t0 + 3000L;
        player.addYellowCard(t1);
        assertEquals(1, player.getYellowCards());
        assertFalse(player.isSentOff());
        assertTrue(player.isPlaying());
        assertTrue(player.canUndoCard());

        // 2nd yellow card -> auto-pauses, sent off, cannot play
        long t2 = t1 + 4000L;
        player.addYellowCard(t2);
        assertEquals(2, player.getYellowCards());
        assertTrue(player.isSentOff());
        assertFalse(player.isPlaying()); // Auto-paused
        assertFalse(player.canPlay());
        assertFalse(player.play(t2 + 1000L)); // Play is disabled
    }

    @Test
    public void testDirectRedCardAndAutoPause() {
        long t0 = 1000L;
        player.play(t0);
        assertTrue(player.isPlaying());

        long t1 = t0 + 2000L;
        player.addRedCard(t1);
        assertTrue(player.hasDirectRedCard());
        assertTrue(player.isSentOff());
        assertFalse(player.isPlaying()); // Auto-paused
        assertFalse(player.canPlay());
    }

    @Test
    public void testUndoCardActions() {
        long t0 = 1000L;
        player.addYellowCard(t0);
        assertEquals(1, player.getYellowCards());

        // 2nd yellow card
        player.addYellowCard(t0 + 1000L);
        assertEquals(2, player.getYellowCards());
        assertTrue(player.isSentOff());

        // Undo 2nd yellow
        assertNotNull(player.undoLastCard());
        assertEquals(1, player.getYellowCards());
        assertFalse(player.isSentOff());
        assertTrue(player.canPlay());

        // Undo 1st yellow
        assertNotNull(player.undoLastCard());
        assertEquals(0, player.getYellowCards());
        assertFalse(player.canUndoCard());
        assertNull(player.undoLastCard());

        // Now test direct red card undo
        player.addRedCard(t0 + 2000L);
        assertTrue(player.hasDirectRedCard());
        assertTrue(player.isSentOff());

        assertNotNull(player.undoLastCard());
        assertFalse(player.hasDirectRedCard());
        assertFalse(player.isSentOff());
        assertTrue(player.canPlay());
    }

    @Test
    public void testPlayerSerializationAndDeserialization() throws Exception {
        Player original = new Player(10, "Zinedine", "Team A", 1);
        original.setChecked(true);
        original.play(5000L);
        original.pause(15000L); // 10 seconds accumulated
        original.addYellowCard(20000L, 99);

        org.json.JSONObject json = original.toJson();
        Player restored = Player.fromJson(json);

        assertEquals(original.getId(), restored.getId());
        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getTeamName(), restored.getTeamName());
        assertEquals(original.getTeamIndex(), restored.getTeamIndex());
        assertEquals(original.isChecked(), restored.isChecked());
        assertEquals(original.isPlaying(), restored.isPlaying());
        assertEquals(original.getTotalPlayedTimeMillis(25000L), restored.getTotalPlayedTimeMillis(25000L));
        assertEquals(original.getYellowCards(), restored.getYellowCards());
        assertEquals(original.hasDirectRedCard(), restored.hasDirectRedCard());
        assertTrue(restored.canUndoCard());

        // Verify that undoing on restored player also works and yields original logId
        Player.CardRecord undone = restored.undoLastCard();
        assertNotNull(undone);
        assertEquals(Player.CardAction.YELLOW, undone.getAction());
        assertEquals(99, undone.getLogId());
        assertEquals(0, restored.getYellowCards());
    }

    @Test
    public void testBatchActionClearsCheckedStateEvenIfAlreadyInTargetState() {
        Player playerPaused = new Player(1, "Alice", "Team 1", 1);
        playerPaused.setChecked(true);
        assertFalse(playerPaused.isPlaying());

        // Simulate pauseAllSelected on an already paused player
        if (playerPaused.isChecked()) {
            if (playerPaused.isPlaying()) {
                playerPaused.pause(1000L);
            }
            playerPaused.setChecked(false);
        }
        assertFalse(playerPaused.isChecked());
        assertFalse(playerPaused.isPlaying());

        Player playerPlaying = new Player(2, "Bob", "Team 1", 1);
        playerPlaying.play(1000L);
        playerPlaying.setChecked(true);
        assertTrue(playerPlaying.isPlaying());

        // Simulate playAllSelected on an already playing player
        if (playerPlaying.isChecked()) {
            if (!playerPlaying.isPlaying() && playerPlaying.canPlay()) {
                playerPlaying.play(2000L);
            }
            playerPlaying.setChecked(false);
        }
        assertFalse(playerPlaying.isChecked());
        assertTrue(playerPlaying.isPlaying());
    }
}
