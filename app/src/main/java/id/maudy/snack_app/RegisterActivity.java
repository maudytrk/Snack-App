package id.maudy.snack_app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import id.maudy.snack_app.R;

public class RegisterActivity extends AppCompatActivity {

    private EditText etNama, etNim, etEmail, etPassword;
    private Button btnRegister;
    private TextView tvToLogin;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Inisialisasi Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // PENGUNCIAN LINK DATABASE REGIONAL ASIA AGAR TIDAK FREEZE / TIMEOUT
        String URL_DATABASE = "https://snack-app-d6ffc-default-rtdb.asia-southeast1.firebasedatabase.app/";
        mDatabase = FirebaseDatabase.getInstance(URL_DATABASE).getReference();

        // Inisialisasi View
        etNama = findViewById(R.id.etRegisterNama);
        etNim = findViewById(R.id.etRegisterNim);
        etEmail = findViewById(R.id.etRegisterEmail);
        etPassword = findViewById(R.id.etRegisterPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvToLogin = findViewById(R.id.tvToLogin);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        tvToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Kembali ke halaman Login
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            }
        });
    }

    private void registerUser() {
        String nama = etNama.getText().toString().trim();
        String nim = etNim.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validasi form kosong
        if (TextUtils.isEmpty(nama)) { etNama.setError("Nama harus diisi"); return; }
        if (TextUtils.isEmpty(nim)) { etNim.setError("NIM harus diisi"); return; }
        if (TextUtils.isEmpty(email)) { etEmail.setError("Email harus diisi"); return; }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etPassword.setError("Password minimal 6 karakter");
            return;
        }

        // Matikan tombol sementara agar user tidak melakukan klik ganda saat proses loading
        btnRegister.setEnabled(false);
        btnRegister.setText("Mendaftarkan Akun...");

        // Proses pembuatan user di Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String uid = firebaseUser.getUid();

                            // Foto profil diset "" awal-awal (kosong) sesuai format Base64 nanti
                            User user = new User(uid, nama, nim, email, "");

                            // Simpan ke Realtime Database: Users / {uid}
                            mDatabase.child("Users").child(uid).setValue(user)
                                    .addOnCompleteListener(dbTask -> {
                                        // Kembalikan status tombol ke keadaan semula
                                        btnRegister.setEnabled(true);
                                        btnRegister.setText("DAFTAR");

                                        if (dbTask.isSuccessful()) {
                                            Toast.makeText(RegisterActivity.this, "Registrasi Berhasil!", Toast.LENGTH_SHORT).show();
                                            // Langsung lempar ke MainActivity
                                            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            Toast.makeText(RegisterActivity.this, "Gagal simpan data: " + dbTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    });
                        }
                    } else {
                        // Kembalikan status tombol ke keadaan semula jika proses autentikasi gagal
                        btnRegister.setEnabled(true);
                        btnRegister.setText("DAFTAR");
                        Toast.makeText(RegisterActivity.this, "Autentikasi Gagal: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}