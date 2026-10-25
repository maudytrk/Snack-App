package id.maudy.snack_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

import id.maudy.snack_app.R;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProdukAdapter adapter;
    private List<Produk> produkList;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    private DatabaseReference dbRef;
    private ValueEventListener produkListener; // Disimpan ke variabel agar bisa dimatikan saat fragment tidak aktif

    public HomeFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = view.findViewById(R.id.rvHomeProduk);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmpty = view.findViewById(R.id.tvEmpty);

        // Mengatur agar layout RecyclerView berbentuk Grid (2 kolom kesamping agar rapi)
        recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 2));

        produkList = new ArrayList<>();

        // Inisialisasi adapter dengan click listener menuju DetailProdukActivity
        adapter = new ProdukAdapter(getActivity(), produkList, new ProdukAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Produk produk) {
                Intent intent = new Intent(getActivity(), DetailProdukActivity.class);
                // Mengirimkan id produk yang diklik ke halaman detail
                intent.putExtra("ID_PRODUK", produk.getIdProduk());
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(adapter);

        // PENGUNCIAN LINK DATABASE REGIONAL ASIA AGAR TIDAK FREEZE / STUCK TIMEOUT
        String URL_DATABASE = "https://snack-app-d6ffc-default-rtdb.asia-southeast1.firebasedatabase.app/";
        dbRef = FirebaseDatabase.getInstance(URL_DATABASE).getReference("Produk");

        loadSemuaProduk();

        return view;
    }

    private void loadSemuaProduk() {
        progressBar.setVisibility(View.VISIBLE);

        // Membaca data dari Firebase secara Realtime dengan aman
        produkListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Memastikan fragment masih menempel pada activity sebelum memproses UI
                if (!isAdded() || getActivity() == null) return;

                produkList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Produk produk = dataSnapshot.getValue(Produk.class);
                    if (produk != null) {
                        produkList.add(produk);
                    }
                }

                progressBar.setVisibility(View.GONE);

                if (produkList.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                }

                // Memberitahu adapter bahwa data telah berubah agar UI diperbarui
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded() || getActivity() == null) return;

                progressBar.setVisibility(View.GONE);
                Toast.makeText(getActivity(), "Gagal memuat data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        // Pasang listener ke database reference
        dbRef.addValueEventListener(produkListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // OPTIMASI: Lepas pendengar database saat fragment dihancurkan agar menghemat memori HP dan tidak crash di background
        if (dbRef != null && produkListener != null) {
            dbRef.removeEventListener(produkListener);
        }
    }
}