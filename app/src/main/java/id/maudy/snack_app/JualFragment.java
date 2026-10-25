package id.maudy.snack_app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import id.maudy.snack_app.R;

public class JualFragment extends Fragment {

    private ImageView imgPreview;
    private EditText etNama, etHarga, etDeskripsi;
    private Spinner spinnerKategori;
    private Button btnUpload;

    private String encodedImage = ""; // Menampung string representasi Base64 Gambar
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private ActivityResultLauncher<Intent> galleryLauncher;

    public JualFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();

        // PENGUNCIAN LINK DATABASE REGIONAL ASIA AGAR TIDAK FREEZE / TIMEOUT
        String URL_DATABASE = "https://snack-app-d6ffc-default-rtdb.asia-southeast1.firebasedatabase.app/";
        mDatabase = FirebaseDatabase.getInstance(URL_DATABASE).getReference("Produk");

        // Registrasi launcher untuk menangani hasil pengambilan gambar dari galeri HP
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            // Mengubah file URI gambar menjadi Bitmap
                            if (getActivity() != null) {
                                InputStream inputStream = getActivity().getContentResolver().openInputStream(imageUri);
                                Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);

                                // Tampilkan pratinjau ke ImageView
                                imgPreview.setImageBitmap(originalBitmap);

                                // Mengonversi bitmap gambar menjadi string Base64 gratisan
                                encodedImage = encodeImageToBase64(originalBitmap);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "Gagal memuat gambar", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_jual, container, false);

        imgPreview = view.findViewById(R.id.imgJualPreview);
        etNama = view.findViewById(R.id.etJualNama);
        etHarga = view.findViewById(R.id.etJualHarga);
        etDeskripsi = view.findViewById(R.id.etJualDeskripsi);
        spinnerKategori = view.findViewById(R.id.spinnerJualKategori);
        btnUpload = view.findViewById(R.id.btnJualUpload);

        // Mengatur pilihan item pada kombobox Spinner kategori camilan
        if (getActivity() != null) {
            String[] kategoriArray = {"Manis", "Gurih"};
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, kategoriArray);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerKategori.setAdapter(spinnerAdapter);
        }

        // Klik gambar untuk membuka galeri handphone
        imgPreview.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        });

        btnUpload.setOnClickListener(v -> uploadProduk());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Cek sesi login yang aman tanpa mengunci UI thread
        if (mAuth.getCurrentUser() == null && getActivity() != null) {
            Toast.makeText(getActivity(), "Anda harus login terlebih dahulu untuk menjual camilan!", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Bersihkan tumpukan halaman
            startActivity(intent);
        }
    }

    private void uploadProduk() {
        String nama = etNama.getText().toString().trim();
        String harga = etHarga.getText().toString().trim();
        String deskripsi = etDeskripsi.getText().toString().trim();
        String kategori = spinnerKategori.getSelectedItem().toString();

        // Validasi kelengkapan formulir input
        if (TextUtils.isEmpty(nama)) { etNama.setError("Nama produk wajib diisi"); return; }
        if (TextUtils.isEmpty(harga)) { etHarga.setError("Harga produk wajib diisi"); return; }
        if (TextUtils.isEmpty(deskripsi)) { etDeskripsi.setError("Deskripsi produk wajib diisi"); return; }
        if (TextUtils.isEmpty(encodedImage)) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Silakan pilih foto produk camilan terlebih dahulu!", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        if (mAuth.getCurrentUser() == null) return;

        String idPenjual = mAuth.getCurrentUser().getUid();
        // Generate kunci unik ID otomatis untuk node produk baru di Firebase
        String idProduk = mDatabase.push().getKey();

        // Status awal produk yang baru diunggah diatur otomatis menjadi "Tersedia"
        Produk produkBaru = new Produk(idProduk, nama, harga, deskripsi, kategori, encodedImage, idPenjual, "Tersedia");

        if (idProduk != null) {
            btnUpload.setEnabled(false);
            btnUpload.setText("Memproses Upload...");

            mDatabase.child(idProduk).setValue(produkBaru)
                    .addOnCompleteListener(task -> {
                        if (!isAdded() || getActivity() == null) return;

                        btnUpload.setEnabled(true);
                        btnUpload.setText("UPLOAD PRODUK");
                        if (task.isSuccessful()) {
                            Toast.makeText(getActivity(), "Produk camilan berhasil diiklankan!", Toast.LENGTH_SHORT).show();
                            resetForm();
                        } else {
                            Toast.makeText(getActivity(), "Gagal upload: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private String encodeImageToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width > 600 || height > 600) {
            float bitmapRatio = (float) width / (float) height;
            if (bitmapRatio > 1) {
                width = 600;
                height = (int) (width / bitmapRatio);
            } else {
                height = 600;
                width = (int) (height * bitmapRatio);
            }
            bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        }

        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private void resetForm() {
        etNama.setText("");
        etHarga.setText("");
        etDeskripsi.setText("");
        imgPreview.setImageResource(android.R.drawable.ic_menu_camera);
        encodedImage = "";
    }
}