package com.example.playtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LogAndNoteTest {

    @Test
    public void testLogEntryFormat() {
        LogEntry entry = new LogEntry(1, "09:45 pm", "Bob (Out)", null);
        assertEquals("09:45 pm -> Bob (Out)", entry.getFormattedLogLine());
        assertNull(entry.getNoteId());

        LogEntry noteEntry = new LogEntry(2, "09:57 pm", "Goal scored by Alice", 42);
        assertEquals("09:57 pm -> Goal scored by Alice", noteEntry.getFormattedLogLine());
        assertEquals(Integer.valueOf(42), noteEntry.getNoteId());
    }

    @Test
    public void testNoteCreationAndDeletionSyncWithLogs() {
        List<MatchNote> notes = new ArrayList<>();
        List<LogEntry> logs = new ArrayList<>();

        // Add 2 player events
        logs.add(new LogEntry(1, "09:45 pm", "Bob (in)", null));
        logs.add(new LogEntry(2, "09:50 pm", "Bob (Yellow Card)", null));

        // Add a note
        int noteId = 101;
        MatchNote note = new MatchNote(noteId, "09:52 pm", "Foul near penalty box");
        notes.add(note);
        logs.add(new LogEntry(3, "09:52 pm", note.getText(), noteId));

        // Add another note
        int noteId2 = 102;
        MatchNote note2 = new MatchNote(noteId2, "09:55 pm", "Substitution requested");
        notes.add(note2);
        logs.add(new LogEntry(4, "09:55 pm", note2.getText(), noteId2));

        assertEquals(2, notes.size());
        assertEquals(4, logs.size());

        // Simulate deleting the first note
        notes.remove(0);
        Iterator<LogEntry> iterator = logs.iterator();
        while (iterator.hasNext()) {
            LogEntry entry = iterator.next();
            if (entry.getNoteId() != null && entry.getNoteId() == noteId) {
                iterator.remove();
            }
        }

        // Verify state after deletion
        assertEquals(1, notes.size());
        assertEquals("Substitution requested", notes.get(0).getText());
        assertEquals(3, logs.size());
        assertEquals("09:45 pm -> Bob (in)", logs.get(0).getFormattedLogLine());
        assertEquals("09:50 pm -> Bob (Yellow Card)", logs.get(1).getFormattedLogLine());
        assertEquals("09:55 pm -> Substitution requested", logs.get(2).getFormattedLogLine());
    }

    @Test
    public void testNoteAndLogSerialization() throws Exception {
        MatchNote note = new MatchNote(5, "10:30 am", "Penalty kick");
        org.json.JSONObject noteJson = note.toJson();
        MatchNote restoredNote = MatchNote.fromJson(noteJson);

        assertEquals(note.getId(), restoredNote.getId());
        assertEquals(note.getTimestamp(), restoredNote.getTimestamp());
        assertEquals(note.getText(), restoredNote.getText());
        assertEquals(note.getDisplayText(), restoredNote.getDisplayText());

        LogEntry log = new LogEntry(7, "10:31 am", "Penalty kick", 5);
        org.json.JSONObject logJson = log.toJson();
        LogEntry restoredLog = LogEntry.fromJson(logJson);

        assertEquals(log.getId(), restoredLog.getId());
        assertEquals(log.getTimestamp(), restoredLog.getTimestamp());
        assertEquals(log.getMessage(), restoredLog.getMessage());
        assertEquals(log.getNoteId(), restoredLog.getNoteId());
        assertEquals(log.getFormattedLogLine(), restoredLog.getFormattedLogLine());
    }

    @Test
    public void testCardLogRemovalOnUndo() {
        List<LogEntry> logs = new ArrayList<>();
        Player player = new Player(1, "David", "Team 1", 1);

        // Player plays
        logs.add(new LogEntry(1, "09:40 pm", "David (in)", null));

        // Player receives yellow card
        LogEntry cardLog = new LogEntry(2, "09:45 pm", "David (Yellow Card)", null);
        logs.add(cardLog);
        player.addYellowCard(1000L, cardLog.getId());

        assertEquals(2, logs.size());
        assertEquals("09:45 pm -> David (Yellow Card)", logs.get(1).getFormattedLogLine());

        // Undo the card
        Player.CardRecord undoneRecord = player.undoLastCard();
        assertNotNull(undoneRecord);

        // Remove from log
        for (int i = logs.size() - 1; i >= 0; i--) {
            if (logs.get(i).getId() == undoneRecord.getLogId()) {
                logs.remove(i);
                break;
            }
        }

        // Verify that the card log is removed and only David (in) remains
        assertEquals(1, logs.size());
        assertEquals("09:40 pm -> David (in)", logs.get(0).getFormattedLogLine());
    }
}
