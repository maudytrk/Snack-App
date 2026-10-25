package id.maudy.snack_app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import id.maudy.snack_app.R;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvToRegister;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        // LOGIKA PENYELAMAT ANR: Cek apakah user sudah login sebelumnya dengan aman (Auto-Login)
        if (mAuth.getCurrentUser() != null) {
            // Berikan jeda 500ms menggunakan Handler agar Main Thread Android tidak freeze/stuck
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }, 500);
            return; // Hentikan baris kode di bawahnya agar tidak memuat ulang UI secara mubazir
        }

        etEmail = findViewById(R.id.etLoginEmail);
        etPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvToRegister = findViewById(R.id.tvToRegister);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        tvToRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
                finish();
            }
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) { etEmail.setError("Email harus diisi"); return; }
        if (TextUtils.isEmpty(password)) { etPassword.setError("Password harus diisi"); return; }

        // Ubah teks tombol untuk memberi indikasi visual ke user agar tidak klik berkali-kali
        btnLogin.setEnabled(false);
        btnLogin.setText("Memproses Masuk...");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    // Kembalikan status tombol ke semula
                    btnLogin.setEnabled(true);
                    btnLogin.setText("MASUK");

                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Login Berhasil!", Toast.LENGTH_SHORT).show();
                        // Masuk ke Halaman Utama setelah Login
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Login Gagal: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}