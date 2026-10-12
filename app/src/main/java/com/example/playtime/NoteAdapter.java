package com.example.playtime;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.playtime.databinding.ItemNoteBinding;

import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    public interface OnNoteDeleteListener {
        void onNoteDelete(MatchNote note);
    }

    private final List<MatchNote> notes;
    private final OnNoteDeleteListener deleteListener;

    public NoteAdapter(List<MatchNote> notes, OnNoteDeleteListener deleteListener) {
        this.notes = notes;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNoteBinding binding = ItemNoteBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new NoteViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        MatchNote note = notes.get(position);
        holder.binding.tvNoteContent.setText(note.getDisplayText());
        holder.binding.btnDeleteNote.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onNoteDelete(note);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        final ItemNoteBinding binding;

        NoteViewHolder(ItemNoteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
