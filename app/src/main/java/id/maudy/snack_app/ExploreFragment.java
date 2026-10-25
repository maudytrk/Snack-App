package id.maudy.snack_app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import id.maudy.snack_app.R;

public class ExploreFragment extends Fragment {

    private SearchView searchView;
    private TabLayout tabLayout;
    private EditText etMinHarga, etMaxHarga;
    private Button btnApplyFilter;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    private ProdukAdapter adapter;
    private List<Produk> masterProdukList; // Menampung semua data asli dari Firebase
    private List<Produk> filteredList;     // Menampung data hasil filter dinamis

    private DatabaseReference dbRef;
    private ValueEventListener exploreListener; // Disimpan agar bisa dilepas dengan aman

    // Status Filter Global
    private String currentQuery = "";
    private String currentCategory = "Semua";
    private int minPrice = 0;
    private int maxPrice = Integer.MAX_VALUE;

    public ExploreFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_explore, container, false);

        // Inisialisasi View komponen UI
        searchView = view.findViewById(R.id.searchViewExplore);
        tabLayout = view.findViewById(R.id.tabLayoutKategori);
        etMinHarga = view.findViewById(R.id.etMinHarga);
        etMaxHarga = view.findViewById(R.id.etMaxHarga);
        btnApplyFilter = view.findViewById(R.id.btnApplyFilter);
        recyclerView = view.findViewById(R.id.rvExploreProduk);
        progressBar = view.findViewById(R.id.progressBarExplore);
        tvEmpty = view.findViewById(R.id.tvEmptyExplore);

        // Konfigurasi RecyclerView dengan Grid 2 kolom (sama seperti Home)
        recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 2));

        masterProdukList = new ArrayList<>();
        filteredList = new ArrayList<>();

        adapter = new ProdukAdapter(getActivity(), filteredList, new ProdukAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Produk produk) {
                // Arahkan ke DetailProdukActivity ketika salah satu item diklik
                Intent intent = new Intent(getActivity(), DetailProdukActivity.class);
                intent.putExtra("ID_PRODUK", produk.getIdProduk());
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(adapter);

        // PENGUNCIAN LINK DATABASE REGIONAL ASIA AGAR TIDAK FREEZE / STUCK TIMEOUT
        String URL_DATABASE = "https://snack-app-d6ffc-default-rtdb.asia-southeast1.firebasedatabase.app/";
        dbRef = FirebaseDatabase.getInstance(URL_DATABASE).getReference("Produk");

        // Ambil data utama dari Firebase
        fetchMasterData();

        // 1. Event Listener untuk Pencarian (SearchView)
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentQuery = query;
                applyFilterRules();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentQuery = newText;
                applyFilterRules();
                return true;
            }
        });

        // 2. Event Listener untuk Tab Layout Kategori
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getText() != null) {
                    currentCategory = tab.getText().toString();
                    applyFilterRules();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // 3. Event Listener untuk Tombol Filter Harga Min-Max
        btnApplyFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String minStr = etMinHarga.getText().toString().trim();
                String maxStr = etMaxHarga.getText().toString().trim();

                minPrice = TextUtils.isEmpty(minStr) ? 0 : Integer.parseInt(minStr);
                maxPrice = TextUtils.isEmpty(maxStr) ? Integer.MAX_VALUE : Integer.parseInt(maxStr);

                if (minPrice > maxPrice) {
                    etMinHarga.setError("Harga minimal tidak boleh lebih besar");
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Rentang harga salah!", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }

                applyFilterRules();
            }
        });

        return view;
    }

    private void fetchMasterData() {
        progressBar.setVisibility(View.VISIBLE);

        exploreListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getActivity() == null) return;

                masterProdukList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Produk produk = ds.getValue(Produk.class);
                    if (produk != null) {
                        masterProdukList.add(produk);
                    }
                }
                applyFilterRules(); // Jalankan aturan filter saat data pertama kali dimuat atau diperbarui
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded() || getActivity() == null) return;
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getActivity(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        dbRef.addValueEventListener(exploreListener);
    }

    /**
     * Menggabungkan semua parameter filter (Pencarian, Kategori Tab, Rentang Harga)
     */
    private void applyFilterRules() {
        // Memastikan fragment masih aktif menempel pada activity saat modifikasi UI dijalankan
        if (!isAdded() || getActivity() == null) return;

        filteredList.clear();

        for (Produk produk : masterProdukList) {

            // Check Parameter 1: Pencarian Nama
            boolean matchesSearch = true;
            if (!currentQuery.isEmpty() && produk.getNama() != null) {
                matchesSearch = produk.getNama().toLowerCase().contains(currentQuery.toLowerCase());
            }

            // Check Parameter 2: Kategori dari TabLayout
            boolean matchesCategory = true;
            if (!currentCategory.equalsIgnoreCase("Semua") && produk.getKategori() != null) {
                matchesCategory = produk.getKategori().equalsIgnoreCase(currentCategory);
            }

            // Check Parameter 3: Rentang Harga
            boolean matchesPrice = true;
            if (produk.getHarga() != null) {
                try {
                    int hargaProduk = Integer.parseInt(produk.getHarga().replace(".", "")); // Menghapus titik pemisah nominal jika ada
                    matchesPrice = (hargaProduk >= minPrice && hargaProduk <= maxPrice);
                } catch (NumberFormatException e) {
                    matchesPrice = true; // Jika format harga gagal parsing, abaikan filter harga untuk item ini
                }
            }

            // Jika item produk lolos semua validasi penyaringan, masukkan ke list filtered
            if (matchesSearch && matchesCategory && matchesPrice) {
                filteredList.add(produk);
            }
        }

        progressBar.setVisibility(View.GONE);

        // Atur visibilitas teks peringatan kosong
        if (filteredList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
        }

        // Segarkan RecyclerView
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // OPTIMASI PENYELAMAT MEMORI: Lepaskan listener saat fragmen ditutup/pindah tab
        if (dbRef != null && exploreListener != null) {
            dbRef.removeEventListener(exploreListener);
        }
    }
}
