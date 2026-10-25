package id.maudy.snack_app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import java.util.List;

import id.maudy.snack_app.R;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MsgViewHolder> {
    private static final int MSG_TYPE_LEFT = 0;
    private static final int MSG_TYPE_RIGHT = 1;

    private Context context;
    private List<Message> messageList;
    private String currentUid;

    public MessageAdapter(Context context, List<Message> messageList) {
        this.context = context;
        this.messageList = messageList;
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            this.currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            this.currentUid = "";
        }
    }

    @NonNull
    @Override
    public MsgViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == MSG_TYPE_RIGHT) {
            View v = LayoutInflater.from(context).inflate(R.layout.item_message_right, parent, false);
            return new MsgViewHolder(v, true);
        } else {
            View v = LayoutInflater.from(context).inflate(R.layout.item_message_left, parent, false);
            return new MsgViewHolder(v, false);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MsgViewHolder holder, int position) {
        Message msg = messageList.get(position);

        // PENGAMANAN: Pastikan text tidak null agar tidak crash saat di-render
        if (msg.getText() != null) {
            holder.tvText.setText(msg.getText());
        } else {
            holder.tvText.setText("");
        }
    }

    @Override
    public int getItemCount() { return messageList.size(); }

    @Override
    public int getItemViewType(int position) {
        Message msg = messageList.get(position);

        // Proteksi tambahan jika senderId kosong atau null
        if (msg != null && msg.getSenderId() != null && msg.getSenderId().equals(currentUid)) {
            return MSG_TYPE_RIGHT;
        } else {
            return MSG_TYPE_LEFT;
        }
    }

    public static class MsgViewHolder extends RecyclerView.ViewHolder {
        TextView tvText;

        public MsgViewHolder(@NonNull View itemView, boolean isRight) {
            super(itemView);
            // PERBAIKAN MUTLAK: Pencarian ID dipisah secara tegas berdasarkan jenis layoutnya
            if (isRight) {
                tvText = itemView.findViewById(R.id.tvMessageTextRight);
            } else {
                tvText = itemView.findViewById(R.id.tvMessageTextLeft);
            }
        }
    }
}