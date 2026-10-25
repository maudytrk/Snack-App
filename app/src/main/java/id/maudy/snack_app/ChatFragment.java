package id.maudy.snack_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import id.maudy.snack_app.R;

public class ChatFragment extends Fragment {
    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private ChatListAdapter adapter;
    private List<ChatRoom> chatRoomList;
    private String currentUid;

    private DatabaseReference databaseRoot;
    private DatabaseReference roomsRef;
    private ValueEventListener chatRoomsListener;

    public ChatFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(id.maudy.snack_app.R.layout.fragment_chat, container, false);
        recyclerView = view.findViewById(id.maudy.snack_app.R.id.rvChatList);
        tvEmpty = view.findViewById(R.id.tvChatListEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        chatRoomList = new ArrayList<>();

        adapter = new ChatListAdapter(getActivity(), chatRoomList, chatRoom -> {
            Intent intent = new Intent(getActivity(), RoomChatActivity.class);
            intent.putExtra("ROOM_ID", chatRoom.getChatRoomId());
            intent.putExtra("LAWAN_ID", chatRoom.getLawanId());
            intent.putExtra("LAWAN_NAMA", chatRoom.getNamaLawan());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        // PENGUNCIAN LINK DATABASE REGIONAL ASIA AGAR TIDAK FREEZE / STUCK TIMEOUT
        String URL_DATABASE = "https://snack-app-d6ffc-default-rtdb.asia-southeast1.firebasedatabase.app/";
        databaseRoot = FirebaseDatabase.getInstance(URL_DATABASE).getReference();

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            loadChatRooms();
        } else {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("Silakan login untuk melihat pesan");
        }

        return view;
    }

    private void loadChatRooms() {
        roomsRef = databaseRoot.child("ChatRooms");

        chatRoomsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getActivity() == null) return;

                chatRoomList.clear();

                if (!snapshot.exists()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    adapter.notifyDataSetChanged();
                    return;
                }

                // Gunakan counter untuk melacak penyelesaian pembacaan asinkronus nama user
                final long totalRooms = snapshot.getChildrenCount();
                final int[] processedRooms = {0};

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String roomId = ds.getKey();
                    if (roomId != null && roomId.contains(currentUid)) {
                        String lastMsg = ds.child("lastMessage").getValue(String.class);
                        Long timestamp = ds.child("timestamp").getValue(Long.class);
                        if (timestamp == null) timestamp = 0L;

                        String[] uids = roomId.split("_");
                        // Validasi pengamanan pecahan string ID Room
                        if (uids.length < 2) {
                            processedRooms[0]++;
                            checkAndUpdateUI(processedRooms[0], totalRooms);
                            continue;
                        }

                        String lawanId = uids[0].equals(currentUid) ? uids[1] : uids[0];
                        long finalTimestamp = timestamp;

                        databaseRoot.child("Users").child(lawanId)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                        if (!isAdded() || getActivity() == null) return;

                                        String namaLawan = userSnapshot.child("nama").getValue(String.class);
                                        if (namaLawan == null) namaLawan = "User Camilan";

                                        // Bersihkan room lama jika duplikat sebelum menambahkan yang baru
                                        for (int i = 0; i < chatRoomList.size(); i++) {
                                            if (chatRoomList.get(i).getChatRoomId().equals(roomId)) {
                                                chatRoomList.remove(i);
                                                break;
                                            }
                                        }

                                        chatRoomList.add(new ChatRoom(roomId, lawanId, namaLawan, lastMsg, finalTimestamp));

                                        processedRooms[0]++;
                                        checkAndUpdateUI(processedRooms[0], totalRooms);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        processedRooms[0]++;
                                        checkAndUpdateUI(processedRooms[0], totalRooms);
                                    }
                                });
                    } else {
                        // Jika room tidak melibatkan currentUid, tetap hitung counter agar tidak gantung
                        processedRooms[0]++;
                        checkAndUpdateUI(processedRooms[0], totalRooms);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        roomsRef.addValueEventListener(chatRoomsListener);
    }

    // Fungsi pembantu untuk mengurutkan data dan menyegarkan UI setelah semua room selesai diproses
    private void checkAndUpdateUI(int processed, long total) {
        if (processed >= total && isAdded() && getActivity() != null) {
            // Urutkan chat room berdasarkan timestamp pesan terbaru secara menurun (descending)
            Collections.sort(chatRoomList, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
            adapter.notifyDataSetChanged();

            if (chatRoomList.isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
            } else {
                tvEmpty.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // OPTIMASI: Putus koneksi listener saat pengguna pindah dari tab chat agar menghemat memori HP
        if (roomsRef != null && chatRoomsListener != null) {
            roomsRef.removeEventListener(chatRoomsListener);
        }
    }
}