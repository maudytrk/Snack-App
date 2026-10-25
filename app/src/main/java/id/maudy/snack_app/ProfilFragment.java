package id.maudy.snack_app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import id.maudy.snack_app.R;

public class ProfilFragment extends Fragment {

    private ImageView imgFoto;
    private TextView tvNama, tvNim, tvEmail;
    private ImageButton btnPengaturan;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    private ValueEventListener profilListener; // Disimpan agar bisa dilepas dengan aman saat pindah tab
    private String currentUid;
    private ActivityResultLauncher<Intent> profilImageLauncher;

    public ProfilFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();

        // Inisialisasi Launcher Pemilihan Foto dari Galeri Smartphone
        profilImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectImageUri = result.getData().getData();
                        try {
                            if (getActivity() == null) return;
                            InputStream inputStream = getActivity().getContentResolver().openInputStream(selectImageUri);
                            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);

                            if (originalBitmap != null) {
                                // Tampilkan langsung di antarmuka lokal
                                imgFoto.setImageBitmap(originalBitmap);

                                // Mengubah bitmap menjadi teks Base64 murah meriah tanpa Storage berbayar
                                String base64ImageString = encodeImageToBase64(originalBitmap);

                                // Simpan String Gambar baru ke Firebase Realtime Database
                                if (currentUid != null && userRef != null) {
                                    userRef.child("fotoBase64").setValue(base64ImageString)
                                            .addOnSuccessListener(aVoid -> {
                                                if (isAdded() && getContext() != null) {
                                                    Toast.makeText(getContext(), "Foto profil berhasil diperbarui!", Toast.LENGTH_SHORT).show();
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                if (isAdded() && getContext() != null) {
                                                    Toast.makeText(getContext(), "Gagal menyimpan foto: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                }
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                            if (isAdded() && getContext() != null) {
                                Toast.makeText(getContext(), "Gagal memproses gambar", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profil, container, false);

        imgFoto = view.findViewById(R.id.imgProfilFoto);
        tvNama = view.findViewById(R.id.tvProfilNama);
        tvNim = view.findViewById(R.id.tvProfilNim);
        tvEmail = view.findViewById(R.id.tvProfilEmail);
        btnPengaturan = view.findViewById(R.id.btnProfilPengaturan);
        progressBar = view.findViewById(R.id.progressProfil);

        // PENGUNCIAN LINK DATABASE REGIONAL ASIA AGAR TIDAK FREEZE / STUCK TIMEOUT
        String URL_DATABASE = "https://snack-app-d6ffc-default-rtdb.asia-southeast1.firebasedatabase.app/";

        if (mAuth.getCurrentUser() != null) {
            currentUid = mAuth.getCurrentUser().getUid();
            userRef = FirebaseDatabase.getInstance(URL_DATABASE).getReference("Users").child(currentUid);
            loadDataProfilMahasiswa();
        } else {
            tvNama.setText("Tamu (Belum Login)");
            tvNim.setText("NIM: -");
            tvEmail.setText("Email: -");
        }

        // Event Klik Gambar untuk Mengubah Foto Profil
        imgFoto.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() != null) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                profilImageLauncher.launch(intent);
            } else {
                Toast.makeText(getActivity(), "Silakan login untuk memodifikasi profil", Toast.LENGTH_SHORT).show();
            }
        });

        // Event Klik Tombol Pengaturan menuju PengaturanActivity
        btnPengaturan.setOnClickListener(v -> {
            if (getActivity() != null) {
                Intent intent = new Intent(getActivity(), PengaturanActivity.class);
                startActivity(intent);
            }
        });

        return view;
    }

    private void loadDataProfilMahasiswa() {
        progressBar.setVisibility(View.VISIBLE);
        profilListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null) return; // Mencegah crash jika fragment dihancurkan di tengah jalan

                progressBar.setVisibility(View.GONE);
                if (snapshot.exists()) {
                    String nama = snapshot.child("nama").getValue(String.class);
                    String nim = snapshot.child("nim").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String base64Photo = snapshot.child("fotoBase64").getValue(String.class);

                    tvNama.setText(nama != null ? nama : "User Camilan");
                    tvNim.setText("NIM: " + (nim != null ? nim : "-"));
                    tvEmail.setText("Email: " + (email != null ? email : "-"));

                    // Dekode Teks Base64 kembali menjadi Objek Bitmap jika ada datanya
                    if (base64Photo != null && !base64Photo.isEmpty()) {
                        try {
                            byte[] decodedBytes = Base64.decode(base64Photo, Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                            imgFoto.setImageBitmap(bitmap);
                        } catch (Exception e) {
                            imgFoto.setImageResource(android.R.drawable.ic_menu_myplaces);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded() && getContext() != null) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Gagal mengambil data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        };

        userRef.addValueEventListener(profilListener);
    }

    /**
     * Memproses gambar menjadi Base64 berukuran kecil agar database tetap responsif dan lancar
     */
    private String encodeImageToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        // Downscale dimensi gambar jika terlalu besar untuk batasan string database
        int maxDimension = 400; // Cukup 400px untuk resolusi foto profil bulat kecil
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width > maxDimension || height > maxDimension) {
            float ratio = (float) width / (float) height;
            if (ratio > 1) {
                width = maxDimension;
                height = (int) (width / ratio);
            } else {
                height = maxDimension;
                width = (int) (height * ratio);
            }
            bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        }

        // Kompresi kualitas gambar JPEG ke angka 50%
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // OPTIMASI PENYELAMAT MEMORI: Putus hubungan listener agar data tidak bocor saat pindah menu
        if (userRef != null && profilListener != null) {
            userRef.removeEventListener(profilListener);
        }
    }
}