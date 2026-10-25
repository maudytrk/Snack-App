package id.maudy.snack_app;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import id.maudy.snack_app.R;

public class RoomChatActivity extends AppCompatActivity {
    private TextView tvHeaderNama;
    private RecyclerView recyclerView;
    private EditText etInput;
    private Button btnSend;

    private MessageAdapter adapter;
    private List<Message> messageList;

    private String roomId, lawanId, lawanNama, currentUid;
    private DatabaseReference dbRef;
    private ValueEventListener messagesListener; // Disimpan ke variabel agar bisa dilepas saat halaman ditutup

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(id.maudy.snack_app.R.layout.activity_room_chat);

        tvHeaderNama = findViewById(id.maudy.snack_app.R.id.tvHeaderNama);
        recyclerView = findViewById(id.maudy.snack_app.R.id.rvRoomChat);
        etInput = findViewById(id.maudy.snack_app.R.id.etChatInput);
        btnSend = findViewById(R.id.btnChatSend);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "Akses ditolak, silakan login terlebih dahulu", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        lawanId = getIntent().getStringExtra("LAWAN_ID");
        lawanNama = getIntent().getStringExtra("LAWAN_NAMA");
        roomId = getIntent().getStringExtra("ROOM_ID");

        // Jika dipanggil dari DetailProduk, Room ID di-generate otomatis memakai struktur gabungan unik alfabetis
        if (roomId == null && lawanId != null) {
            if (currentUid.compareTo(lawanId) < 0) {
                roomId = currentUid + "_" + lawanId;
            } else {
                roomId = lawanId + "_" + currentUid;
            }
        }

        tvHeaderNama.setText(lawanNama != null ? lawanNama : "User Camilan");

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Memaksa scroll otomatis ke bawah jika ada pesan baru layaknya WhatsApp
        recyclerView.setLayoutManager(layoutManager);

        messageList = new ArrayList<>();
        adapter = new MessageAdapter(this, messageList);
        recyclerView.setAdapter(adapter);

        // PENGUNCIAN LINK DATABASE REGIONAL ASIA AGAR TIDAK FREEZE / STUCK TIMEOUT
        String URL_DATABASE = "https://snack-app-d6ffc-default-rtdb.asia-southeast1.firebasedatabase.app/";
        dbRef = FirebaseDatabase.getInstance(URL_DATABASE).getReference("ChatRooms").child(roomId);

        readMessages();

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String msgText = etInput.getText().toString().trim();
        if (TextUtils.isEmpty(msgText)) return;

        String msgId = dbRef.child("messages").push().getKey();
        long now = System.currentTimeMillis();

        Message msg = new Message(msgId, currentUid, msgText, now);

        if (msgId != null) {
            // Update struktur pesan di dalam room
            dbRef.child("messages").child(msgId).setValue(msg);

            // Update informasi ringkasan last message di room utama
            Map<String, Object> roomUpdates = new HashMap<>();
            roomUpdates.put("lastMessage", msgText);
            roomUpdates.put("timestamp", now);
            dbRef.updateChildren(roomUpdates);

            etInput.setText("");
        }
    }

    private void readMessages() {
        messagesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing()) return;

                messageList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Message msg = ds.getValue(Message.class);
                    if (msg != null) {
                        messageList.add(msg);
                    }
                }
                adapter.notifyDataSetChanged();
                if (!messageList.isEmpty()) {
                    recyclerView.smoothScrollToPosition(messageList.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RoomChatActivity.this, "Gagal memuat pesan: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        dbRef.child("messages").addValueEventListener(messagesListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // OPTIMASI PENYELAMAT MEMORI: Putus koneksi listener realtime saat ruang obrolan ditutup oleh user
        if (dbRef != null && messagesListener != null) {
            dbRef.child("messages").removeEventListener(messagesListener);
        }
    }
}