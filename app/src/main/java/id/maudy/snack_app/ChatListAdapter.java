package id.maudy.snack_app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

import id.maudy.snack_app.R;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder> {
    private Context context;
    private List<ChatRoom> chatRoomList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(ChatRoom chatRoom);
    }

    public ChatListAdapter(Context context, List<ChatRoom> chatRoomList, OnItemClickListener listener) {
        this.context = context;
        this.chatRoomList = chatRoomList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_chat_list, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatRoom cr = chatRoomList.get(position);
        holder.tvNama.setText(cr.getNamaLawan());

        // OPTIMASI: Validasi pesan terakhir jika room baru dibuat (belum ada chat masuk)
        String lastMessage = cr.getLastMessage();
        if (lastMessage == null || lastMessage.trim().isEmpty()) {
            holder.tvPesan.setText("Belum ada pesan");
            // Mengubah teks menjadi miring (Italic) dengan benar
            holder.tvPesan.setTypeface(null, android.graphics.Typeface.ITALIC);
        } else {
            holder.tvPesan.setText(lastMessage);
            // Mengembalikan tipe teks menjadi normal kembali jika ada pesan
            holder.tvPesan.setTypeface(null, android.graphics.Typeface.NORMAL);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(cr));
    }

    @Override
    public int getItemCount() { return chatRoomList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNama, tvPesan;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNama = itemView.findViewById(R.id.tvChatNamaLawan);
            tvPesan = itemView.findViewById(R.id.tvChatLastMessage);
        }
    }
}