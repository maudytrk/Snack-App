package id.maudy.snack_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

import id.maudy.snack_app.R;

public class PengaturanActivity extends AppCompatActivity {

    private LinearLayout btnAbout, btnLogout;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pengaturan);

        mAuth = FirebaseAuth.getInstance();

        btnAbout = findViewById(R.id.btnMenuAbout);
        btnLogout = findViewById(R.id.btnMenuLogout);

        // Berpindah ke Halaman Tentang Aplikasi
        btnAbout.setOnClickListener(v -> {
            Intent intent = new Intent(PengaturanActivity.this, AboutActivity.class);
            startActivity(intent);
        });

        // Memicu Dialog Konfirmasi Sesi Logout Akun
        btnLogout.setOnClickListener(v -> tunjukkanDialogLogout());
    }

    private void tunjukkanDialogLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Keluar Akun")
                .setMessage("Apakah Anda yakin ingin keluar dari aplikasi SnackieSnack?")
                .setPositiveButton("Ya, Keluar", (dialog, which) -> {
                    // Eksekusi fungsi logout bawaan Firebase Auth
                    mAuth.signOut();

                    Toast.makeText(PengaturanActivity.this, "Berhasil keluar akun!", Toast.LENGTH_SHORT).show();

                    // Arahkan kembali ke LoginActivity dan bersihkan tumpukan activity (clear task stack)
                    Intent intent = new Intent(PengaturanActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Batal", null)
                .show();
    }
}