package id.maudy.snack_app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import id.maudy.snack_app.R;

public class DetailProdukActivity extends AppCompatActivity {

    private ImageView imgGambar;
    private TextView tvHarga, tvNama, tvKategori, tvDeskripsi, tvBadgeSoldOut, tvProdukHabisText;
    private LinearLayout llPenjualLayout, llPembeliLayout;
    private Button btnHapus, btnSoldOut, btnChatPenjual;

    private DatabaseReference databaseRoot;
    private DatabaseReference produkRef, userRef;
    private FirebaseAuth mAuth;
    private ValueEventListener detailProdukListener; // Disimpan ke variabel agar bisa dilepas saat activity ditutup

    private String idProduk, idPenjual, namaPenjual, currentUid;
    private Produk produkSakarang;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_produk);

        // Inisialisasi Firebase Komponen
        mAuth = FirebaseAuth.getInstance();
        currentUid = (mAuth.getCurrentUser() != null) ? mAuth.getCurrentUser().getUid() : "";

        // PENGUNCIAN LINK DATABASE REGIONAL ASIA AGAR TIDAK FREEZE / STUCK TIMEOUT
        String URL_DATABASE = "https://snack-app-d6ffc-default-rtdb.asia-southeast1.firebasedatabase.app/";
        databaseRoot = FirebaseDatabase.getInstance(URL_DATABASE).getReference();

        // Tangkap ID Produk dari intent pelempar (Home/Explore Fragment)
        idProduk = getIntent().getStringExtra("ID_PRODUK");

        // Inisialisasi View Komponen UI
        imgGambar = findViewById(R.id.imgDetailGambar);
        tvHarga = findViewById(R.id.tvDetailHarga);
        tvNama = findViewById(R.id.tvDetailNama);
        tvKategori = findViewById(R.id.tvDetailKategori);
        tvDeskripsi = findViewById(R.id.tvDetailDeskripsi);
        tvBadgeSoldOut = findViewById(R.id.tvDetailBadgeSoldOut);
        tvProdukHabisText = findViewById(R.id.tvDetailProdukHabis);

        llPenjualLayout = findViewById(R.id.llAksiPenjual);
        llPembeliLayout = findViewById(R.id.llAksiPembeli);

        btnHapus = findViewById(R.id.btnDetailHapus);
        btnSoldOut = findViewById(R.id.btnDetailSoldOut);
        btnChatPenjual = findViewById(R.id.btnDetailChatPenjual);

        if (idProduk != null) {
            produkRef = databaseRoot.child("Produk").child(idProduk);
            ambilDetailProduk();
        } else {
            Toast.makeText(this, "ID Produk tidak valid", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Aksi Tombol 1: Penjual Klik Set Sold Out
        btnSoldOut.setOnClickListener(v -> setProdukSoldOut());

        // Aksi Tombol 2: Penjual Klik Hapus Produk
        btnHapus.setOnClickListener(v -> konfirmasiHapusProduk());

        // Aksi Tombol 3: Pembeli Klik Chat Penjual
        btnChatPenjual.setOnClickListener(v -> bukaRoomChatWhatsAppStyle());
    }

    private void ambilDetailProduk() {
        detailProdukListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || !snapshot.exists()) return;

                produkSakarang = snapshot.getValue(Produk.class);
                if (produkSakarang != null) {
                    idPenjual = produkSakarang.getIdPenjual();

                    // Isi komponen teks data produk
                    tvNama.setText(produkSakarang.getNama());
                    tvHarga.setText("Rp " + produkSakarang.getHarga());
                    tvKategori.setText("Kategori: " + produkSakarang.getKategori());
                    tvDeskripsi.setText(produkSakarang.getDeskripsi());

                    // Ambil string gambar Base64 gratisan lalu konversi ke Bitmap
                    String base64Image = produkSakarang.getGambarBase64();
                    if (base64Image != null && !base64Image.isEmpty()) {
                        try {
                            byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                            imgGambar.setImageBitmap(bitmap);
                        } catch (Exception e) {
                            imgGambar.setImageResource(android.R.drawable.ic_menu_report_image);
                        }
                    }

                    // Ambil Nama Penjual secara asinkronus dari node Users
                    if (idPenjual != null) {
                        ambilNamaPenjual(idPenjual);
                    }

                    // Penentuan Hak Akses Interface
                    aturTampilanAksiBawah(produkSakarang.getStatus());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DetailProdukActivity.this, "Gagal mengambil detail: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        produkRef.addValueEventListener(detailProdukListener);
    }

    private void ambilNamaPenjual(String uidPenjual) {
        userRef = databaseRoot.child("Users").child(uidPenjual);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing()) return;
                if (snapshot.exists()) {
                    namaPenjual = snapshot.child("nama").getValue(String.class);
                } else {
                    namaPenjual = "Penjual Camilan";
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void aturTampilanAksiBawah(String status) {
        // Cek apakah produk ini berstatus Sold Out secara umum
        if ("Sold Out".equalsIgnoreCase(status)) {
            tvBadgeSoldOut.setVisibility(View.VISIBLE);
        } else {
            tvBadgeSoldOut.setVisibility(View.GONE);
        }

        // Kondisi A: Jika pengakses ADALAH Pemilik Produk (Penjual)
        if (currentUid.equals(idPenjual)) {
            llPenjualLayout.setVisibility(View.VISIBLE);
            llPembeliLayout.setVisibility(View.GONE);

            // Jika sudah terlanjur sold out, matikan tombol set sold out agar tidak mubazir diklik
            if ("Sold Out".equalsIgnoreCase(status)) {
                btnSoldOut.setEnabled(false);
                btnSoldOut.setText("SUDAH SOLD OUT");
                btnSoldOut.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.GRAY));
            } else {
                btnSoldOut.setEnabled(true);
                btnSoldOut.setText("SET SOLD OUT");
                btnSoldOut.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#607D8B")));
            }
        }
        // Kondisi B: Jika pengakses ADALAH Calon Pembeli (Bukan Pemilik) / Tamu
        else {
            llPenjualLayout.setVisibility(View.GONE);
            llPembeliLayout.setVisibility(View.VISIBLE);

            if ("Sold Out".equalsIgnoreCase(status)) {
                // Tampilkan teks info "Produk Habis" jika penjual mengeset sold out
                btnChatPenjual.setVisibility(View.GONE);
                tvProdukHabisText.setVisibility(View.VISIBLE);
            } else {
                // Tampilkan tombol chat penjual jika status "Tersedia"
                btnChatPenjual.setVisibility(View.VISIBLE);
                tvProdukHabisText.setVisibility(View.GONE);
            }
        }
    }

    private void setProdukSoldOut() {
        if (produkRef != null) {
            produkRef.child("status").setValue("Sold Out")
                    .addOnSuccessListener(aVoid -> Toast.makeText(DetailProdukActivity.this, "Produk berhasil diset Sold Out!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(DetailProdukActivity.this, "Gagal memperbarui status: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void konfirmasiHapusProduk() {
        new AlertDialog.Builder(this)
                .setTitle("Hapus Produk")
                .setMessage("Apakah Anda yakin ingin menghapus iklan camilan ini secara permanen?")
                .setPositiveButton("Ya, Hapus", (dialog, which) -> {
                    if (produkRef != null) {
                        produkRef.removeValue().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(DetailProdukActivity.this, "Produk berhasil dihapus!", Toast.LENGTH_SHORT).show();
                                finish(); // Tutup halaman detail dan kembali ke katalog utama
                            } else {
                                Toast.makeText(DetailProdukActivity.this, "Gagal menghapus produk", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void bukaRoomChatWhatsAppStyle() {
        // Jika pembeli belum login, tidak boleh mengirimkan pesan
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Anda wajib login akun terlebih dahulu untuk chat penjual!", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }

        if (idPenjual == null) {
            Toast.makeText(this, "Data penjual belum siap, silakan coba lagi", Toast.LENGTH_SHORT).show();
            return;
        }

        // Rumus Unik ID Kamar Obrolan Terpadu Alfabetis agar halaman chat tidak double (WhatsApp Concept)
        String generatedRoomId;
        if (currentUid.compareTo(idPenjual) < 0) {
            generatedRoomId = currentUid + "_" + idPenjual;
        } else {
            generatedRoomId = idPenjual + "_" + currentUid;
        }

        Intent intent = new Intent(DetailProdukActivity.this, RoomChatActivity.class);
        intent.putExtra("ROOM_ID", generatedRoomId);
        intent.putExtra("LAWAN_ID", idPenjual);
        intent.putExtra("LAWAN_NAMA", namaPenjual != null ? namaPenjual : "Penjual Camilan");
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // OPTIMASI PENYELAMAT MEMORI: Lepaskan listener realtime saat halaman ditutup/di-back oleh user
        if (produkRef != null && detailProdukListener != null) {
            produkRef.removeEventListener(detailProdukListener);
        }
    }
}