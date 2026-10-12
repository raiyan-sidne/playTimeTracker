package com.example.playtime;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.playtime.databinding.ItemPlayerBinding;
import com.example.playtime.databinding.ItemTeamHeaderBinding;

import java.util.List;

public class PlayerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final String PAYLOAD_TIMER = "PAYLOAD_TIMER";

    public interface PlayerActionListener {
        void onPlayPauseClicked(Player player);
        void onYellowCardClicked(Player player);
        void onRedCardClicked(Player player);
        void onUndoCardClicked(Player player);
        void onPlayerCheckChanged(Player player, boolean isChecked);
    }

    private final List<MatchListItem> items;
    private final PlayerActionListener listener;

    public PlayerAdapter(List<MatchListItem> items, PlayerActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getItemType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == MatchListItem.TYPE_HEADER) {
            ItemTeamHeaderBinding binding = ItemTeamHeaderBinding.inflate(inflater, parent, false);
            return new HeaderViewHolder(binding);
        } else {
            ItemPlayerBinding binding = ItemPlayerBinding.inflate(inflater, parent, false);
            return new PlayerViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        onBindViewHolder(holder, position, java.util.Collections.emptyList());
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        MatchListItem item = items.get(position);

        if (holder instanceof HeaderViewHolder) {
            HeaderItem header = (HeaderItem) item;
            ((HeaderViewHolder) holder).binding.tvTeamHeader.setText(header.getTitle());
            return;
        }

        if (holder instanceof PlayerViewHolder) {
            PlayerViewHolder playerHolder = (PlayerViewHolder) holder;
            Player player = ((PlayerItem) item).getPlayer();
            long now = System.currentTimeMillis();

            // Partial bind for timer tick only
            if (!payloads.isEmpty() && payloads.contains(PAYLOAD_TIMER)) {
                playerHolder.binding.tvPlayTime.setText(player.getFormattedPlayTime(now));
                return;
            }

            // Full bind
            Context context = holder.itemView.getContext();
            playerHolder.binding.tvPlayerName.setText(player.getName());
            playerHolder.binding.tvPlayTime.setText(player.getFormattedPlayTime(now));

            // Checkbox
            playerHolder.binding.cbSelect.setOnCheckedChangeListener(null);
            playerHolder.binding.cbSelect.setChecked(player.isChecked());
            playerHolder.binding.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                player.setChecked(isChecked);
                if (listener != null) {
                    listener.onPlayerCheckChanged(player, isChecked);
                }
            });

            // Sent-off / Card state styling
            boolean isSentOff = player.isSentOff();
            if (isSentOff) {
                playerHolder.binding.playerCard.setCardBackgroundColor(
                        ContextCompat.getColor(context, R.color.card_row_light_red));
                playerHolder.binding.playerCard.setStrokeColor(
                        ContextCompat.getColor(context, R.color.card_row_light_red_border));
            } else {
                playerHolder.binding.playerCard.setCardBackgroundColor(
                        ContextCompat.getColor(context, R.color.card_bg));
                playerHolder.binding.playerCard.setStrokeColor(
                        ContextCompat.getColor(context, R.color.gray_border));
            }

            // Card status indicator text
            if (player.hasDirectRedCard()) {
                playerHolder.binding.tvCardStatus.setVisibility(View.VISIBLE);
                playerHolder.binding.tvCardStatus.setText("RED CARD");
                playerHolder.binding.tvCardStatus.setTextColor(ContextCompat.getColor(context, R.color.red_card));
            } else if (player.getYellowCards() >= 2) {
                playerHolder.binding.tvCardStatus.setVisibility(View.VISIBLE);
                playerHolder.binding.tvCardStatus.setText("2 YELLOW CARDS (RED)");
                playerHolder.binding.tvCardStatus.setTextColor(ContextCompat.getColor(context, R.color.red_card));
            } else if (player.getYellowCards() == 1) {
                playerHolder.binding.tvCardStatus.setVisibility(View.VISIBLE);
                playerHolder.binding.tvCardStatus.setText("1 Yellow Card");
                playerHolder.binding.tvCardStatus.setTextColor(ContextCompat.getColor(context, R.color.yellow_card));
            } else {
                playerHolder.binding.tvCardStatus.setVisibility(View.GONE);
            }

            // Play / Pause Toggle Button
            if (isSentOff) {
                playerHolder.binding.btnPlayPause.setEnabled(false);
                playerHolder.binding.btnPlayPause.setAlpha(0.3f);
                playerHolder.binding.btnPlayPause.setImageResource(R.drawable.ic_play);
                playerHolder.binding.btnPlayPause.setImageTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(context, R.color.text_secondary)));
            } else {
                playerHolder.binding.btnPlayPause.setEnabled(true);
                playerHolder.binding.btnPlayPause.setAlpha(1.0f);
                if (player.isPlaying()) {
                    playerHolder.binding.btnPlayPause.setImageResource(R.drawable.ic_pause);
                    playerHolder.binding.btnPlayPause.setImageTintList(
                            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.pause_orange)));
                } else {
                    playerHolder.binding.btnPlayPause.setImageResource(R.drawable.ic_play);
                    playerHolder.binding.btnPlayPause.setImageTintList(
                            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.play_green)));
                }
            }

            playerHolder.binding.btnPlayPause.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlayPauseClicked(player);
                }
            });

            // Yellow Card Button
            playerHolder.binding.btnYellowCard.setEnabled(!isSentOff);
            playerHolder.binding.btnYellowCard.setAlpha(isSentOff ? 0.3f : 1.0f);
            playerHolder.binding.btnYellowCard.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onYellowCardClicked(player);
                }
            });

            // Red Card Button
            playerHolder.binding.btnRedCard.setEnabled(!isSentOff);
            playerHolder.binding.btnRedCard.setAlpha(isSentOff ? 0.3f : 1.0f);
            playerHolder.binding.btnRedCard.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRedCardClicked(player);
                }
            });

            // Undo Card Button
            playerHolder.binding.btnUndoCard.setEnabled(player.canUndoCard());
            playerHolder.binding.btnUndoCard.setAlpha(player.canUndoCard() ? 1.0f : 0.3f);
            playerHolder.binding.btnUndoCard.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUndoCardClicked(player);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        final ItemTeamHeaderBinding binding;

        HeaderViewHolder(ItemTeamHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static class PlayerViewHolder extends RecyclerView.ViewHolder {
        final ItemPlayerBinding binding;

        PlayerViewHolder(ItemPlayerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
